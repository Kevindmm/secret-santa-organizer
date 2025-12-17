package com.kdmm.secretsanta.repository;

import com.kdmm.secretsanta.model.Participant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    List<Participant> findByGameId(String gameId);
}
