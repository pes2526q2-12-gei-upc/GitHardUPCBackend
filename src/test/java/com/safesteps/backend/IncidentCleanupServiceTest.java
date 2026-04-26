package com.safesteps.backend;

import com.safesteps.backend.domain.incidents.model.Incident;
import com.safesteps.backend.domain.incidents.model.Vote;
import com.safesteps.backend.domain.incidents.repository.IncidentRepository;
import com.safesteps.backend.domain.incidents.repository.IncidentVoteRepository;
import com.safesteps.backend.domain.incidents.service.IncidentCleanupService;
import com.safesteps.backend.domain.incidents.service.IncidentVoteService;
import com.safesteps.backend.domain.users.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentCleanupServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private IncidentVoteRepository voteRepository;

    @Mock
    private IncidentVoteService voteService;

    @Mock
    private UserService userService;

    @InjectMocks
    private IncidentCleanupService cleanupService;

    @Test
    void cleanupOldIncidents_Success() {
        Incident i = new Incident();
        i.setId(1L);
        i.setGoogleId("creator1");

        Vote v = new Vote();
        v.setGoogleId("voter1");
        when(incidentRepository.findIncidentsOlderThan(any(OffsetDateTime.class))).thenReturn(List.of(i));
        when(voteService.getVoters(1L)).thenReturn(List.of(v));
        cleanupService.cleanupOldIncidents();
        verify(userService).rewardForExpiredIncident(List.of(v), "creator1");
        verify(voteRepository).deleteVotesFromOldIncidents(any(OffsetDateTime.class));
        verify(incidentRepository).deleteIncidentsOlderThan(any(OffsetDateTime.class));
    }
}
