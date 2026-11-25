package com.kdmm.secretsanta.dto;

/**
 * Participant count per game, used to build the game list without one query
 * per game.
 */
public record GameParticipantCount(String gameId, long total) {
}
