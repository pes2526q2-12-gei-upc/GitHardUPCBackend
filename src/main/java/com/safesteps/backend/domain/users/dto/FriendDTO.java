package com.safesteps.backend.domain.users.dto;

import com.safesteps.backend.domain.users.model.User;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FriendDTO {

    private String googleId;
    private String username;
    private String email;
    private String pictureUrl;

    public FriendDTO(User user) {
        if (user == null) return;
        this.googleId = user.getGoogleId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.pictureUrl = user.getPictureUrl();
    }
}
