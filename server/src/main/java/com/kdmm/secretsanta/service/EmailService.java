package com.kdmm.secretsanta.service;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String brevoApiKey;
    private final String fromAddress;
    private final String senderName;

    public EmailService(ObjectMapper objectMapper,
                        @Value("${brevo.api.key:}") String brevoApiKey,
                        @Value("${app.email.sender:}") String fromAddress,
                        @Value("${app.email.sender-name:Secret Santa Organizer}") String senderName) {
        this.objectMapper = objectMapper;
        this.brevoApiKey = brevoApiKey;
        this.fromAddress = fromAddress;
        this.senderName = senderName;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public void sendAssignment(Participant giver, Participant receiver, Game game) {
        String maskedGiver = EmailMasker.mask(giver.getEmail());

        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            log.info("Email service disabled. No API key configured.");
            log.info("SIMULATED EMAIL To: {}, Giver: {}, Receiver: {}",
                    maskedGiver, giver.getName(), receiver.getName());
            return;
        }

        if (!isValidEmail(giver.getEmail()) || fromAddress == null || fromAddress.isBlank()) {
            log.error("Skipping assignment email: missing recipient or sender address.");
            return;
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

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            log.info("Email sent successfully to: {}", maskedGiver);
                        } else {
                            // Never log the response body: it may contain recipient data.
                            log.error("Failed to send email to {}: HTTP {}",
                                    maskedGiver, response.statusCode());
                        }
                    })
                    .exceptionally(e -> {
                        log.error("Error sending email to {}: {}", maskedGiver, e.getMessage());
                        return null;
                    });

        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", maskedGiver, e.getMessage());
        }
    }

    // Package-private so the payload (escaping) can be unit tested.
    String buildEmailJson(Participant giver, Participant receiver, Game game)
            throws JacksonException {
        String wishList = receiver.getWishList() != null && !receiver.getWishList().isBlank()
                ? receiver.getWishList() : "No ha compartido lista de deseos";

        // User-provided values are HTML-escaped before being embedded in the email.
        String safeGiverName = HtmlUtils.htmlEscape(singleLine(giver.getName()));
        String safeReceiverName = HtmlUtils.htmlEscape(singleLine(receiver.getName()));
        String safeWishList = HtmlUtils.htmlEscape(wishList);

        String htmlContent = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        line-height: 1.6;
                        color: #333;
                        margin: 0;
                        padding: 0;
                        -webkit-text-size-adjust: 100%%;
                        -ms-text-size-adjust: 100%%;
                    }
                    .container {
                        max-width: 600px;
                        margin: 0 auto;
                        padding: 20px;
                    }
                    .header {
                        background: linear-gradient(135deg, #c53030, #e53e3e);
                        color: white;
                        padding: 30px 20px;
                        text-align: center;
                        border-radius: 10px 10px 0 0;
                    }
                    .header h1 {
                        margin: 0;
                        font-size: 28px;
                    }
                    .content {
                        background: white;
                        padding: 25px;
                        border: 2px solid #e2e8f0;
                        border-top: none;
                        border-radius: 0 0 10px 10px;
                    }
                    .assignment-box {
                        background: #f7fafc;
                        padding: 20px;
                        border-radius: 8px;
                        margin: 20px 0;
                        border-left: 4px solid #c53030;
                    }
                    .assignment-name {
                        font-size: 24px;
                        color: #c53030;
                        font-weight: bold;
                        margin: 10px 0;
                    }
                    .info-row {
                        margin: 15px 0;
                        padding: 15px;
                        background: #edf2f7;
                        border-radius: 8px;
                        font-size: 16px;
                    }
                    .label {
                        font-weight: bold;
                        color: #2d3748;
                        display: block;
                        margin-bottom: 5px;
                        font-size: 14px;
                    }
                    .tips-box {
                        margin-top: 30px;
                        padding: 20px;
                        background: #fffbeb;
                        border-radius: 8px;
                        border-left: 4px solid #f59e0b;
                    }
                    .footer {
                        text-align: center;
                        margin-top: 30px;
                        color: #718096;
                        font-size: 14px;
                        padding-top: 20px;
                        border-top: 1px solid #e2e8f0;
                    }
                    @media only screen and (max-width: 600px) {
                        .container {
                            padding: 10px;
                        }
                        .header {
                            padding: 25px 15px;
                        }
                        .header h1 {
                            font-size: 24px;
                        }
                        .content {
                            padding: 20px 15px;
                        }
                        .assignment-name {
                            font-size: 22px;
                        }
                        .info-row {
                            padding: 12px;
                            font-size: 15px;
                        }
                    }
                    @media only screen and (max-width: 400px) {
                        .header h1 {
                            font-size: 22px;
                        }
                        .assignment-name {
                            font-size: 20px;
                        }
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🎄 SECRET SANTA 🎄</h1>
                    </div>
                    <div class="content">
                        <h2 style="margin-top: 0;">¡Hola %s! 👋</h2>
                        <p style="font-size: 16px;">¡Excelentes noticias! La asignación del amigo invisible ya está lista.</p>
                        <div class="assignment-box">
                            <div class="label">🎯 TU AMIGO SECRETO ES:</div>
                            <div class="assignment-name">%s</div>
                        </div>
                        <div class="info-row">
                            <span class="label">📝 Lista de deseos:</span>
                            %s
                        </div>
                        <div class="info-row">
                            <span class="label">💰 Presupuesto máximo:</span> %s €
                        </div>
                        <div class="info-row">
                            <span class="label">📅 Fecha del intercambio:</span> %s
                        </div>
                        <div class="tips-box">
                            <h3 style="margin-top: 0; color: #d97706;">✨ Consejos:</h3>
                            <ul style="margin: 10px 0; padding-left: 20px;">
                                <li>Sé creativo con tu regalo 🎁</li>
                                <li>Lo importante es el detalle 💝</li>
                                <li>¡Mantén el secreto! 🤫</li>
                            </ul>
                        </div>
                        <div class="footer">
                            <p style="margin: 5px 0;">Que la magia de la Navidad te acompañe 🎅🎁</p>
                            <p style="margin: 5px 0; font-weight: bold;">Secret Santa Organizer</p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """,
                safeGiverName,
                safeReceiverName,
                safeWishList,
                game.getMaxPrice(),
                game.getExchangeDate()
        );

        String textContent = String.format("""
            ╔══════════════════════════════════════╗
                         🎄 SECRET SANTA 🎄
            ╚══════════════════════════════════════╝
            
            Hola %s,
            
            ¡Excelentes noticias! La asignación del amigo invisible 
            ya está lista y estamos emocionados de compartirla contigo.
            
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            🎯 PERSONA ASIGNADA:
               %s
            
            📝 LISTA DE DESEOS:
               %s
            
            💰 PRESUPUESTO MÁXIMO:
               %s €
            
            📅 FECHA DEL INTERCAMBIO:
               %s
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            
            ✨ Consejos:
            • Sé creativo con tu regalo
            • Lo importante es el detalle
            • ¡Mantén el secreto! 🤫
            
            Que la magia de la Navidad te acompañe en esta 
            bonita tradición.
            
            ──────────────────────────────────────
            Con cariño navideño,
            El equipo de Secret Santa Organizer
            🎅🎁🎄
            """,
                singleLine(giver.getName()),
                singleLine(receiver.getName()),
                wishList,
                game.getMaxPrice(),
                game.getExchangeDate()
        );

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sender", Map.of(
                "email", fromAddress == null ? "" : fromAddress,
                "name", singleLine(senderName).isEmpty()
                        ? "Secret Santa Organizer" : singleLine(senderName)));
        payload.put("to", List.of(Map.of(
                "email", giver.getEmail() == null ? "" : giver.getEmail(),
                "name", singleLine(giver.getName()))));
        payload.put("subject", "🎁 ¡Tu amigo invisible ha sido asignado!");
        payload.put("htmlContent", htmlContent);
        payload.put("textContent", textContent);

        // Jackson handles JSON escaping, preventing broken payloads or injection.
        return objectMapper.writeValueAsString(payload);
    }

    private static String singleLine(String value) {
        return value == null ? "" : value.replaceAll("[\\r\\n]+", " ").trim();
    }

    private static boolean isValidEmail(String email) {
        return email != null
                && !email.isBlank()
                && email.indexOf('@') > 0
                && !email.contains("\r")
                && !email.contains("\n");
    }
}