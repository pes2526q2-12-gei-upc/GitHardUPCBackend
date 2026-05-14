package com.safesteps.backend.controller;

import com.safesteps.backend.domain.routecalculator.*;
import com.safesteps.backend.domain.admin.service.AdminMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.safesteps.backend.domain.common.exception.BadRequestException;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Enrutamiento", description = "API para la planificación y cálculo de rutas seguras en Barcelona")
public class ApiAddressController {

    private final RouteCalculatorService routeCalculatorService;
    private final FiltreService filtreSv;
    private final RouteEvaluationSafetyService routeEvaluationSafetyService;
    private final EventIntegrationService eventIntegrationService;
    private final AdminMetricsService adminMetricsService;
    private static final Logger logger = LoggerFactory.getLogger(ApiAddressController.class);

    public ApiAddressController(RouteCalculatorService routeCalculatorService,
                                FiltreService filtreSv,
                                RouteEvaluationSafetyService routeEvaluationSafetyService,
                                EventIntegrationService eventIntegrationService,
                                AdminMetricsService adminMetricsService) {
        this.routeCalculatorService = routeCalculatorService;
        this.filtreSv = filtreSv;
        this.routeEvaluationSafetyService = routeEvaluationSafetyService;
        this.eventIntegrationService = eventIntegrationService;
        this.adminMetricsService = adminMetricsService;
    }

    @Operation(
            summary = "Calcular la mejor ruta",
            description = "Recibe coordenadas de origen y destino, valida que estén en Barcelona y devuelve los detalles de la ruta óptima (nodos, tiempo, distancia)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ruta calculada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Petición malformada o coordenadas fuera de los límites (Bad Request)", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno en el cálculo del grafo espacial", content = @Content)
    })
    @PostMapping("/calculate-route")
    public ResponseEntity<RouteResponseDTO> calculateRoute(@Valid @RequestBody RouteRequestDTO request) {
        long startNanos = System.nanoTime();
        boolean success = false;
        try {
            FiltreEnum f = request.getFiltre();
            Filtre filtre = filtreSv.getFiltre(request.getGoogleId(), f);

            RouteResponseDTO response = routeCalculatorService.getBestRoute(
                    request.getOrigin(),
                    request.getDestination(),
                    request.getNRoutes(),
                    filtre
            );

            success = true;
            return ResponseEntity.ok(response);
        } finally {
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
            adminMetricsService.recordRouteRequest(request, durationMs, success);
        }
    }

    @Operation(
            summary = "Valorar la seguridad de una ruta",
            description = "Recibe un conjunto de coordenadas que representan una ruta y devuelve una valoración de seguridad basada en datos internos."
    )
    @io.swagger.v3.oas.annotations.Parameter(
            name = "X-API-KEY",
            description = "Token de seguridad para acceder a la API.",
            required = true,
            in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Valoración de seguridad calculada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Lista de coordenadas inválida o vacía.", content = @Content),
    })
    @PostMapping("/evaluate-route-security")
    public ResponseEntity<ExternalSafetyResponseDTO> evaluateRouteSecurity(@RequestBody ExternalRouteRequestDTO request) {

        if (request.getRoutePoints() == null || request.getRoutePoints().size() < 2) {
            logger.warn("evaluateRouteSecurity bad request: insufficient route points: {}", request.getRoutePoints() == null ? 0 : request.getRoutePoints().size());
            throw new BadRequestException("Necessitem almenys 2 punts per fer una ruta"); // Necessitem almenys 2 punts per fer una ruta
        }

        // Cridem al servei per avaluar la llista de coordenades
        ExternalSafetyResponseDTO response = routeEvaluationSafetyService.evaluateSafetyIndex(request.getRoutePoints());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Obtenir POIs d'esdeveniments culturals propers a una ruta")
    @PostMapping("/route-events")
    public ResponseEntity<List<PoiDTO>> getEventsForRoute(@RequestBody ExternalRouteRequestDTO request) {

        if (request.getRoutePoints() == null || request.getRoutePoints().isEmpty()) {
            throw new BadRequestException("La llista de coordenades no pot estar buida.");
        }

        List<PoiDTO> eventPois = eventIntegrationService.getEventsForRoute(request.getRoutePoints());

        return ResponseEntity.ok(eventPois);
    }
}
