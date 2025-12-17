package com.kdmm.secretsanta.repository;

import com.kdmm.secretsanta.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, String> {
    //findById auto by jpa
}
