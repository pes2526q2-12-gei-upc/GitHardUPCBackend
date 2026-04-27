package com.safesteps.backend.domain.routecalculator;

import com.safesteps.backend.domain.routecalculator.projections.FiltreDBProjection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;


@Table(name = "filtres")
@Data
@Entity
@NoArgsConstructor
public class Filtre {

    /*
        En tots els casos es considera que si es un filtre predeterminat
        el valor "Default" sera 0. En canvi, si el filtre es "PERSONALITZAT"
        el valor "Default" sera 0.5.
    */

    @Id
    @Column(name = "google_id")
    private String googleId;

    //Seguretat
    @Column(name = "comissaries", columnDefinition = "float8 default 0")
    @Min(value = 0, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    @Max(value = 1, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    private double comissaries = 0;
    @Column(name = "fets_penals", columnDefinition = "float8 default 0")
    @Min(value = 0, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    @Max(value = 1, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    private double fetsPenals = 0;
    @Column(name = "cameres_seguretat", columnDefinition = "float8 default 0")
    @Min(value = 0, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    @Max(value = 1, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    private double cameresSeguretat = 0;
    @Column(name = "infraccions", columnDefinition = "float8 default 0")
    @Min(value = 0, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    @Max(value = 1, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    private double infraccions = 0;


    //Confort
    @Column(name = "fonts_aigua", columnDefinition = "float8 default 0")
    @Min(value = 0, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    @Max(value = 1, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    private double fontsAigua = 0;
    @Column(name = "bancs", columnDefinition = "float8 default 0")
    @Min(value = 0, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    @Max(value = 1, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    private double bancs = 0;
    @Column(name = "contaminacio_acustica", columnDefinition = "float8 default 0")
    @Min(value = 0, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    @Max(value = 1, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    private double contaminacioAcustica = 0;
    @Column(name = "escales_mecaniques", columnDefinition = "float8 default 0")
    @Min(value = 0, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    @Max(value = 1, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    private double escalesMecaniques = 0;

    //Clima


    @Column(name = "arbres", columnDefinition = "float8 default 0")
    @Min(value = 0, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    @Max(value = 1, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    private double arbres = 0;
    @Column(name = "refugis_climatics", columnDefinition = "float8 default 0")
    @Min(value = 0, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    @Max(value = 1, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    private double refugisClimatics = 0;
    @Column(name = "qualitat_aire", columnDefinition = "float8 default 0")
    @Min(value = 0, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    @Max(value = 1, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    private double qualitatAire = 0;

    public Filtre(FiltreEnum f) {
        if (f == FiltreEnum.SEGURETAT) {
            comissaries = 1;
            fetsPenals = 1;
            cameresSeguretat = 1;
            infraccions = 1;
        } else if  (f == FiltreEnum.CLIMA) {
            arbres = 1;
            refugisClimatics = 1;
            qualitatAire = 1;
        } else if (f == FiltreEnum.CONFORT) {
            fontsAigua = 1;
            bancs = 1;
            contaminacioAcustica = 1;
            escalesMecaniques = 1;
        }
    }
    
    public Filtre(FiltreDBProjection fPj) {
        if (fPj == null) return;
        this.comissaries = (fPj.getComissaries() == null)? 0.5: fPj.getComissaries();
        this.fetsPenals = (fPj.getFetsPenals() == null)? 0.5: fPj.getFetsPenals();
        this.cameresSeguretat = (fPj.getCameresSeguretat() == null)? 0.5: fPj.getCameresSeguretat();
        this.infraccions = (fPj.getInfraccions() == null)? 0.5: fPj.getInfraccions();
        this.fontsAigua = (fPj.getFontsAigua() == null)? 0.5: fPj.getFontsAigua();
        this.bancs = (fPj.getBancs() == null)? 0.5: fPj.getBancs();
        this.contaminacioAcustica = (fPj.getContaminacioAcustica() == null)? 0.5: fPj.getContaminacioAcustica();
        this.escalesMecaniques = (fPj.getEscalesMecaniques() == null)? 0.5: fPj.getEscalesMecaniques();
        this.arbres = (fPj.getArbres() == null)? 0.5: fPj.getArbres();
        this.refugisClimatics = (fPj.getRefugisClimatics() == null)? 0.5: fPj.getRefugisClimatics();
        this.qualitatAire = (fPj.getQualitatAire() == null)? 0.5: fPj.getQualitatAire();
    }

}