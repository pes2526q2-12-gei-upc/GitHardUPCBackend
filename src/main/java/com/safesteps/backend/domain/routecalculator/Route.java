package com.safesteps.backend.domain.routecalculator;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Route {
    @Schema(description = "Llista de coordenades [Longitud, Latitud] que formen la ruta.", example = "[[2.1685, 41.3873], [2.1744, 41.4036]]")
    private List<Double[]> coordinates;

    @Schema(description = "Distància total de la ruta en metres.", example = "1500.5")
    private Double distanceMeters;

    @Schema(description = "Temps estimat a peu en minuts.", example = "18")
    private Integer estimatedTimeMinutes;

    @Schema(description = "Llista de Punts d'Interès (POIs) propers a aquesta ruta.")
    private List<PoiDTO> pois = new ArrayList<>();
}