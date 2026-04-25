package com.safesteps.backend.domain.incidents.dto;

import com.safesteps.backend.domain.incidents.model.IncidentTypeEnum;
import com.safesteps.backend.domain.routecalculator.Coord;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class IncidentRequestDTO {
    @Valid
    @NotNull(message = "El id del usuario es obligatorio")
    @Schema(description = "Identificador del usuario", requiredMode = Schema.RequiredMode.REQUIRED)
    private String googleId;

    @Valid
    @NotNull(message = "El tipo de incidencia es obligatorio")
    @Schema(description = "El tipo de incidencia reportada: OBRES, ILLUMINACIO, SEGURETAT, ALTRES", requiredMode = Schema.RequiredMode.REQUIRED)
    private IncidentTypeEnum type;

    @Valid
    @Schema(description = "Informacion extra sobre la incidencia")
    private String description;

    @Valid
    @NotNull(message = "La localizacion de la incidencia es obligatoria")
    @Schema(description = "Localizacion de la incidencia, con latitud y longitud dentro del Area Metropolitana de Barcelona", requiredMode = Schema.RequiredMode.REQUIRED)
    private Coord coordinates;
}
