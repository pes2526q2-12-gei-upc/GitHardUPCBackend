package com.safesteps.backend.domain.users.service;

import com.safesteps.backend.domain.users.dto.AdminUserDTO;
import com.safesteps.backend.domain.users.model.User;
import com.safesteps.backend.domain.users.model.UserStatus;
import com.safesteps.backend.domain.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;

    public Page<AdminUserDTO> searchUsers(String query, Pageable pageable) {
        Page<User> users;
        if (query == null || query.trim().isEmpty()) {
            users = userRepository.findAll(pageable);
        } else {
            users = userRepository.searchUsers(query, pageable);
        }
        return users.map(this::mapToAdminDTO);
    }

    public Optional<AdminUserDTO> getUserProfile(Long id) {
        return userRepository.findById(id).map(this::mapToAdminDTO);
    }

    @Transactional
    public Optional<AdminUserDTO> updateUserStatus(Long id, UserStatus newStatus) {
        return userRepository.findById(id).map(user -> {
            user.setStatus(newStatus);
            userRepository.save(user);
            return mapToAdminDTO(user);
        });
    }

    private AdminUserDTO mapToAdminDTO(User user) {
        AdminUserDTO dto = new AdminUserDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setUsername(user.getUsername());
        dto.setPictureUrl(user.getPictureUrl());
        dto.setPoints(user.getPoints());
        dto.setLevel(user.getLevel());
        dto.setReputacio(user.getReputacio());
        dto.setStatus(user.getStatus());
        return dto;
    }
}
