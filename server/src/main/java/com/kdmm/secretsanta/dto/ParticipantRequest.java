package com.kdmm.secretsanta.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ParticipantRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        String wishList
) {}