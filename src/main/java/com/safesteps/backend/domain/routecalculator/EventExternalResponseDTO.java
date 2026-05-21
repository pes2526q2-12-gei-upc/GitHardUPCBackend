package com.safesteps.backend.domain.routecalculator;

import lombok.Data;
import java.util.List;

// 1. La classe principal (PÚBLICA perquè coincideix amb el nom del fitxer)
@Data
public class EventExternalResponseDTO {

    private List<EventDTO> results;

    // Ara les fiquem A DINS de la classe principal i les fem 'public static'

    @Data
    public static class EventDTO {
        private String denomination;
        private Double latitude;
        private Double longitude;
        private String description;
    }

    @Data
    public static class CategoryDTO {
        private String name;
    }
}