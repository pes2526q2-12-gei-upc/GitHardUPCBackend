package com.safesteps.backend.controller;

import com.safesteps.backend.domain.routecalculator.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Enrutamiento", description = "API para la planificación y cálculo de rutas seguras en Barcelona")
public class ApiAddressController {

    private final RouteCalculatorService routeCalculatorService;
    private final FiltreService filtreSv;
    private final RouteEvaluationSafetyService routeEvaluationSafetyService;

    public ApiAddressController(RouteCalculatorService routeCalculatorService,  
                                FiltreService filtreSv, 
                                RouteEvaluationSafetyService routeEvaluationSafetyService) {
        this.routeCalculatorService = routeCalculatorService;
        this.filtreSv = filtreSv;
        this.routeEvaluationSafetyService = routeEvaluationSafetyService;
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
        FiltreEnum f = request.getFiltre();
        Filtre filtre = filtreSv.getFiltre(request.getGoogleId(), f);
        if (filtre == null) return ResponseEntity.badRequest().build();

        RouteResponseDTO response = routeCalculatorService.getBestRoute(
                request.getOrigin(),
                request.getDestination(),
                request.getNRoutes(),
                filtre
        );

        return ResponseEntity.ok(response);
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
            return ResponseEntity.badRequest().build(); // Necessitem almenys 2 punts per fer una ruta
        }

        // Cridem al servei per avaluar la llista de coordenades
        ExternalSafetyResponseDTO response = routeEvaluationSafetyService.evaluateSafetyIndex(request.getRoutePoints());

        return ResponseEntity.ok(response);
    }
}