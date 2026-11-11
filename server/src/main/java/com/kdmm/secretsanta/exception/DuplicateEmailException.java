package com.kdmm.secretsanta.exception;

import com.kdmm.secretsanta.util.EmailMasker;

public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String email, String gameId) {
        super("Email '" + EmailMasker.mask(email) + "' already registered in game: " + gameId);
    }
}
