package com.kdmm.secretsanta.dto;

/**
 * Outcome of a draw, so callers can tell whether every assignment email was
 * accepted by the mail provider. Previously the API returned an empty body and
 * the UI assumed success even when no email could be sent.
 */
public record AssignmentResult(
        int emailsDispatched,
        int participants
) {}
