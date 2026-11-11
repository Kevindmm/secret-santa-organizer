package com.kdmm.secretsanta.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ParticipantRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank
        @Email
        @Size(max = 254)
        @Pattern(regexp = "^[^\\r\\n]*$", message = "El email contiene caracteres no permitidos")
        String email,
        @Size(max = 1000) String wishList
) {}
