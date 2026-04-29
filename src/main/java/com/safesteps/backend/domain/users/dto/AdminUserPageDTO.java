package com.safesteps.backend.domain.users.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserPageDTO {
    private List<AdminUserDTO> content;
    private int number;
    private int totalPages;
    private long totalElements;
    private boolean first;
    private boolean last;
}
