package com.safesteps.backend.controller;

import com.safesteps.backend.domain.routecalculator.RouteCalculatorService;
import com.safesteps.backend.domain.routecalculator.RouteRequestDTO;
import com.safesteps.backend.domain.routecalculator.RouteResponseDTO;
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

    public ApiAddressController(RouteCalculatorService routeCalculatorService) {
        this.routeCalculatorService = routeCalculatorService;
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

        RouteResponseDTO response = routeCalculatorService.getBestRoute(
                request.getOrigin(),
                request.getDestination()
        );

        return ResponseEntity.ok(response);
    }
}