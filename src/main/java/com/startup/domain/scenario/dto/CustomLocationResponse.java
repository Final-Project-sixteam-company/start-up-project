package com.startup.domain.scenario.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CustomLocationResponse {
    private Long locationId;
    private String name;
    private String description;
    private Integer mapX;
    private Integer mapY;
    private Integer evidenceCount;
}
