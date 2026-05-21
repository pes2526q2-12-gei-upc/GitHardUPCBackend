package com.safesteps.backend;

import com.safesteps.backend.domain.routecalculator.Coord;
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
        ReflectionTestUtils.setField(eventIntegrationService, "apiUrl", "http://fake-api.com/events/");
        ReflectionTestUtils.setField(eventIntegrationService, "apiToken", "fake-token");
        ReflectionTestUtils.setField(eventIntegrationService, "restTemplate", restTemplate);

        sampleCoord = new Coord();
        sampleCoord.setLat(41.3850);
        sampleCoord.setLon(2.1734);
    }

    @Test
    void getEventsForRoute_ShouldCleanNamesAndProcessAllEvents() {
        // Arrange: Preparem 2 esdeveniments sense el flag 'free' ni categories
        EventDTO ev1 = new EventDTO();
        ev1.setDenomination("Exposició \"Art Modern\"");
        ev1.setLatitude(41.3851);
        ev1.setLongitude(2.1735);
        ev1.setDescription("Descripció de l'art modern");

        EventDTO ev2 = new EventDTO();
        ev2.setDenomination("Concert Privat");
        ev2.setLatitude(41.3852);
        ev2.setLongitude(2.1736);
        ev2.setDescription("Descripció del concert");

        EventExternalResponseDTO responseDTO = new EventExternalResponseDTO();
        responseDTO.setResults(Arrays.asList(ev1, ev2));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(EventExternalResponseDTO.class)))
                .thenReturn(ResponseEntity.ok(responseDTO));

        // Act
        List<PoiDTO> result = eventIntegrationService.getEventsForRoute(Collections.singletonList(sampleCoord));

        // Assert: Esperem 2 resultats
        assertEquals(2, result.size());

        // Comprovem que la neteja de nom funciona.
        // Com que hem tret categories, el type serà el defecte: "ESDEVENIMENT"
        boolean foundCleanedName = result.stream().anyMatch(p -> p.getName().equals("Art Modern") && p.getType().equals("ESDEVENIMENT"));
        assertTrue(foundCleanedName);
    }

    @Test
    void getEventsForRoute_ShouldLimitToThreeEventsPerPoint() {
        // Arrange
        EventDTO ev1 = createBasicEvent("Ev1");
        EventDTO ev2 = createBasicEvent("Ev2");
        EventDTO ev3 = createBasicEvent("Ev3");
        EventDTO ev4 = createBasicEvent("Ev4");

        EventExternalResponseDTO responseDTO = new EventExternalResponseDTO();
        responseDTO.setResults(Arrays.asList(ev1, ev2, ev3, ev4));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(EventExternalResponseDTO.class)))
                .thenReturn(ResponseEntity.ok(responseDTO));

        // Act
        List<PoiDTO> result = eventIntegrationService.getEventsForRoute(Collections.singletonList(sampleCoord));

        // Assert: S'ha de limitar a 3
        assertEquals(3, result.size());
    }

    @Test
    void getEventsForRoute_ShouldHandleApiExceptionsGracefully() {
        // Arrange
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(EventExternalResponseDTO.class)))
                .thenThrow(new RestClientException("Error de connexió simulada"));

        // Act
        List<PoiDTO> result = eventIntegrationService.getEventsForRoute(Collections.singletonList(sampleCoord));

        // Assert
        assertTrue(result.isEmpty());
    }

    // --- Mètode d'ajuda actualitzat ---
    private EventDTO createBasicEvent(String name) {
        EventDTO ev = new EventDTO();
        ev.setDenomination(name);
        ev.setLatitude(41.0);
        ev.setLongitude(2.0);
        ev.setDescription("Descripció de prova");
        return ev;
    }
}