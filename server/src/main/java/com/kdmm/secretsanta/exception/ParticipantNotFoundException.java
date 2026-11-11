package com.kdmm.secretsanta.exception;

public class ParticipantNotFoundException extends RuntimeException {
    public ParticipantNotFoundException(Long participantId) {
        super("Participante no encontrado con ID: " + participantId);
    }
}
