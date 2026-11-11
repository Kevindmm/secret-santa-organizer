package com.kdmm.secretsanta.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kdmm.secretsanta.util.EmailMasker;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ParticipantDTO(
        Long id,
        String name,
        String email,
        Boolean assigned
) {
    /**
     * Full addresses never leave the backend through this DTO.
     */
    public ParticipantDTO {
        email = EmailMasker.mask(email);
    }

    public ParticipantDTO(Long id, String name, String email) {
        this(id, name, email, null);
    }
}