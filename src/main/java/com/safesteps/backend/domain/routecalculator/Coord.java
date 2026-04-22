package com.safesteps.backend.domain.routecalculator;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Objeto que representa una coordenada geográfica dentro del Bounding Box de Barcelona.")
public class Coord {

    public static final String BCN_LAT_MIN = "41.32";
    public static final String BCN_LAT_MAX = "41.47";
    public static final String BCN_LON_MIN = "2.05";
    public static final String BCN_LON_MAX = "2.23";

    @NotNull(message = "La latitud es obligatoria")
    @DecimalMin(value = BCN_LAT_MIN, message = "Latitud fuera de rango (Mín: " + BCN_LAT_MIN + "). El servicio solo cubre Barcelona.")
    @DecimalMax(value = BCN_LAT_MAX, message = "Latitud fuera de rango (Máx: " + BCN_LAT_MAX + "). El servicio solo cubre Barcelona.")
    @Schema(description = "Latitud de la coordenada", example = "41.3874", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double lat;

    @NotNull(message = "La longitud es obligatoria")
    @DecimalMin(value = BCN_LON_MIN, message = "Longitud fuera de rango (Mín: " + BCN_LON_MIN + "). El servicio solo cubre Barcelona.")
    @DecimalMax(value = BCN_LON_MAX, message = "Longitud fuera de rango (Máx: " + BCN_LON_MAX + "). El servicio solo cubre Barcelona.")
    @Schema(description = "Longitud de la coordenada", example = "2.1686", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double lon;
}