package com.kdmm.secretsanta.service;

import jakarta.annotation.PostConstruct;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.kdmm.secretsanta.model.Game;
import com.kdmm.secretsanta.model.Participant;
import com.kdmm.secretsanta.util.EmailMasker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";
    private static final DateTimeFormatter SPANISH_DATE =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-ES"));

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String brevoApiKey;
    private final String fromAddress;
    private final String senderName;
    /** Human-readable reason why emails cannot be sent, or {@code null} when configured. */
    private final String unavailableReason;

    public EmailService(ObjectMapper objectMapper,
                        @Value("${brevo.api.key:}") String brevoApiKey,
                        @Value("${app.email.sender:}") String fromAddress,
                        @Value("${app.email.sender-name:Secret Santa Organizer}") String senderName) {
        this.objectMapper = objectMapper;
        this.brevoApiKey = trimToNull(brevoApiKey);
        this.fromAddress = trimToNull(fromAddress);
        this.senderName = trimToNull(senderName) == null ? "Secret Santa Organizer" : senderName.trim();
        this.unavailableReason = resolveUnavailableReason(this.brevoApiKey, this.fromAddress);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    /**
     * Logs the email configuration once at startup. A missing MAIL_FROM with a
     * working API key is a deployment error and must be visible in the logs
     * before a draw is attempted.
     */
    @PostConstruct
    void logConfiguration() {
        if (unavailableReason == null) {
            log.info("Email delivery enabled with sender: {}", EmailMasker.mask(fromAddress));
        } else if (brevoApiKey == null) {
            log.info("Email delivery disabled: {}. Emails are simulated.", unavailableReason);
        } else {
            log.error("Email delivery misconfigured: {}. Assignment emails will not be sent.",
                    unavailableReason);
        }
    }

    private static String resolveUnavailableReason(String apiKey, String sender) {
        if (apiKey == null) {
            return "BREVO_API_KEY is not set";
        }
        if (sender == null) {
            return "MAIL_FROM is not set";
        }
        if (!isValidEmail(sender)) {
            return "MAIL_FROM is not a valid email address";
        }
        return null;
    }

    /**
     * Sends the assignment to the giver.
     *
     * @return a future that completes with {@code true} when Brevo accepted the
     *         message and {@code false} when it was skipped or rejected. Skipped
     *         cases complete immediately, so callers can collect every request
     *         and report the real outcome instead of assuming success.
     */
    public CompletableFuture<Boolean> sendAssignment(Participant giver, Participant receiver, Game game) {
        String maskedGiver = EmailMasker.mask(giver.getEmail());

        if (unavailableReason != null) {
            if (brevoApiKey == null) {
                log.info("SIMULATED EMAIL To: {}, Giver: {}, Receiver: {}",
                        maskedGiver, giver.getName(), receiver.getName());
            } else {
                log.warn("Assignment email not sent to {}: {}", maskedGiver, unavailableReason);
            }
            return CompletableFuture.completedFuture(false);
        }

        if (!isValidEmail(giver.getEmail())) {
            log.error("Skipping assignment email: invalid recipient address for {}.", maskedGiver);
            return CompletableFuture.completedFuture(false);
        }

        log.info("Sending assignment email to: {}", maskedGiver);

        try {
            String jsonBody = buildEmailJson(giver, receiver, game);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BREVO_API_URL))
                    .header("accept", "application/json")
                    .header("api-key", brevoApiKey)
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            log.info("Email sent successfully to: {}", maskedGiver);
                            return true;
                        }
                        // Never log the response body: it may contain recipient data.
                        log.error("Failed to send email to {}: HTTP {}. {}",
                                maskedGiver, response.statusCode(),
                                brevoHint(response.statusCode()));
                        return false;
                    })
                    .exceptionally(e -> {
                        log.error("Error sending email to {}: {}", maskedGiver, e.getMessage());
                        return false;
                    });

        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", maskedGiver, e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    private static String brevoHint(int statusCode) {
        return switch (statusCode) {
            case 400 -> "Check that MAIL_FROM is a verified sender in Brevo.";
            case 401, 403 -> "Check that BREVO_API_KEY is a valid v3 API key.";
            case 429 -> "Brevo rate limit reached.";
            default -> "Unexpected response from Brevo.";
        };
    }

    // Package-private so the payload (escaping) can be unit tested.
    String buildEmailJson(Participant giver, Participant receiver, Game game)
            throws JacksonException {
        String wishList = receiver.getWishList() != null && !receiver.getWishList().isBlank()
                ? truncate(receiver.getWishList(), 800) : "No ha compartido lista de deseos";

        // User-provided values are HTML-escaped before being embedded in the email.
        String plainGiverName = singleLine(giver.getName());
        String plainReceiverName = singleLine(receiver.getName());
        String plainGameName = singleLine(game.getName());

        String safeGiverName = HtmlUtils.htmlEscape(plainGiverName);
        String safeReceiverName = HtmlUtils.htmlEscape(plainReceiverName);
        String safeGameName = HtmlUtils.htmlEscape(plainGameName);
        String safeWishList = escapeMultiline(wishList);

        String price = game.getMaxPrice() == null
                ? null
                : game.getMaxPrice().stripTrailingZeros().toPlainString() + " \u20AC";
        String exchangeDate = game.getExchangeDate() == null
                ? null
                : game.getExchangeDate().format(SPANISH_DATE);
        String seasonYear = game.getExchangeDate() == null
                ? "" : " " + game.getExchangeDate().getYear();

        String greeting = safeGiverName.isEmpty()
                ? "\u00A1Hola!" : "\u00A1Hola, " + safeGiverName + "!";
        String plainGreeting = plainGiverName.isEmpty()
                ? "\u00A1Hola!" : "\u00A1Hola, " + plainGiverName + "!";

        StringBuilder details = new StringBuilder();
        if (!safeGameName.isEmpty()) {
            details.append(detailRow("Partida", safeGameName));
        }
        details.append(detailRow("Lista de deseos", safeWishList));
        if (price != null) {
            details.append(detailRow("Presupuesto m\u00E1ximo", price));
        }
        if (exchangeDate != null) {
            details.append(detailRow("Fecha del intercambio", exchangeDate));
        }

        String htmlContent = """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <meta name="color-scheme" content="light">
                    <meta name="supported-color-schemes" content="light">
                    <title>Tu amigo invisible</title>
                </head>
                <body style="margin:0;padding:0;background-color:#f4f1ec;">
                    <div style="display:none;max-height:0;overflow:hidden;mso-hide:all;font-size:1px;line-height:1px;color:#f4f1ec;">
                        Ya tienes tu amigo invisible. Abre este correo para ver a qui\u00E9n regalas.
                    </div>
                    <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:#f4f1ec;">
                        <tr>
                            <td align="center" style="padding:28px 12px;">
                                <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" style="width:100%;max-width:600px;background-color:#ffffff;border:1px solid #e4ded4;border-radius:14px;">
                                    <tr>
                                        <td bgcolor="#14492f" style="background-color:#14492f;border-radius:14px 14px 0 0;padding:30px 28px;">
                                            <p style="margin:0 0 8px 0;font-family:Arial,Helvetica,sans-serif;font-size:11px;font-weight:bold;letter-spacing:2px;text-transform:uppercase;color:#e9cb7b;">Secret Santa Organizer</p>
                                            <h1 style="margin:0;font-family:Georgia,'Times New Roman',serif;font-size:26px;line-height:1.3;font-weight:normal;color:#ffffff;">Tu amigo invisible{{YEAR}}</h1>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding:30px 28px 22px 28px;font-family:Arial,Helvetica,sans-serif;">
                                            <p style="margin:0 0 16px 0;font-size:17px;line-height:1.6;color:#1b2a22;">{{GREETING}}</p>
                                            <p style="margin:0 0 20px 0;font-size:16px;line-height:1.6;color:#4a5a51;">El sorteo ya est\u00E1 hecho. Esta es tu asignaci\u00F3n:</p>
                                            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0">
                                                <tr>
                                                    <td style="background-color:#fdf4f2;border-left:4px solid #b83c38;border-radius:0 10px 10px 0;padding:18px 20px;">
                                                        <p style="margin:0 0 6px 0;font-size:11px;font-weight:bold;letter-spacing:2px;text-transform:uppercase;color:#8a4a45;font-family:Arial,Helvetica,sans-serif;">Tu persona asignada</p>
                                                        <p style="margin:0;font-family:Georgia,'Times New Roman',serif;font-size:24px;line-height:1.35;font-weight:bold;color:#b83c38;">{{RECEIVER}}</p>
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>
                                    {{DETAILS}}
                                    <tr>
                                        <td style="padding:8px 28px 0 28px;font-family:Arial,Helvetica,sans-serif;">
                                            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0">
                                                <tr>
                                                    <td style="background-color:#fbf6e9;border-left:4px solid #e9cb7b;border-radius:0 10px 10px 0;padding:16px 20px;">
                                                        <p style="margin:0 0 8px 0;font-size:11px;font-weight:bold;letter-spacing:2px;text-transform:uppercase;color:#8a7333;font-family:Arial,Helvetica,sans-serif;">Tres consejos</p>
                                                        <p style="margin:0;font-size:15px;line-height:1.7;color:#4a4a3a;font-family:Arial,Helvetica,sans-serif;">
                                                            S\u00E9 creativo con el regalo.<br>
                                                            Lo importante es el detalle.<br>
                                                            Mant\u00E9n el secreto: es parte de la gracia.
                                                        </p>
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding:28px 28px 30px 28px;font-family:Arial,Helvetica,sans-serif;">
                                            <p style="margin:0 0 10px 0;font-size:13px;line-height:1.6;color:#5b6b62;">Guarda este correo: es tu \u00FAnica copia de la asignaci\u00F3n. Si algo no cuadra, contacta con quien organiza el juego.</p>
                                            <p style="margin:0;font-size:12px;color:#8b9a92;">Secret Santa Organizer</p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.replace("{{GREETING}}", greeting)
                .replace("{{RECEIVER}}", safeReceiverName)
                .replace("{{DETAILS}}", details)
                .replace("{{YEAR}}", seasonYear);

        StringBuilder text = new StringBuilder();
        text.append(plainGreeting).append("\n\n");
        if (plainGameName.isEmpty()) {
            text.append("El sorteo ya est\u00E1 hecho.\n\n");
        } else {
            text.append("El sorteo de \"").append(plainGameName).append("\" ya est\u00E1 hecho.\n\n");
        }
        text.append("TU PERSONA ASIGNADA\n").append(plainReceiverName).append("\n\n");
        text.append("LISTA DE DESEOS\n").append(wishList).append("\n\n");
        if (price != null) {
            text.append("PRESUPUESTO M\u00C1XIMO\n").append(price).append("\n\n");
        }
        if (exchangeDate != null) {
            text.append("FECHA DEL INTERCAMBIO\n").append(exchangeDate).append("\n\n");
        }
        text.append("""
                TRES CONSEJOS
                - S\u00E9 creativo con el regalo.
                - Lo importante es el detalle.
                - Mant\u00E9n el secreto: es parte de la gracia.

                Guarda este correo: es tu \u00FAnica copia de la asignaci\u00F3n.
                Secret Santa Organizer
                """);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sender", Map.of(
                "email", fromAddress == null ? "" : fromAddress,
                "name", singleLine(senderName).isEmpty()
                        ? "Secret Santa Organizer" : singleLine(senderName)));
        payload.put("to", List.of(Map.of(
                "email", giver.getEmail() == null ? "" : giver.getEmail(),
                "name", singleLine(giver.getName()))));
        payload.put("subject", seasonYear.isEmpty()
                ? "\uD83C\uDF81 \u00A1Tu amigo invisible ha sido asignado!"
                : "\uD83C\uDF81 Tu amigo invisible" + seasonYear + " ya est\u00E1 listo");
        payload.put("htmlContent", htmlContent);
        payload.put("textContent", text.toString());

        // Jackson handles JSON escaping, preventing broken payloads or injection.
        return objectMapper.writeValueAsString(payload);
    }

    private static String detailRow(String label, String valueHtml) {
        return """
                <tr>
                    <td style="padding:0 28px 12px 28px;font-family:Arial,Helvetica,sans-serif;">
                        <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0">
                            <tr>
                                <td style="background-color:#f6f7f5;border-radius:10px;padding:14px 16px;">
                                    <p style="margin:0 0 4px 0;font-size:11px;font-weight:bold;letter-spacing:2px;text-transform:uppercase;color:#5b6b62;">{{LABEL}}</p>
                                    <p style="margin:0;font-size:16px;line-height:1.6;color:#1b2a22;">{{VALUE}}</p>
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
                """.replace("{{LABEL}}", label).replace("{{VALUE}}", valueHtml);
    }

    private static String escapeMultiline(String value) {
        String normalized = value.replace("\r\n", "\n").replace('\r', '\n');
        return HtmlUtils.htmlEscape(normalized).replace("\n", "<br>");
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength) + "...";
    }

    private static String singleLine(String value) {
        return value == null ? "" : value.replaceAll("[\\r\\n]+", " ").trim();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean isValidEmail(String email) {
        return email != null
                && !email.isBlank()
                && email.indexOf('@') > 0
                && !email.contains("\r")
                && !email.contains("\n");
    }
}
