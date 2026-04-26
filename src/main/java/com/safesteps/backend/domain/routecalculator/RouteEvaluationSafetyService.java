package com.safesteps.backend.domain.routecalculator;

import com.safesteps.backend.domain.routecalculator.projections.RouteAveragesDBProjection;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RouteEvaluationSafetyService {
    private final CarrerRepository carrerRepository;

    // Constants de l'algorisme (ajusta-les segons les teves dades)
    private static final double MAX_DELICTES_PER_CARRER = 50.0;

    public RouteEvaluationSafetyService(CarrerRepository carrerRepository) {
        this.carrerRepository = carrerRepository;
    }

    public ExternalSafetyResponseDTO evaluateSafetyIndex(List<Coord> routePoints) {
        // 1. Construir el String WKT (ex: "LINESTRING(2.16 41.38, 2.17 41.39)")
        String wktLine = buildLineString(routePoints);

        // 2. Cridar a la Base de Dades
        RouteAveragesDBProjection averages = carrerRepository.getRouteAveragesFromWKT(wktLine);

        // 3. Extreure valors
        double avgDelictes = averages.getAvgDelictes();
        double avgCameres = averages.getAvgCameres();
        double avgComisaries = averages.getAvgComissaries();

        // 4. ALGORISME (Criteri 0 - 10)
        double safetyScore = 10.0;

        // --- 1. Factor de Protecció Local (Micro-seguretat) ---
        // Calculem un índex de "vida" i "vigilància" al carrer. De 0.0 (desert) a 1.0 (súper protegit)
        double nivellVigilancia = (avgComisaries / 100.0) + (avgCameres * 20.0);
        nivellVigilancia = Math.min(1.0, nivellVigilancia);// Mai pot passar d'1.0

        // --- 2. Penalització de Delictes (Mitigada) ---
        double penalitzacioBaseDelictes = Math.min((avgDelictes / 15000.0) * 5.0, 5.0); // Màxim 5 punts de penalització

        double penalitzacioRealDelictes = penalitzacioBaseDelictes * (1.0 - (0.30 * nivellVigilancia));
        safetyScore -= penalitzacioRealDelictes;

        if (nivellVigilancia < 0.3) {
            double factorRisc = Math.max(0.0, (avgDelictes - 8000.0) / 10000.0);
            factorRisc = Math.min(factorRisc, 1.0);

            safetyScore -= (0.3 - nivellVigilancia) * 5.0 * factorRisc;
        }

        // 5. Assegurar limits i arrodonir
        safetyScore = Math.max(0.0, Math.min(10.0, safetyScore));
        safetyScore = Math.round(safetyScore * 10.0) / 10.0;

        String level = getLevelFromScore(safetyScore);

        return new ExternalSafetyResponseDTO(safetyScore, level);
    }

    // Mètode helper per convertir Llista de Objectes a TEXT GIS
    private String buildLineString(List<Coord> points) {
        String coordPairs = points.stream()
                .map(p -> p.getLon() + " " + p.getLat())
                .collect(Collectors.joining(", "));
        return "LINESTRING(" + coordPairs + ")";
    }

    private String getLevelFromScore(double score) {
        if (score >= 7.5) return "ALTA";
        if (score >= 4.0) return "MODERADA";
        return "BAIXA";
    }
}
