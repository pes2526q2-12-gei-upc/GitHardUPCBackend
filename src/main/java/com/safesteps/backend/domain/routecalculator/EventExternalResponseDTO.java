package com.safesteps.backend.domain.routecalculator;

import lombok.Data;
import java.util.List;

// 1. La classe principal (PÚBLICA perquè coincideix amb el nom del fitxer)
@Data
public class EventExternalResponseDTO {
    private List<EventDTO> results;
}

@Data
class EventDTO {
    private String denomination;
    private Double latitude;
    private Double longitude;
    private Boolean free;
    private List<CategoryDTO> categories;
}

@Data
class CategoryDTO {
    private String name;
}