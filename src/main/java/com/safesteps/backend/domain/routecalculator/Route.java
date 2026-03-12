package com.safesteps.backend.domain.routecalculator;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Route {
    @Schema(description = "Lista de coordenadas [Longitud, Latitud] que forman la ruta para dibujar en el mapa.", example = "[[2.1685, 41.3873], [2.1744, 41.4036]]")
    private List<Double[]> coordinates;

    @Schema(description = "Distancia total de la ruta expresada en metros.", example = "1500.5")
    private Double distanceMeters;

    @Schema(description = "Tiempo estimado a pie expresado en minutos (calculado a una velocidad media de 5 km/h).", example = "18")
    private Integer estimatedTimeMinutes;
}
