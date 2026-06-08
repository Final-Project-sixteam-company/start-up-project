package com.startup.domain.scenario.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CustomLocationCreateRequest {

    @NotBlank(message = "장소 이름은 필수입니다.")
    private String name;

    private String description;
    private String floor;
    private Integer mapX;
    private Integer mapY;
    private String imageAssetKey;
}
