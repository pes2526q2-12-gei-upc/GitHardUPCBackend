package com.safesteps.backend.domain.routecalculator;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;


@Data
public class Filtre {

    @Min(value = 0, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    @Max(value = 1, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    private float seguretat = 0; //+comissaries -fets delictius


    @Min(value = 0, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    @Max(value = 1, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    private float fontsAigua = 0;


    @Min(value = 0, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    @Max(value = 1, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    private float ombra = 0;


    @Min(value = 0, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    @Max(value = 1, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    private float escalesMecaniques = 0;


    @Min(value = 0, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    @Max(value = 1, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    private float refugisClimatics = 0;


    @Min(value = 0, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    @Max(value = 1, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    private float qualitatAire = 0;


    @Min(value = 0, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    @Max(value = 1, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    private float bancs = 0;


    @Min(value = 0, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    @Max(value = 1, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    private float localsOberts = 0; //Iluminacio

}