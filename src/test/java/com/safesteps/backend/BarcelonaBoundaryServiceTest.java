package com.safesteps.backend.domain.routecalculator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Geometry;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BarcelonaBoundaryServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private BarcelonaBoundaryService boundaryService;

    @BeforeEach
    void setUp() {
        // Reiniciamos el estado interno antes de cada test para evitar contaminación
        ReflectionTestUtils.setField(boundaryService, "barcelonaBoundary", null);
        ReflectionTestUtils.setField(boundaryService, "isUsingFallback", false);
    }

    @Test
    void loadBarcelonaBoundary_DbFails_UsesFallback() {
        // Arrange: Simulamos que la BD está caída o no tiene la vista
        when(jdbcTemplate.queryForObject(anyString(), eq(byte[].class)))
                .thenThrow(new DataRetrievalFailureException("Simulated DB failure"));
        doThrow(new DataRetrievalFailureException("Simulated View Creation failure"))
                .when(jdbcTemplate).execute(anyString());

        // Act
        boundaryService.loadBarcelonaBoundary();

        // Assert: Debería haber activado el fallback
        assertTrue(boundaryService.isUsingFallbackBoundary(), "Debería usar el fallback si la BD falla");
        assertNotNull(boundaryService.getBarcelonaBoundary(), "El polígono no debe ser nulo");
    }

    @Test
    void isWithinBarcelona_UsingFallback_PointInside_ReturnsTrue() {
        // Arrange: Forzamos el fallback
        forceFallback();

        // Act: Plaça Catalunya (Centro de BCN, dentro del fallback)
        boolean result = boundaryService.isWithinBarcelona(2.1700, 41.3871);

        // Assert
        assertTrue(result, "Plaça Catalunya debería estar dentro de BCN");
    }

    @Test
    void isWithinBarcelona_UsingFallback_PointOutside_ReturnsFalse() {
        // Arrange: Forzamos el fallback
        forceFallback();

        // Act: Madrid (Fuera de BCN)
        boolean result = boundaryService.isWithinBarcelona(-3.7038, 40.4168);

        // Assert
        assertFalse(result, "Madrid no debería estar dentro de BCN");
    }

    @Test
    void isWithinBarcelona_UsingFallback_PointOnEdge_ReturnsTrue() {
        // Arrange
        forceFallback();

        // Act: Esquina Noroeste del Fallback (2.05, 41.47)
        boolean result = boundaryService.isWithinBarcelona(2.05, 41.47);

        // Assert
        assertTrue(result, "Los bordes del polígono deberían considerarse dentro");
    }

    // --- Métodos Auxiliares ---

    private void forceFallback() {
        when(jdbcTemplate.queryForObject(anyString(), eq(byte[].class)))
                .thenThrow(new RuntimeException("Fail"));
        doThrow(new RuntimeException("Fail")).when(jdbcTemplate).execute(anyString());
        boundaryService.loadBarcelonaBoundary();
    }
}