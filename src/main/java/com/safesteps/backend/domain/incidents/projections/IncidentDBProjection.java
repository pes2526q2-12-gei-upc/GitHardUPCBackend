package com.safesteps.backend.domain.incidents.projections;

import org.geolatte.geom.G2D;
import org.geolatte.geom.Point;

import java.time.LocalDateTime;

public interface IncidentDBProjection {
    Long getId();
    String getUsername();
    Long getUserLevel();
    String getType();
    String getDescription();
    Point<G2D> getLocation();
    Long getPositiveVotes();
    Long getNegativeVotes();
    Double getReliabilityIndex();
    Double getExpirationIndex();
    String getStatus();
    LocalDateTime getCreated();
    LocalDateTime getUpdated();
}
