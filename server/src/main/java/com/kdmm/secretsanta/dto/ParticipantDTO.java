package com.kdmm.secretsanta.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ParticipantDTO(
        Long id,
        String name,
        String email,
        Boolean assigned
) {
    public ParticipantDTO(Long id, String name, String email) {
        this(id, name, email, null);
    }
}