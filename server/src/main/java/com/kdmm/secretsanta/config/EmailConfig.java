package com.kdmm.secretsanta.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import java.util.Properties;

@Configuration
public class EmailConfig {

    // 1. Inject values from application.properties
    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.port}")
    private int port;

    @Value("${spring.mail.username}") // Your verified email (e.g., personal Gmail)
    private String username;

    @Value("${spring.mail.password}") // Your App Password or SMTP key
    private String password;

    @Value("${spring.mail.properties.mail.smtp.auth}")
    private boolean auth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable}")
    private boolean starttls;

    @Value("${spring.mail.from.address:no-reply@kdmm.com}") // Default value
    private String fromAddress;

    // 2. Primary Bean: Configured JavaMailSender
    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", String.valueOf(auth));
        props.put("mail.smtp.starttls.enable", String.valueOf(starttls));
        // Add other properties if needed
        props.put("mail.debug", "true"); // Enable only for development

        return mailSender;
    }

    // 3. Bean for the "From" address (optional but clean)
    @Bean
    public String emailFromAddress() {
        return fromAddress;
    }
}