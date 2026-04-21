package com.safesteps.backend.domain.users.service;

import com.safesteps.backend.domain.users.dto.UserRequestDTO;
import com.safesteps.backend.domain.users.dto.UserResponseDTO;
import com.safesteps.backend.domain.users.model.User;
import com.safesteps.backend.domain.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponseDTO::new)
                .toList();
    }

    public UserResponseDTO getUserByGoogleId(String googleId) {
        return userRepository.findByGoogleId(googleId)
                .map(UserResponseDTO::new)
                .orElse(null);
    }

    public UserResponseDTO getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(UserResponseDTO::new)
                .orElse(null);
    }

    @Transactional
    public UserResponseDTO createUser(UserRequestDTO req) {
        if (userRepository.existsByEmail(req.getEmail()) || userRepository.existsByGoogleId(req.getGoogleId())) {
            return null;
        }

        User user = new User();
        user.setEmail(req.getEmail());
        user.setUsername(req.getUsername());
        user.setGoogleId(req.getGoogleId());
        user.setPictureUrl(req.getPictureUrl());
        user.setLanguage(req.getLanguage());
        user.setIsAnonymous(req.getIsAnonymous());
        user.setPoints(0);
        user.setLevel(1L);
        user.setReputacio(1);

        return new UserResponseDTO(userRepository.save(user));
    }

    @Transactional
    public UserResponseDTO updateUser(String googleId, UserRequestDTO req) {
        return userRepository.findByGoogleId(googleId).map(user -> {
            user.setUsername(req.getUsername());
            user.setPictureUrl(req.getPictureUrl());
            user.setLanguage(req.getLanguage());
            user.setIsAnonymous(req.getIsAnonymous());
            return new UserResponseDTO(userRepository.save(user));
        }).orElse(null);
    }

    @Transactional
    public boolean deleteUserByGoogleId(String googleId) {
        return userRepository.findByGoogleId(googleId).map(user -> {
            userRepository.delete(user);
            return true;
        }).orElse(false);
    }
}