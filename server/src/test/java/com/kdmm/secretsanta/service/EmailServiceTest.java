package com.kdmm.secretsanta.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.kdmm.secretsanta.model.Game;
import com.kdmm.secretsanta.model.Participant;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EmailService emailService =
            new EmailService(objectMapper, "test-api-key", "from@test.com", "Secret Santa");

    @Test
    void buildEmailJson_producesValidJsonAndEscapesUserContent() throws Exception {
        Participant giver = participant(1L, "Ana \"La Jefa\"\nBcc: evil@test.com", "ana@test.com");
        Participant receiver = participant(2L, "Bruno <script>alert('x')</script>", "bruno@test.com");
        receiver.setWishList("Quiero \"un libro\"\n<script>alert('x')</script>");

        Game game = new Game("ABC12345");
        game.setMaxPrice(new BigDecimal("50"));
        game.setExchangeDate(LocalDate.of(2026, 12, 24));

        String json = emailService.buildEmailJson(giver, receiver, game);
        JsonNode payload = objectMapper.readTree(json);

        // A JSON parser can read the payload even with quotes, newlines and HTML in names.
        assertEquals("ana@test.com", payload.path("to").get(0).path("email").asText());
        assertEquals("from@test.com", payload.path("sender").path("email").asText());
        assertEquals("Secret Santa", payload.path("sender").path("name").asText());
        assertFalse(json.contains("test-api-key"), "The API key must never be part of the payload");

        String recipientName = payload.path("to").get(0).path("name").asText();
        assertFalse(recipientName.contains("\n"), "Names must not contain header-breaking newlines");
        assertTrue(recipientName.contains("Bcc: evil@test.com"));

        String html = payload.path("htmlContent").asText();
        assertTrue(html.contains("&lt;script&gt;"));
        assertTrue(html.contains("&quot;un libro&quot;"));
        assertTrue(html.contains("&quot;La Jefa&quot;"));
        assertFalse(html.contains("<script>alert"), "HTML must be escaped inside the email");

        String text = payload.path("textContent").asText();
        assertTrue(text.contains("Bruno <script>alert('x')</script>"));
    }

    @Test
    void buildEmailJson_usesFallbackWhenWishListIsEmpty() throws Exception {
        Participant giver = participant(1L, "Ana", "ana@test.com");
        Participant receiver = participant(2L, "Bruno", "bruno@test.com");
        receiver.setWishList("   ");

        Game game = new Game("ABC12345");
        game.setMaxPrice(new BigDecimal("0"));
        game.setExchangeDate(LocalDate.of(2026, 12, 24));

        JsonNode payload = objectMapper.readTree(
                emailService.buildEmailJson(giver, receiver, game));

        assertTrue(payload.path("textContent").asText()
                .contains("No ha compartido lista de deseos"));
        assertTrue(payload.path("htmlContent").asText()
                .contains("No ha compartido lista de deseos"));
    }

    private static Participant participant(long id, String name, String email) {
        Participant participant = new Participant();
        participant.setId(id);
        participant.setName(name);
        participant.setEmail(email);
        return participant;
    }
}
