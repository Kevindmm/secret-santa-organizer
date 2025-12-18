package com.kdmm.secretsanta.service;

import com.kdmm.secretsanta.model.Game;
import com.kdmm.secretsanta.model.Participant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    private final HttpClient httpClient;

    @Value("${brevo.api.key:}")
    private String brevoApiKey;

    @Value("${app.email.sender:no-reply@secret-santa.com}")
    private String fromAddress;

    public EmailService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Async
    public void sendAssignment(Participant giver, Participant receiver, Game game) {
        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            log.info("Email service disabled. No API key configured.");
            log.info("SIMULATED EMAIL To: {}, Giver: {}, Receiver: {}",
                    giver.getEmail(), giver.getName(), receiver.getName());
            return;
        }

        log.info("Sending assignment email to: {}", giver.getEmail());

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
                            log.info("Email sent successfully to: {}", giver.getEmail());
                        } else {
                            log.error("Failed to send email to {}: HTTP {} - {}",
                                    giver.getEmail(), response.statusCode(), response.body());
                        }
                    })
                    .exceptionally(e -> {
                        log.error("Error sending email to {}: {}", giver.getEmail(), e.getMessage());
                        return null;
                    });

        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", giver.getEmail(), e.getMessage());
        }
    }

    private String buildEmailJson(Participant giver, Participant receiver, Game game) {
        String wishList = receiver.getWishList() != null ?
                receiver.getWishList() : "No ha compartido lista de deseos";

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
                giver.getName(),
                receiver.getName(),
                wishList,
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
                giver.getName(),
                receiver.getName(),
                wishList,
                game.getMaxPrice(),
                game.getExchangeDate()
        );

        String escapedHtml = htmlContent
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\t", "\\t")
                .replace("\r", "\\r");

        String escapedText = textContent
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\t", "\\t")
                .replace("\r", "\\r");

        return String.format("""
            {
                "sender": {
                    "email": "%s",
                    "name": "Secret Santa Organizer"
                },
                "to": [
                    {
                        "email": "%s",
                        "name": "%s"
                    }
                ],
                "subject": "🎁 ¡Tu amigo invisible ha sido asignado!",
                "htmlContent": "%s",
                "textContent": "%s"
            }
            """,
                fromAddress,
                giver.getEmail(),
                giver.getName(),
                escapedHtml,
                escapedText
        );
    }
}