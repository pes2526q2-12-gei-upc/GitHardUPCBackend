package com.safesteps.backend.domain.routecalculator;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO que encapsula la respuesta del cálculo de la ruta. Incluye la geometría (coordenadas), la distancia total y el tiempo estimado.")
public class RouteResponseDTO {

    @Schema(description = "Lista de coordenadas [Longitud, Latitud] que forman la ruta para dibujar en el mapa.", example = "[[2.1685, 41.3873], [2.1744, 41.4036]]")
    private List<Double[]> ruta;

    @Schema(description = "Distancia total de la ruta expresada en metros.", example = "1500.5")
    private Double distanceMeters;

    @Schema(description = "Tiempo estimado a pie expresado en minutos (calculado a una velocidad media de 5 km/h).", example = "18")
    private Integer estimatedTimeMinutes;
}