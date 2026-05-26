package com.safesteps.backend.domain.chats.dto;

import com.safesteps.backend.domain.chats.model.Chat;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class ChatResponseDTO {
    private Long id;
    private String type;
    private String name;
    private OffsetDateTime createdAt;
    private List<String> participantUsernames;
    private List<String> participantGoogleIds;
    private String creatorGoogleId;
    private List<String> adminGoogleIds;

    public ChatResponseDTO(Chat chat) {
        if (chat == null) return;
        this.id = chat.getId();
        this.type = chat.getType();
        this.name = chat.getName();
        this.createdAt = chat.getCreatedAt();
        this.creatorGoogleId = chat.getCreatorGoogleId();

        if (chat.getParticipants() != null) {
            this.participantUsernames = chat.getParticipants().stream()
                    .map(p -> p.getUser().getUsername())
                    .toList();
            this.participantGoogleIds = chat.getParticipants().stream()
                    .map(p -> p.getUser().getGoogleId())
                    .toList();
            this.adminGoogleIds = chat.getParticipants().stream()
                    .filter(p -> "ADMIN".equalsIgnoreCase(p.getRole()))
                    .map(p -> p.getUser().getGoogleId())
                    .toList();
        } else {
            this.participantUsernames = List.of();
            this.participantGoogleIds = List.of();
            this.adminGoogleIds = List.of();
        }
    }
}