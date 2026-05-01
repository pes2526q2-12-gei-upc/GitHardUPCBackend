package com.safesteps.backend;

import com.safesteps.backend.domain.routecalculator.Coord;
import com.safesteps.backend.domain.routecalculator.CarrerRepository;
import com.safesteps.backend.domain.routecalculator.ExternalSafetyResponseDTO;
import com.safesteps.backend.domain.routecalculator.RouteEvaluationSafetyService;
import com.safesteps.backend.domain.routecalculator.projections.RouteAveragesDBProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RouteEvaluationSafetyServiceTest {

    @Mock
    private CarrerRepository carrerRepository;

    @InjectMocks
    private RouteEvaluationSafetyService routeEvaluationSafetyService;

    private List<Coord> sampleRoute;
    private String expectedWkt;

    @BeforeEach
    void setUp() {
        // Preparem una ruta de prova estàndard
        Coord p1 = new Coord();
        p1.setLon(2.1600);
        p1.setLat(41.3800);

        Coord p2 = new Coord();
        p2.setLon(2.1700);
        p2.setLat(41.3900);

        sampleRoute = Arrays.asList(p1, p2);

        // Aquest és el String que hauria de generar el mètode buildLineString
        expectedWkt = "LINESTRING(2.16 41.38, 2.17 41.39)";
    }

    @Test
    void evaluateSafetyIndex_ShouldReturnAlta_WhenSafeArea() {
        // Arrange: Barri segur (pocs delictes, força vigilància)
        RouteAveragesDBProjection averagesMock = new RouteAveragesDBProjection() {
            @Override public Double getAvgDelictes() { return 1000.0; }
            @Override public Double getAvgCameres() { return 0.05; } // 0.05 * 20 = 1.0 (Vigilància max)
            @Override public Double getAvgComissaries() { return 10.0; }
        };

        when(carrerRepository.getRouteAveragesFromWKT(eq(expectedWkt))).thenReturn(averagesMock);

        // Act
        ExternalSafetyResponseDTO response = routeEvaluationSafetyService.evaluateSafetyIndex(sampleRoute);

        assertNotNull(response);
        assertEquals(9.8, response.getSafetyIndex());
        assertEquals("ALTA", response.getSafetyLevel());

        verify(carrerRepository, times(1)).getRouteAveragesFromWKT(eq(expectedWkt));
    }

    @Test
    void evaluateSafetyIndex_ShouldReturnModerada_WhenMediumRiskAndNoCameras() {
        // Arrange: Barri amb risc mitjà i sense càmeres (s'activa la penalització extra de foscor)
        RouteAveragesDBProjection averagesMock = new RouteAveragesDBProjection() {
            @Override public Double getAvgDelictes() { return 10000.0; }
            @Override public Double getAvgCameres() { return 0.0; }
            @Override public Double getAvgComissaries() { return 0.0; }
            };

        when(carrerRepository.getRouteAveragesFromWKT(eq(expectedWkt))).thenReturn(averagesMock);

        // Act
        ExternalSafetyResponseDTO response = routeEvaluationSafetyService.evaluateSafetyIndex(sampleRoute);

        // Assert
        // Penalització crim: (10000/15000)*5 = 3.33 (No mitigada perquè no hi ha vigilància)
        // Penalització foscor: Vigilància < 0.3. Risc = (10000-8000)/10000 = 0.2. Penalty = (0.3-0) * 5.0 * 0.2 = 0.3
        // Total: 10 - 3.33 - 0.3 = 6.36 -> arrodonit 6.4
        assertNotNull(response);
        assertEquals(6.4, response.getSafetyIndex());
        assertEquals("MODERADA", response.getSafetyLevel());

        verify(carrerRepository, times(1)).getRouteAveragesFromWKT(anyString());
    }

    @Test
    void evaluateSafetyIndex_ShouldReturnBaixa_WhenHighRisk() {
        // Arrange: Barri molt perillós (molts delictes, superant el màxim, i sense vigilància)
        RouteAveragesDBProjection averagesMock = new RouteAveragesDBProjection() {
            @Override public Double getAvgDelictes() { return 25000.0; } // Supera el topall de 15000
            @Override public Double getAvgCameres() { return 0.0; }
            @Override public Double getAvgComissaries() { return 0.0; }
        };

        when(carrerRepository.getRouteAveragesFromWKT(anyString())).thenReturn(averagesMock);

        // Act
        ExternalSafetyResponseDTO response = routeEvaluationSafetyService.evaluateSafetyIndex(sampleRoute);

        // Assert
        // Penalització crim: Màxim aplicat = 5.0. No mitigada (0 vigilància).
        // Penalització foscor: Risc = (25000-8000)/10000 = 1.7 -> Limitat a 1.0. Penalty = 0.3 * 5.0 * 1.0 = 1.5.
        // Total: 10 - 5.0 - 1.5 = 3.5
        assertNotNull(response);
        assertEquals(3.5, response.getSafetyIndex());
        assertEquals("BAIXA", response.getSafetyLevel());
    }
}