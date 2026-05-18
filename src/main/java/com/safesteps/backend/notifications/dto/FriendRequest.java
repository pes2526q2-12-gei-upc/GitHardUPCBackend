package com.safesteps.backend.notifications.dto;

import com.safesteps.backend.domain.users.model.FriendshipStatus;
import lombok.Data;

@Data
public class FriendRequest {
    private String fromUser;
    private FriendshipStatus status;

    public  FriendRequest(String fromUser, FriendshipStatus status) {
        this.fromUser = fromUser;
        this.status = status;
    }
}
