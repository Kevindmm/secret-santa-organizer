package com.kdmm.secretsanta.service;

import com.kdmm.secretsanta.model.Game;
import com.kdmm.secretsanta.model.Participant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress; // Injected from EmailConfig

    // Constructor injection
    @Autowired
    public EmailService(JavaMailSender mailSender,
                        @Qualifier("emailFromAddress") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Async
    public void sendAssignment(Participant giver, Participant receiver, Game game) {
        log.info("Sending assignment email to: {}", giver.getEmail());
        log.debug("Giver: {}, Receiver: {}, Game: {}",
                giver.getName(), receiver.getName(), game.getName());

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(this.fromAddress); // Use the configured address
            message.setTo(giver.getEmail());
            message.setSubject("🎁 ¡Tu amigo invisible ha sido asignado!");

            String text = """
            Hola %s,
            
            ¡Tu amigo invisible ha sido asignado!
            
            Tienes que hacerle un regalo a: %s
            Lista de deseos: %s
            
            Precio máximo: %s€
            Fecha del intercambio: %s
            
            ¡Guarda el secreto! 🎅
            """.formatted(
                    giver.getName(),
                    receiver.getName(),
                    receiver.getWishList() != null ? receiver.getWishList() : "No ha compartido lista de deseos",
                    game.getMaxPrice(),
                    game.getExchangeDate()
            );

            message.setText(text);

            mailSender.send(message);
            log.info("Email sent successfully to: {}", giver.getEmail());

        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", giver.getEmail(), e.getMessage());
        }
    }
}