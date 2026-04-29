package com.safesteps.backend.domain.users.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteCompletionResponseDTO {
    private Long level;
    private boolean levelUpdated;
    private Long pointsAdded;
    private Long totalPoints;
    private Long recompenses;
}
