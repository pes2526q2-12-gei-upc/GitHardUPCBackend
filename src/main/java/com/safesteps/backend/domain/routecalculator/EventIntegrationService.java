package com.safesteps.backend.domain.routecalculator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.safesteps.backend.domain.routecalculator.EventExternalResponseDTO.EventDTO;
import java.util.*;

@Service
public class EventIntegrationService {

    @Value("${external.events.url}")
    private String apiUrl;

    @Value("${external.events.token}")
    private String apiToken;

    private final RestTemplate restTemplate = new RestTemplate();

    public List<PoiDTO> getEventsForRoute(List<Coord> routePoints) {
        Set<PoiDTO> filteredPois = new HashSet<>();

        // Optimització: Saltem de 5 en 5 punts per no fer massa crides
        for (int i = 0; i < routePoints.size(); i += 5) {
            Coord p = routePoints.get(i);
            // Hem extret la lògica per reduir la complexitat cognitiva (SonarQube)
            fetchAndProcessEventsForCoordinate(p, filteredPois);
        }

        return new ArrayList<>(filteredPois);
    }


    // Aquest mètode s'encarrega d'1 sola coordenada, reduint la niuada de codi
    private void fetchAndProcessEventsForCoordinate(Coord p, Set<PoiDTO> filteredPois) {
        try {
            String url = apiUrl + "?latitude=" + p.getLat() + "&longitude=" + p.getLon();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Token " + apiToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<EventExternalResponseDTO> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, EventExternalResponseDTO.class);

            // Guardem el body en una variable local per evitar l'error de NullPointerException de SonarQube
            EventExternalResponseDTO responseBody = response.getBody();

            if (responseBody != null && responseBody.getResults() != null) {
                List<EventDTO> fetchedEvents = responseBody.getResults();
                int eventsAddedForThisPoint = 0;

                for (EventDTO event : fetchedEvents) {
                    PoiDTO poi = mapToPoiDTO(event);

                    if (filteredPois.add(poi)) {
                        eventsAddedForThisPoint++;
                    }

                    if (eventsAddedForThisPoint >= 3) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error cridant a l'API d'esdeveniments: " + e.getMessage());
        }
    }

    private PoiDTO mapToPoiDTO(EventDTO event) {
        String type = "ESDEVENIMENT"; // Valor per defecte

        String rawName = event.getDenomination();
        String cleanedName = rawName;

        if (rawName != null) {
            // SonarQube Fix: Afegit 'u' al flag (?iu) per suportar l'accent de 'Exposició'
            cleanedName = rawName.replaceFirst("(?iu)^(Exposició|Activitat|Taller)\\s*:?\\s*", "");
            cleanedName = cleanedName.replaceFirst("^\"", "");
            cleanedName = cleanedName.replaceFirst("\"$", "");
        }

        return new PoiDTO(
                type,
                cleanedName,
                event.getDescription(),
                event.getLatitude(),
                event.getLongitude()
        );
    }
}