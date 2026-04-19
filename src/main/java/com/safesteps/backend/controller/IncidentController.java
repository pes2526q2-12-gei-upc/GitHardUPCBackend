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
        if (i == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(i);
    }

    @PutMapping("/{id}")
    public ResponseEntity<IncidentResponseDTO> editIncident(@PathVariable Long id, @Valid @RequestBody IncidentRequestDTO request) {
        IncidentResponseDTO incidentResponseDTO = incidentService.editIncidentById(id, request);
        if (incidentResponseDTO != null) return ResponseEntity.ok(incidentResponseDTO);
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIncident(@PathVariable Long id) {
        boolean deleteStatus = incidentService.deleteIncidentById(id);
        if (deleteStatus) return ResponseEntity.noContent().build();
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/count-votes")
    public ResponseEntity<VoteCountDTO> getInfoVotes(@PathVariable("id") Long incidentId) {
        VoteCountDTO count = incidentService.getVoteCount(incidentId);
        if (count == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<IncidentResponseDTO>> getUserIncidents(@PathVariable Long userId) {
        return ResponseEntity.ok(incidentService.getIncidentsByUserId(userId));
    }



    @PostMapping("/{id}/votes")
    public ResponseEntity<VoteResponseDTO> newVote(@PathVariable("id") Long incidenceId, @Valid @RequestBody VoteRequestDTO voteRequest) {
        //Request user reliability
        VoteResponseDTO updatedIncident = voteService.createVote(incidenceId, voteRequest);
        return ResponseEntity.ok(updatedIncident);
    }

    @DeleteMapping("/votes/{voteId}")
    public ResponseEntity<Void> deleteVoteByVoteId(@PathVariable("voteId") Long voteId) {
        boolean deleteStatus = voteService.deleteVoteByVoteId(voteId);
        if (deleteStatus) return ResponseEntity.noContent().build();
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{incidenceId}/users/{userId}/vote")
    public ResponseEntity<Void> deleteByUserAndIncidence(@PathVariable("incidenceId") Long incidenceId, @PathVariable("userId") Long userId) {
        boolean deleteStatus = voteService.deleteByUserAndIncidence(incidenceId, userId);
        if (deleteStatus) return ResponseEntity.noContent().build();
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/votes/users/{userId}")
    public ResponseEntity<List<VoteResponseDTO>> getUserVotes(@PathVariable Long userId) {
        return ResponseEntity.ok(voteService.getUserVotes(userId));
    }
}