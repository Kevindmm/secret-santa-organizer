package com.kdmm.secretsanta.exception;

public class ParticipantLimitException extends RuntimeException {
    public ParticipantLimitException(int limit) {
        super("El juego ha alcanzado el límite de " + limit + " participantes");
    }
}
