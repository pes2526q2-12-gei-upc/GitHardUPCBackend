package com.safesteps.backend.domain.incidents.service;

import com.safesteps.backend.domain.common.exception.BadRequestException;
import com.safesteps.backend.domain.common.exception.ResourceNotFoundException;
import com.safesteps.backend.domain.incidents.dto.IncidentRequestDTO;
import com.safesteps.backend.domain.incidents.dto.IncidentResponseDTO;
import com.safesteps.backend.domain.incidents.model.IncidentStatusEnum;
import com.safesteps.backend.domain.incidents.dto.VoteCountDTO;
import com.safesteps.backend.domain.incidents.model.Incident;
import com.safesteps.backend.domain.incidents.projections.IncidentDBProjection;
import com.safesteps.backend.domain.incidents.projections.VoteCountDBProjection;
import com.safesteps.backend.domain.incidents.repository.IncidentRepository;
import com.safesteps.backend.domain.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class IncidentService {
        private final IncidentRepository incidentRepo;
        private final UserRepository userRepo;
        private static final String INCIDENCE_NOT_FOUND = "Incidencia no trobada amb id ";

        public IncidentService(IncidentRepository incidentRepository, UserRepository userRepo) {
            this.incidentRepo = incidentRepository;
            this.userRepo = userRepo;
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
            i.setGoogleId(req.getGoogleId());
            i.setType(req.getType());
            i.setDescription(req.getDescription());
            if (req.getCoordinates() == null) throw new BadRequestException("Les coordenades no poden ser null.");
            i.setLocation(req.getCoordinates().toPoint());
            i.setStatus(IncidentStatusEnum.PENDING.name());
            i.setPositiveVotes(0);
            i.setNegativeVotes(0);
            i.setReliabilityIndex(0.0);
            i = incidentRepo.save(i);
            return findIncidentById(i.getId());
        }

        public IncidentResponseDTO findIncidentById(Long id){
            Optional<IncidentDBProjection> incident = incidentRepo.findIncidentWithUserById(id);
            if (incident.isEmpty()) throw new ResourceNotFoundException(INCIDENCE_NOT_FOUND + id);
            return new IncidentResponseDTO(incident.get());
        }

        @Transactional
        public IncidentResponseDTO editIncidentById(Long id, IncidentRequestDTO req) {
            Optional<Incident> i = incidentRepo.findById(id);
            if (i.isEmpty()) throw new ResourceNotFoundException(INCIDENCE_NOT_FOUND + id);
            Incident incident = i.get();
            incident.setType(req.getType());
            if (req.getDescription() != null) incident.setDescription(req.getDescription());
            incidentRepo.save(incident);
            return findIncidentById(id);
        }

        public void deleteIncidentById(Long id){
            if (!incidentRepo.existsById(id)) {
                throw new ResourceNotFoundException("No es pot esborrar: Incidencia no trobada");
            }
            incidentRepo.deleteById(id);
        }

        public List<IncidentResponseDTO> getIncidentsByUserId(String googleId) {
            if (!userRepo.existsByGoogleId(googleId)) throw new ResourceNotFoundException("Usuari no trobat amb googleId: " + googleId);
            List<IncidentDBProjection> incidents = incidentRepo.getAllByUserId(googleId);
            List<IncidentResponseDTO> response = new ArrayList<>();
            for (IncidentDBProjection incident : incidents)
                response.add(new IncidentResponseDTO(incident));
            return response;
        }

        public VoteCountDTO getVoteCount(Long id) {
            Optional<VoteCountDBProjection> voteDB = incidentRepo.getVoteCount(id);
            if (voteDB.isEmpty()) throw new ResourceNotFoundException(INCIDENCE_NOT_FOUND + id);
            return new VoteCountDTO(voteDB.get());
        }

        @Transactional
        public void updateIncidentVoteCount(double voteScore, Long incidentId, boolean delete) {
            Optional<Incident> i = incidentRepo.findById(incidentId);
            if (i.isEmpty()) throw new ResourceNotFoundException(INCIDENCE_NOT_FOUND + incidentId);
            Incident incident = i.get();

            if (delete) {
                if (voteScore >= 0) {
                    incident.setPositiveVotes(incident.getPositiveVotes() - 1);
                } else {
                    incident.setNegativeVotes(incident.getNegativeVotes() - 1);
                }
                incident.setReliabilityIndex(incident.getReliabilityIndex() - voteScore);
            } else {
                if (voteScore >= 0) {
                    incident.setPositiveVotes(incident.getPositiveVotes() + 1);
                } else {
                    incident.setNegativeVotes(incident.getNegativeVotes() + 1);
                }

                incident.setReliabilityIndex(incident.getReliabilityIndex() + voteScore);
            }

            incidentRepo.save(incident);
        }

        public void updateIncidentStatus(Long incidentId, IncidentStatusEnum status) {
            Optional<Incident> i = incidentRepo.findById(incidentId);
            if (i.isEmpty()) throw new ResourceNotFoundException(INCIDENCE_NOT_FOUND + incidentId);
            Incident incident = i.get();
            incident.setStatus(status.name());
            incidentRepo.save(incident);
        }

        public String getIncidentCreatorGoogleId(Long id) {
            Optional<Incident> incident = incidentRepo.findById(id);
            if (incident.isEmpty()) throw new  ResourceNotFoundException("Usuari no trobat amb id " + id);
            return incident.get().getGoogleId();
        }
}
