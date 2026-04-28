package com.safesteps.backend.controller;

import com.safesteps.backend.domain.incidents.dto.*;
import com.safesteps.backend.domain.incidents.service.IncidentService;
import com.safesteps.backend.domain.incidents.service.IncidentVoteService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/incidents")
@Tag(name = "Incidencias", description = "Gestión de las incidencias")
public class IncidentController {

    private final IncidentService incidentService;
    private final IncidentVoteService voteService;

    public IncidentController(IncidentService incidentService,  IncidentVoteService voteService) {
        this.incidentService = incidentService;
        this.voteService = voteService;
    }

    @GetMapping
    public ResponseEntity<List<IncidentResponseDTO>> getIncidents() {
        return ResponseEntity.ok(incidentService.getIncidents());
    }

    @PostMapping
    public ResponseEntity<IncidentResponseDTO> createIncident(@Valid @RequestBody IncidentRequestDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(incidentService.createIncident(req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentResponseDTO> getIncident(@PathVariable Long id) {
        IncidentResponseDTO i = incidentService.findIncidentById(id);
        return ResponseEntity.ok(i);
    }

    @PutMapping("/{id}")
    public ResponseEntity<IncidentResponseDTO> editIncident(@PathVariable Long id, @Valid @RequestBody IncidentRequestDTO request) {
        IncidentResponseDTO incidentResponseDTO = incidentService.editIncidentById(id, request);
        return ResponseEntity.ok(incidentResponseDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIncident(@PathVariable Long id) {
        incidentService.deleteIncidentById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/count-votes")
    public ResponseEntity<VoteCountDTO> getInfoVotes(@PathVariable("id") Long incidentId) {
        VoteCountDTO count = incidentService.getVoteCount(incidentId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/users/{googleId}")
    public ResponseEntity<List<IncidentResponseDTO>> getUserIncidents(@PathVariable String googleId) {
        return ResponseEntity.ok(incidentService.getIncidentsByUserId(googleId));
    }



    @PostMapping("/{id}/votes")
    public ResponseEntity<VoteResponseDTO> newVote(@PathVariable("id") Long incidenceId, @Valid @RequestBody VoteRequestDTO voteRequest) {
        VoteResponseDTO updatedIncident = voteService.createVote(incidenceId, voteRequest);
        return ResponseEntity.ok(updatedIncident);
    }

    @DeleteMapping("/votes/{voteId}")
    public ResponseEntity<Void> deleteVoteByVoteId(@PathVariable("voteId") Long voteId) {
        voteService.deleteVoteByVoteId(voteId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{incidenceId}/users/{googleId}/vote")
    public ResponseEntity<Void> deleteByUserAndIncidence(@PathVariable("incidenceId") Long incidenceId, @PathVariable("googleId") String googleId) {
        voteService.deleteByUserAndIncidence(incidenceId, googleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/votes/users/{googleId}")
    public ResponseEntity<List<VoteResponseDTO>> getUserVotes(@PathVariable String googleId) {
        return ResponseEntity.ok(voteService.getUserVotes(googleId));
    }
}