package com.safesteps.backend;

import com.safesteps.backend.domain.users.dto.FilterRequestDTO;
import com.safesteps.backend.domain.users.dto.UserRequestDTO;
import com.safesteps.backend.domain.users.dto.UserResponseDTO;
import com.safesteps.backend.domain.users.model.User;
import com.safesteps.backend.domain.users.model.UserFilter;
import com.safesteps.backend.domain.users.repository.FilterRepository;
import com.safesteps.backend.domain.users.repository.UserRepository;
import com.safesteps.backend.domain.users.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

    @Mock
    private FilterRepository filterRepository;

    @InjectMocks
    private UserService userService;

    private User user;
    private UserRequestDTO userRequestDTO;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setGoogleId("g-123");
        user.setEmail("test@test.com");
        user.setUsername("testuser");
        user.setLanguage("ca");
        user.setReputacio(1);
        user.setIsAnonymous(false);

        userRequestDTO = new UserRequestDTO();
        userRequestDTO.setGoogleId("g-123");
        userRequestDTO.setEmail("test@test.com");
        userRequestDTO.setUsername("testuser");
        userRequestDTO.setIsAnonymous(false);
        userRequestDTO.setLanguage("es");
    }

    // --- TESTS DE LECTURA (GET) ---

    @Test
    @DisplayName("Debe retornar una lista de usuarios")
    void getAllUsers_ReturnsList() {
        when(userRepository.findAll()).thenReturn(Arrays.asList(user));
        List<UserResponseDTO> result = userService.getAllUsers();
        assertEquals(1, result.size());
        assertEquals("testuser", result.get(0).getUsername());
    }

    @Test
    @DisplayName("Debe retornar el usuario si existe por GoogleId")
    void getUserByGoogleId_WhenExists_ReturnsDTO() {
        when(userRepository.findByGoogleId("g-123")).thenReturn(Optional.of(user));
        UserResponseDTO result = userService.getUserByGoogleId("g-123");
        assertNotNull(result);
        assertEquals("g-123", result.getGoogleId());
    }

    @Test
    @DisplayName("Debe retornar null si el usuario no existe por GoogleId")
    void getUserByGoogleId_WhenNotExists_ReturnsNull() {
        when(userRepository.findByGoogleId("none")).thenReturn(Optional.empty());
        assertNull(userService.getUserByGoogleId("none"));
    }

    @Test
    @DisplayName("Debe retornar el usuario si existe por Email")
    void getUserByEmail_WhenExists_ReturnsDTO() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        UserResponseDTO result = userService.getUserByEmail("test@test.com");
        assertNotNull(result);
        assertEquals("test@test.com", result.getEmail());
    }

    @Test
    @DisplayName("Debe retornar null si el usuario no existe por Email")
    void getUserByEmail_WhenNotExists_ReturnsNull() {
        when(userRepository.findByEmail("none@test.com")).thenReturn(Optional.empty());
        assertNull(userService.getUserByEmail("none@test.com"));
    }

    // --- TESTS DE CREACIÓN ---

    @Test
    @DisplayName("Debe crear un usuario con valores por defecto y filtros")
    void createUser_Success_VerifiesDefaultsAndFilters() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByGoogleId(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponseDTO result = userService.createUser(userRequestDTO);

        assertNotNull(result);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        assertEquals("es", userCaptor.getValue().getLanguage()); // Viene del DTO
        assertEquals(1, userCaptor.getValue().getReputacio()); // Valor por defecto

        verify(filterRepository).save(any(UserFilter.class));
    }

    @Test
    @DisplayName("No debe crear un usuario si el email o googleId ya existen")
    void createUser_WhenExists_ReturnsNull() {
        when(userRepository.existsByEmail("test@test.com")).thenReturn(true);
        assertNull(userService.createUser(userRequestDTO));

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByGoogleId("g-123")).thenReturn(true);
        assertNull(userService.createUser(userRequestDTO));

        verify(userRepository, never()).save(any());
    }

    // --- TESTS DE ACTUALIZACIÓN ---

    @Test
    @DisplayName("Debe actualizar un usuario existente")
    void updateUser_WhenExists_ReturnsUpdatedDTO() {
        when(userRepository.findByGoogleId("g-123")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        userRequestDTO.setUsername("updatedName");
        UserResponseDTO result = userService.updateUser("g-123", userRequestDTO);

        assertNotNull(result);
        assertEquals("updatedName", user.getUsername());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Debe retornar null al intentar actualizar un usuario que no existe")
    void updateUser_WhenNotExists_ReturnsNull() {
        when(userRepository.findByGoogleId("none")).thenReturn(Optional.empty());
        UserResponseDTO result = userService.updateUser("none", userRequestDTO);
        assertNull(result);
    }

    @Test
    @DisplayName("Debe actualizar los filtros de un usuario")
    void updateFilters_WhenExists_UpdatesCorrectFields() {
        UserFilter existingFilter = new UserFilter();
        existingFilter.setGoogleId("g-123");
        existingFilter.setArbres(0.5);

        when(filterRepository.findById("g-123")).thenReturn(Optional.of(existingFilter));
        when(filterRepository.save(any())).thenReturn(existingFilter);

        FilterRequestDTO req = new FilterRequestDTO();
        req.setArbres(0.9);

        UserFilter result = userService.updateFilters("g-123", req);

        assertNotNull(result);
        assertEquals(0.9, result.getArbres());
        verify(filterRepository).save(existingFilter);
    }

    @Test
    @DisplayName("Debe retornar null al actualizar filtros de usuario que no existe")
    void updateFilters_WhenNotExists_ReturnsNull() {
        when(filterRepository.findById("none")).thenReturn(Optional.empty());
        UserFilter result = userService.updateFilters("none", new FilterRequestDTO());
        assertNull(result);
    }

    @Test
    @DisplayName("Debe actualizar el idioma correctamente")
    void updateLanguage_UpdatesSuccessfully() {
        when(userRepository.findByGoogleId("g-123")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        UserResponseDTO result = userService.updateLanguage("g-123", "en");

        assertNotNull(result);
        assertEquals("en", user.getLanguage());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Debe retornar null al intentar actualizar idioma de usuario inexistente")
    void updateLanguage_WhenNotExists_ReturnsNull() {
        when(userRepository.findByGoogleId("none")).thenReturn(Optional.empty());
        assertNull(userService.updateLanguage("none", "en"));
    }

    // --- TESTS DE BORRADO ---

    @Test
    @DisplayName("Debe borrar el usuario y sus filtros en cascada")
    void deleteUser_Success_VerifiesCascadeDelete() {
        when(userRepository.findByGoogleId("g-123")).thenReturn(Optional.of(user));

        boolean deleted = userService.deleteUserByGoogleId("g-123");

        assertTrue(deleted);
        verify(filterRepository).deleteById("g-123");
        verify(userRepository).delete(user);
    }

    @Test
    @DisplayName("Debe retornar false al intentar borrar un usuario inexistente")
    void deleteUser_WhenNotExists_ReturnsFalse() {
        when(userRepository.findByGoogleId("none")).thenReturn(Optional.empty());
        boolean deleted = userService.deleteUserByGoogleId("none");
        assertFalse(deleted);
        verify(filterRepository, never()).deleteById(anyString());
        verify(userRepository, never()).delete(any(User.class));
    }
}