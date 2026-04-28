package com.safesteps.backend.controller;

import com.safesteps.backend.domain.users.dto.FilterRequestDTO;
import com.safesteps.backend.domain.users.dto.PremiDTO;
import com.safesteps.backend.domain.users.dto.UserRequestDTO;
import com.safesteps.backend.domain.users.dto.UserResponseDTO;
import com.safesteps.backend.domain.users.model.UserFilter;
import com.safesteps.backend.domain.users.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Usuarios", description = "Gestión de los usuarios y sus preferencias")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getAll() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{googleId}")
    public ResponseEntity<UserResponseDTO> getByGoogleId(@PathVariable String googleId) {
        UserResponseDTO user = userService.getUserByGoogleId(googleId);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/search")
    public ResponseEntity<UserResponseDTO> getByEmail(@RequestParam String email) {
        UserResponseDTO user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }

    @PostMapping
    @Operation(summary = "Crear usuario y sus filtros por defecto")
    public ResponseEntity<UserResponseDTO> create(@Valid @RequestBody UserRequestDTO req) {
        UserResponseDTO created = userService.createUser(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{googleId}")
    public ResponseEntity<UserResponseDTO> update(@PathVariable String googleId, @Valid @RequestBody UserRequestDTO req) {
        UserResponseDTO updated = userService.updateUser(googleId, req);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{googleId}")
    @Operation(summary = "Eliminar usuario y sus filtros")
    public ResponseEntity<Void> delete(@PathVariable String googleId) {
        userService.deleteUserByGoogleId(googleId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{googleId}/filters")
    @Operation(summary = "Actualizar los filtros de seguridad del usuario")
    public ResponseEntity<UserFilter> updateFilters(
            @PathVariable String googleId,
            @RequestBody FilterRequestDTO req) {
        UserFilter updated = userService.updateFilters(googleId, req);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{googleId}/language")
    @Operation(summary = "Actualizar el idioma preferido")
    public ResponseEntity<UserResponseDTO> updateLanguage(
            @PathVariable String googleId,
            @RequestParam String lang) {
        UserResponseDTO updated = userService.updateLanguage(googleId, lang);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{googleId}/open-prize")
    @Operation(summary = "Obre una de les recompenses disponibles actualment de l'usuari.")
    public ResponseEntity<PremiDTO> openPrize(
            @PathVariable String googleId) {
        PremiDTO premi = userService.openPrize(googleId);
        return ResponseEntity.ok(premi);
    }
}