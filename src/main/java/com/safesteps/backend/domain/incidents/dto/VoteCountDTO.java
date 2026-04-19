package com.safesteps.backend.domain.incidents.dto;

import com.safesteps.backend.domain.incidents.projections.VoteCountDBProjection;
import lombok.Data;

@Data
public class VoteCountDTO {
    Long id;
    Long positiveVotes;
    Long negativeVotes;
    Long reliabilityIndex;

    public VoteCountDTO(VoteCountDBProjection voteDB) {
        this.id = voteDB.getId();
        this.positiveVotes = voteDB.getPositiveVotes();
        this.negativeVotes = voteDB.getNegativeVotes();
        this.reliabilityIndex = voteDB.getReliabilityIndex();
    }
}
