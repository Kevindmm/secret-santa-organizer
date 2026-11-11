package com.kdmm.secretsanta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

public record GameRequest(
        @NotBlank String name,
        @PositiveOrZero BigDecimal maxPrice,
        LocalDate exchangeDate
) {}