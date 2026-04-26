package com.safesteps.backend.domain.routecalculator;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Objeto de respuesta con la evaluación de seguridad de la ruta solicitada.")
public class ExternalSafetyResponseDTO {

    @Schema(description = "Índice global de seguridad de la ruta, calculado en base a factores ambientales y criminalidad.",
            example = "7.5")
    private double safetyIndex;

    @Schema(description = "Clasificación textual del nivel de seguridad.",
            example = "ALTA",
            allowableValues = {"ALTA", "MODERADA", "BAIXA"})
    private String safetyLevel;

    public ExternalSafetyResponseDTO(double safetyIndex, String safetyLevel) {
        this.safetyIndex = safetyIndex;
        this.safetyLevel = safetyLevel;
    }

    public double getSafetyIndex() {
        return safetyIndex;
    }

    public void setSafetyIndex(double safetyIndex) {
        this.safetyIndex = safetyIndex;
    }

    public String getSafetyLevel() {
        return safetyLevel;
    }

    public void setSafetyLevel(String safetyLevel) {
        this.safetyLevel = safetyLevel;
    }
}
