package com.safesteps.backend;

import com.safesteps.backend.controller.AdminAuthController;
import com.safesteps.backend.security.AdminSessionInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AdminAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private com.safesteps.backend.domain.admin.service.AdminMetricsService adminMetricsService;

    @Autowired
    private AdminAuthController adminAuthController;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adminAuthController, "adminPassword", "secret");
    }

    @Test
    void login_CorrectPassword_ReturnsOk() throws Exception {
        mockMvc.perform(post("/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"));
    }

    @Test
    void login_IncorrectPassword_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("ERROR"));
    }

    @Test
    void logout_ReturnsOk() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AdminSessionInterceptor.SESSION_ATTR, true);

        mockMvc.perform(post("/admin/logout").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"));
    }

    @Test
    void checkAuth_ValidSession_ReturnsOk() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AdminSessionInterceptor.SESSION_ATTR, true);

        mockMvc.perform(get("/admin/check-auth").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"));
    }

    @Test
    void checkAuth_InvalidSession_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/admin/check-auth"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("UNAUTHORIZED"));
    }
}
