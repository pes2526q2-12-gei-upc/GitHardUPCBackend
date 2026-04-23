package com.safesteps.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.safesteps.backend.controller.UserController;
import com.safesteps.backend.domain.users.dto.UserRequestDTO;
import com.safesteps.backend.domain.users.dto.UserResponseDTO;
import com.safesteps.backend.domain.users.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
    }

    // --- TESTS PARA GET ALL ---

    @Test
    void getAll_ReturnsList() throws Exception {
        UserResponseDTO user1 = new UserResponseDTO();
        user1.setGoogleId("1");
        List<UserResponseDTO> users = Arrays.asList(user1, new UserResponseDTO());

        when(userService.getAllUsers()).thenReturn(users);

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2));
    }

    // --- TESTS PARA GET BY GOOGLE ID ---

    @Test
    void getByGoogleId_WhenUserExists_ReturnsOk() throws Exception {
        String googleId = "google-123";
        UserResponseDTO response = new UserResponseDTO();
        response.setGoogleId(googleId);

        when(userService.getUserByGoogleId(googleId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/{googleId}", googleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.googleId").value(googleId));
    }

    @Test
    void getByGoogleId_WhenUserDoesNotExist_ReturnsNotFound() throws Exception {
        when(userService.getUserByGoogleId("none")).thenReturn(null);

        mockMvc.perform(get("/api/v1/users/none"))
                .andExpect(status().isNotFound());
    }

    // --- TESTS PARA GET BY EMAIL ---

    @Test
    void getByEmail_WhenUserExists_ReturnsOk() throws Exception {
        String email = "test@safesteps.com";
        UserResponseDTO response = new UserResponseDTO();
        response.setEmail(email);

        when(userService.getUserByEmail(email)).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/search").param("email", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void getByEmail_WhenUserDoesNotExist_ReturnsNotFound() throws Exception {
        String email = "notfound@mail.com";
        when(userService.getUserByEmail(email)).thenReturn(null);

        mockMvc.perform(get("/api/v1/users/search").param("email", email))
                .andExpect(status().isNotFound());
    }

    // --- TESTS PARA CREATE ---

    @Test
    void create_WhenSuccessful_ReturnsCreated() throws Exception {
        UserRequestDTO request = createValidRequest("new@user.com", "newuser");
        UserResponseDTO response = new UserResponseDTO();
        response.setEmail("new@user.com");

        when(userService.createUser(any(UserRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new@user.com"));
    }

    @Test
    void create_WhenConflict_ReturnsConflict() throws Exception {
        // Usamos un DTO válido para que pase la validación de Spring, pero simulamos conflicto en el servicio
        UserRequestDTO request = createValidRequest("existing@user.com", "user");

        when(userService.createUser(any(UserRequestDTO.class))).thenReturn(null);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    // --- TESTS PARA UPDATE ---

    @Test
    void update_WhenUserExists_ReturnsOk() throws Exception {
        String googleId = "g-123";
        UserRequestDTO request = createValidRequest("fixed@mail.com", "updatedName");

        UserResponseDTO updatedResponse = new UserResponseDTO();
        updatedResponse.setUsername("updatedName");

        when(userService.updateUser(eq(googleId), any(UserRequestDTO.class))).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/v1/users/{googleId}", googleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("updatedName"));
    }

    @Test
    void update_WhenUserDoesNotExist_ReturnsNotFound() throws Exception {
        String googleId = "non-existent";
        UserRequestDTO request = createValidRequest("test@test.com", "user");

        when(userService.updateUser(eq(googleId), any(UserRequestDTO.class))).thenReturn(null);

        mockMvc.perform(put("/api/v1/users/{googleId}", googleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // --- TESTS PARA DELETE ---

    @Test
    void delete_WhenUserExists_ReturnsNoContent() throws Exception {
        String googleId = "g-123";
        when(userService.deleteUserByGoogleId(googleId)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/users/{googleId}", googleId))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_WhenUserDoesNotExist_ReturnsNotFound() throws Exception {
        String googleId = "not-found";
        when(userService.deleteUserByGoogleId(googleId)).thenReturn(false);

        mockMvc.perform(delete("/api/v1/users/{googleId}", googleId))
                .andExpect(status().isNotFound());
    }

    // --- HELPER METHOD ---

    /**
     * Crea un DTO con los campos obligatorios llenos para pasar las validaciones @Valid
     */
    private UserRequestDTO createValidRequest(String email, String username) {
        UserRequestDTO request = new UserRequestDTO();
        request.setEmail(email);
        request.setUsername(username);
        request.setGoogleId("g-id-test");
        request.setIsAnonymous(false); // Evita el error "must not be null"
        return request;
    }
}