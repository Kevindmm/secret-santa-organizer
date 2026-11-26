package com.kdmm.secretsanta.repository;

import com.kdmm.secretsanta.dto.GameJoinStats;
import com.kdmm.secretsanta.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GameRepository extends JpaRepository<Game, String> {

    /**
     * Resolves the game, its participant total and how many participants
     * already use this email in one query. Checking all three separately cost
     * three network round trips to Supabase per join.
     *
     * <p>Empty when the game does not exist.</p>
     */
    @Query("""
            select new com.kdmm.secretsanta.dto.GameJoinStats(
                g.id,
                count(p),
                count(case when lower(p.email) = lower(:email) then 1 end))
            from Game g left join g.participants p
            where g.id = :gameId
            group by g.id
            """)
    Optional<GameJoinStats> findJoinStats(@Param("gameId") String gameId,
                                          @Param("email") String email);
}
