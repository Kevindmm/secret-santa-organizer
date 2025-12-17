package com.kdmm.secretsanta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record GameRequest(
        @NotBlank String name,
        @Positive BigDecimal maxPrice,
        LocalDate exchangeDate
) {}