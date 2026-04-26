package com.safesteps.backend;

import com.safesteps.backend.domain.incidents.dto.IncidentResponseDTO;
import com.safesteps.backend.domain.incidents.dto.VoteRequestDTO;
import com.safesteps.backend.domain.incidents.dto.VoteResponseDTO;
import com.safesteps.backend.domain.incidents.model.IncidentStatusEnum;
import com.safesteps.backend.domain.incidents.model.Vote;
import com.safesteps.backend.domain.incidents.projections.VoteDBProjection;
import com.safesteps.backend.domain.incidents.repository.IncidentVoteRepository;
import com.safesteps.backend.domain.incidents.service.IncidentService;
import com.safesteps.backend.domain.incidents.service.IncidentVoteService;
import com.safesteps.backend.domain.users.dto.UserResponseDTO;
import com.safesteps.backend.domain.users.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentVoteServiceTest {

    @Mock
    private IncidentVoteRepository voteRepository;

    @Mock
    private IncidentService incidentSv;

    @InjectMocks
    private IncidentVoteService voteService;

    @Mock
    private UserService userSv;


    @Test
    void createVote_OK() {
        UserResponseDTO user = mock(UserResponseDTO.class);
        Long incidentId = 1L;
        VoteRequestDTO req = new VoteRequestDTO();
        req.setVoteScore(1);
        req.setGoogleId("100L");

        // formula: 1 / (1 + (7/7)^4) = 1/2 = 0.5
        IncidentResponseDTO incident = new IncidentResponseDTO();
        incident.setCreatedAt(OffsetDateTime.now().minusDays(7).toLocalDateTime());
        incident.setId(incidentId);
        incident.setReliabilityIndex(1.0);
        when(incidentSv.findIncidentById(incidentId)).thenReturn(incident);
        when(voteRepository.save(any(Vote.class))).thenAnswer(i -> i.getArguments()[0]);
        when(userSv.getUserByGoogleId(any())).thenReturn(user);
        when(user.getReputacio()).thenReturn(1.0);

        VoteResponseDTO result = voteService.createVote(incidentId, req);


        assertNotNull(result);
        assertEquals(0.5, result.getScore(), 0.001);

        verify(voteRepository).save(any(Vote.class));
        verify(incidentSv).updateIncidentVoteCount(0.5, incidentId, false);
    }

    @Test
    void createVote_ZERO() {
        UserResponseDTO user = mock(UserResponseDTO.class);

        Long incidentId = 1L;
        VoteRequestDTO req = new VoteRequestDTO();
        req.setVoteScore(0); // Vot 0
        req.setGoogleId("100L");

        IncidentResponseDTO incident = new IncidentResponseDTO();
        incident.setCreatedAt(OffsetDateTime.now().minusDays(7).toLocalDateTime());

        when(incidentSv.findIncidentById(incidentId)).thenReturn(incident);
        when(userSv.getUserByGoogleId(any())).thenReturn(user);
        when(user.getReputacio()).thenReturn(1.0);

        VoteResponseDTO r = voteService.createVote(incidentId, req);

        verify(voteRepository, never()).save(any());

        assertNotNull(r);
        assertEquals(0.0, r.getScore(), 0.001);
        assertNull(r.getCreatedAt());
        assertNull(r.getIncidenceId());
        assertNull(r.getGoogleId());
        assertNull(r.getId());
    }


    @Test
    void deleteVoteByVoteId_TRUE() {
        Long voteId = 10L;
        Vote vote = new Vote();
        vote.setId(voteId);
        vote.setIncidenceId(1L);
        vote.setScore(0.8);

        when(voteRepository.findById(voteId)).thenReturn(Optional.of(vote));
        IncidentResponseDTO i = new IncidentResponseDTO();
        i.setReliabilityIndex(1.0);
        i.setStatus(IncidentStatusEnum.ACCEPTED.name());
        when(incidentSv.findIncidentById(1L)).thenReturn(i);
        boolean result = voteService.deleteVoteByVoteId(voteId);

        assertTrue(result);
        verify(incidentSv).updateIncidentVoteCount(0.8, 1L, true);
        verify(voteRepository).deleteById(voteId);
    }

    @Test
    void deleteVoteByVoteId_EMPTY() {
        Long voteId = 10L;
        Vote vote = new Vote();
        vote.setId(voteId);
        vote.setIncidenceId(1L);
        vote.setScore(0.0);

        when(voteRepository.findById(voteId)).thenReturn(Optional.empty());

        boolean result = voteService.deleteVoteByVoteId(voteId);

        assertFalse(result);
        verify(voteRepository, never()).deleteById(any());
        verify(incidentSv, never()).updateIncidentVoteCount(0.8, 1L, true);
    }

    @Test
    void deleteByUserAndIncidence_TRUE() {
        Long voteId = 10L;
        Vote vote = new Vote();
        vote.setId(voteId);
        vote.setIncidenceId(1L);
        vote.setScore(0.8);
        when(voteRepository.findByUserAndIncidence(1L, "100L")).thenReturn(Optional.of(vote));
        IncidentResponseDTO i = new IncidentResponseDTO();
        i.setReliabilityIndex(1.0);
        i.setStatus(IncidentStatusEnum.ACCEPTED.name());
        when(incidentSv.findIncidentById(1L)).thenReturn(i);
        boolean result = voteService.deleteByUserAndIncidence(1L, "100L");

        assertTrue(result);
        verify(incidentSv).updateIncidentVoteCount(0.8, 1L, true);
        verify(voteRepository).deleteById(voteId);
    }

    @Test
    void deleteByUserAndIncidence_EMPTY() {
        when(voteRepository.findByUserAndIncidence(1L, "100L")).thenReturn(Optional.empty());

        boolean result = voteService.deleteByUserAndIncidence(1L, "100L");

        assertFalse(result);
        verify(voteRepository, never()).deleteById(any());
        verify(incidentSv, never()).updateIncidentVoteCount(0.8, 1L, true);
    }


    @Test
    void getUserVotes_DTO() {
        String userId = "100L";

        VoteDBProjection p = new VoteDBProjection() {
            @Override public Long getId() { return 1L; }
            @Override public Long getIncidenceId() { return 10L; }
            @Override public String getGoogleId() { return "100L"; }
            @Override public float getScore() { return 0.5f; }
            @Override public LocalDateTime getCreatedAt() { return LocalDateTime.now(); }
        };

        when(voteRepository.findAllByGoogleId(userId)).thenReturn(List.of(p));

        List<VoteResponseDTO> result = voteService.getUserVotes(userId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(0.5, result.getFirst().getScore());
    }

    @Test
    void updateIncidentVoteCount_Zero() {
        voteService.updateIncidentVoteCount(0.0, 1L, false);
        verify(voteRepository, never()).save(any());
    }

    @Test
    void getVoters() {
        VoteDBProjection v = mock(VoteDBProjection.class);
        when(v.getGoogleId()).thenReturn("voter1");
        when(voteRepository.findAllByIncidenceId(1L)).thenReturn(List.of(v));

        List<Vote> result = voteService.getVoters(1L);
        assertEquals(1, result.size());
        assertEquals("voter1", result.get(0).getGoogleId());
    }

    @Test
    void checkValidation_Accepted() {
        IncidentResponseDTO incident = new IncidentResponseDTO();
        incident.setReliabilityIndex(15.0); // Supera el threshold de 10
        incident.setStatus(IncidentStatusEnum.PENDING.name());

        when(incidentSv.findIncidentById(1L)).thenReturn(incident);
        when(incidentSv.getIncidentCreatorGoogleId(1L)).thenReturn("creator1"); // Nuevo mock necesario

        VoteDBProjection vp = mock(VoteDBProjection.class);
        when(voteRepository.findAllByIncidenceId(1L)).thenReturn(List.of(vp));

        voteService.checkValidation(1L);

        verify(userSv).updateUserReliability(anyList(), eq(true), eq("creator1"));
        verify(incidentSv).updateIncidentStatus(1L, IncidentStatusEnum.ACCEPTED);
    }

    @Test
    void checkValidation_Rejected() {
        IncidentResponseDTO incident = new IncidentResponseDTO();
        incident.setReliabilityIndex(-15.0); // Supera el threshold de |-10|
        incident.setStatus(IncidentStatusEnum.PENDING.name());

        when(incidentSv.findIncidentById(1L)).thenReturn(incident);
        when(incidentSv.getIncidentCreatorGoogleId(1L)).thenReturn("creator1");

        voteService.checkValidation(1L);

        verify(userSv).updateUserReliability(anyList(), eq(false), eq("creator1"));
        verify(incidentSv).updateIncidentStatus(1L, IncidentStatusEnum.REJECTED);
    }
}