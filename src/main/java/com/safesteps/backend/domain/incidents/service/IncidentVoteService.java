package com.safesteps.backend.domain.incidents.service;

import com.safesteps.backend.domain.common.exception.ResourceNotFoundException;
import com.safesteps.backend.domain.incidents.dto.IncidentResponseDTO;
import com.safesteps.backend.domain.incidents.dto.VoteRequestDTO;
import com.safesteps.backend.domain.incidents.dto.VoteResponseDTO;
import com.safesteps.backend.domain.incidents.model.IncidentStatusEnum;
import com.safesteps.backend.domain.incidents.model.Vote;
import com.safesteps.backend.domain.incidents.projections.VoteDBProjection;
import com.safesteps.backend.domain.incidents.repository.*;
import com.safesteps.backend.domain.users.dto.UserResponseDTO;
import com.safesteps.backend.domain.users.service.UserService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.lang.Math.abs;

@Service
public class IncidentVoteService {
    private final IncidentVoteRepository voteRepository;
    private final IncidentService incidentSv;
    private final UserService userSv;
    private static final int THRESHOLD = 10;

    public IncidentVoteService(IncidentVoteRepository voteRepository,  IncidentService incidentSv, UserService userSv) {
        this.voteRepository = voteRepository;
        this.incidentSv = incidentSv;
        this.userSv = userSv;
    }

    @Transactional
    public VoteResponseDTO createVote(Long id, VoteRequestDTO vote) {
        IncidentResponseDTO incident = incidentSv.findIncidentById(id);
        LocalDateTime now = LocalDateTime.now();

        double diffDays = ChronoUnit.DAYS.between(incident.getCreatedAt().toLocalDate(), now.toLocalDate());
        diffDays = Math.max(0, diffDays); // Extraído de develop: protege contra días negativos
        double timeFactor = 1.0 / (1.0 + Math.pow(diffDays / 7.0, 4));

        UserResponseDTO user = userSv.getUserByGoogleId(vote.getGoogleId());
        Vote v = new Vote();
        v.setIncidenceId(id);
        v.setGoogleId(vote.getGoogleId());
        v.setReliability(user.getReputacio());
        v.setDataScore(timeFactor);

        double score = v.getReliability() * v.getDataScore() * vote.getVoteScore();
        v.setScore(score);

        if (score != 0) {
            v = voteRepository.save(v);
            this.updateIncidentVoteCount(score, id, false);
            checkValidation(id); // Extraído de develop: vital para que funcione el cambio de estado
            return new VoteResponseDTO(v);
        }

        return new VoteResponseDTO();
    }

    @Transactional
    public boolean deleteVoteByVoteId(Long voteId) {
        Optional<Vote> vote = voteRepository.findById(voteId);

        if (vote.isPresent()) {
            Vote v = vote.get();
            IncidentResponseDTO incident = incidentSv.findIncidentById(v.getIncidenceId());

            if (incident.getStatus().equals(IncidentStatusEnum.PENDING.name())) {
                this.updateIncidentVoteCount(v.getScore(), v.getIncidenceId(), true);
                voteRepository.deleteById(voteId);
                return true;
            } else if (incident.getStatus().equals(IncidentStatusEnum.ACCEPTED.name())) {
                incidentSv.updateExpirationIndex(-v.getScore(), v.getIncidenceId());
                voteRepository.deleteById(voteId);
                return true;
            }
        }
        return false;
    }

    @Transactional
    public boolean deleteByUserAndIncidence(Long incidenceId, String googleId) {
        Optional<Vote> vote = voteRepository.findByUserAndIncidence(incidenceId, googleId);

        if (vote.isPresent()) {
            Vote v = vote.get();
            IncidentResponseDTO incident = incidentSv.findIncidentById(incidenceId);
            if (incident.getStatus().equals(IncidentStatusEnum.PENDING.name())) {
                this.updateIncidentVoteCount(v.getScore(), incidenceId, true);
                voteRepository.deleteById(v.getId());
                return true;
            } else if (incident.getStatus().equals(IncidentStatusEnum.ACCEPTED.name())) {
                incidentSv.updateExpirationIndex(-v.getScore(), incidenceId);
                voteRepository.deleteById(v.getId());
                return true;
            }
        }
        return false;
    }

    public List<VoteResponseDTO> getUserVotes(String googleId) {
        userSv.getUserByGoogleId(googleId); // Extraído de develop: verifica que el usuario existe
        List<VoteDBProjection> votes = voteRepository.findAllByGoogleId(googleId);
        List<VoteResponseDTO> result = new ArrayList<>();
        for (VoteDBProjection v : votes) {
            result.add(new VoteResponseDTO(v));
        }
        return result;
    }

    public List<Vote> getVoters(Long incidentId) {
        IncidentResponseDTO i =  incidentSv.findIncidentById(incidentId);
        if (i == null) throw new  ResourceNotFoundException("Incidencia no trobada amb id: " + incidentId);
        List<VoteDBProjection> votes = voteRepository.findAllByIncidenceId(incidentId);
        List<Vote> result = new ArrayList<>();
        for (VoteDBProjection v : votes) {
            result.add(new Vote(v));
        }
        return result;
    }

    @Transactional
    public void updateIncidentVoteCount(double voteScore, Long incidentId, boolean delete) {
        incidentSv.updateIncidentVoteCount(voteScore, incidentId, delete);
    }

    //Checks if incidence has surpassed the validation threshold and updates its status if necessary
    @Transactional
    public void checkValidation(Long incidentId) {
        IncidentResponseDTO i = incidentSv.findIncidentById(incidentId);
        double iReliability = i.getReliabilityIndex();

        if (abs(iReliability) >= THRESHOLD && i.getStatus().equals(IncidentStatusEnum.PENDING.name())) {
            List<Vote> users = this.getVoters(incidentId);
            boolean isAccepted = iReliability > 0;

            String creatorGoogleId = incidentSv.getIncidentCreatorGoogleId(incidentId);

            userSv.updateUserReliability(users, isAccepted, creatorGoogleId);

            voteRepository.deleteAll(users);

            IncidentStatusEnum stat = isAccepted ? IncidentStatusEnum.ACCEPTED : IncidentStatusEnum.REJECTED;
            incidentSv.updateIncidentStatus(incidentId, stat);
        }
    }

    @Transactional
    public VoteResponseDTO createResolutionVote(Long id, VoteRequestDTO voteDto) {
        IncidentResponseDTO incident = incidentSv.findIncidentById(id);

        if (incident == null || !incident.getStatus().equals(IncidentStatusEnum.ACCEPTED.name())) {
            return new VoteResponseDTO();
        }

        long days = ChronoUnit.DAYS.between(incident.getCreatedAt().toLocalDate(), LocalDateTime.now().toLocalDate());
        double timeFactor = (2.0 * days + (0.5 * 10.0)) / (days + 10.0);

        UserResponseDTO user = userSv.getUserByGoogleId(voteDto.getGoogleId());

        Vote v = new Vote();
        v.setIncidenceId(id);
        v.setGoogleId(voteDto.getGoogleId());
        v.setReliability(user.getReputacio());
        v.setDataScore(timeFactor);

        double finalScore = v.getReliability() * timeFactor * voteDto.getVoteScore();
        v.setScore(finalScore);

        if (finalScore != 0) {
            v = voteRepository.save(v);
            incidentSv.updateExpirationIndex(finalScore, id);
            return new VoteResponseDTO(v);
        }
        return new VoteResponseDTO();
    }
}