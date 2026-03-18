package com.safesteps.backend.domain.routecalculator;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class Filtres {
    @Valid
    @Min(value = 0, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    @Max(value = 1, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    private float seguretat = 0; //+comissaries -fets delictius

    @Valid
    @Min(value = 0, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    @Max(value = 1, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    private float fontsAigua = 0;

    @Valid
    @Min(value = 0, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    @Max(value = 1, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    private float ombra = 0;

    @Valid
    @Min(value = 0, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    @Max(value = 1, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    private float escalesMecaniques = 0;

    @Valid
    @Min(value = 0, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    @Max(value = 1, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    private float refugisClimatics = 0;

    @Valid
    @Min(value = 0, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    @Max(value = 1, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    private float qualitatAire = 0;

    @Valid
    @Min(value = 0, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    @Max(value = 1, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    private float bancs = 0;

    @Valid
    @Min(value = 0, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    @Max(value = 1, message = "El valor del filtro tiene que ser un valor entre 0 y 1.")
    private float localsOberts = 0; //Ilumincacio

}