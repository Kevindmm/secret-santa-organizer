package com.kdmm.secretsanta.service;

import jakarta.annotation.PostConstruct;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
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
        log.info("Email provider: Brevo HTTP API ({})", BREVO_API_URL);
        if (unavailableReason == null) {
            log.info("Email delivery enabled. sender={}, senderName='{}'",
                    EmailMasker.mask(fromAddress), senderName);
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
        // SMTP keys are a different credential and are rejected by the HTTP API.
        // Only the prefix is inspected; the key itself is never logged.
        if (apiKey.startsWith("xsmtpsib-")) {
            return "BREVO_API_KEY is a Brevo SMTP key; the HTTP API needs an API v3 key (xkeysib-)";
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

        String gameCode = game.getId() == null ? "unknown" : game.getId();
        log.info("Sending assignment email: game={}, recipient={}, sender={}",
                gameCode, maskedGiver, EmailMasker.mask(fromAddress));

        long startedAt = System.nanoTime();

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
                        long durationMs = elapsedMillis(startedAt);
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            // Brevo answers with the id of the queued message, which
                            // can be searched in the Brevo dashboard.
                            log.info("Brevo accepted assignment email: game={}, recipient={}, "
                                            + "httpStatus={}, messageId={}, durationMs={}",
                                    gameCode, maskedGiver, response.statusCode(),
                                    messageId(response.body()), durationMs);
                            return true;
                        }
                        // Never log the response body: it may contain recipient data.
                        log.error("Brevo rejected assignment email: game={}, recipient={}, "
                                        + "httpStatus={}, durationMs={}. {}",
                                gameCode, maskedGiver, response.statusCode(), durationMs,
                                brevoHint(response.statusCode()));
                        return false;
                    })
                    .exceptionally(e -> {
                        log.error("Assignment email failed: game={}, recipient={}, durationMs={}, error={}",
                                gameCode, maskedGiver, elapsedMillis(startedAt), e.getMessage());
                        return false;
                    });

        } catch (Exception e) {
            log.error("Assignment email could not be built or queued: game={}, recipient={}, "
                            + "durationMs={}, error={}",
                    gameCode, maskedGiver, elapsedMillis(startedAt), e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Reads the id of the queued message. Brevo returns {@code {"messageId":...}}.
     * Only the id is used; the response body is never logged as a whole.
     */
    private String messageId(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "unknown";
        }
        try {
            JsonNode payload = objectMapper.readTree(responseBody);
            JsonNode id = payload.get("messageId");
            return id == null || id.isNull() ? "unknown" : id.asText();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static long elapsedMillis(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000;
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
                    <meta name="x-apple-disable-message-reformatting">
                    <meta name="color-scheme" content="light">
                    <meta name="supported-color-schemes" content="light">
                    <title>Tu amigo invisible</title>
                    <style>
                        @media only screen and (max-width: 620px) {
                            .wrap { padding: 16px 10px 26px 10px !important; }
                            .px { padding-left: 20px !important; padding-right: 20px !important; }
                            .tag { padding: 20px 18px !important; }
                            .title { font-size: 26px !important; }
                            .name { font-size: 27px !important; }
                            .header { padding: 30px 20px 28px 20px !important; }
                        }
                    </style>
                </head>
                <body style="margin:0;padding:0;background-color:#f3eee4;">
                    <div style="display:none;max-height:0;overflow:hidden;mso-hide:all;font-size:1px;line-height:1px;color:#f3eee4;">
                        Ya tienes tu amigo invisible. Abre este correo para ver a qui\u00E9n regalas.
                    </div>
                    <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:#f3eee4;">
                        <tr>
                            <td align="center" class="wrap" style="padding:32px 12px 38px 12px;">
                                <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" style="width:100%;max-width:600px;background-color:#fffdf8;border:1px solid #e5dccb;border-radius:16px;overflow:hidden;">
                                    <tr>
                                        <td style="height:6px;background-color:#b83c38;background-image:linear-gradient(90deg,#b83c38 0%,#d9b96a 50%,#14492f 100%);font-size:0;line-height:0;">&nbsp;</td>
                                    </tr>
                                    <tr>
                                        <td class="header" bgcolor="#123f2a" style="background-color:#123f2a;background-image:linear-gradient(135deg,#123f2a 0%,#1c5c3d 100%);padding:36px 32px 34px 32px;">
                                            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0">
                                                <tr>
                                                    <td style="font-family:Arial,Helvetica,sans-serif;font-size:10px;font-weight:bold;letter-spacing:3px;text-transform:uppercase;color:#e9cb7b;">Secret Santa Organizer</td>
                                                    <td align="right" style="font-size:15px;letter-spacing:5px;color:#5c9077;">\u2744 \u2744 \u2744</td>
                                                </tr>
                                                <tr>
                                                    <td colspan="2" class="title" style="padding-top:12px;font-family:Georgia,'Times New Roman',serif;font-size:32px;line-height:1.22;color:#ffffff;">Tu amigo invisible<span style="color:#e9cb7b;">{{YEAR}}</span></td>
                                                </tr>
                                                <tr>
                                                    <td colspan="2" style="padding-top:20px;">
                                                        <table role="presentation" cellpadding="0" cellspacing="0" border="0">
                                                            <tr>
                                                                <td width="38" style="font-size:0;line-height:0;">
                                                                    <div style="width:34px;height:1px;background-color:#4f8266;font-size:0;line-height:0;">&#8203;</div>
                                                                </td>
                                                                <td style="padding-left:10px;font-family:Arial,Helvetica,sans-serif;font-size:12px;letter-spacing:1px;color:#a9c9b6;">El sorteo ya est\u00E1 hecho</td>
                                                            </tr>
                                                        </table>
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="px" style="padding:32px 32px 0 32px;font-family:Arial,Helvetica,sans-serif;">
                                            <p style="margin:0 0 10px 0;font-size:18px;line-height:1.5;font-weight:bold;color:#1b2a22;">{{GREETING}}</p>
                                            <p style="margin:0 0 26px 0;font-size:15px;line-height:1.65;color:#5b6b62;">Ya tienes tu asignaci\u00F3n. Marca este correo como favorito: es tu \u00FAnica copia y lo necesitar\u00E1s el d\u00EDa del intercambio.</p>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="px" style="padding:0 32px;">
                                            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:#fff9ee;border:2px dashed #d9bd7c;border-radius:14px;">
                                                <tr>
                                                    <td class="tag" style="padding:24px 26px;">
                                                        <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0">
                                                            <tr>
                                                                <td colspan="2" style="font-family:Arial,Helvetica,sans-serif;font-size:10px;font-weight:bold;letter-spacing:2.5px;text-transform:uppercase;color:#a4584f;padding-bottom:10px;">Tu persona asignada</td>
                                                            </tr>
                                                            <tr>
                                                                <td class="name" valign="middle" style="font-family:Georgia,'Times New Roman',serif;font-size:31px;line-height:1.25;font-weight:bold;color:#b83c38;">{{RECEIVER}}</td>
                                                                <td valign="middle" align="right" width="52" style="font-size:28px;line-height:1;">\uD83C\uDF81</td>
                                                            </tr>
                                                            <tr>
                                                                <td colspan="2" style="padding-top:12px;font-family:Arial,Helvetica,sans-serif;font-size:12px;line-height:1.5;color:#8a6d3f;">Solo para tus ojos: guarda el secreto hasta el d\u00EDa del intercambio.</td>
                                                            </tr>
                                                        </table>
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="height:26px;font-size:0;line-height:0;">&nbsp;</td>
                                    </tr>
                                    {{DETAILS}}
                                    <tr>
                                        <td class="px" style="padding:26px 32px 0 32px;font-family:Arial,Helvetica,sans-serif;">
                                            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:#fbf5e4;border-left:4px solid #e0c179;border-radius:4px 12px 12px 4px;">
                                                <tr>
                                                    <td style="padding:20px 22px;">
                                                        <p style="margin:0 0 10px 0;font-size:10px;font-weight:bold;letter-spacing:2.5px;text-transform:uppercase;color:#93722f;">Tres consejos</p>
                                                        <p style="margin:0;font-size:15px;line-height:1.9;color:#4a4a3a;">
                                                            <span style="color:#c9a44f;">\u2726</span> S\u00E9 creativo con el regalo.<br>
                                                            <span style="color:#c9a44f;">\u2726</span> Lo importante es el detalle.<br>
                                                            <span style="color:#c9a44f;">\u2726</span> Mant\u00E9n el secreto: es parte de la gracia.
                                                        </p>
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="px" style="padding:30px 32px 34px 32px;font-family:Arial,Helvetica,sans-serif;">
                                            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0">
                                                <tr>
                                                    <td style="border-top:1px solid #e8e0d2;padding-top:20px;">
                                                        <p style="margin:0 0 12px 0;font-size:13px;line-height:1.65;color:#6b7f74;">Guarda este correo: es tu \u00FAnica copia de la asignaci\u00F3n. Si algo no cuadra, contacta con quien organiza el juego.</p>
                                                        <p style="margin:0;font-size:10px;letter-spacing:1.6px;text-transform:uppercase;color:#9aa89f;">Secret Santa Organizer</p>
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>
                                </table>
                                <p style="margin:18px 0 0 0;font-family:Arial,Helvetica,sans-serif;font-size:11px;line-height:1.6;color:#7f9187;text-align:center;">Hecho con cari\u00F1o para que nadie se quede sin regalo.</p>
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
            text.append("El sorteo ya est\u00E1 hecho. Esta es tu asignaci\u00F3n:\n\n");
        } else {
            text.append("\u00A1El sorteo de \"").append(plainGameName)
                    .append("\" ya est\u00E1 hecho! Esta es tu asignaci\u00F3n:\n\n");
        }
        text.append("\u2726 TU PERSONA ASIGNADA\n").append(plainReceiverName).append("\n\n");
        text.append("LISTA DE DESEOS\n").append(wishList).append("\n\n");
        if (price != null) {
            text.append("PRESUPUESTO M\u00C1XIMO\n").append(price).append("\n\n");
        }
        if (exchangeDate != null) {
            text.append("FECHA DEL INTERCAMBIO\n").append(exchangeDate).append("\n\n");
        }
        text.append("""
                TRES CONSEJOS
                \u2726 S\u00E9 creativo con el regalo.
                \u2726 Lo importante es el detalle.
                \u2726 Mant\u00E9n el secreto: es parte de la gracia.

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
                    <td class="px" style="padding:0 32px;">
                        <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0">
                            <tr>
                                <td style="padding:15px 0;border-bottom:1px solid #ece4d5;">
                                    <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0">
                                        <tr>
                                            <td width="18" valign="top" style="padding-top:7px;">
                                                <div style="width:7px;height:7px;background-color:#d5b26a;border-radius:50%;font-size:0;line-height:0;">&nbsp;</div>
                                            </td>
                                            <td style="padding-left:6px;font-family:Arial,Helvetica,sans-serif;">
                                                <p style="margin:0 0 4px 0;font-size:10px;font-weight:bold;letter-spacing:2px;text-transform:uppercase;color:#8b9b91;">{{LABEL}}</p>
                                                <p style="margin:0;font-size:16px;line-height:1.6;color:#1b2a22;">{{VALUE}}</p>
                                            </td>
                                        </tr>
                                    </table>
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
