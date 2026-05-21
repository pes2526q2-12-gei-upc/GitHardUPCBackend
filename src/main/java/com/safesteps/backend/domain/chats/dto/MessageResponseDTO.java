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
    private SharedRouteDTO sharedRoute;

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

        if (message.getSharedRoute() != null) {
            this.sharedRoute = new SharedRouteDTO();
            this.sharedRoute.setId(message.getSharedRoute().getId());
            this.sharedRoute.setOriginLat(message.getSharedRoute().getOriginLat());
            this.sharedRoute.setOriginLng(message.getSharedRoute().getOriginLng());
            this.sharedRoute.setDestLat(message.getSharedRoute().getDestLat());
            this.sharedRoute.setDestLng(message.getSharedRoute().getDestLng());
            this.sharedRoute.setScheduledDate(message.getSharedRoute().getScheduledDate());
            this.sharedRoute.setRouteType(message.getSharedRoute().getRouteType());
            this.sharedRoute.setDistanceMeters(message.getSharedRoute().getDistanceMeters());
            this.sharedRoute.setDurationMinutes(message.getSharedRoute().getDurationMinutes());
            this.sharedRoute.setOriginAddress(message.getSharedRoute().getOriginAddress());
            this.sharedRoute.setDestAddress(message.getSharedRoute().getDestAddress());
        }
    }
}