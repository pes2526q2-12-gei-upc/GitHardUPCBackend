package com.safesteps.backend.domain.incidents.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VoteRequestDTO {

    @Valid
    @NotNull(message = "El google id del usuario es obligatorio")
    @Schema(description = "Identificador del usuario", requiredMode = Schema.RequiredMode.REQUIRED)
    private String googleId;

    @Valid
    @Max(1)
    @Min(-1)
    @NotNull(message = "La puntuacion d'una incidencia es obligatoria")
    @Schema(description = "Puntuacion dada al voto de una incidencia", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer voteScore;

    //Nomes 1 o -1
    @AssertTrue(message = "El valor ha de ser 1 o -1")
    public boolean isVoteScoreValid() {
         return voteScore != null && (voteScore == 1 || voteScore == -1);
    }

}
