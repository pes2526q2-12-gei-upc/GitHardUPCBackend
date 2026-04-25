package com.safesteps.backend.domain.incidents.service;

import com.safesteps.backend.domain.incidents.dto.IncidentResponseDTO;
import com.safesteps.backend.domain.incidents.dto.VoteRequestDTO;
import com.safesteps.backend.domain.incidents.dto.VoteResponseDTO;
import com.safesteps.backend.domain.incidents.model.Vote;
import com.safesteps.backend.domain.incidents.projections.VoteDBProjection;
import com.safesteps.backend.domain.incidents.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.lang.Math.pow;


@Service
public class IncidentVoteService {
    private final IncidentVoteRepository voteRepository;
    private final IncidentService incidentSv;

    public IncidentVoteService(IncidentVoteRepository voteRepository,  IncidentService incidentSv) {
        this.voteRepository = voteRepository;
        this.incidentSv = incidentSv;
    }

    @Transactional
    public VoteResponseDTO createVote(Long id, VoteRequestDTO vote) {
        IncidentResponseDTO i = incidentSv.findIncidentById(id);

        LocalDateTime now = LocalDateTime.now();

        double diffDays = ChronoUnit.DAYS.between(i.getCreatedAt().toLocalDate(), now.toLocalDate());
        diffDays = Math.max(0, diffDays);
        double scoreDay = 1.0 / (1.0 + pow(diffDays/7, 4));

        Vote v = new Vote();
        v.setIncidenceId(id);
        v.setGoogleId(vote.getGoogleId());
        //AL TENIR EL MODEL D'USUARI POSAR LA SEVA FIABILITAT !!
        v.setReliability(1.0);
        v.setDataScore(scoreDay);

        double score = v.getReliability() * v.getDataScore() * vote.getVoteScore();
        v.setScore(score);

        if (score != 0) {
            v = voteRepository.save(v);
            updateIncidentVoteCount(score, id, false);
            return new VoteResponseDTO(v);
        }
        return new VoteResponseDTO();
    }

    @Transactional
    public boolean deleteVoteByVoteId(Long id){
        return deleteVote(voteRepository.findById(id));
    }

    @Transactional
    public boolean deleteByUserAndIncidence(Long incidenceId, String googleId){
        return deleteVote(voteRepository.findByUserAndIncidence(incidenceId, googleId));
    }

    @Transactional
    public boolean deleteVote(Optional<Vote> v) {
        if (v.isEmpty()) return false;
        Vote vote = v.get();
        updateIncidentVoteCount(vote.getScore(), vote.getIncidenceId(), true);
        voteRepository.deleteById(v.get().getId());
        return true;
    }

    public List<VoteResponseDTO> getUserVotes(String googleId) {
        List<VoteDBProjection> votes = voteRepository.findAllByGoogleId(googleId);
        List<VoteResponseDTO> result = new ArrayList<>();
        for (VoteDBProjection v : votes) {
            result.add(new VoteResponseDTO(v));
        }
        return result;
    }

    @Transactional
    public void updateIncidentVoteCount(double voteScore, Long incidentId, boolean delete) {
        if (voteScore != 0) incidentSv.updateIncidentVoteCount(voteScore, incidentId, delete);
    }
}
