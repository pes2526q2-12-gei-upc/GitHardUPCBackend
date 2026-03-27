package com.safesteps.backend.domain.routecalculator;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Objeto que representa una coordenada geográfica (Latitud y Longitud) dentro del Área Metropolitana de Barcelona.")
public class Coord {
    @NotNull(message = "La longitud es obligatoria")
    @DecimalMin(value = "2.05", message = "La longitud debe estar dentro de los límites de Barcelona (Mínimo 2.05)")
    @DecimalMax(value = "2.23", message = "La longitud debe estar dentro de los límites de Barcelona (Máximo 2.23)")
    @Schema(description = "Longitud de la coordenada geolocalizada", example = "2.1686", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double lon;

    @NotNull(message = "La latitud es obligatoria")
    @DecimalMin(value = "41.32", message = "La latitud debe estar dentro de los límites de Barcelona (Mínimo 41.32)")
    @DecimalMax(value = "41.47", message = "La latitud debe estar dentro de los límites de Barcelona (Máximo 41.47)")
    @Schema(description = "Latitud de la coordenada geolocalizada", example = "41.3874", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double lat;

}