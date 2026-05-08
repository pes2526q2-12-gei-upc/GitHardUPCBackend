package com.safesteps.backend;

import com.safesteps.backend.domain.common.exception.BadRequestException;
import com.safesteps.backend.domain.common.exception.ResourceNotFoundException;
import com.safesteps.backend.domain.common.exception.UserBannedException;
import com.safesteps.backend.domain.common.exception.UserSuspendedException;
import com.safesteps.backend.domain.incidents.model.Vote;
import com.safesteps.backend.domain.users.dto.FilterRequestDTO;
import com.safesteps.backend.domain.users.dto.PremiDTO;
import com.safesteps.backend.domain.users.dto.RouteCompletionResponseDTO;
import com.safesteps.backend.domain.users.dto.UserProfileDTO;
import com.safesteps.backend.domain.users.dto.UserRequestDTO;
import com.safesteps.backend.domain.users.dto.UserResponseDTO;
import com.safesteps.backend.domain.users.dto.UserSearchResultDTO;
import com.safesteps.backend.domain.users.model.Premi;
import com.safesteps.backend.domain.users.model.User;
import com.safesteps.backend.domain.users.model.UserFilter;
import com.safesteps.backend.domain.users.model.UserStatus;
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
        user.setStatus(UserStatus.ACTIVE);

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
        assertEquals("testuser", result.getFirst().getUsername());
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
    @DisplayName("Debe lanzar ResourceNotFoundException si el usuario no existe por GoogleId")
    void getUserByGoogleId_WhenNotExists_ThrowsResourceNotFoundException() {
        when(userRepository.findByGoogleId("none")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.getUserByGoogleId("none"));
    }

    @Test
    @DisplayName("Debe lanzar UserBannedException al obtener usuario baneado por GoogleId")
    void getUserByGoogleId_WhenBanned_ThrowsUserBannedException() {
        User bannedUser = new User();
        bannedUser.setStatus(UserStatus.BANNED);
        when(userRepository.findByGoogleId("g-123")).thenReturn(Optional.of(bannedUser));

        assertThrows(UserBannedException.class, () -> userService.getUserByGoogleId("g-123"));
    }

    @Test
    @DisplayName("Debe lanzar UserSuspendedException al obtener usuario suspendido por GoogleId")
    void getUserByGoogleId_WhenSuspended_ThrowsUserSuspendedException() {
        User suspendedUser = new User();
        suspendedUser.setStatus(UserStatus.SUSPENDED);
        when(userRepository.findByGoogleId("g-123")).thenReturn(Optional.of(suspendedUser));

        assertThrows(UserSuspendedException.class, () -> userService.getUserByGoogleId("g-123"));
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
    @DisplayName("Debe lanzar ResourceNotFoundException si el usuario no existe por Email")
    void getUserByEmail_WhenNotExists_ThrowsResourceNotFoundException() {
        when(userRepository.findByEmail("none@test.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.getUserByEmail("none@test.com"));
    }

    @Test
    @DisplayName("Debe lanzar UserBannedException al obtener usuario baneado por Email")
    void getUserByEmail_WhenBanned_ThrowsUserBannedException() {
        User bannedUser = new User();
        bannedUser.setStatus(UserStatus.BANNED);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(bannedUser));

        assertThrows(UserBannedException.class, () -> userService.getUserByEmail("test@test.com"));
    }

    @Test
    @DisplayName("Debe lanzar UserSuspendedException al obtener usuario suspendido por Email")
    void getUserByEmail_WhenSuspended_ThrowsUserSuspendedException() {
        User suspendedUser = new User();
        suspendedUser.setStatus(UserStatus.SUSPENDED);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(suspendedUser));

        assertThrows(UserSuspendedException.class, () -> userService.getUserByEmail("test@test.com"));
    }

    // --- TESTS DE CREACIÓN ---

    @Test
    @DisplayName("Debe crear un usuario con valores por defecto y filtros")
    void createUser_Success_VerifiesDefaultsAndFilters() {
        when(userRepository.findByGoogleId(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
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
    void createUser_WhenExists_ThrowsBadRequestException() {
        when(userRepository.findByGoogleId("g-123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        assertThrows(BadRequestException.class, () -> userService.createUser(userRequestDTO));

        when(userRepository.findByGoogleId("g-123")).thenReturn(Optional.of(user));
        assertThrows(BadRequestException.class, () -> userService.createUser(userRequestDTO));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("No debe crear un usuario si ya existe y está baneado")
    void createUser_WhenBanned_ThrowsUserBannedException() {
        User bannedUser = new User();
        bannedUser.setStatus(UserStatus.BANNED);
        when(userRepository.findByGoogleId("g-123")).thenReturn(Optional.of(bannedUser));

        assertThrows(UserBannedException.class, () -> userService.createUser(userRequestDTO));
    }

    @Test
    @DisplayName("No debe crear un usuario si ya existe y está suspendido")
    void createUser_WhenSuspended_ThrowsUserSuspendedException() {
        User suspendedUser = new User();
        suspendedUser.setStatus(UserStatus.SUSPENDED);
        when(userRepository.findByGoogleId("g-123")).thenReturn(Optional.of(suspendedUser));

        assertThrows(UserSuspendedException.class, () -> userService.createUser(userRequestDTO));
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
    @DisplayName("Debe lanzar ResourceNotFoundException al intentar actualizar un usuario que no existe")
    void updateUser_WhenNotExists_ThrowsResourceNotFoundException() {
        when(userRepository.findByGoogleId("none")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.updateUser("none", userRequestDTO));

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
    @DisplayName("Debe lanzar ResourceNotFoundException al actualizar filtros de usuario que no existe")
    void updateFilters_WhenNotExists_ThrowsResourceNotFoundException() {
        when(filterRepository.findById("none")).thenReturn(Optional.empty());

        FilterRequestDTO request = new FilterRequestDTO();
        String userId = "none";

        assertThrows(ResourceNotFoundException.class, () -> userService.updateFilters(userId, request));
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
    @DisplayName("Debe lanzar ResourceNotFoundException al intentar actualizar idioma de usuario inexistente")
    void updateLanguage_WhenNotExists_ThrowsResourceNotFoundException() {
        when(userRepository.findByGoogleId("none")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.updateLanguage("none", "en"));

    }

    // --- TESTS DE BORRADO ---

    @Test
    @DisplayName("Debe borrar el usuario y sus filtros en cascada")
    void deleteUser_Success_VerifiesCascadeDelete() {
        when(userRepository.findByGoogleId("g-123")).thenReturn(Optional.of(user));

        userService.deleteUserByGoogleId("g-123");

        verify(filterRepository).deleteById("g-123");
        verify(userRepository).delete(user);
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException al intentar borrar un usuario inexistente")
    void deleteUser_WhenNotExists_ThrowsResourceNotFoundException() {
        when(userRepository.findByGoogleId("none")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.deleteUserByGoogleId("none"));
        verify(filterRepository, never()).deleteById(anyString());
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    @DisplayName("Debe actualizar reputación y puntos de votantes y del creador al aceptar")
    void updateUserReliability_Accepted_WithCreator() {
        Vote v = new Vote(); v.setGoogleId("voter1"); v.setScore(0.5f);
        User voter = new User(); voter.setGoogleId("voter1"); voter.setReputacio(1.0); voter.setPoints(0L);
        User creator = new User(); creator.setGoogleId("creator1"); creator.setReputacio(1.0); creator.setPoints(0L);

        when(userRepository.findByGoogleId("voter1")).thenReturn(Optional.of(voter));
        when(userRepository.findByGoogleId("creator1")).thenReturn(Optional.of(creator));

        // Simulamos que la incidencia es aceptada y le pasamos el ID del creador
        userService.updateUserReliability(List.of(v), true, "creator1");

        assertEquals(1.01, voter.getReputacio(), 0.001);
        assertEquals(10, voter.getPoints());
        assertEquals(1.01, creator.getReputacio(), 0.001);
        assertEquals(10, creator.getPoints());
        verify(userRepository, times(2)).save(any(User.class));
    }

    @Test
    @DisplayName("Debe dar puntos por incidencia expirada a votantes y creador")
    void rewardForExpiredIncident_Success() {
        Vote v = new Vote(); v.setGoogleId("voter1");
        User voter = new User(); voter.setGoogleId("voter1"); voter.setPoints(1000L); voter.setRecompenses(0L);
        User creator = new User(); creator.setGoogleId("creator1"); creator.setPoints(0L);

        when(userRepository.findByGoogleId("voter1")).thenReturn(Optional.of(voter));
        when(userRepository.findByGoogleId("creator1")).thenReturn(Optional.of(creator));

        userService.rewardForExpiredIncident(List.of(v), "creator1");

        assertEquals(1010, voter.getPoints());
        assertEquals(10, creator.getPoints());
        verify(userRepository, times(3)).save(any(User.class));
    }

    @Test
    @DisplayName("Debe sumar minutos estimados de ruta como puntos y recalcular nivel")
    void completeRoute_AddsRoutePointsAndUpdatesLevel() {
        user.setPoints(99L);
        user.setLevel(1L);
        user.setRecompenses(0L);

        when(userRepository.findByGoogleId("g-123")).thenReturn(Optional.of(user));

        RouteCompletionResponseDTO result = userService.completeRoute("g-123", 75.0);

        assertEquals(1L, result.getPointsAdded());
        assertEquals(100L, result.getTotalPoints());
        assertEquals(2L, result.getLevel());
        assertTrue(result.isLevelUpdated());
        assertEquals(1L, result.getRecompenses());
        verify(userRepository, times(2)).save(user);
    }

    @Test
    @DisplayName("Debe rechazar metros negativos al completar ruta")
    void completeRoute_NegativeMeters_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> userService.completeRoute("g-123", -1.0));
        verify(userRepository, never()).findByGoogleId(anyString());
    }

    @Test
    @DisplayName("Debe devolver 0 puntos si la ruta completada tiene 0 metros")
    void completeRoute_ZeroMeters_AddsNoPoints() {
        user.setPoints(0L);
        user.setLevel(1L);
        user.setRecompenses(0L);

        when(userRepository.findByGoogleId("g-123")).thenReturn(Optional.of(user));

        RouteCompletionResponseDTO result = userService.completeRoute("g-123", 0.0);

        assertEquals(0L, result.getPointsAdded());
        assertEquals(0L, result.getTotalPoints());
        assertEquals(1L, result.getLevel());
        assertFalse(result.isLevelUpdated());
        verify(userRepository).save(user);
    }


    @Test
    void openPrize_OK() {
        String googleId = user.getGoogleId();
        Premi p1  = new Premi(); p1.setId("p1"); p1.setUrl("url1"); p1.setProbability(0.1);
        Premi p2  = new Premi(); p2.setId("p2"); p2.setUrl("url2"); p2.setProbability(0.5);
        List<Premi> lp = List.of(p1, p2);
        user.setRecompenses(1L);

        when(userRepository.decrementPendingRewards(googleId)).thenReturn(1);
        when(userRepository.getUserAvailablePrizes(googleId)).thenReturn(lp);
        when(userRepository.findByGoogleId(googleId)).thenReturn(Optional.of(user));

        PremiDTO p = userService.openPrize(user.getGoogleId());
        assertNotNull(p);
        assertTrue(lp.stream().anyMatch(pr -> pr.getId().equals(p.getId())));
        verify(userRepository).decrementPendingRewards(googleId);
        verify(userRepository).insertUserPrize(googleId, p.getId());
    }

    @Test
    void openPrize_NOK() {
        assertThrows(ResourceNotFoundException.class, () -> userService.openPrize("voter1"));
    }

    @Test
    void openPrize_ERROR() {
        String googleId = user.getGoogleId();
        when(userRepository.decrementPendingRewards(googleId)).thenReturn(0);
        when(userRepository.findByGoogleId(googleId)).thenReturn(Optional.of(user));

        assertThrows(BadRequestException.class, () -> userService.openPrize(googleId));
        verify(userRepository, never()).insertUserPrize(anyString(), anyString());
    }

    // --- TESTS PARA BÚSQUEDA POR USERNAME ---

    @Test
    @DisplayName("Debe retornar lista de DTOs con username y picture cuando hay coincidencias")
    void searchUsersByUsername_WhenMatches_ReturnsList() {
        User u1 = new User(); u1.setUsername("marc_dev"); u1.setPictureUrl("url1"); u1.setIsAnonymous(false); u1.setStatus(UserStatus.ACTIVE);
        User u2 = new User(); u2.setUsername("marceline"); u2.setPictureUrl("url2"); u2.setIsAnonymous(false); u2.setStatus(UserStatus.ACTIVE);
        when(userRepository.findByUsernameContainingIgnoreCase("marc")).thenReturn(List.of(u1, u2));

        List<UserSearchResultDTO> result = userService.searchUsersByUsername("marc");

        assertEquals(2, result.size());
        assertEquals("marc_dev", result.get(0).getUsername());
        assertEquals("url1", result.get(0).getPictureUrl());
    }

    @Test
    @DisplayName("Debe retornar lista vacía cuando no hay coincidencias")
    void searchUsersByUsername_WhenNoMatches_ReturnsEmptyList() {
        when(userRepository.findByUsernameContainingIgnoreCase("zzz")).thenReturn(List.of());

        List<UserSearchResultDTO> result = userService.searchUsersByUsername("zzz");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Debe excluir usuarios anónimos de la búsqueda por username")
    void searchUsersByUsername_ExcludesAnonymousUsers() {
        User visible  = new User(); visible.setUsername("marc_dev"); visible.setIsAnonymous(false); visible.setStatus(UserStatus.ACTIVE);
        User anonymous = new User(); anonymous.setUsername("marc_anon"); anonymous.setIsAnonymous(true); anonymous.setStatus(UserStatus.ACTIVE);
        when(userRepository.findByUsernameContainingIgnoreCase("marc")).thenReturn(List.of(visible, anonymous));

        List<UserSearchResultDTO> result = userService.searchUsersByUsername("marc");

        assertEquals(1, result.size());
        assertEquals("marc_dev", result.get(0).getUsername());
    }

    @Test
    @DisplayName("Debe excluir usuarios baneados de la búsqueda por username")
    void searchUsersByUsername_ExcludesBannedUsers() {
        User active = new User(); active.setUsername("marc_dev"); active.setIsAnonymous(false); active.setStatus(UserStatus.ACTIVE);
        User banned = new User(); banned.setUsername("marc_bad"); banned.setIsAnonymous(false); banned.setStatus(UserStatus.BANNED);
        when(userRepository.findByUsernameContainingIgnoreCase("marc")).thenReturn(List.of(active, banned));

        List<UserSearchResultDTO> result = userService.searchUsersByUsername("marc");

        assertEquals(1, result.size());
        assertEquals("marc_dev", result.get(0).getUsername());
    }

    // --- TESTS PARA PERFIL POR USERNAME ---

    @Test
    @DisplayName("Debe retornar el perfil completo cuando el username existe")
    void getUserProfileByUsername_WhenExists_ReturnsProfile() {
        user.setPoints(200L);
        user.setLevel(4L);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        UserProfileDTO result = userService.getUserProfileByUsername("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals(200L, result.getPoints());
        assertEquals(4L, result.getLevel());
        assertEquals(UserStatus.ACTIVE, result.getStatus());
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si el username no existe")
    void getUserProfileByUsername_WhenNotExists_ThrowsResourceNotFoundException() {
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.getUserProfileByUsername("nobody"));
    }

    @Test
    @DisplayName("Debe devolver el perfil con status BANNED (no lanza excepción)")
    void getUserProfileByUsername_WhenBanned_ReturnsProfileWithBannedStatus() {
        User bannedUser = new User();
        bannedUser.setUsername("banned_user");
        bannedUser.setStatus(UserStatus.BANNED);
        when(userRepository.findByUsername("banned_user")).thenReturn(Optional.of(bannedUser));

        UserProfileDTO result = userService.getUserProfileByUsername("banned_user");

        assertNotNull(result);
        assertEquals(UserStatus.BANNED, result.getStatus());
    }

    @Test
    @DisplayName("Debe devolver el perfil con status SUSPENDED (no lanza excepción)")
    void getUserProfileByUsername_WhenSuspended_ReturnsProfileWithSuspendedStatus() {
        User suspendedUser = new User();
        suspendedUser.setUsername("suspended_user");
        suspendedUser.setStatus(UserStatus.SUSPENDED);
        when(userRepository.findByUsername("suspended_user")).thenReturn(Optional.of(suspendedUser));

        UserProfileDTO result = userService.getUserProfileByUsername("suspended_user");

        assertNotNull(result);
        assertEquals(UserStatus.SUSPENDED, result.getStatus());
    }
}