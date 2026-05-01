package com.safesteps.backend;

import com.safesteps.backend.domain.incidents.dto.VoteCountDTO;
import com.safesteps.backend.domain.incidents.dto.VoteResponseDTO;
import com.safesteps.backend.domain.incidents.model.Vote;
import com.safesteps.backend.domain.incidents.projections.VoteCountDBProjection;
import com.safesteps.backend.domain.incidents.projections.VoteDBProjection;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VoteDTOTest {

    @Test
    void checkVoteProjToVoteCount() {
        VoteCountDBProjection p = new VoteCountDBProjection() {
            @Override public Long getId() { return 1L; }
            @Override public Long getPositiveVotes() { return 10L; }
            @Override public Long getNegativeVotes() { return 2L; }
            @Override public Double getReliabilityIndex() { return 0.45; }
        };
        VoteCountDTO r = new VoteCountDTO(p);
        assertEquals(1L, r.getId());
        assertEquals(10L, r.getPositiveVotes());
        assertEquals(2L, r.getNegativeVotes());
        assertEquals(0.45, r.getReliabilityIndex());
    }

    @Test
    void checkVoteProjToVoteResponse() {
        LocalDateTime now = LocalDateTime.now();
        VoteDBProjection p = new VoteDBProjection() {
            @Override public Long getId() { return 1L; }
            @Override public Long getIncidenceId() { return 10L; }
            @Override public String getGoogleId() { return "100L"; }
            @Override public float getScore() { return 1f; }
            @Override public LocalDateTime getCreatedAt() { return now; }
        };

        VoteResponseDTO r = new VoteResponseDTO(p);
        assertEquals(1L, r.getId());
        assertEquals(10L, r.getIncidenceId());
        assertEquals("100L", r.getGoogleId());
        assertEquals(1f, r.getScore());
        assertEquals(now, r.getCreatedAt());
    }

    @Test
    void checkVoteToVoteResponse() {
        Vote v =  new Vote();
        v.setId(1L);
        v.setIncidenceId(2L);
        v.setGoogleId("3L");
        v.setScore(-1f);
        LocalDateTime createdAt = LocalDateTime.now();
        v.setCreatedAt(createdAt);
        VoteResponseDTO r = new VoteResponseDTO(v);

        assertEquals(1L, r.getId());
        assertEquals(2L, r.getIncidenceId());
        assertEquals("3L", r.getGoogleId());
        assertEquals(-1f, r.getScore());
        assertEquals(createdAt, r.getCreatedAt());
    }

    @Test
    void checkVoteProjToVote() {
        LocalDateTime createdAt = LocalDateTime.now();
        VoteDBProjection p = new VoteDBProjection() {
            @Override public Long getId() { return 1L; }
            @Override public Long getIncidenceId() { return 2L; }
            @Override public String getGoogleId() { return "100L"; }
            @Override public float getScore() { return 1f; }
            @Override public LocalDateTime getCreatedAt() { return createdAt; }
        };
        Vote v =  new Vote(p);
        assertEquals(1L, v.getId());
        assertEquals(2L, v.getIncidenceId());
        assertEquals("100L", v.getGoogleId());
        assertEquals(1f, v.getScore());
        assertEquals(createdAt, v.getCreatedAt());
    }
}
