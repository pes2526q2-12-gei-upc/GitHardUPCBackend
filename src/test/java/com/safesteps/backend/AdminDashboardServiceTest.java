package com.safesteps.backend;

import com.safesteps.backend.domain.admin.dto.AdminDashboardDTO;
import com.safesteps.backend.domain.admin.service.AdminDashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
public class AdminDashboardServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private AdminDashboardService adminDashboardService;

    @BeforeEach
    void setUp() {
        adminDashboardService = new AdminDashboardService(jdbcTemplate, "src/scripts/logs");
    }

    @Test
    void getDashboard_ReturnsDashboardDTO() {
        AdminDashboardDTO result = adminDashboardService.getDashboard();
        
        assertNotNull(result);
        assertNotNull(result.getRouteTypeDistribution());
        assertNotNull(result.getTopOrigins());
        assertNotNull(result.getTopDestinations());
        assertNotNull(result.getActiveUsers());
        assertNotNull(result.getIncidentFunnel());
        assertNotNull(result.getTopReporters());
        assertNotNull(result.getPipelineStatus());
        assertNotNull(result.getLatency());
        assertNotNull(result.getErrorRate());
    }
}
