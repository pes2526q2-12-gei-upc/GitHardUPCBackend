package com.safesteps.backend.domain.incidents.projections;

import java.time.LocalDateTime;

public interface VoteDBProjection {
    Long getId();
    Long getIncidenceId();
    Long getUserId();
    float getScore();
    LocalDateTime getCreatedAt();
}
