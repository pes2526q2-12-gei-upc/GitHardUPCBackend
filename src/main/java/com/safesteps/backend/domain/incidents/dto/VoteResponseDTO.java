package com.safesteps.backend.domain.incidents.dto;

import com.safesteps.backend.domain.incidents.model.Vote;
import com.safesteps.backend.domain.incidents.projections.VoteDBProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@Data
public class VoteResponseDTO {
    private Long id;
    private Long incidenceId;
    private Long userId;
    private double score;
    private LocalDateTime createdAt;

    public VoteResponseDTO(Vote v) {
        this.id = v.getId();
        this.incidenceId = v.getIncidenceId();
        this.userId = v.getUserId();
        this.score = v.getScore();
        this.createdAt = v.getCreatedAt();
    }

    public VoteResponseDTO(VoteDBProjection v) {
        this.id = v.getId();
        this.incidenceId = v.getIncidenceId();
        this.userId = v.getUserId();
        this.score = v.getScore();
        this.createdAt = v.getCreatedAt();
    }
}
