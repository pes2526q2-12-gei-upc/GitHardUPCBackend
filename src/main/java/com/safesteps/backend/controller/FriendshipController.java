package com.safesteps.backend.controller;

import com.safesteps.backend.domain.users.dto.FriendDTO;
import com.safesteps.backend.domain.users.dto.FriendshipRequestDTO;
import com.safesteps.backend.domain.users.service.FriendshipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/friendships")
@Tag(name = "Friendships", description = "Gestión de amistades entre usuarios")
public class FriendshipController {

    private final FriendshipService friendshipService;

    public FriendshipController(FriendshipService friendshipService) {
        this.friendshipService = friendshipService;
    }

    @GetMapping("/{googleId}/accepted")
    @Operation(
        summary = "Listar amigos aceptados",
        description = "Devuelve la lista de amigos con estado ACCEPTED del usuario. Incluye googleId, username, email y pictureUrl de cada amigo.")
    public ResponseEntity<List<FriendDTO>> getAcceptedFriends(@PathVariable String googleId) {
        return ResponseEntity.ok(friendshipService.getAcceptedFriends(googleId));
    }

    @GetMapping("/{googleId}/pending")
    @Operation(
        summary = "Listar solicitudes de amistad pendientes recibidas",
        description = "Devuelve las solicitudes de amistad en estado PENDING recibidas por el usuario. Incluye los datos del remitente.")
    public ResponseEntity<List<FriendDTO>> getPendingRequests(@PathVariable String googleId) {
        return ResponseEntity.ok(friendshipService.getPendingRequests(googleId));
    }

    @PostMapping
    @Operation(
        summary = "Enviar solicitud de amistad",
        description = "Crea una relación de amistad en estado PENDING entre dos usuarios. El cuerpo debe incluir senderGoogleId y receiverGoogleId.")
    public ResponseEntity<Void> sendFriendRequest(@Valid @RequestBody FriendshipRequestDTO req) {
        friendshipService.sendFriendRequest(req);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{googleId}/friend/{friendGoogleId}")
    @Operation(
        summary = "Eliminar amistad",
        description = "Elimina la amistad existente (aceptada o pendiente) entre dos usuarios.")
    public ResponseEntity<Void> removeFriend(
            @PathVariable String googleId,
            @PathVariable String friendGoogleId) {
        friendshipService.removeFriend(googleId, friendGoogleId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{receiverGoogleId}/accept/{senderGoogleId}")
    @Operation(
        summary = "Aceptar solicitud de amistad",
        description = "El receptor acepta la solicitud PENDING enviada por el sender. Cambia el estado a ACCEPTED en la base de datos.")
    public ResponseEntity<Void> acceptFriendRequest(
            @PathVariable String receiverGoogleId,
            @PathVariable String senderGoogleId) {
        friendshipService.acceptFriendRequest(receiverGoogleId, senderGoogleId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{receiverGoogleId}/decline/{senderGoogleId}")
    @Operation(
        summary = "Denegar solicitud de amistad",
        description = "El receptor rechaza la solicitud PENDING enviada por el sender. Elimina el registro de la base de datos.")
    public ResponseEntity<Void> declineFriendRequest(
            @PathVariable String receiverGoogleId,
            @PathVariable String senderGoogleId) {
        friendshipService.declineFriendRequest(receiverGoogleId, senderGoogleId);
        return ResponseEntity.noContent().build();
    }
}
