package com.safesteps.backend.domain.incidents.service;

import com.safesteps.backend.domain.incidents.dto.IncidentRequestDTO;
import com.safesteps.backend.domain.incidents.dto.IncidentResponseDTO;
import com.safesteps.backend.domain.incidents.dto.VoteCountDTO;
import com.safesteps.backend.domain.incidents.model.Incident;
import com.safesteps.backend.domain.incidents.projections.IncidentDBProjection;
import com.safesteps.backend.domain.incidents.projections.VoteCountDBProjection;
import com.safesteps.backend.domain.incidents.repository.IncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class IncidentService {
        private final IncidentRepository incidentRepo;

        public IncidentService(IncidentRepository incidentRepository) {
            this.incidentRepo = incidentRepository;
        }

        public List<IncidentResponseDTO> getIncidents() {
            List<IncidentDBProjection> incidents = incidentRepo.all();
            List<IncidentResponseDTO> response = new ArrayList<>();
            for (IncidentDBProjection incident : incidents)
                response.add(new IncidentResponseDTO(incident));
            return response;
        }

        @Transactional
        public IncidentResponseDTO createIncident(IncidentRequestDTO req) {
            Incident i = new Incident();
            i.setUserId(req.getUserId());
            i.setType(req.getType());
            i.setDescription(req.getDescription());
            i.setLocation(req.getCoordinates().toPoint());
            i.setStatus("pending");
            i.setPositiveVotes(0);
            i.setNegativeVotes(0);
            i.setReliabilityIndex(0.0);
            i = incidentRepo.save(i);
            return findIncidentById(i.getId());
        }

        public IncidentResponseDTO findIncidentById(Long id){
            Optional<IncidentDBProjection> incident = incidentRepo.findIncidentWithUserById(id);
            //Retornar Exception
            if (incident.isEmpty()) return null;
            return new IncidentResponseDTO(incident.get());
        }

        public IncidentResponseDTO editIncidentById(Long id, IncidentRequestDTO req) {
            Optional<Incident> i = incidentRepo.findById(id);
            //Retornar exception
            if (i.isEmpty()) return null;
            Incident incident = i.get();
            incident.setType(req.getType());
            incident.setDescription(req.getDescription());
            incidentRepo.save(incident);
            return findIncidentById(id);
        }

        public boolean deleteIncidentById(Long id){
            if (!incidentRepo.existsById(id)) return false;
            incidentRepo.deleteById(id);
            return true;
        }

        public List<IncidentResponseDTO> getIncidentsByUserId(Long userId) {
            List<IncidentDBProjection> incidents = incidentRepo.getAllByUserId(userId);
            List<IncidentResponseDTO> response = new ArrayList<>();
            for (IncidentDBProjection incident : incidents)
                response.add(new IncidentResponseDTO(incident));
            return response;
        }

        public VoteCountDTO getVoteCount(Long id) {
            Optional<VoteCountDBProjection> voteDB = incidentRepo.getVoteCount(id);
            //Retornar exception
            if (voteDB.isEmpty()) return null;
            return new VoteCountDTO(voteDB.get());
        }

        @Transactional
        public void updateIncidentVoteCount(int voteScore, Long incidentId) {
            Optional<Incident> i = incidentRepo.findById(incidentId);
            if (i.isEmpty()) return;
            Incident incident = i.get();

            if (voteScore > 0) {
                incident.setPositiveVotes(incident.getPositiveVotes() + voteScore);
            } else {
                incident.setNegativeVotes(incident.getNegativeVotes() + voteScore);
            }
            int totalVotes = incident.getPositiveVotes() - incident.getNegativeVotes();
            if (totalVotes > 0) {
                double reliabilityIndex = (double) incident.getPositiveVotes() / totalVotes;
                incident.setReliabilityIndex(reliabilityIndex);
            }
            incidentRepo.save(incident);
        }
}
