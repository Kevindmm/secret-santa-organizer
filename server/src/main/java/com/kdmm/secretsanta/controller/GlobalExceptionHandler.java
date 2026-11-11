package com.kdmm.secretsanta.controller;

import com.kdmm.secretsanta.exception.AlreadyAssignedException;
import com.kdmm.secretsanta.exception.DuplicateEmailException;
import com.kdmm.secretsanta.exception.GameNotFoundException;
import com.kdmm.secretsanta.exception.InsufficientParticipantsException;
import com.kdmm.secretsanta.exception.ParticipantLimitException;
import com.kdmm.secretsanta.exception.ParticipantNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler({GameNotFoundException.class, ParticipantNotFoundException.class})
    public ResponseEntity<Map<String, String>> handleNotFoundException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler({AlreadyAssignedException.class, DuplicateEmailException.class,
            ParticipantLimitException.class})
    public ResponseEntity<Map<String, String>> handleConflictException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(InsufficientParticipantsException.class)
    public ResponseEntity<Map<String, String>> handleInsufficientParticipants(
            InsufficientParticipantsException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Unknown paths are a normal client mistake, not a server failure. They are
     * answered with a plain 404 instead of being logged as an unhandled error.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, String>> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Recurso no encontrado."));
    }

    /**
     * Last resort. Internal details are logged, never returned to the client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpectedException(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Se produjo un error inesperado. Inténtalo de nuevo."));
    }
}
