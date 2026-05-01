<<<<<<< develop
package com.safesteps.backend;

import com.safesteps.backend.domain.routecalculator.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("integr ation")
@Tag("integration")
class RouteCalculatorPerformanceTest {

    @Autowired
    private RouteCalculatorService routeCalculatorService;

    @Test
    void testCalculateRoute_UnderTwoAndHalfSeconds_NFR01() {
        // 1. Preparar Dades (Ruta < 5km dins de BCN)
        // Origen: Plaça Catalunya
        Coord origin = new Coord();
        origin.setLat(41.3879);
        origin.setLon(2.1699);

        // Destí: Sagrada Família
        Coord dest = new Coord();
        dest.setLat(41.4036);
        dest.setLon(2.1744);

        Filtre filtre = new Filtre(FiltreEnum.SEGURETAT); // Forcem a calcular amb pes de seguretat

        // 2. Validar Rendiment Estricte (NFR-01: menys de 2.5 segons)
        assertTimeout(Duration.ofMillis(2500), () -> {

            // Demanem 3 rutes alternatives tal com permet el teu DTO
            RouteResponseDTO response = routeCalculatorService.getBestRoute(origin, dest, 3, filtre);

            // 3. Validacions de consistència
            assertNotNull(response, "La resposta no pot ser nul·la");
            assertFalse(response.getRoutes().isEmpty(), "Ha de retornar almenys una ruta");
            assertTrue(response.getRoutes().size() <= 3, "No pot retornar més rutes de les demanades");

        }, "CRÍTIC: El càlcul de la ruta ha superat el temps límit de 2.5 segons (Incompliment del NFR-01)");
    }
=======
package com.safesteps.backend;

import com.safesteps.backend.domain.routecalculator.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("local")
@Tag("integration")
class RouteCalculatorPerformanceTest {

    @Autowired
    private RouteCalculatorService routeCalculatorService;

    @Test
    void testCalculateRoute_UnderTwoAndHalfSeconds_NFR01() {
        // 1. Preparar Dades (Ruta < 5km dins de BCN)
        // Origen: Plaça Catalunya
        Coord origin = new Coord();
        origin.setLat(41.3879);
        origin.setLon(2.1699);

        // Destí: Sagrada Família
        Coord dest = new Coord();
        dest.setLat(41.4036);
        dest.setLon(2.1744);

        Filtre filtre = new Filtre(FiltreEnum.SEGURETAT); // Forcem a calcular amb pes de seguretat

        // 2. Validar Rendiment Estricte (NFR-01: menys de 2.5 segons)
        assertTimeout(Duration.ofMillis(2500), () -> {

            // Demanem 3 rutes alternatives tal com permet el teu DTO
            RouteResponseDTO response = routeCalculatorService.getBestRoute(origin, dest, 3, filtre);

            // 3. Validacions de consistència
            assertNotNull(response, "La resposta no pot ser nul·la");
            assertFalse(response.getRoutes().isEmpty(), "Ha de retornar almenys una ruta");
            assertTrue(response.getRoutes().size() <= 3, "No pot retornar més rutes de les demanades");

        }, "CRÍTIC: El càlcul de la ruta ha superat el temps límit de 2.5 segons (Incompliment del NFR-01)");
    }
>>>>>>> main
}