package com.safesteps.backend.domain.routecalculator;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RouteCalculatorService {

    // 1. Injecció per constructor (Clean Architecture, adeu @Autowired!)
    private final CarrerRepository carrerRepository;

    public RouteCalculatorService(CarrerRepository carrerRepository) {
        this.carrerRepository = carrerRepository;
    }

    public RouteResponseDTO getBestRoute(Coord org, Coord dest) {
        // Com ens arriba en lat i long, ho hem de passar als identificadors dels carrers mes propers
        Long start = carrerRepository.findNearestNode(org.getLat(), org.getLon());
        Long end = carrerRepository.findNearestNode(dest.getLat(), dest.getLon());

        // Executar Dijkstra a prova de futur: extreu el node i la longitud real topogràfica del tram
        List<Object[]> pathResult = carrerRepository.findShortestPathWithCost(start, end);

        // Extraurem els IDs dels nodes i sumem la distància física real pas a pas
        Long[] rutaFID = new Long[pathResult.size()];
        double distanceMeters = 0.0;

        for (int i = 0; i < pathResult.size(); i++) {
            Object[] row = pathResult.get(i);
            rutaFID[i] = ((Number) row[0]).longValue();

            // Sumem els metres reals d'aquest tram concret per blindar el càlcul
            distanceMeters += ((Number) row[1]).doubleValue();
        }

        // Necessitem passar els fids dels nodes a coordenades per a que el frontend pugui treballar amb elles
        List<Object[]> rutaFIDCoords = carrerRepository.getCoordsFromNodeIds(rutaFID);

        // S'ha de transformar la llista de Object[] a una llista de Double[]
        List<Double[]> ruta = rutaFIDCoords.stream()
                .map(row -> new Double[]{ ((Number) row[0]).doubleValue(), ((Number) row[1]).doubleValue() })
                .toList();

        // Calcular el temps estimat (Velocitat a peu de 5 km/h -> ~83.33 m/min)
        int estimatedTimeMinutes = (int) Math.round(distanceMeters / 83.33);
        if (estimatedTimeMinutes == 0 && distanceMeters > 0) {
            estimatedTimeMinutes = 1; // Mínim 1 minut si hi ha desplaçament real
        }

        // Muntar la resposta enriquida per al Frontend
        RouteResponseDTO response = new RouteResponseDTO();
        response.setRuta(ruta);
        // Arrodonim la distància a 2 decimals (ex: 1540.25 m) per tenir un JSON net
        response.setDistanceMeters(Math.round(distanceMeters * 100.0) / 100.0);
        response.setEstimatedTimeMinutes(estimatedTimeMinutes);

        return response;
    }
}