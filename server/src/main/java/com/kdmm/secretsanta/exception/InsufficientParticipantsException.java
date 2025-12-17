package com.kdmm.secretsanta.exception;

public class InsufficientParticipantsException extends RuntimeException {
    public InsufficientParticipantsException(int current, int required) {
        super("Se necesitan al menos " + required + " participantes. Actual: " + current);
    }
}