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

    @Column(name = "comissaries")
    private Double comissaries = 0.5;
    @Column(name = "fets_penals")
    private Double fetsPenals = 0.5;
    @Column(name = "cameres_seguretat")
    private Double cameresSeguretat = 0.5;
    @Column(name = "infraccions")
    private Double infraccions = 0.5;
    @Column(name = "fonts_aigua")
    private Double fontsAigua = 0.5;
    @Column(name = "bancs")
    private Double bancs = 0.5;
    @Column(name = "contaminacio_acustica")
    private Double contaminacioAcustica = 0.5;
    @Column(name = "escales_mecaniques")
    private Double escalesMecaniques = 0.5;
    @Column(name = "arbres")
    private Double arbres = 0.5;
    @Column(name = "refugis_climatics")
    private Double refugisClimatics = 0.5;
    @Column(name = "qualitat_aire")
    private Double qualitatAire = 0.5;
}