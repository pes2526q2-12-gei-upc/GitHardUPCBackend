package com.safesteps.backend;

import com.safesteps.backend.domain.incidents.dto.VoteRequestDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VoteRequestDTOTest {

    @Test
    void testGettersAndSetters() {
        VoteRequestDTO dto = new VoteRequestDTO();
        dto.setGoogleId("12345");
        dto.setVoteScore(1);

        assertEquals("12345", dto.getGoogleId());
        assertEquals(1, dto.getVoteScore());
    }

    @Test
    void testIsVoteScoreValid() {
        VoteRequestDTO dto = new VoteRequestDTO();
        
        // Test null
        dto.setVoteScore(null);
        assertFalse(dto.isVoteScoreValid());

        // Test valid scores
        dto.setVoteScore(1);
        assertTrue(dto.isVoteScoreValid());

        dto.setVoteScore(-1);
        assertTrue(dto.isVoteScoreValid());

        // Test invalid scores
        dto.setVoteScore(0);
        assertFalse(dto.isVoteScoreValid());

        dto.setVoteScore(2);
        assertFalse(dto.isVoteScoreValid());

        dto.setVoteScore(-2);
        assertFalse(dto.isVoteScoreValid());
    }

    @Test
    void testEqualsAndHashCode() {
        VoteRequestDTO dto1 = new VoteRequestDTO();
        dto1.setGoogleId("123");
        dto1.setVoteScore(1);
        
        VoteRequestDTO dto2 = new VoteRequestDTO();
        dto2.setGoogleId("123");
        dto2.setVoteScore(1);
        
        VoteRequestDTO dto3 = new VoteRequestDTO();
        dto3.setGoogleId("456");
        dto3.setVoteScore(-1);
        
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
        assertNotEquals(dto1, dto3);
    }
}
