package com.safesteps.backend.domain.incidents.projections;

import java.time.LocalDateTime;

public interface VoteDBProjection {
    Long getId();
    Long getIncidenceId();
    String getGoogleId();
    float getScore();
    LocalDateTime getCreatedAt();
}
