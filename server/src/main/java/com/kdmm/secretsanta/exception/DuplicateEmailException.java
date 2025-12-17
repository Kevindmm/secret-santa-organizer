package com.kdmm.secretsanta.exception;

public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String email, String gameId) {
        super("Email '" + email + "' already registered in game: " + gameId);
    }
}