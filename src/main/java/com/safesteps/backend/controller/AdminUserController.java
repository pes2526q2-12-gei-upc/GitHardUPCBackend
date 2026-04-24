package com.safesteps.backend.controller;

import com.safesteps.backend.domain.users.dto.AdminUserDTO;
import com.safesteps.backend.domain.users.model.UserStatus;
import com.safesteps.backend.domain.users.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.safesteps.backend.domain.incidents.service.IncidentService;
import com.safesteps.backend.domain.incidents.dto.IncidentResponseDTO;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final IncidentService incidentService;

    @GetMapping
    public ResponseEntity<Page<AdminUserDTO>> searchUsers(
            @RequestParam(required = false) String query,
            Pageable pageable) {
        return ResponseEntity.ok(adminUserService.searchUsers(query, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminUserDTO> getUserProfile(@PathVariable Long id) {
        return adminUserService.getUserProfile(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminUserDTO> updateUserStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        
        if (!payload.containsKey("status")) {
            return ResponseEntity.badRequest().build();
        }

        try {
            UserStatus newStatus = UserStatus.valueOf(payload.get("status").toUpperCase());
            return adminUserService.updateUserStatus(id, newStatus)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}/incidents")
    public ResponseEntity<List<IncidentResponseDTO>> getUserIncidents(@PathVariable Long id) {
        return ResponseEntity.ok(incidentService.getIncidentsByUserId(id));
    }
}
