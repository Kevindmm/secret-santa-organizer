package com.kdmm.secretsanta.dto;

/**
 * Participant total and duplicate-email count for one game, resolved in a
 * single query when someone joins.
 */
public record GameJoinStats(String gameId, long total, long emailMatches) {
}
