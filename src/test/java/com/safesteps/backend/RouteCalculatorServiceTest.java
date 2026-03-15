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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;

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
        origin = new Coord();
        origin.setLat(41.3874);
        origin.setLon(2.1686);

        destination = new Coord();
        destination.setLat(41.3986);
        destination.setLon(2.1686);
    }

    @Test
    void getBestRoute_ShouldIncludePoisWithCorrectTypesAndRadii() {
        // Creem instàncies anònimes per evitar fer mocks de projeccions
        RouteDBProjection routeMock = new RouteDBProjection() {
            @Override public Long getNode() { return 100L; }
            @Override public Long getEdge() { return 1L; }
            @Override public Integer getSeq() { return 1; }
            @Override public Double getCost() { return 150.0; }
        };

        when(carrerRepository.findPathWithPenalties(anyLong(), anyLong(), anyString()))
                .thenReturn(List.of(routeMock));

        CoordDBProjection coordMock = new CoordDBProjection() {
            @Override public Double getLon() { return 2.1686; }
            @Override public Double getLat() { return 41.3874; }
        };

        when(carrerRepository.getCoordsFromNodeIds(any())).thenReturn(List.of(coordMock));

        // Punts d'Interès (També usem classes anònimes per seguretat)
        PoiDBProjection comissariaMock = new PoiDBProjection() {
            @Override public String getName() { return "Comissaria Test"; }
            @Override public Double getLat() { return 41.3880; }
            @Override public Double getLon() { return 2.1690; }
        };
        when(carrerRepository.findComissariesNearRoute(any(), eq(500.0))).thenReturn(List.of(comissariaMock));

        PoiDBProjection fontMock = new PoiDBProjection() {
            @Override public String getName() { return "Font del Gat"; }
            @Override public Double getLat() { return 41.3875; }
            @Override public Double getLon() { return 2.1687; }
        };
        when(carrerRepository.findFontsNearRoute(any(), eq(75.0))).thenReturn(List.of(fontMock));

        // Llistes buides per la resta
        when(carrerRepository.findBancsNearRoute(any(), anyDouble())).thenReturn(Collections.emptyList());
        when(carrerRepository.findCameresNearRoute(any(), anyDouble())).thenReturn(Collections.emptyList());
        when(carrerRepository.findEscalesNearRoute(any(), anyDouble())).thenReturn(Collections.emptyList());
        when(carrerRepository.findArbresNearRoute(any(), anyDouble())).thenReturn(Collections.emptyList());
        when(carrerRepository.findArbresZonaNearRoute(any(), anyDouble())).thenReturn(Collections.emptyList());

        RouteResponseDTO response = routeCalculatorService.getBestRoute(origin, destination, 1);

        assertNotNull(response);
        assertEquals(1, response.getRoutes().size());

        Route route = response.getRoutes().get(0);
        assertEquals(2, route.getPois().size());

        PoiDTO poiComissaria = route.getPois().stream().filter(p -> p.getType().equals("COMISSARIA")).findFirst().orElse(null);
        assertNotNull(poiComissaria);
        assertEquals("Comissaria Test", poiComissaria.getName());
        assertEquals(41.3880, poiComissaria.getLat());

        PoiDTO poiFont = route.getPois().stream().filter(p -> p.getType().equals("FONT")).findFirst().orElse(null);
        assertNotNull(poiFont);
        assertEquals("Font del Gat", poiFont.getName());
        assertEquals(41.3875, poiFont.getLat());

        verify(carrerRepository, times(1)).findComissariesNearRoute(any(), eq(500.0));
        verify(carrerRepository, times(1)).findFontsNearRoute(any(), eq(75.0));
    }

    @Test
    void getBestRouteTwoRoutesWithCorrectResult() {
        Coord origin = new Coord(); origin.setLat(41.3889087); origin.setLon(2.1130685);
        Coord dest = new Coord(); dest.setLat(41.3864032); dest.setLon(2.1171256);
        int nRoutes = 2;

        when(carrerRepository.findNearestNode(anyDouble(), anyDouble()))
                .thenReturn(1L, 5L);

        RouteDBProjection n1 = mock(RouteDBProjection.class);
        when(n1.getEdge()).thenReturn(100L);
        when(n1.getNode()).thenReturn(10L);
        when(n1.getCost()).thenReturn(250.0);

        RouteDBProjection n2 = mock(RouteDBProjection.class);
        when(n2.getEdge()).thenReturn(107L);
        when(n2.getNode()).thenReturn(15L);
        when(n2.getCost()).thenReturn(311.0);

        when(carrerRepository.findPathWithPenalties(anyLong(), anyLong(), anyString()))
                .thenReturn(List.of(n1, n2));

        CoordDBProjection c1 = mock(CoordDBProjection.class);
        when(c1.getLat()).thenReturn(41.3865032);
        when(c1.getLon()).thenReturn(2.1140685);

        CoordDBProjection c2 = mock(CoordDBProjection.class);
        when(c2.getLat()).thenReturn(41.3875032);
        when(c2.getLon()).thenReturn(2.1160685);

        when(carrerRepository.getCoordsFromNodeIds(any()))
                .thenReturn(List.of(c1, c2));

        RouteResponseDTO response = routeCalculatorService.getBestRoute(origin, dest, nRoutes);

        assertNotNull(response);
        assertEquals(nRoutes, response.getRoutes().size(), "Ha de retornar exactament 2 rutes");

        Route primeraRuta = response.getRoutes().getFirst();
        assertEquals(561.0, primeraRuta.getDistanceMeters(), "La distancia ha de sumar 561m");
        assertEquals(7, primeraRuta.getEstimatedTimeMinutes(), "561m a 83.33m/min -> 6.7 -> 7m");
        assertEquals(2, primeraRuta.getCoordinates().size(), "Ha de tenir 1 coordenada simulada");

        verify(carrerRepository, times(1)).findNearestNode(origin.getLat(), origin.getLon());
        verify(carrerRepository, times(1)).findNearestNode(dest.getLat(), dest.getLon());
        verify(carrerRepository, times(2)).findPathWithPenalties(anyLong(), anyLong(), anyString());
    }

    @Test
    void getBestRouteTwoRoutesWithCorrectResult2() {
        Coord origin2 = new Coord(); origin2.setLat(41.3889087); origin2.setLon(2.1130685);
        Coord dest2 = new Coord(); dest2.setLat(41.3889088); dest2.setLon(2.1130686);
        int nRoutes = 2;

        when(carrerRepository.findNearestNode(anyDouble(), anyDouble()))
                .thenReturn(5L, 5L);
        when(carrerRepository.findPathWithPenalties(anyLong(), anyLong(), anyString()))
                .thenReturn(List.of());
        CoordDBProjection c1 = mock(CoordDBProjection.class);
        when(c1.getLat()).thenReturn(41.3865032);
        when(c1.getLon()).thenReturn(2.1140685);

        when(carrerRepository.getCoordsFromNodeIds(any()))
                .thenReturn(List.of(c1));

        RouteResponseDTO response = routeCalculatorService.getBestRoute(origin2, dest2, nRoutes);

        assertNotNull(response);
        assertEquals(nRoutes, response.getRoutes().size(), "Ha de retornar exactament 2 rutes");

        Route primeraRuta = response.getRoutes().getFirst();
        assertEquals(0.0, primeraRuta.getDistanceMeters(), "No ens hem de moure -> 0m");
        assertEquals(0, primeraRuta.getEstimatedTimeMinutes(), "0m a 83.3m/min -> 0m");
        assertEquals(1, primeraRuta.getCoordinates().size(), "Com no ens hem de moure no ha de tenir cap node");

        verify(carrerRepository, times(1)).findNearestNode(origin2.getLat(), origin2.getLon());
        verify(carrerRepository, times(1)).findNearestNode(dest2.getLat(), dest2.getLon());
        verify(carrerRepository, times(2)).findPathWithPenalties(anyLong(), anyLong(), anyString());
    }

    @Test
    void getBestRouteTwoRoutesWithCorrectResult3() {
        Coord origin = new Coord(); origin.setLat(41.3889087); origin.setLon(2.1130685);
        Coord dest = new Coord(); dest.setLat(41.3889087); dest.setLon(2.1130686);
        int nRoutes = 2;

        when(carrerRepository.findNearestNode(anyDouble(), anyDouble()))
                .thenReturn(5L, 6L);
        RouteDBProjection n1 = mock(RouteDBProjection.class);
        when(n1.getEdge()).thenReturn(100L);
        when(n1.getNode()).thenReturn(10L);
        when(n1.getCost()).thenReturn(5.0);
        when(carrerRepository.findPathWithPenalties(anyLong(), anyLong(), anyString()))
                .thenReturn(List.of(n1));
        when(carrerRepository.getCoordsFromNodeIds(any()))
                .thenReturn(List.of());

        RouteResponseDTO response = routeCalculatorService.getBestRoute(origin, dest, nRoutes);

        assertNotNull(response);
        assertEquals(nRoutes, response.getRoutes().size(), "Ha de retornar exactament 2 rutes");

        Route primeraRuta = response.getRoutes().getFirst();
        assertEquals(5.0, primeraRuta.getDistanceMeters(), "No ens hem de moure -> 0m");
        assertEquals(1, primeraRuta.getEstimatedTimeMinutes(), "0m a 83.3m/min -> 0m");
        assertEquals(0, primeraRuta.getCoordinates().size(), "Com no ens hem de moure no ha de tenir cap node");

        verify(carrerRepository, times(1)).findNearestNode(origin.getLat(), origin.getLon());
        verify(carrerRepository, times(1)).findNearestNode(dest.getLat(), dest.getLon());
        verify(carrerRepository, times(2)).findPathWithPenalties(anyLong(), anyLong(), anyString());
    }

    @Test
    void getBestRouteTwoRoutesError() {
        Coord origin = new Coord(); origin.setLat(41.3889087); origin.setLon(2.1130685);
        Coord dest = new Coord(); dest.setLat(41.3889087); dest.setLon(2.1130686);
        int nRoutes = 2;

        when(carrerRepository.findNearestNode(anyDouble(), anyDouble()))
                .thenReturn(5L, 6L);
        RouteDBProjection n1 = mock(RouteDBProjection.class);
        when(n1.getEdge()).thenReturn(null);
        RouteDBProjection n2 = mock(RouteDBProjection.class);
        when(n2.getEdge()).thenReturn(100L); // He corregit un petit typo aquí: posava when(n1.getEdge()) de nou!
        when(n2.getNode()).thenReturn(10L);
        when(n2.getCost()).thenReturn(0.0);

        when(carrerRepository.findPathWithPenalties(anyLong(), anyLong(), anyString()))
                .thenReturn(List.of(n1, n2));
        when(carrerRepository.getCoordsFromNodeIds(any()))
                .thenReturn(List.of());

        RouteResponseDTO response = routeCalculatorService.getBestRoute(origin, dest, nRoutes);

        assertNotNull(response);
        assertEquals(nRoutes, response.getRoutes().size(), "Ha de retornar exactament 2 rutes");

        Route primeraRuta = response.getRoutes().getFirst();
        assertEquals(0.0, primeraRuta.getDistanceMeters(), "No ens hem de moure -> 0m");
        assertEquals(0, primeraRuta.getEstimatedTimeMinutes(), "0m a 83.3m/min -> 0m");
        assertEquals(0, primeraRuta.getCoordinates().size(), "Com no ens hem de moure no ha de tenir cap node");

        verify(carrerRepository, times(1)).findNearestNode(origin.getLat(), origin.getLon());
        verify(carrerRepository, times(1)).findNearestNode(dest.getLat(), dest.getLon());
        verify(carrerRepository, times(2)).findPathWithPenalties(anyLong(), anyLong(), anyString());
    }
}