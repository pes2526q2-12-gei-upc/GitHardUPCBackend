package com.safesteps.backend.domain.users.dto;

import com.safesteps.backend.domain.users.model.UserStatus;
import lombok.Data;

@Data
public class AdminUserDTO {
    private Long id;
    private String email;
    private String username;
    private String pictureUrl;
    private Integer points;
    private Long level;
    private Integer reputacio;
    private UserStatus status;
}
