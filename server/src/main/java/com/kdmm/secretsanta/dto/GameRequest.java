package com.kdmm.secretsanta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record GameRequest(
        @NotBlank @Size(max = 100) String name,
        @PositiveOrZero BigDecimal maxPrice,
        LocalDate exchangeDate
) {}
