package com.safesteps.backend.domain.chats.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class ChatRequestDTO {

    @NotBlank(message = "El tipus de xat és obligatori")
    @Schema(description = "Pot ser PRIVATE o GROUP", requiredMode = Schema.RequiredMode.REQUIRED)
    private String type;

    @Schema(description = "Nom del grup (només necessari si type és GROUP)")
    private String name;

    @NotEmpty(message = "Cal afegir almenys un participant")
    @Schema(description = "Llista de Google IDs dels usuaris a afegir", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> participantGoogleIds;
}
