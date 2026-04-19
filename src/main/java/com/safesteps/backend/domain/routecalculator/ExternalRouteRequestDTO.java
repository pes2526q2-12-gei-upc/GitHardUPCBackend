package com.safesteps.backend.domain.routecalculator;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Objeto de petición que contiene la lista de coordenadas de la ruta a evaluar.")
public class ExternalRouteRequestDTO {

    @Schema(description = "Lista de puntos (latitud y longitud) que forman la ruta. Debe contener al menos 2 puntos.",
            example = "[{\"lat\": 41.385063, \"lon\": 2.173404}, {\"lat\": 41.387200, \"lon\": 2.175500}]")
    private List<Coord> routePoints;

    public List<Coord> getRoutePoints() {
        return routePoints;
    }

    public void setRoutePoints(List<Coord> routePoints) {
        this.routePoints = routePoints;
    }
}
