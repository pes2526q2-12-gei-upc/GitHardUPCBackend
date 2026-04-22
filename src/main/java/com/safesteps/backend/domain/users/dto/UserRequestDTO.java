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
    @Schema(description = "Email del usuario", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Schema(description = "Nombre de usuario", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Schema(description = "ID de Google (opcional)")
    private String googleId;

    @Schema(description = "URL de la foto")
    private String pictureUrl;

    @Schema(description = "Idioma")
    private String language;

    @NotNull
    private Boolean isAnonymous;
}