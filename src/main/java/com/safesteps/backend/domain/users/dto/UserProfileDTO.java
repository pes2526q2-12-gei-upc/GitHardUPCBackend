package com.safesteps.backend.domain.users.dto;

import com.safesteps.backend.domain.users.model.User;
import com.safesteps.backend.domain.users.model.UserStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
public class UserProfileDTO {
    private String username;
    private String pictureUrl;
    private Long points;
    private Long level;
    private OffsetDateTime createdAt;
    private UserStatus status;

    public UserProfileDTO(User user) {
        if (user == null) return;
        this.username = user.getUsername();
        this.pictureUrl = user.getPictureUrl();
        this.points = user.getPoints();
        this.level = user.getLevel();
        this.createdAt = user.getCreatedAt();
        this.status = user.getStatus();
    }
}
