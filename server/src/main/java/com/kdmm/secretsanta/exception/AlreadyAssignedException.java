package com.kdmm.secretsanta.exception;

public class AlreadyAssignedException extends RuntimeException {
    public AlreadyAssignedException(String gameId) {
        super("Los amigo invisibles ya fueron asignados para el juego: " + gameId);
    }
}