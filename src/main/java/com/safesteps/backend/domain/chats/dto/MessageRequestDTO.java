package com.safesteps.backend.domain.chats.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MessageRequestDTO {

    @NotBlank(message = "El contingut no pot estar buit")
    @Schema(description = "El text del missatge", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @NotBlank(message = "L'ID de Google del remitent és obligatori")
    @Schema(description = "El Google ID de l'usuari que envia el missatge", requiredMode = Schema.RequiredMode.REQUIRED)
    private String senderGoogleId;
}
