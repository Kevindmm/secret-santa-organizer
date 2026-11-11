package com.kdmm.secretsanta.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GameSummaryDTO(
        String gameId,
        String name,
        BigDecimal maxPrice,
        LocalDate exchangeDate,
        int participantCount,
        boolean assignmentsDone
) {}
