package com.kdmm.secretsanta.repository;

import com.kdmm.secretsanta.dto.GameJoinStats;
import com.kdmm.secretsanta.model.Game;
import com.kdmm.secretsanta.model.Participant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates the join check query against H2 so the JPQL and the email
 * comparison are covered by a real database, not only by mocks.
 */
@DataJpaTest
class GameRepositoryTest {

    @Autowired
    private GameRepository gameRepo;

    @Autowired
    private ParticipantRepository participantRepo;

    @Test
    void findJoinStats_countsParticipantsAndMatchesEmailInOneQuery() {
        Game game = gameRepo.save(game("AAA11111"));
        participantRepo.save(participant(game, "ana@test.com"));
        participantRepo.save(participant(game, "bruno@test.com"));
        participantRepo.flush();

        GameJoinStats stats = gameRepo.findJoinStats("AAA11111", "ANA@TEST.COM")
                .orElseThrow();

        assertEquals(2, stats.total());
        assertEquals(1, stats.emailMatches());
    }

    @Test
    void findJoinStats_reportsZeroForAGameWithoutParticipants() {
        gameRepo.save(game("BBB22222"));

        GameJoinStats stats = gameRepo.findJoinStats("BBB22222", "ana@test.com")
                .orElseThrow();

        assertEquals(0, stats.total());
        assertEquals(0, stats.emailMatches());
    }

    @Test
    void findJoinStats_isEmptyWhenTheGameDoesNotExist() {
        assertTrue(gameRepo.findJoinStats("ZZZ99999", "ana@test.com").isEmpty());
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
