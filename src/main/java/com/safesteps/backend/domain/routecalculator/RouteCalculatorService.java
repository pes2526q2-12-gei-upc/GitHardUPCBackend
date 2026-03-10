package com.safesteps.backend.domain.routecalculator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RouteCalculatorService {

    @Autowired
    private CarrerRepository carrerRepository;

    public RouteResponseDTO getBestRoute(Coord org, Coord dest) {
        //Com ens arriba en lat i long, ho hem de passar als identificadors dels carrers mes propers
        Long start = carrerRepository.findNearestNode(org.getLat(), org.getLon());
        Long end = carrerRepository.findNearestNode(dest.getLat(), dest.getLon());

        //Un cop els tinguem, podem trobar el cami mes curt (ens retorna una llista de nodes)
        Long[] rutaFID = carrerRepository.findShortestPath(start, end);

        //Necessitem passar els fids dels nodes a coordenades per a que el frontend pugui treballar amb elles (lat i long)
        List<Object[]> rutaFIDCoords = carrerRepository.getCoordsFromNodeIds(rutaFID);

        //S'ha de transformar la llista de Object[] a una llista de Double[] per a que sigui mes facil de treballar en el frontend
        List<Double[]> ruta = rutaFIDCoords.stream()
                .map(row -> new Double[]{ ((Number) row[0]).doubleValue(), ((Number) row[1]).doubleValue() })
                .toList();

        //De moment nomes es retorna la llista de coordenades, pero es podria ampliar per a retornar mes informacio (distancia total, temps estimat, etc.)
        RouteResponseDTO response = new RouteResponseDTO();
        response.setRuta(ruta);
        return response;
    }
}