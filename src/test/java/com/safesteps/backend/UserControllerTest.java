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
        // Registramos el módulo de tiempo para que Jackson entienda OffsetDateTime si aparece
        objectMapper.registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
    }

    @Test
    void getByGoogleId_WhenUserExists_ReturnsOk() throws Exception {
        String googleId = "google-123";
        UserResponseDTO response = new UserResponseDTO();
        response.setGoogleId(googleId);
        response.setEmail("test@safesteps.com");

        when(userService.getUserByGoogleId(googleId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/{googleId}", googleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.googleId").value(googleId))
                .andExpect(jsonPath("$.email").value("test@safesteps.com"));
    }

    @Test
    void getByGoogleId_WhenUserDoesNotExist_ReturnsNotFound() throws Exception {
        String googleId = "non-existent";
        when(userService.getUserByGoogleId(googleId)).thenReturn(null);

        mockMvc.perform(get("/api/v1/users/{googleId}", googleId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByEmail_ReturnsOk() throws Exception {
        String email = "test@safesteps.com";
        UserResponseDTO response = new UserResponseDTO();
        response.setEmail(email);

        when(userService.getUserByEmail(email)).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/search")
                        .param("email", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void create_WhenSuccessful_ReturnsCreated() throws Exception {
        UserRequestDTO request = new UserRequestDTO();
        request.setEmail("new@user.com");
        request.setUsername("newuser");
        request.setGoogleId("g-999");
        request.setIsAnonymous(false);

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
    void update_WhenUserExists_ReturnsOk() throws Exception {
        String googleId = "g-123";
        UserRequestDTO request = new UserRequestDTO();
        request.setUsername("updatedName");
        request.setIsAnonymous(true);
        request.setEmail("fixed@mail.com"); // Aunque no se edite, el DTO lo requiere
        request.setGoogleId(googleId);

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
    void delete_WhenUserExists_ReturnsNoContent() throws Exception {
        String googleId = "g-123";
        when(userService.deleteUserByGoogleId(googleId)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/users/{googleId}", googleId))
                .andExpect(status().isNoContent());
    }
}