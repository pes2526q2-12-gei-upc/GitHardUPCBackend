package com.safesteps.backend.domain.routecalculator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class EventIntegrationService {

    @Value("${external.events.url}")
    private String apiUrl;

    @Value("${external.events.token}")
    private String apiToken;

    private final RestTemplate restTemplate = new RestTemplate();

    public List<PoiDTO> getEventsForRoute(List<Coord> routePoints) {
        // Utilitzem un HashSet per evitar que s'afegeixin esdeveniments duplicats
        Set<PoiDTO> filteredPois = new HashSet<>();

        // Optimització: Saltem de 5 en 5 punts per no fer massa crides a la seva API
        for (int i = 0; i < routePoints.size(); i += 5) {
            Coord p = routePoints.get(i);

            try {
                String url = apiUrl + "?latitude=" + p.getLat() + "&longitude=" + p.getLon();

                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "Token " + apiToken);
                HttpEntity<String> entity = new HttpEntity<>(headers);

                ResponseEntity<EventExternalResponseDTO> response = restTemplate.exchange(
                        url, HttpMethod.GET, entity, EventExternalResponseDTO.class);

                if (response.getBody() != null && response.getBody().getResults() != null) {
                    List<EventDTO> fetchedEvents = response.getBody().getResults();

                    int eventsAddedForThisPoint = 0;

                    for (EventDTO event : fetchedEvents) {

                            // Transformar EventDTO a PoiDTO
                        PoiDTO poi = mapToPoiDTO(event);

                            // Si l'hem pogut afegir (no estava repetit), sumem 1
                        if (filteredPois.add(poi)) {
                            eventsAddedForThisPoint++;
                        }


                        // FILTRE: Màxim 3 esdeveniments nous per cada coordenada analitzada
                        if (eventsAddedForThisPoint >= 3) {
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error cridant a l'API d'esdeveniments: " + e.getMessage());
            }
        }

        return new ArrayList<>(filteredPois);
    }

    // --- Mètode Privat d'Ajuda per fer la traducció ---
    private PoiDTO mapToPoiDTO(EventDTO event) {
        String type = "ESDEVENIMENT"; // Valor per defecte

        // Si tenim la llista de categories, agafem la primera i la posem en majúscules
        if (event.getCategories() != null && !event.getCategories().isEmpty()) {
            type = event.getCategories().get(0).getName().toUpperCase();
        }

        String rawName = event.getDenomination();
        String cleanedName = rawName;

        if (rawName != null) {
            // 1. Eliminem el prefix 'Exposició "' (ignorant majúscules/minúscules amb (?i))
            // 2. Eliminem la cometa final '"'
            cleanedName = rawName
                    .replaceFirst("(?i)^Exposició\\s+\"", "") // Busca "Exposició " + cometa al principi
                    .replaceFirst("\"$", "");                // Busca la cometa final a l'últim caràcter
        }
        return new PoiDTO(
                type,
                cleanedName,
                event.getLatitude(),
                event.getLongitude()
        );
    }
}