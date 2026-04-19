package com.safesteps.backend.domain.incidents.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VoteRequestDTO {

    @Valid
    @NotNull(message = "El id del usuario es obligatorio")
    @Schema(description = "Identificador del usuario", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long userId;

    @Valid
    @NotNull(message = "El id de la incidencia es obligatorio")
    @Schema(description = "Identificador de la incidencia", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long incidentId;

    @Valid
    @NotNull(message = "La puntuacion d'una incidencia es obligatoria")
    @Schema(description = "Puntuacion dada al voto de una incidencia", requiredMode = Schema.RequiredMode.REQUIRED)
    private int score;
}
