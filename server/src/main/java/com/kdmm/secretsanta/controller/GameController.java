package com.kdmm.secretsanta.controller;

import com.kdmm.secretsanta.dto.GameRequest;
import com.kdmm.secretsanta.dto.ParticipantRequest;
import com.kdmm.secretsanta.service.GameService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> createGame(@Valid @RequestBody GameRequest request) {
        String gameId = gameService.createGame(request);
        String joinUrl = "https://tudominio.com/join/" + gameId; //TODO actualizar linea con web real

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "gameId", gameId,
                        "joinUrl", joinUrl
                ));
    }

    @PostMapping("/{gameId}/participants")
    public ResponseEntity<Void> joinGame(
            @PathVariable String gameId,
            @Valid @RequestBody ParticipantRequest request) {
        gameService.addParticipant(gameId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{gameId}/assign")
    public ResponseEntity<Void> assignSecretSantas(@PathVariable String gameId) {
        gameService.assignSecretSantas(gameId);
        return ResponseEntity.ok().build();
    }
}