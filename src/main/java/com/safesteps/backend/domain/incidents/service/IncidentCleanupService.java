package com.safesteps.backend.domain.incidents.service;

import com.safesteps.backend.domain.incidents.model.Incident;
import com.safesteps.backend.domain.incidents.model.Vote;
import com.safesteps.backend.domain.incidents.repository.IncidentRepository;
import com.safesteps.backend.domain.incidents.repository.IncidentVoteRepository;
import com.safesteps.backend.domain.users.service.UserService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class IncidentCleanupService {

    private final IncidentRepository incidentRepository;
    private final IncidentVoteRepository voteRepository;
    private final IncidentVoteService voteService;
    private final UserService userService;

    public IncidentCleanupService(IncidentRepository incidentRepository,
                                  IncidentVoteRepository voteRepository,
                                  IncidentVoteService voteService,
                                  UserService userService) {
        this.incidentRepository = incidentRepository;
        this.voteRepository = voteRepository;
        this.voteService = voteService;
        this.userService = userService;
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void cleanupOldIncidents() {
        OffsetDateTime limitDate = OffsetDateTime.now().minusDays(14);
        List<Incident> toDelete = incidentRepository.findIncidentsOlderThan(limitDate);

        for (Incident incident : toDelete) {
            List<Vote> voters = voteService.getVoters(incident.getId());
            userService.rewardForExpiredIncident(voters, incident.getGoogleId());
        }
        voteRepository.deleteVotesFromOldIncidents(limitDate);
        incidentRepository.deleteIncidentsOlderThan(limitDate);
    }
}