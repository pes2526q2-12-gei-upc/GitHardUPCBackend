package com.safesteps.backend.domain.chats.dto;

import com.safesteps.backend.domain.chats.model.Message;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
public class MessageResponseDTO {
    private Long id;
    private Long chatId;
    private String senderUsername;
    private String senderGoogleId;
    private String content;
    private OffsetDateTime createdAt;

    public MessageResponseDTO(Message message) {
        if (message == null) return;
        this.id = message.getId();
        this.chatId = message.getChat() != null ? message.getChat().getId() : null;
        this.content = message.getContent();
        this.createdAt = message.getCreatedAt();

        if (message.getSender() != null) {
            this.senderUsername = message.getSender().getUsername();
            this.senderGoogleId = message.getSender().getGoogleId();
        }
    }
}