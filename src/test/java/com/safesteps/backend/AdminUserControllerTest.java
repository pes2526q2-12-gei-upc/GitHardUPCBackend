package com.safesteps.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safesteps.backend.controller.AdminUserController;
import com.safesteps.backend.domain.users.dto.AdminUserDTO;
import com.safesteps.backend.domain.admin.service.AdminMetricsService;
import com.safesteps.backend.domain.users.model.User;
import com.safesteps.backend.domain.users.model.UserStatus;
import com.safesteps.backend.domain.users.repository.UserRepository;
import com.safesteps.backend.domain.users.service.AdminUserService;
import com.safesteps.backend.domain.incidents.service.IncidentService;
import com.safesteps.backend.security.AdminSessionInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters if any for simple unit test
public class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminUserService adminUserService;

    @MockBean
    private IncidentService incidentService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private AdminMetricsService adminMetricsService;

    @Autowired
    private ObjectMapper objectMapper;

    private AdminUserDTO mockUserDTO;

    // Sessio HTTP simulada amb l'atribut d'autenticacio d'admin
    private MockHttpSession adminSession;

    @BeforeEach
    void setUp() {
        mockUserDTO = new AdminUserDTO();
        mockUserDTO.setId(1L);
        mockUserDTO.setEmail("test@example.com");
        mockUserDTO.setUsername("testuser");
        mockUserDTO.setPoints(100L);
        mockUserDTO.setLevel(2L);
        mockUserDTO.setReputacio(5);
        mockUserDTO.setStatus(UserStatus.ACTIVE);

        // Creem una sessio simulada amb l'atribut d'admin autenticat
        adminSession = new MockHttpSession();
        adminSession.setAttribute(AdminSessionInterceptor.SESSION_ATTR, true);
    }

    @Test
    void searchUsers_ReturnsOk() throws Exception {
        Page<AdminUserDTO> page = new PageImpl<>(List.of(mockUserDTO));
        when(adminUserService.searchUsers(any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/users").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("test@example.com"))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));
    }

    @Test
    void getUserProfile_UserExists_ReturnsOk() throws Exception {
        when(adminUserService.getUserProfile(1L)).thenReturn(Optional.of(mockUserDTO));

        mockMvc.perform(get("/api/admin/users/1").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.level").value(2));
    }

    @Test
    void getUserProfile_UserNotFound_ReturnsNotFound() throws Exception {
        when(adminUserService.getUserProfile(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/admin/users/1").session(adminSession))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateUserStatus_ValidStatus_ReturnsOk() throws Exception {
        mockUserDTO.setStatus(UserStatus.BANNED);
        when(adminUserService.updateUserStatus(eq(1L), eq(UserStatus.BANNED))).thenReturn(Optional.of(mockUserDTO));

        mockMvc.perform(patch("/api/admin/users/1/status")
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "BANNED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BANNED"));
    }

    @Test
    void updateUserStatus_InvalidPayload_ReturnsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/admin/users/1/status")
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("invalidKey", "BANNED"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUserStatus_InvalidStatus_ReturnsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/admin/users/1/status")
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "INVALID_STATUS"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUserStatus_UserNotFound_ReturnsNotFound() throws Exception {
        when(adminUserService.updateUserStatus(eq(1L), eq(UserStatus.BANNED))).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/admin/users/1/status")
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "BANNED"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUserIncidents_ReturnsEmptyList() throws Exception {
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setGoogleId("testGoogleId");
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        mockMvc.perform(get("/api/admin/users/1/incidents").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}
