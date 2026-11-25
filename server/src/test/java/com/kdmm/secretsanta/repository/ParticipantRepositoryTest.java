package com.kdmm.secretsanta.repository;

import com.kdmm.secretsanta.dto.GameParticipantCount;
import com.kdmm.secretsanta.model.Game;
import com.kdmm.secretsanta.model.Participant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the custom queries against H2 (PostgreSQL compatibility mode) so a
 * broken JPQL statement fails here instead of at runtime.
 */
@DataJpaTest
class ParticipantRepositoryTest {

    @Autowired
    private ParticipantRepository participantRepo;

    @Autowired
    private GameRepository gameRepo;

    @Test
    void countGroupedByGame_returnsCountPerGameInOneQuery() {
        Game first = gameRepo.save(game("AAA11111"));
        Game second = gameRepo.save(game("BBB22222"));
        gameRepo.save(game("CCC33333"));

        participantRepo.save(participant(first, "ana@test.com"));
        participantRepo.save(participant(first, "bruno@test.com"));
        participantRepo.save(participant(second, "carla@test.com"));
        participantRepo.flush();

        Map<String, Long> counts = participantRepo.countGroupedByGame().stream()
                .collect(Collectors.toMap(
                        GameParticipantCount::gameId,
                        GameParticipantCount::total));

        assertEquals(2L, counts.get("AAA11111"));
        assertEquals(1L, counts.get("BBB22222"));
        // Games without participants are absent; the service defaults them to 0.
        assertFalse(counts.containsKey("CCC33333"));
    }

    @Test
    void existsByGameIdAndEmailIgnoreCase_matchesEmailCaseInsensitively() {
        Game first = gameRepo.save(game("AAA11111"));
        Game second = gameRepo.save(game("BBB22222"));
        participantRepo.save(participant(first, "ana@test.com"));
        participantRepo.flush();

        assertTrue(participantRepo.existsByGameIdAndEmailIgnoreCase("AAA11111", "ANA@TEST.COM"));
        // The same email in another game is still allowed.
        assertFalse(participantRepo.existsByGameIdAndEmailIgnoreCase("BBB22222", "ana@test.com"));
        assertFalse(participantRepo.existsByGameIdAndEmailIgnoreCase("AAA11111", "other@test.com"));
    }

    private static Game game(String id) {
        Game game = new Game(id);
        game.setName("Navidad en casa");
        return game;
    }

    private static Participant participant(Game game, String email) {
        Participant participant = new Participant();
        participant.setName("Ana");
        participant.setEmail(email);
        participant.setWishList("Un libro");
        participant.setGame(game);
        participant.setEmailSent(false);
        return participant;
    }
}
