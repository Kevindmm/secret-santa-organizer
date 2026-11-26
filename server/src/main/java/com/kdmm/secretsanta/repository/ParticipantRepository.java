package com.kdmm.secretsanta.repository;

import com.kdmm.secretsanta.dto.GameParticipantCount;
import com.kdmm.secretsanta.model.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    List<Participant> findByGameId(String gameId);

    /**
     * Counts participants for every game in one query. The game list used to
     * run one count per game, which grows linearly with the number of games.
     */
    @Query("""
            select new com.kdmm.secretsanta.dto.GameParticipantCount(p.game.id, count(p))
            from Participant p
            group by p.game.id
            """)
    List<GameParticipantCount> countGroupedByGame();
}
