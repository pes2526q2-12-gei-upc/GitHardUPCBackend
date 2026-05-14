package com.safesteps.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.safesteps.backend.controller.UserController;
import com.safesteps.backend.domain.common.exception.BadRequestException;
import com.safesteps.backend.domain.common.exception.GlobalExceptionHandler;
import com.safesteps.backend.domain.common.exception.ResourceNotFoundException;
import com.safesteps.backend.domain.users.dto.FilterRequestDTO;
import com.safesteps.backend.domain.users.dto.PremiDTO;
import com.safesteps.backend.domain.users.dto.RouteCompletionResponseDTO;
import com.safesteps.backend.domain.users.dto.UserRequestDTO;
import com.safesteps.backend.domain.users.dto.UserResponseDTO;
import com.safesteps.backend.domain.users.dto.UserProfileDTO;
import com.safesteps.backend.domain.users.dto.UserSearchResultDTO;
import com.safesteps.backend.domain.users.model.User;
import com.safesteps.backend.domain.users.model.UserFilter;
import com.safesteps.backend.domain.users.model.UserStatus;
import com.safesteps.backend.domain.users.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // --- TESTS PARA GET ALL ---
    @Test
    @DisplayName("GET /api/v1/users - Retorna lista vacía o llena (OK)")
    void getAll_ReturnsList() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(new UserResponseDTO()));

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1));
    }

    // --- TESTS PARA GET BY ID ---
    @Test
    @DisplayName("GET /api/v1/users/{googleId} - Existe (OK)")
    void getByGoogleId_WhenExists_ReturnsOk() throws Exception {
        when(userService.getUserByGoogleId("g-123")).thenReturn(new UserResponseDTO());

        mockMvc.perform(get("/api/v1/users/g-123"))
                .andExpect(status().isOk());
    }

    // --- TESTS PARA SEARCH POR USERNAME ---
    @Test
    @DisplayName("GET /api/v1/users/search/username - Hay coincidencias (OK)")
    void searchByUsername_WhenMatches_ReturnsOk() throws Exception {
        UserSearchResultDTO dto = new UserSearchResultDTO();
        dto.setUsername("marc_dev");
        dto.setPictureUrl("https://example.com/avatar.jpg");
        dto.setEmail("marc@example.com");
        when(userService.searchUsersByUsername("marc")).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/users/search/username").param("username", "marc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value("marc_dev"))
                .andExpect(jsonPath("$[0].pictureUrl").value("https://example.com/avatar.jpg"))
                .andExpect(jsonPath("$[0].email").value("marc@example.com"));
    }

    @Test
    @DisplayName("GET /api/v1/users/search/username - Sin coincidencias devuelve lista vacía (OK)")
    void searchByUsername_WhenNoMatches_ReturnsEmptyList() throws Exception {
        when(userService.searchUsersByUsername("zzz")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/users/search/username").param("username", "zzz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // --- TESTS PARA PERFIL POR GOOGLEID ---
    @Test
    @DisplayName("GET /api/v1/users/profile/{googleId} - Usuario existe (OK)")
    void getProfileByGoogleId_WhenExists_ReturnsOk() throws Exception {
        UserProfileDTO profile = new UserProfileDTO();
        profile.setUsername("marc_dev");
        profile.setPictureUrl("https://example.com/avatar.jpg");
        profile.setPoints(500L);
        profile.setLevel(3L);
        profile.setStatus(UserStatus.ACTIVE);
        when(userService.getUserProfileByGoogleId("googleId123")).thenReturn(profile);

        mockMvc.perform(get("/api/v1/users/profile/googleId123"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("marc_dev"))
                .andExpect(jsonPath("$.points").value(500))
                .andExpect(jsonPath("$.level").value(3))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /api/v1/users/profile/{googleId} - Usuario no existe (Not Found)")
    void getProfileByGoogleId_WhenNotExists_ReturnsNotFound() throws Exception {
        when(userService.getUserProfileByGoogleId("unknownId"))
                .thenThrow(new ResourceNotFoundException("User not found for googleId: unknownId"));

        mockMvc.perform(get("/api/v1/users/profile/unknownId"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/users/profile/{googleId} - Usuario baneado devuelve perfil con status BANNED")
    void getProfileByGoogleId_WhenBanned_ReturnsProfileWithBannedStatus() throws Exception {
        UserProfileDTO profile = new UserProfileDTO();
        profile.setUsername("banned_user");
        profile.setStatus(UserStatus.BANNED);
        when(userService.getUserProfileByGoogleId("bannedGoogleId")).thenReturn(profile);

        mockMvc.perform(get("/api/v1/users/profile/bannedGoogleId"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BANNED"));
    }

    @Test
    @DisplayName("GET /api/v1/users/{googleId} - No existe (Not Found)")
    void getByGoogleId_WhenNotExists_ReturnsNotFound() throws Exception {
        when(userService.getUserByGoogleId("none"))
                .thenThrow(new ResourceNotFoundException("User not found for Google ID: none"));

        mockMvc.perform(get("/api/v1/users/none"))
                .andExpect(status().isNotFound());
    }

    // --- TESTS PARA SEARCH (EMAIL) ---
    @Test
    @DisplayName("GET /api/v1/users/search - Email existe (OK)")
    void getByEmail_WhenExists_ReturnsOk() throws Exception {
        when(userService.getUserByEmail("test@test.com")).thenReturn(new UserResponseDTO());

        mockMvc.perform(get("/api/v1/users/search").param("email", "test@test.com"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/users/search - Email no existe (Not Found)")
    void getByEmail_WhenNotExists_ReturnsNotFound() throws Exception {
        when(userService.getUserByEmail("none@test.com"))
                .thenThrow(new ResourceNotFoundException("User not found for email: none@test.com"));

        mockMvc.perform(get("/api/v1/users/search").param("email", "none@test.com"))
                .andExpect(status().isNotFound());
    }

    // --- TESTS PARA CREATE ---
    @Test
    @DisplayName("POST /api/v1/users - Creación exitosa (Created)")
    void create_WhenSuccess_ReturnsCreated() throws Exception {
        UserRequestDTO request = createValidRequest("new@test.com", "newuser");
        when(userService.createUser(any(UserRequestDTO.class))).thenReturn(new UserResponseDTO());

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /api/v1/users - Conflicto de email o id (Bad Request)")
    void create_WhenConflict_ReturnsBadRequest() throws Exception {
        UserRequestDTO request = createValidRequest("existing@test.com", "user");
        when(userService.createUser(any(UserRequestDTO.class)))
                .thenThrow(new BadRequestException("User already exists with the provided email or Google ID."));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // --- TESTS PARA UPDATE (GENERAL) ---
    @Test
    @DisplayName("PUT /api/v1/users/{googleId} - Usuario existe (OK)")
    void update_WhenUserExists_ReturnsOk() throws Exception {
        UserRequestDTO request = createValidRequest("test@test.com", "updatedName");
        when(userService.updateUser(eq("g-123"), any(UserRequestDTO.class))).thenReturn(new UserResponseDTO());

        mockMvc.perform(put("/api/v1/users/g-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/v1/users/{googleId} - Usuario no existe (Not Found)")
    void update_WhenUserNotExists_ReturnsNotFound() throws Exception {
        UserRequestDTO request = createValidRequest("test@test.com", "updatedName");
        when(userService.updateUser(eq("none"), any(UserRequestDTO.class)))
                .thenThrow(new ResourceNotFoundException("User not found for Google ID: none"));

        mockMvc.perform(put("/api/v1/users/none")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // --- TESTS PARA DELETE ---
    @Test
    @DisplayName("DELETE /api/v1/users/{googleId} - Borrado exitoso (No Content)")
    void delete_WhenUserExists_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/users/g-123"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{googleId} - Usuario no existe (Bad Request)")
    void delete_WhenUserDoesNotExist_ReturnsBadRequest() throws Exception {
        doThrow(new ResourceNotFoundException("User not found for Google ID: none"))
                .when(userService).deleteUserByGoogleId("none");

        mockMvc.perform(delete("/api/v1/users/none"))
                .andExpect(status().isNotFound());
    }

    // --- TESTS PARA FILTROS ---
    @Test
    @DisplayName("PUT /api/v1/users/{googleId}/filters - Usuario existe (OK)")
    void updateFilters_WhenUserExists_ReturnsOk() throws Exception {
        FilterRequestDTO filterReq = new FilterRequestDTO();
        filterReq.setArbres(0.8);

        when(userService.updateFilters(eq("g-123"), any(FilterRequestDTO.class)))
                .thenReturn(new UserFilter());

        mockMvc.perform(put("/api/v1/users/g-123/filters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filterReq)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/v1/users/{googleId}/filters - Usuario no existe (Bad Request)")
    void updateFilters_WhenUserNotExists_ReturnsBadRequest() throws Exception {
        when(userService.updateFilters(eq("none"), any(FilterRequestDTO.class)))
                .thenThrow(new ResourceNotFoundException("User filters not found for Google ID: none"));

        mockMvc.perform(put("/api/v1/users/none/filters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FilterRequestDTO())))
                .andExpect(status().isNotFound());
    }

    // --- TESTS PARA IDIOMA ---
    @Test
    @DisplayName("PATCH /api/v1/users/{googleId}/language - Usuario existe (OK)")
    void updateLanguage_WhenUserExists_ReturnsOk() throws Exception {
        when(userService.updateLanguage("g-123", "en")).thenReturn(new UserResponseDTO());

        mockMvc.perform(patch("/api/v1/users/g-123/language")
                        .param("lang", "en"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /api/v1/users/{googleId}/language - Usuario no existe (Bad Request)")
    void updateLanguage_WhenUserDoesNotExist_ReturnsBadRequest() throws Exception {
        when(userService.updateLanguage("none", "es"))
                .thenThrow(new ResourceNotFoundException("User not found for Google ID: none"));

        mockMvc.perform(patch("/api/v1/users/none/language")
                        .param("lang", "es"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/users/{googleId}/open-prize - Usuario existe (OK)")
    void requestPrize_OK() throws Exception {
        PremiDTO p = new PremiDTO();
        p.setId("prize-123");
        p.setUrl("http://example.com/prize.png");

        when(userService.openPrize("googleId")).thenReturn(p);

        mockMvc.perform(get("/api/v1/users/googleId/open-prize"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("prize-123"))
                .andExpect(jsonPath("$.url").value("http://example.com/prize.png"));
    }

    @Test
    @DisplayName("GET /api/v1/users/{googleId}/complete-route - Suma puntos y devuelve nivel")
    void completeRoute_OK() throws Exception {
        RouteCompletionResponseDTO response = new RouteCompletionResponseDTO(2L, true, 2L, 101L, 1L);

        when(userService.completeRoute("googleId", 150.0)).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/googleId/complete-route")
                        .param("meters", "150"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.level").value(2))
                .andExpect(jsonPath("$.levelUpdated").value(true))
                .andExpect(jsonPath("$.pointsAdded").value(2))
                .andExpect(jsonPath("$.totalPoints").value(101))
                .andExpect(jsonPath("$.recompenses").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/users/{googleId}/complete-route - Falta distancia")
    void completeRoute_MissingMeters_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/users/googleId/complete-route"))
                .andExpect(status().isBadRequest());
    }


    // -- Update token

    @Test
    void updateToken_Ok() throws Exception {
        String googleId = "googleId";
        String token = "fcm-token";

        mockMvc.perform(post("/api/v1/users/" + googleId + "/fcm-token") // Si el teu controlador té un prefix (ex: /api/users), afegeix-lo aquí
                        .param("token", token)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk());

        verify(userService, times(1)).updateToken(googleId, token);
    }


    // --- HELPER METHOD ---

    private UserRequestDTO createValidRequest(String email, String username) {
        UserRequestDTO request = new UserRequestDTO();
        request.setEmail(email);
        request.setUsername(username);
        request.setGoogleId("g-123");
        request.setIsAnonymous(false);
        return request;
    }



    @Test
    @DisplayName("GET /api/v1/users/{googleId}/emergency-contacts")
    void getEmergencyContacts() throws Exception {
        User u = new User();
        u.setGoogleId("googleId");
        u.setUsername("username");
        List<UserProfileDTO> users = List.of(new UserProfileDTO(u));

        when(userService.getEmergencyContacts("googleId")).thenReturn(users);

        mockMvc.perform(get("/api/v1/users/googleId/emergency-contacts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value("username"));
    }

    @Test
    @DisplayName("GET /api/v1/users/{googleId}/emergency-contacts None")
    void getNoneEmergencyContacts() throws Exception {
        List<UserProfileDTO> users = new ArrayList<>();

        when(userService.getEmergencyContacts("googleId")).thenReturn(users);

        mockMvc.perform(get("/api/v1/users/googleId/emergency-contacts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{googleId}/emergency-contacts Null")
    void deleteNullEmergencyContacts() throws Exception {
        mockMvc.perform(delete("/api/v1/users/googleId/emergency-contacts")
                        .param("emergencyContacts", ""))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{googleId}/emergency-contacts")
    void deleteEmergencyContacts() throws Exception {
        List<String> users = List.of("u1", "u2", "u3");

        mockMvc.perform(delete("/api/v1/users/googleId/emergency-contacts")
                .param("emergencyContacts", users.toArray(new String[0])))
                .andExpect(status().isNoContent());
        verify(userService).deleteEmergencyContact("googleId", users);
    }

    @Test
    @DisplayName("POST /api/v1/users/{googleId}/emergency-contacts adding my as my emergency contact")
    void newEmergencyContactImMyContact() throws Exception {
        List<String> users = List.of("googleId");

        when(userService.newEmergencyContact(any(), any())).thenThrow(new BadRequestException("Cannot add yourself as an emergency contact."));

        mockMvc.perform(post("/api/v1/users/googleId/emergency-contacts")
                        .param("emergencyContacts", users.toArray(new String[0])))
                .andExpect(status().isBadRequest());
        verify(userService).newEmergencyContact("googleId", users);
    }

    @Test
    @DisplayName("POST /api/v1/users/{googleId}/emergency-contacts none")
    void newEmergencyContactNone() throws Exception {

        mockMvc.perform(post("/api/v1/users/googleId/emergency-contacts")
                        .param("emergencyContacts", ""))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /api/v1/users/{googleId}/emergency-contacts none")
    void newEmergencyContact() throws Exception {
        List<String> users = List.of("googleId");

        mockMvc.perform(post("/api/v1/users/googleId/emergency-contacts")
                        .param("emergencyContacts", users.toArray(new String[0])))
                .andExpect(status().isCreated());
        verify(userService).newEmergencyContact("googleId", users);
    }
}
