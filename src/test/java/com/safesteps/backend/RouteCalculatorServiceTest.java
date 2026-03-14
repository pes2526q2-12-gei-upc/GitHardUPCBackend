package com.safesteps.backend;

import com.safesteps.backend.domain.routecalculator.*;
import com.safesteps.backend.domain.routecalculator.projections.CoordDBProjection;
import com.safesteps.backend.domain.routecalculator.projections.PoiDBProjection;
import com.safesteps.backend.domain.routecalculator.projections.RouteDBProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RouteCalculatorServiceTest {

    @Mock
    private CarrerRepository carrerRepository;

    @InjectMocks
    private RouteCalculatorService routeCalculatorService;

    private Coord origin;
    private Coord destination;

    @BeforeEach
    void setUp() {
        // Preparamos unas coordenadas de prueba en Barcelona
        origin = new Coord();
        origin.setLat(41.3874);
        origin.setLon(2.1686);

        destination = new Coord();
        destination.setLat(41.3986);
        destination.setLon(2.1686);
    }

    @Test
    void getBestRoute_ShouldIncludePoisWithCorrectTypesAndRadii() {
        // -------------------------------------------------------------------
        // 1. GIVEN: Configuramos los "Mocks" (El comportamiento falso de la BD)
        // -------------------------------------------------------------------

        // Simulamos que encuentra los nodos más cercanos
        when(carrerRepository.findNearestNode(origin.getLat(), origin.getLon())).thenReturn(100L);
        when(carrerRepository.findNearestNode(destination.getLat(), destination.getLon())).thenReturn(200L);

        // Simulamos que el algoritmo de Dijkstra devuelve un tramo de ruta
        RouteDBProjection routeMock = mock(RouteDBProjection.class);
        when(routeMock.getNode()).thenReturn(100L);
        when(routeMock.getCost()).thenReturn(150.0); // 150 metros
        when(carrerRepository.findPathWithPenalties(anyLong(), anyLong(), anyString()))
                .thenReturn(List.of(routeMock));

        // Simulamos las coordenadas de la ruta
        CoordDBProjection coordMock = mock(CoordDBProjection.class);
        when(coordMock.getLat()).thenReturn(41.3874);
        when(coordMock.getLon()).thenReturn(2.1686);
        when(carrerRepository.getCoordsFromNodeIds(any())).thenReturn(List.of(coordMock));

        // --- LA MAGIA DE LOS POIs ---

        // A. Simulamos que la BD encuentra 1 Comisaría
        PoiDBProjection comissariaMock = mock(PoiDBProjection.class);
        when(comissariaMock.getName()).thenReturn("Comissaria Test");
        when(comissariaMock.getLat()).thenReturn(41.3880);
        when(comissariaMock.getLon()).thenReturn(2.1690);
        // Verificamos que se llame con el radio exacto de 500.0 metros
        when(carrerRepository.findComissariesNearRoute(any(), eq(500.0))).thenReturn(List.of(comissariaMock));

        // B. Simulamos que la BD encuentra 1 Fuente
        PoiDBProjection fontMock = mock(PoiDBProjection.class);
        when(fontMock.getName()).thenReturn("Font del Gat");
        when(fontMock.getLat()).thenReturn(41.3875);
        when(fontMock.getLon()).thenReturn(2.1687);
        // Verificamos que se llame con el radio exacto de 75.0 metros
        when(carrerRepository.findFontsNearRoute(any(), eq(75.0))).thenReturn(List.of(fontMock));

        // C. El resto de POIs los devolvemos vacíos para no alargar el test
        when(carrerRepository.findBancsNearRoute(any(), anyDouble())).thenReturn(Collections.emptyList());
        when(carrerRepository.findCameresNearRoute(any(), anyDouble())).thenReturn(Collections.emptyList());
        when(carrerRepository.findEscalesNearRoute(any(), anyDouble())).thenReturn(Collections.emptyList());
        when(carrerRepository.findArbresNearRoute(any(), anyDouble())).thenReturn(Collections.emptyList());
        when(carrerRepository.findArbresZonaNearRoute(any(), anyDouble())).thenReturn(Collections.emptyList());


        // -------------------------------------------------------------------
        // 2. WHEN: Ejecutamos el método real de nuestro servicio
        // -------------------------------------------------------------------
        RouteResponseDTO response = routeCalculatorService.getBestRoute(origin, destination, 1);


        // -------------------------------------------------------------------
        // 3. THEN: Verificamos que todo ha funcionado perfectamente
        // -------------------------------------------------------------------
        assertNotNull(response);
        assertEquals(1, response.getRoutes().size());

        Route route = response.getRoutes().get(0);

        // Verificamos que ha empaquetado exactamente 2 POIs (la comisaría y la fuente)
        assertEquals(2, route.getPois().size());

        // Verificamos que el mapeo al PoiDTO es correcto y asigna el "type" bien
        PoiDTO poiComissaria = route.getPois().stream().filter(p -> p.getType().equals("COMISSARIA")).findFirst().orElse(null);
        assertNotNull(poiComissaria);
        assertEquals("Comissaria Test", poiComissaria.getName());
        assertEquals(41.3880, poiComissaria.getLat());

        PoiDTO poiFont = route.getPois().stream().filter(p -> p.getType().equals("FONT")).findFirst().orElse(null);
        assertNotNull(poiFont);
        assertEquals("Font del Gat", poiFont.getName());
        assertEquals(41.3875, poiFont.getLat());

        // Verificamos que el servicio realmente ha hecho las 7 llamadas a la BD
        verify(carrerRepository, times(1)).findComissariesNearRoute(any(), eq(500.0));
        verify(carrerRepository, times(1)).findFontsNearRoute(any(), eq(75.0));
        verify(carrerRepository, times(1)).findBancsNearRoute(any(), eq(30.0));
        verify(carrerRepository, times(1)).findCameresNearRoute(any(), eq(100.0));
        verify(carrerRepository, times(1)).findEscalesNearRoute(any(), eq(50.0));
        verify(carrerRepository, times(1)).findArbresNearRoute(any(), eq(15.0));
        verify(carrerRepository, times(1)).findArbresZonaNearRoute(any(), eq(15.0));
    }
}