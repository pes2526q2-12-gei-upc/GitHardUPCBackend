package com.safesteps.backend.domain.routecalculator;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "DTO que encapsula los datos de la petición para calcular la ruta. Requiere las coordenadas exactas de origen y destino.")
public class RouteRequestDTO {

    @Valid
    @Schema(description = "El identificador de google de usuario", requiredMode = Schema.RequiredMode.REQUIRED)
    private String googleId;

    @Valid
    @NotNull(message = "El punto de origen es obligatorio")
    @Schema(description = "Coordenadas del punto de origen", requiredMode = Schema.RequiredMode.REQUIRED)
    private Coord origin;

    @Valid
    @NotNull(message = "El punto de destino es obligatorio")
    @Schema(description = "Coordenadas del punto de destino", requiredMode = Schema.RequiredMode.REQUIRED)
    private Coord destination;

    @Valid
    @Min(value = 1, message = "Se necesita minimo una ruta por peticion")
    @Max(value = 5, message = "No se pueden pedir mas de 5 rutas por peticion")
    private int nRoutes = 3;

    @Valid
    @NotNull
    @Schema(description = "Filtros de preferencia para el calculo de la ruta. Cada filtro debe tener un valor entre 0 y 1, donde 0 significa que el filtro no se tiene en cuenta y 1 significa que el filtro es muy importante.")
    private FiltreEnum filtre;
}