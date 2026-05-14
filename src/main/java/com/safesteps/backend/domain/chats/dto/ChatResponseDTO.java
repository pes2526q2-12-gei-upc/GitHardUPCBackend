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

    public ChatResponseDTO(Chat chat) {
        if (chat == null) return;
        this.id = chat.getId();
        this.type = chat.getType();
        this.name = chat.getName();
        this.createdAt = chat.getCreatedAt();
        this.participantUsernames = chat.getParticipants() == null ? List.of() :
                chat.getParticipants().stream()
                        .map(p -> p.getUser().getUsername())
                        .toList();
    }
}
