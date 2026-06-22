package com.startup.domain.ai.dto;

public record SuspectProfile(
        Long id,
        String code,
        String name,
        String role,
        String relationToVictim,
        String publicProfile,
        String publicStatement,
        String alibi
) {
    public SuspectProfile(Long id,
                          String name,
                          String role,
                          String relationToVictim,
                          String publicProfile,
                          String publicStatement,
                          String alibi) {
        this(id, null, name, role, relationToVictim, publicProfile, publicStatement, alibi);
    }
}
