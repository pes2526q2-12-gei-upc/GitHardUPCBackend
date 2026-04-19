package com.safesteps.backend.domain.incidents.projections;

import java.time.LocalDateTime;

public interface VoteDBProjection {
    Long getId();
    Long getIdIncidence();
    Long getIdUser();
    float getScore();
    LocalDateTime getCreatedAt();
}
