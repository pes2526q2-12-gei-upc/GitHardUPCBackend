package com.safesteps.backend.domain.routecalculator;

import com.safesteps.backend.domain.routecalculator.projections.CoordDBProjection;
import com.safesteps.backend.domain.routecalculator.projections.RouteDBProjection;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RouteCalculatorService {

    //Injeccio per constructor (Clean Architecture)
    private final CarrerRepository carrerRepository;

    public RouteCalculatorService(CarrerRepository carrerRepository) {
        this.carrerRepository = carrerRepository;
    }

    public RouteResponseDTO getBestRoute(Coord org, Coord dest, int nRoutes) {
        // Com ens arriba en lat i long, ho hem de passar als identificadors dels carrers mes propers
        Long start = carrerRepository.findNearestNode(org.getLat(), org.getLon());
        Long end = carrerRepository.findNearestNode(dest.getLat(), dest.getLon());

        // Conjunt de nodes que s'han visitat en la ruta principal o en les alternatives. Ens permet crear rutes alt. que intentin no passar per aquests nodes i així generar rutes diferents.
        Set<Long> edgesVisitats = new HashSet<>();
        RouteResponseDTO response = new RouteResponseDTO();
        for (int i = 0; i < nRoutes; ++i)
            response.getRoutes().add(calculRuta(edgesVisitats,  start, end));

        return response;
    }

    private Route calculRuta(Set<Long> edgesVisitats, Long org, Long dest) {
        String formattedEdges = formatEdgesSetForDB(edgesVisitats);
        List<RouteDBProjection> rutaPrincipal = carrerRepository.findPathWithPenalties(org, dest, formattedEdges);

        Long[] rutaPrincipalFID = new Long[rutaPrincipal.size()];
        double distanceMeters = 0.0;
        int i = 0;
        // Extraurem els IDs dels nodes i sumem la distància física real pas a pas
        for (RouteDBProjection rp : rutaPrincipal) {
            if (rp.getEdge() != null && rp.getEdge() > 0) edgesVisitats.add(rp.getEdge());
            rutaPrincipalFID[i++] = rp.getNode();
            distanceMeters += rp.getCost();
        }

        // Necessitem passar els fids dels nodes a coordenades per a que el frontend pugui treballar amb elles
        List<CoordDBProjection> rutaFIDCoords = carrerRepository.getCoordsFromNodeIds(rutaPrincipalFID);

        // S'ha de transformar la llista de Object[] a una llista de Double[]
        List<Double[]> ruta = rutaFIDCoords.stream()
                .map(r -> new Double[]{ r.getLon(), r.getLat() })
                .toList();

        return new Route(ruta, Math.round(distanceMeters * 100.0) / 100.0, calculateEstimatedTime(distanceMeters));
    }

    // Calcular el temps estimat (Velocitat a peu de 5 km/h -> ~83.33 m/min)
    private int calculateEstimatedTime(double distance) {
        int time = (int) Math.round(distance / 83.33);
        return (time == 0 && distance > 0) ? 1  : time;
    }

    private String formatEdgesSetForDB(Set<Long> edgesVisitats) {
        return edgesVisitats.isEmpty() ? "-1" : String.join(",",
                edgesVisitats.stream().map(String::valueOf).toArray(String[]::new));
    }
}