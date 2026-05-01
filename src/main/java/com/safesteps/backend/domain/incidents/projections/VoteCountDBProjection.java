package com.safesteps.backend.domain.incidents.projections;

public interface VoteCountDBProjection {
    Long getId();
    Long getPositiveVotes();
    Long getNegativeVotes();
    Double getReliabilityIndex();
}
