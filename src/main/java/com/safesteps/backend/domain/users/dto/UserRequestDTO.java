package com.safesteps.backend.domain.users.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserRequestDTO {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Formato de email inválido")
    private String email;

    @NotBlank(message = "El nombre de usuario es obligatorio")
    private String username;

    @NotBlank(message = "El Google ID es obligatorio")
    @Schema(description = "ID de Google proporcionado por el login", requiredMode = Schema.RequiredMode.REQUIRED)
    private String googleId;

    private String pictureUrl;
    private String language;

    @NotNull
    private Boolean isAnonymous;
}