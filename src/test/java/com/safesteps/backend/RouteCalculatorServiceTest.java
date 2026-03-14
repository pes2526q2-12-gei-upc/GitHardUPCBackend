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
        //when(routeMock.getNode()).thenReturn(100L);
        //when(routeMock.getCost()).thenReturn(150.0); // 150 metros
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

    @Test
    void getBestRouteTwoRoutesWithCorrectResult() {
        // Preparem les dades d'entrada i simulem la BD
        Coord origin = new Coord(); origin.setLat(41.3889087); origin.setLon(2.1130685);
        Coord dest = new Coord(); dest.setLat(41.3864032); dest.setLon(2.1171256);
        int nRoutes = 2; // Demanem 2 rutes

        //Node origen 1, node desti 5
        when(carrerRepository.findNearestNode(anyDouble(), anyDouble()))
                .thenReturn(1L, 5L);

        // Simulem una Projeccio de Ruta (el que retorna pg_routing)
        RouteDBProjection n1 = mock(RouteDBProjection.class);
        when(n1.getEdge()).thenReturn(100L); // ID del carrer
        when(n1.getNode()).thenReturn(10L);  // ID del node
        when(n1.getCost()).thenReturn(250.0); // 250 metres

        RouteDBProjection n2 = mock(RouteDBProjection.class);
        when(n2.getEdge()).thenReturn(107L); // ID del carrer
        when(n2.getNode()).thenReturn(15L);  // ID del node
        when(n2.getCost()).thenReturn(311.0); // 250 metres

        when(carrerRepository.findPathWithPenalties(anyLong(), anyLong(), anyString()))
                .thenReturn(List.of(n1, n2));

        // Simulem una Projeccio de Coordenades
        CoordDBProjection c1 = mock(CoordDBProjection.class);
        when(c1.getLat()).thenReturn(41.3865032);
        when(c1.getLon()).thenReturn(2.1140685);

        CoordDBProjection c2 = mock(CoordDBProjection.class);
        when(c2.getLat()).thenReturn(41.3875032);
        when(c2.getLon()).thenReturn(2.1160685);

        when(carrerRepository.getCoordsFromNodeIds(any()))
                .thenReturn(List.of(c1, c2));


        // Executem el metode real
        RouteResponseDTO response = routeCalculatorService.getBestRoute(origin, dest, nRoutes);


        // Comprovem que el resultat es l'esperat
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

    //Org = Dest
    @Test
    void getBestRouteTwoRoutesWithCorrectResult2() {
        Coord origin2 = new Coord(); origin2.setLat(41.3889087); origin2.setLon(2.1130685);
        Coord dest2 = new Coord(); dest2.setLat(41.3889088); dest2.setLon(2.1130686);
        int nRoutes = 2;

        //Simulem una ruta d'inici i desti iguals (no hi ha cami)
        when(carrerRepository.findNearestNode(anyDouble(), anyDouble()))
                .thenReturn(5L, 5L);
        when(carrerRepository.findPathWithPenalties(anyLong(), anyLong(), anyString()))
                .thenReturn(List.of());
        CoordDBProjection c1 = mock(CoordDBProjection.class);
        when(c1.getLat()).thenReturn(41.3865032);
        when(c1.getLon()).thenReturn(2.1140685);

        when(carrerRepository.getCoordsFromNodeIds(any()))
                .thenReturn(List.of(c1));


        // Executem el metode real
        RouteResponseDTO response = routeCalculatorService.getBestRoute(origin2, dest2, nRoutes);


        // Comprovem que el resultat es l'esperat
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

        //Simulem una ruta d'inici i desti iguals (no hi ha cami)
        when(carrerRepository.findNearestNode(anyDouble(), anyDouble()))
                .thenReturn(5L, 6L);
        RouteDBProjection n1 = mock(RouteDBProjection.class);
        when(n1.getEdge()).thenReturn(100L); // ID del carrer
        when(n1.getNode()).thenReturn(10L);  // ID del node
        when(n1.getCost()).thenReturn(5.0); // 250 metres
        when(carrerRepository.findPathWithPenalties(anyLong(), anyLong(), anyString()))
                .thenReturn(List.of(n1));
        when(carrerRepository.getCoordsFromNodeIds(any()))
                .thenReturn(List.of());


        // Executem el metode real
        RouteResponseDTO response = routeCalculatorService.getBestRoute(origin, dest, nRoutes);


        // Comprovem que el resultat es l'esperat
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

        //Simulem una ruta d'inici i desti iguals (no hi ha cami)
        when(carrerRepository.findNearestNode(anyDouble(), anyDouble()))
                .thenReturn(5L, 6L);
        RouteDBProjection n1 = mock(RouteDBProjection.class);
        when(n1.getEdge()).thenReturn(null); // ID del carrer
        RouteDBProjection n2 = mock(RouteDBProjection.class);
        when(n1.getEdge()).thenReturn(100L); // ID del carrer
        when(n1.getNode()).thenReturn(10L);  // ID del node
        when(n1.getCost()).thenReturn(0.0); // 250 metres
        when(carrerRepository.findPathWithPenalties(anyLong(), anyLong(), anyString()))
                .thenReturn(List.of(n1,n2));
        when(carrerRepository.getCoordsFromNodeIds(any()))
                .thenReturn(List.of());

        // Executem el metode real
        RouteResponseDTO response = routeCalculatorService.getBestRoute(origin, dest, nRoutes);

        // Comprovem que el resultat es l'esperat
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