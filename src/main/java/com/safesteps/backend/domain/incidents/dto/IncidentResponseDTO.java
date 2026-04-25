package com.safesteps.backend.domain.incidents.dto;

import com.safesteps.backend.domain.incidents.projections.IncidentDBProjection;
import com.safesteps.backend.domain.routecalculator.Coord;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IncidentResponseDTO {
    private Long id;
    private String type;
    private String description;

    private Coord coordinates;

    private Integer positiveVotes;
    private Integer negativeVotes;
    private Double reliabilityIndex;
    private String status;

    private String authorName;
    private Long authorLevel;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public IncidentResponseDTO(IncidentDBProjection in){
        if (in == null) return;
        this.id = in.getId();
        this.type = in.getType();
        this.description = in.getDescription();
        this.coordinates = new Coord();
        if (in.getLocation() != null) {
            this.coordinates.setLat(in.getLocation().getPosition().getCoordinate(1)); // Y = Latitud
            this.coordinates.setLon(in.getLocation().getPosition().getCoordinate(0)); // X = Longitud
        }
        this.positiveVotes = (in.getPositiveVotes() != null) ? in.getPositiveVotes().intValue() : 0;
        this.negativeVotes = (in.getNegativeVotes() != null) ? in.getNegativeVotes().intValue() : 0;
        this.reliabilityIndex = in.getReliabilityIndex();
        this.status = in.getStatus();
        this.authorName = in.getUsername();
        this.authorLevel = in.getUserLevel();
        this.createdAt = in.getCreated();
        this.updatedAt = in.getUpdated();
    }
}
