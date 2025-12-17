package com.kdmm.secretsanta.exception;

public class GameNotFoundException extends RuntimeException {
    public GameNotFoundException(String gameId) {
        super("Juego no encontrado con ID: " + gameId);
    }
}