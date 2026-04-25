package com.safesteps.backend.domain.incidents.dto;

import com.safesteps.backend.domain.incidents.projections.VoteCountDBProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class VoteCountDTO {
    Long id;
    Long positiveVotes;
    Long negativeVotes;
    Double reliabilityIndex;

    public VoteCountDTO(VoteCountDBProjection voteDB) {
        this.id = voteDB.getId();
        this.positiveVotes = voteDB.getPositiveVotes();
        this.negativeVotes = voteDB.getNegativeVotes();
        this.reliabilityIndex = voteDB.getReliabilityIndex();
    }
}
