package com.safesteps.backend.domain.routecalculator;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "DTO que encapsula los datos de la petición para calcular la ruta. Requiere las coordenadas exactas de origen y destino.")
public class RouteRequestDTO {

    @Valid
    @NotNull(message = "El punto de origen es obligatorio")
    @Schema(description = "Coordenadas del punto de origen", requiredMode = Schema.RequiredMode.REQUIRED)
    private Coord origin;

    @Valid
    @NotNull(message = "El punto de destino es obligatorio")
    @Schema(description = "Coordenadas del punto de destino", requiredMode = Schema.RequiredMode.REQUIRED)
    private Coord destination;
}