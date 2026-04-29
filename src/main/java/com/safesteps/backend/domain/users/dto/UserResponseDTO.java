package com.safesteps.backend.domain.users.dto;

import com.safesteps.backend.domain.users.model.User;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
public class UserResponseDTO {
    private Long id;
    private String email;
    private String username;
    private String googleId;
    private String pictureUrl;
    private String language;
    private Long points;
    private Long level;
    private Boolean isAnonymous;
    private double reputacio;
    private OffsetDateTime createdAt;

    public UserResponseDTO(User user) {
        if (user == null) return;
        this.id = user.getId();
        this.email = user.getEmail();
        this.username = user.getUsername();
        this.googleId = user.getGoogleId();
        this.pictureUrl = user.getPictureUrl();
        this.language = user.getLanguage();
        this.points = user.getPoints();
        this.level = user.getLevel();
        this.isAnonymous = user.getIsAnonymous();
        this.reputacio = user.getReputacio();
        this.createdAt = user.getCreatedAt();
    }
}