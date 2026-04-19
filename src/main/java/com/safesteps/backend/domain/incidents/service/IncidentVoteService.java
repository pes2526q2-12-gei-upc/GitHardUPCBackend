package com.safesteps.backend.domain.incidents.service;

import com.safesteps.backend.domain.incidents.dto.VoteRequestDTO;
import com.safesteps.backend.domain.incidents.dto.VoteResponseDTO;
import com.safesteps.backend.domain.incidents.model.Vote;
import com.safesteps.backend.domain.incidents.projections.VoteDBProjection;
import com.safesteps.backend.domain.incidents.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Service
public class IncidentVoteService {
    private final IncidentVoteRepository voteRepository;
    private final IncidentService incidentSv;

    public IncidentVoteService(IncidentVoteRepository voteRepository,  IncidentService incidentSv) {
        this.voteRepository = voteRepository;
        this.incidentSv = incidentSv;
    }

    public VoteResponseDTO createVote(Long id, VoteRequestDTO vote) {
        Vote v = new Vote();
        v.setIncidenceId(id);
        v.setUserId(vote.getUserId());
        v.setScore(vote.getScore());
        v = voteRepository.save(v);
        updateIncidentVoteCount(vote.getScore(), id);
        return new VoteResponseDTO(v);
    }

    public boolean deleteVoteByVoteId(Long id){
        return deleteVote(voteRepository.findById(id));
    }

    public boolean deleteByUserAndIncidence(Long incidenceId, Long userId){
        return deleteVote(voteRepository.findByUserAndIncidence(incidenceId, userId));
    }

    @Transactional
    public boolean deleteVote(Optional<Vote> v) {
        if (v.isEmpty()) return false;
        updateIncidentVoteCount(-(int) (v.get().getScore()), v.get().getIncidenceId());
        voteRepository.deleteById(v.get().getId());
        return true;
    }

    public List<VoteResponseDTO> getUserVotes(Long userId) {
        List<VoteDBProjection> votes = voteRepository.findAllByUserId(userId);
        List<VoteResponseDTO> result = new ArrayList<>();
        for (VoteDBProjection v : votes) {
            result.add(new VoteResponseDTO(v));
        }
        return result;
    }

    private void updateIncidentVoteCount(int voteScore, Long incidentId) {
        incidentSv.updateIncidentVoteCount(voteScore, incidentId);
    }
}
