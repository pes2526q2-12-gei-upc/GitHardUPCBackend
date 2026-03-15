package com.safesteps.backend.domain.routecalculator;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Objecte que representa un Punt d'Interès (POI) al llarg de la ruta.")
public class PoiDTO {

    @Schema(description = "Tipus de POI (ex: COMISSARIA, FONT, BANC)", example = "COMISSARIA")
    private String type;

    @Schema(description = "Nom descriptiu de l'element", example = "Comissaria Eixample")
    private String name;

    @Schema(description = "Latitud del POI", example = "41.3874")
    private Double lat;

    @Schema(description = "Longitud del POI", example = "2.1686")
    private Double lon;
}