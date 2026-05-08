package com.safesteps.backend.domain.users.dto;

import com.safesteps.backend.domain.users.model.User;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserSearchResultDTO {

    private String username;
    private String pictureUrl;
    private String email;

    public UserSearchResultDTO(User user) {
        if (user == null) return;
        this.username = user.getUsername();
        this.pictureUrl = user.getPictureUrl();
        this.email = user.getEmail();
    }
}
