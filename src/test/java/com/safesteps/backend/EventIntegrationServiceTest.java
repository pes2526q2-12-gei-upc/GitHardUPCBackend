package com.safesteps.backend;

import com.safesteps.backend.domain.routecalculator.Coord;
import com.safesteps.backend.domain.routecalculator.EventExternalResponseDTO.CategoryDTO;
import com.safesteps.backend.domain.routecalculator.EventExternalResponseDTO.EventDTO;
import com.safesteps.backend.domain.routecalculator.EventExternalResponseDTO;
import com.safesteps.backend.domain.routecalculator.EventIntegrationService;
import com.safesteps.backend.domain.routecalculator.PoiDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventIntegrationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private EventIntegrationService eventIntegrationService;

    private Coord sampleCoord;

    @BeforeEach
    void setUp() {
        // Injectem les variables d'entorn simulades i el RestTemplate fals
        ReflectionTestUtils.setField(eventIntegrationService, "apiUrl", "http://fake-api.com/events/");
        ReflectionTestUtils.setField(eventIntegrationService, "apiToken", "fake-token");
        ReflectionTestUtils.setField(eventIntegrationService, "restTemplate", restTemplate);

        sampleCoord = new Coord();
        sampleCoord.setLat(41.3850);
        sampleCoord.setLon(2.1734);
    }

    @Test
    void getEventsForRoute_ShouldCleanNamesAndProcessAllEvents() {
        // Arrange: Preparem 2 esdeveniments (ja no importa si són gratis o de pagament)
        CategoryDTO cat = new CategoryDTO();
        cat.setName("exposicions");

        EventDTO ev1 = new EventDTO();
        ev1.setDenomination("Exposició \"Art Modern\"");
        ev1.setLatitude(41.3851);
        ev1.setLongitude(2.1735);
        ev1.setCategories(Collections.singletonList(cat));

        EventDTO ev2 = new EventDTO();
        ev2.setDenomination("Concert Privat");
        ev2.setLatitude(41.3852);
        ev2.setLongitude(2.1736);

        EventExternalResponseDTO responseDTO = new EventExternalResponseDTO();
        responseDTO.setResults(Arrays.asList(ev1, ev2));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(EventExternalResponseDTO.class)))
                .thenReturn(ResponseEntity.ok(responseDTO));

        // Act
        List<PoiDTO> result = eventIntegrationService.getEventsForRoute(Collections.singletonList(sampleCoord));

        // Assert: Ara esperem 2 resultats perquè ja no hi ha filtre de gratuïtat
        assertEquals(2, result.size());

        // Comprovem que la neteja de nom funciona perfectament en el primer esdeveniment
        boolean foundCleanedName = result.stream().anyMatch(p -> p.getName().equals("Art Modern") && p.getType().equals("EXPOSICIONS"));
        assertTrue(foundCleanedName);
    }

    @Test
    void getEventsForRoute_ShouldLimitToThreeEventsPerPoint() {
        // Arrange: Creem 4 esdeveniments gratuïts
        EventDTO ev1 = createBasicFreeEvent("Ev1");
        EventDTO ev2 = createBasicFreeEvent("Ev2");
        EventDTO ev3 = createBasicFreeEvent("Ev3");
        EventDTO ev4 = createBasicFreeEvent("Ev4");

        EventExternalResponseDTO responseDTO = new EventExternalResponseDTO();
        responseDTO.setResults(Arrays.asList(ev1, ev2, ev3, ev4));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(EventExternalResponseDTO.class)))
                .thenReturn(ResponseEntity.ok(responseDTO));

        // Act
        List<PoiDTO> result = eventIntegrationService.getEventsForRoute(Collections.singletonList(sampleCoord));

        // Assert: El codi hauria de parar al tercer i ignorar el quart
        assertEquals(3, result.size());
    }

    @Test
    void getEventsForRoute_ShouldHandleApiExceptionsGracefully() {
        // Arrange: Forcem un error de xarxa o de connexió
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(EventExternalResponseDTO.class)))
                .thenThrow(new RestClientException("Error de connexió simulada"));

        // Act
        List<PoiDTO> result = eventIntegrationService.getEventsForRoute(Collections.singletonList(sampleCoord));

        // Assert: El codi ha d'atrapar l'excepció i retornar una llista buida sense petar
        assertTrue(result.isEmpty());
    }

    // --- Mètode d'ajuda per crear dades ràpidament ---
    private EventDTO createBasicFreeEvent(String name) {
        EventDTO ev = new EventDTO();
        ev.setDenomination(name);
        ev.setFree(true);
        ev.setLatitude(41.0);
        ev.setLongitude(2.0);
        return ev;
    }
}