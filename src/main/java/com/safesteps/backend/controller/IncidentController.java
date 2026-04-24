package com.safesteps.backend.controller;

import com.safesteps.backend.domain.incidents.dto.*;
import com.safesteps.backend.domain.incidents.service.IncidentService;
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

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
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

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<IncidentResponseDTO>> getUserIncidents(@PathVariable Long userId) {
        return ResponseEntity.ok(incidentService.getIncidentsByUserId(userId));
    }
}