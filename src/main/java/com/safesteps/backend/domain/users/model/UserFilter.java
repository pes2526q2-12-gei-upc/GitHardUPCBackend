package com.safesteps.backend.domain.users.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "filtres")
public class UserFilter {

    @Id
    @Column(name = "google_id", length = 100)
    private String googleId;

    private Double comissaries = 0.5;
    private Double fetsPenals = 0.5;
    private Double cameresSeguretat = 0.5;
    private Double infraccions = 0.5;
    private Double fontsAigua = 0.5;
    private Double bancs = 0.5;
    private Double contaminacioAcustica = 0.5;
    private Double escalesMecaniques = 0.5;
    private Double arbres = 0.5;
    private Double refugisClimatics = 0.5;
    private Double qualitatAire = 0.5;
}