package com.safesteps.backend.domain.users.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FriendshipRequestDTO {

    @NotBlank(message = "senderGoogleId is required")
    private String senderGoogleId;

    @NotBlank(message = "receiverGoogleId is required")
    private String receiverGoogleId;
}
