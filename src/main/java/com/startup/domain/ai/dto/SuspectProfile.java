package com.startup.domain.ai.dto;

public record SuspectProfile(
        Long id,
        String name,
        String role,
        String relationToVictim,
        String publicProfile,
        String publicStatement,
        String alibi
) {
}
