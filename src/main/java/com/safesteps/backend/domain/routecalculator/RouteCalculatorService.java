package com.safesteps.backend.domain.routecalculator;

import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        List<Object[]> rutaPrincipal = carrerRepository.findShortestPathWithCost(start, end);

        // Conjunt de nodes que s'han visitat en la ruta principal o en les alternatives. Ens permet crear rutes alt. que intentin no passar per aquests nodes i així generar rutes diferents.
        Set<Long> nodesVisitats = new HashSet<>();
        rutaPrincipal.forEach(r -> {if((Long) r[2] > 0) nodesVisitats.add(((Number) r[2]).longValue());});

        // Extraurem els IDs dels nodes i sumem la distància física real pas a pas
        Long[] rutaPrincipalFID = new Long[rutaPrincipal.size()];
        double distanceMeters = 0.0;

        for (int i = 0; i < rutaPrincipal.size(); i++) {
            Object[] row = rutaPrincipal.get(i);
            rutaPrincipalFID[i] = ((Number) row[0]).longValue();

            // Sumem els metres reals d'aquest tram concret per blindar el càlcul
            distanceMeters += ((Number) row[1]).doubleValue();
        }

        String formattedEdges = "";
        if (!nodesVisitats.isEmpty()) {
            // Generem un String net separat per comes: "1,2,3,4"
            formattedEdges = String.join(",",
                    nodesVisitats.stream().map(String::valueOf).toArray(String[]::new));
        }

        List<Object[]> rutaAlternativa1 = carrerRepository.findPathWithPenalties(start, end, formattedEdges);
        rutaAlternativa1.forEach(r -> {if((int) r[2] > 0) nodesVisitats.add(((Number) r[2]).longValue());});

        Long[] rutaAlternativa1FID = new Long[rutaAlternativa1.size()];

        for (int i = 0; i < rutaAlternativa1.size(); i++) {
            Object[] row = rutaAlternativa1.get(i);
            rutaAlternativa1FID[i] = ((Number) row[0]).longValue();
        }

        // Necessitem passar els fids dels nodes a coordenades per a que el frontend pugui treballar amb elles
        List<Object[]> rutaFIDCoords = carrerRepository.getCoordsFromNodeIds(rutaPrincipalFID);

        // S'ha de transformar la llista de Object[] a una llista de Double[]
        List<Double[]> ruta = rutaFIDCoords.stream()
                .map(row -> new Double[]{ ((Number) row[0]).doubleValue(), ((Number) row[1]).doubleValue() })
                .toList();


        List<Object[]> rutaAltFIDCoords = carrerRepository.getCoordsFromNodeIds(rutaAlternativa1FID);
        List<Double[]> rutaAlt = rutaAltFIDCoords.stream()
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
        response.setAlt(rutaAlt);

        return response;
    }
}