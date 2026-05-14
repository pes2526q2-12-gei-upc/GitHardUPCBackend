package com.safesteps.backend.controller;

import com.safesteps.backend.domain.chats.dto.ChatRequestDTO;
import com.safesteps.backend.domain.chats.dto.ChatResponseDTO;
import com.safesteps.backend.domain.chats.dto.MessageRequestDTO;
import com.safesteps.backend.domain.chats.dto.MessageResponseDTO;
import com.safesteps.backend.domain.chats.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chats")
@Tag(name = "Chats", description = "Gestió de converses i mensajería entre usuarios")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/user/{googleId}")
    @Operation(summary = "Obtenir els chats de un usuari")
    public ResponseEntity<List<ChatResponseDTO>> getUserChats(@PathVariable String googleId) {
        return ResponseEntity.ok(chatService.getUserChats(googleId));
    }

    @PostMapping
    @Operation(summary = "Crear un nou xat (privat o grup)")
    public ResponseEntity<ChatResponseDTO> create(@Valid @RequestBody ChatRequestDTO req) {
        ChatResponseDTO created = chatService.createChat(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{chatId}/participants")
    @Operation(summary = "Afegir un usuari a un grup (Només ADMINS)")
    public ResponseEntity<Void> addUser(
            @PathVariable Long chatId,
            @RequestParam String adminId,
            @RequestParam String newUserGoogleId) {
        chatService.addUserToGroup(chatId, adminId, newUserGoogleId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{chatId}/participants/exit")
    @Operation(summary = "Sortir d'un grup")
    public ResponseEntity<Void> exitGroup(
            @PathVariable Long chatId,
            @RequestParam String googleId) {
        chatService.exitGroup(chatId, googleId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{chatId}/participants/{targetGoogleId}")
    @Operation(summary = "Eliminar un usuari d'un grup (Només ADMINS)")
    public ResponseEntity<Void> removeUser(
            @PathVariable Long chatId,
            @RequestParam String adminId,
            @PathVariable String targetGoogleId) {
        chatService.removeUserFromGroup(chatId, adminId, targetGoogleId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{chatId}/participants/{targetGoogleId}/admin")
    @Operation(summary = "Donar rang d'ADMIN a un participant (Només ADMINS)")
    public ResponseEntity<Void> grantAdmin(
            @PathVariable Long chatId,
            @RequestParam String adminId,
            @PathVariable String targetGoogleId) {
        chatService.grantAdmin(chatId, adminId, targetGoogleId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{chatId}/messages")
    @Operation(summary = "Enviar un missatge")
    public ResponseEntity<MessageResponseDTO> sendMessage(
            @PathVariable Long chatId,
            @Valid @RequestBody MessageRequestDTO req) {
        MessageResponseDTO sent = chatService.sendMessage(chatId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(sent);
    }

    @GetMapping("/{chatId}/messages")
    public ResponseEntity<List<MessageResponseDTO>> getMessages(@PathVariable Long chatId) {
        return ResponseEntity.ok(chatService.getMessagesByChatId(chatId));
    }

    @PutMapping("/{chatId}/messages/{messageId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long chatId,
            @PathVariable Long messageId,
            @RequestParam String googleId) {
        chatService.markMessageAsRead(messageId, googleId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{chatId}/participants/{targetGoogleId}/admin")
    @Operation(summary = "Treure el rang d'ADMIN a un participant (Només ADMINS)")
    public ResponseEntity<Void> revokeAdmin(
            @PathVariable Long chatId,
            @RequestParam String adminId,
            @PathVariable String targetGoogleId) {
        chatService.revokeAdmin(chatId, adminId, targetGoogleId);
        return ResponseEntity.noContent().build();
    }
}