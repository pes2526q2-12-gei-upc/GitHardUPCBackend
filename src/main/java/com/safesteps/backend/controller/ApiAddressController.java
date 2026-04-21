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

    public ApiAddressController(RouteCalculatorService routeCalculatorService,  FiltreService filtreSv) {
        this.routeCalculatorService = routeCalculatorService;
        this.filtreSv = filtreSv;
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
}