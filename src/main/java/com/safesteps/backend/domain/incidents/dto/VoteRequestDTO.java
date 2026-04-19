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
    @NotNull(message = "La puntuacion d'una incidencia es obligatoria")
    @Schema(description = "Puntuacion dada al voto de una incidencia", requiredMode = Schema.RequiredMode.REQUIRED)
    private int accepted;
    //NOMES 1 o -1 -> 1 es accepted, 0 rebutjat

}
