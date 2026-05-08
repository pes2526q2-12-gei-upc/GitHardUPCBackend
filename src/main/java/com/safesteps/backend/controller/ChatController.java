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
@Tag(name = "Chats", description = "Gestión de conversaciones y mensajería entre usuarios")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/user/{googleId}")
    @Operation(summary = "Obtener los chats de un usuario",
            description = "Retorna la lista de conversaciones en las que participa el usuario.")
    public ResponseEntity<List<ChatResponseDTO>> getUserChats(@PathVariable String googleId) {
        return ResponseEntity.ok(chatService.getUserChats(googleId));
    }

    @PostMapping
    @Operation(summary = "Crear un nuevo chat (Privado o Grupal)",
            description = "Crea una sala de chat y añade los participantes pasados por Google ID.")
    public ResponseEntity<ChatResponseDTO> create(@Valid @RequestBody ChatRequestDTO req) {
        ChatResponseDTO created = chatService.createChat(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{chatId}/messages")
    @Operation(summary = "Enviar un mensaje a un chat específico")
    public ResponseEntity<MessageResponseDTO> sendMessage(
            @PathVariable Long chatId,
            @Valid @RequestBody MessageRequestDTO req) {
        MessageResponseDTO sent = chatService.sendMessage(chatId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(sent);
    }

    @GetMapping("/{chatId}/messages")
    @Operation(summary = "Obtener el historial de mensajes de un chat")
    public ResponseEntity<List<MessageResponseDTO>> getMessages(@PathVariable Long chatId) {
        return ResponseEntity.ok(chatService.getMessagesByChatId(chatId));
    }

    @PutMapping("/{chatId}/messages/{messageId}/read")
    @Operation(summary = "Marcar un mensaje como leído",
            description = "Inicia la cuenta atrás de 24h para el borrado automático del mensaje.")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long chatId,
            @PathVariable Long messageId,
            @RequestParam String googleId) {
        chatService.markMessageAsRead(messageId, googleId);
        return ResponseEntity.noContent().build();
    }
}