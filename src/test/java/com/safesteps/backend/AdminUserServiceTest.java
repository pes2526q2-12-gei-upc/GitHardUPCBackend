package com.safesteps.backend;

import com.safesteps.backend.domain.users.dto.AdminUserDTO;
import com.safesteps.backend.domain.users.model.User;
import com.safesteps.backend.domain.users.model.UserStatus;
import com.safesteps.backend.domain.users.repository.UserRepository;
import com.safesteps.backend.domain.users.service.AdminUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminUserService adminUserService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("test@example.com");
        mockUser.setUsername("testuser");
        mockUser.setPoints(100);
        mockUser.setLevel(2L);
        mockUser.setReputacio(5);
        mockUser.setStatus(UserStatus.ACTIVE);
    }

    @Test
    void searchUsers_WithNullQuery_ReturnsAllUsers() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(mockUser));
        when(userRepository.findAll(pageable)).thenReturn(userPage);

        Page<AdminUserDTO> result = adminUserService.searchUsers(null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("test@example.com", result.getContent().get(0).getEmail());
        verify(userRepository).findAll(pageable);
        verify(userRepository, never()).searchUsers(any(), any());
    }

    @Test
    void searchUsers_WithEmptyQuery_ReturnsAllUsers() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(mockUser));
        when(userRepository.findAll(pageable)).thenReturn(userPage);

        Page<AdminUserDTO> result = adminUserService.searchUsers("   ", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(userRepository).findAll(pageable);
    }

    @Test
    void searchUsers_WithQuery_ReturnsFilteredUsers() {
        String query = "test";
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(mockUser));
        when(userRepository.searchUsers(query, pageable)).thenReturn(userPage);

        Page<AdminUserDTO> result = adminUserService.searchUsers(query, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(userRepository).searchUsers(query, pageable);
    }

    @Test
    void getUserProfile_UserExists_ReturnsUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        Optional<AdminUserDTO> result = adminUserService.getUserProfile(1L);

        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
        assertEquals(UserStatus.ACTIVE, result.get().getStatus());
    }

    @Test
    void getUserProfile_UserDoesNotExist_ReturnsEmpty() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        Optional<AdminUserDTO> result = adminUserService.getUserProfile(1L);

        assertFalse(result.isPresent());
    }

    @Test
    void updateUserStatus_UserExists_UpdatesAndReturnsUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        Optional<AdminUserDTO> result = adminUserService.updateUserStatus(1L, UserStatus.BANNED);

        assertTrue(result.isPresent());
        assertEquals(UserStatus.BANNED, result.get().getStatus());
        assertEquals(UserStatus.BANNED, mockUser.getStatus());
        verify(userRepository).save(mockUser);
    }

    @Test
    void updateUserStatus_UserDoesNotExist_ReturnsEmpty() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        Optional<AdminUserDTO> result = adminUserService.updateUserStatus(1L, UserStatus.BANNED);

        assertFalse(result.isPresent());
        verify(userRepository, never()).save(any());
    }
}
