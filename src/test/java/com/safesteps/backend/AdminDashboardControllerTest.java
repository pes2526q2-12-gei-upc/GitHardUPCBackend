package com.safesteps.backend;

import com.safesteps.backend.controller.AdminDashboardController;
import com.safesteps.backend.domain.admin.dto.AdminDashboardDTO;
import com.safesteps.backend.domain.admin.service.AdminDashboardService;
import com.safesteps.backend.security.AdminSessionInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminDashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private com.safesteps.backend.domain.admin.service.AdminMetricsService adminMetricsService;

    @MockBean
    private AdminDashboardService adminDashboardService;

    private AdminDashboardDTO dashboardDTO;
    private MockHttpSession adminSession;

    @BeforeEach
    void setUp() {
        dashboardDTO = new AdminDashboardDTO();
        
        adminSession = new MockHttpSession();
        adminSession.setAttribute(AdminSessionInterceptor.SESSION_ATTR, true);
    }

    @Test
    void getDashboard_ReturnsOk() throws Exception {
        when(adminDashboardService.getDashboard()).thenReturn(dashboardDTO);

        mockMvc.perform(get("/api/admin/dashboard").session(adminSession))
                .andExpect(status().isOk());
    }
}
