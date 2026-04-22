package com.safesteps.backend;

import com.safesteps.backend.domain.users.dto.UserRequestDTO;
import com.safesteps.backend.domain.users.dto.UserResponseDTO;
import com.safesteps.backend.domain.users.model.User;
import com.safesteps.backend.domain.users.repository.UserRepository;
import com.safesteps.backend.domain.users.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User user;
    private UserRequestDTO userRequestDTO;

    @BeforeEach
    void setUp() {
        // Inicializamos un usuario de ejemplo para los tests
        user = new User();
        user.setGoogleId("g-123");
        user.setEmail("test@test.com");
        user.setUsername("testuser");

        // Inicializamos un DTO de petición de ejemplo
        userRequestDTO = new UserRequestDTO();
        userRequestDTO.setGoogleId("g-123");
        userRequestDTO.setEmail("test@test.com");
        userRequestDTO.setUsername("testuser");
        userRequestDTO.setIsAnonymous(false);
    }

    @Test
    void getAllUsers_ReturnsList() {
        when(userRepository.findAll()).thenReturn(Arrays.asList(user, new User()));

        List<UserResponseDTO> result = userService.getAllUsers();

        assertEquals(2, result.size());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void getUserByGoogleId_WhenExists_ReturnsUser() {
        when(userRepository.findByGoogleId("g-123")).thenReturn(Optional.of(user));

        UserResponseDTO result = userService.getUserByGoogleId("g-123");

        assertNotNull(result);
        assertEquals("test@test.com", result.getEmail());
    }

    @Test
    void getUserByGoogleId_WhenNotExists_ReturnsNull() {
        when(userRepository.findByGoogleId("any")).thenReturn(Optional.empty());

        UserResponseDTO result = userService.getUserByGoogleId("any");

        assertNull(result);
    }

    @Test
    void getUserByEmail_WhenExists_ReturnsUser() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        UserResponseDTO result = userService.getUserByEmail("test@test.com");

        assertNotNull(result);
        assertEquals("g-123", result.getGoogleId());
    }

    @Test
    void createUser_WhenSuccessful_ReturnsDTO() {
        // Simulamos que NO existe ni por email ni por googleId
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByGoogleId(any())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponseDTO result = userService.createUser(userRequestDTO);

        assertNotNull(result);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_WhenEmailExists_ReturnsNull() {
        when(userRepository.existsByEmail(userRequestDTO.getEmail())).thenReturn(true);

        UserResponseDTO result = userService.createUser(userRequestDTO);

        assertNull(result);
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_WhenGoogleIdExists_ReturnsNull() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByGoogleId(userRequestDTO.getGoogleId())).thenReturn(true);

        UserResponseDTO result = userService.createUser(userRequestDTO);

        assertNull(result);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_WhenExists_ReturnsUpdatedDTO() {
        when(userRepository.findByGoogleId("g-123")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponseDTO result = userService.updateUser("g-123", userRequestDTO);

        assertNotNull(result);
        verify(userRepository).save(user);
    }

    @Test
    void updateUser_WhenNotExists_ReturnsNull() {
        when(userRepository.findByGoogleId("none")).thenReturn(Optional.empty());

        UserResponseDTO result = userService.updateUser("none", userRequestDTO);

        assertNull(result);
        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteUserByGoogleId_WhenExists_ReturnsTrue() {
        when(userRepository.findByGoogleId("g-123")).thenReturn(Optional.of(user));

        boolean result = userService.deleteUserByGoogleId("g-123");

        assertTrue(result);
        verify(userRepository).delete(user);
    }

    @Test
    void deleteUserByGoogleId_WhenNotExists_ReturnsFalse() {
        when(userRepository.findByGoogleId("none")).thenReturn(Optional.empty());

        boolean result = userService.deleteUserByGoogleId("none");

        assertFalse(result);
        verify(userRepository, never()).delete(any());
    }
}
