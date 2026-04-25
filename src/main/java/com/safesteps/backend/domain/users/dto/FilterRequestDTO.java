package com.safesteps.backend.domain.users.dto;

import lombok.Data;

@Data
public class FilterRequestDTO {
    private Double comissaries;
    private Double fetsPenals;
    private Double cameresSeguretat;
    private Double infraccions;
    private Double fontsAigua;
    private Double bancs;
    private Double contaminacioAcustica;
    private Double escalesMecaniques;
    private Double arbres;
    private Double refugisClimatics;
    private Double qualitatAire;
}
