package com.safesteps.backend.controller;

import com.safesteps.backend.domain.common.exception.BadRequestException;
import com.safesteps.backend.domain.users.dto.FilterRequestDTO;
import com.safesteps.backend.domain.users.dto.PremiDTO;
import com.safesteps.backend.domain.users.dto.RouteCompletionResponseDTO;
import com.safesteps.backend.domain.users.dto.UserRequestDTO;
import com.safesteps.backend.domain.users.dto.UserResponseDTO;
import com.safesteps.backend.domain.users.dto.UserProfileDTO;
import com.safesteps.backend.domain.users.dto.UserSearchResultDTO;
import com.safesteps.backend.domain.users.model.Premi;
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
        List<PremiDTO> premis = user.getPremis();
        if (premis == null ||premis.isEmpty()) return ResponseEntity.ok(user);
        for (PremiDTO p : premis) {
            Premi premi = new Premi();
            premi.setId(p.getId());
            premi.setName(p.getName());
            premi.setOddity(p.getOddity());
            premi.setUrl(p.getUrl());
            p.setUrl(userService.resolvePrizeUrl(premi));
        }
        user.setPremis(premis);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/search")
    public ResponseEntity<UserResponseDTO> getByEmail(@RequestParam String email) {
        UserResponseDTO user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/search/username")
    @Operation(summary = "Buscar usuarios por username o email (coincidencia parcial)",
               description = "Retorna la lista de usuarios cuyo username o email contiene el string enviado. Incluye googleId, username, email y avatar.")
    public ResponseEntity<List<UserSearchResultDTO>> searchByUsername(@RequestParam String username) {
        return ResponseEntity.ok(userService.searchUsersByUsername(username));
    }

    @GetMapping("/profile/{googleId}")
    @Operation(summary = "Ver el perfil p\u00FAblico de un usuario por googleId",
               description = "Retorna username, picture_url, points, level, created_at y status del usuario.")
    public ResponseEntity<UserProfileDTO> getProfileByGoogleId(@PathVariable String googleId) {
        return ResponseEntity.ok(userService.getUserProfileByGoogleId(googleId));
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

    @PostMapping("/{googleId}/picture")
    @Operation(summary = "Actualizar la foto de perfil del usuario")
    public ResponseEntity<UserResponseDTO> updateProfilePicture(@PathVariable String googleId, @Valid @RequestBody String url) {
        return ResponseEntity.ok(userService.updateProfilePicture(googleId, url));
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

    @GetMapping("/{googleId}/complete-route")
    @Operation(summary = "Completar una ruta y sumar los puntos recorridos")
    public ResponseEntity<RouteCompletionResponseDTO> completeRoute(
            @PathVariable String googleId,
            @RequestParam(required = false) Double meters,
            @RequestParam(required = false) Double metros) {
        Double distanceMeters = meters != null ? meters : metros;
        if (distanceMeters == null) {
            throw new BadRequestException("Missing route meters.");
        }
        RouteCompletionResponseDTO response = userService.completeRoute(googleId, distanceMeters);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{googleId}/emergency-contacts")
    public ResponseEntity<List<UserProfileDTO>> newEmergencyContacts(@PathVariable String googleId, @RequestParam List<String> emergencyContacts) {
        List<UserProfileDTO> result = userService.newEmergencyContact(googleId, emergencyContacts);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @DeleteMapping("/{googleId}/emergency-contacts")
    public ResponseEntity<Void> deleteEmergencyContacts(@PathVariable String googleId, @RequestParam List<String> emergencyContacts) {
        userService.deleteEmergencyContact(googleId, emergencyContacts);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{googleId}/emergency-contacts")
    public ResponseEntity<List<UserProfileDTO>> getEmergencyContacts(@PathVariable String googleId) {
        List<UserProfileDTO> ec = userService.getEmergencyContacts(googleId);
        return ResponseEntity.ok().body(ec);
    }

    @PostMapping("/{googleId}/fcm-token")
    public ResponseEntity<Void> updateToken(@PathVariable String googleId, @RequestParam String token) {
        userService.updateToken(googleId, token);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{googleId}/emergency")
    public ResponseEntity<Boolean> getUserStatusEmergency(@PathVariable String googleId) {
        return ResponseEntity.ok().body(userService.getUserStatusEmergency(googleId));
    }

    @PostMapping("/{googleId}/emergency")
    public ResponseEntity<Void> toggleUserStatusEmergency(@PathVariable String googleId) {
        userService.toggleUserStatusEmergency(googleId);
        return ResponseEntity.ok().build();
    }
}
