package com.safesteps.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.safesteps.backend.controller.UserController;
import com.safesteps.backend.domain.common.exception.BadRequestException;
import com.safesteps.backend.domain.common.exception.GlobalExceptionHandler;
import com.safesteps.backend.domain.common.exception.ResourceNotFoundException;
import com.safesteps.backend.domain.users.dto.FilterRequestDTO;
import com.safesteps.backend.domain.users.dto.PremiDTO;
import com.safesteps.backend.domain.users.dto.UserRequestDTO;
import com.safesteps.backend.domain.users.dto.UserResponseDTO;
import com.safesteps.backend.domain.users.model.UserFilter;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
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

    // --- HELPER METHOD ---

    private UserRequestDTO createValidRequest(String email, String username) {
        UserRequestDTO request = new UserRequestDTO();
        request.setEmail(email);
        request.setUsername(username);
        request.setGoogleId("g-123");
        request.setIsAnonymous(false);
        return request;
    }
}