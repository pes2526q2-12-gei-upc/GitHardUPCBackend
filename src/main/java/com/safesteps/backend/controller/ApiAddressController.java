package com.safesteps.backend.controller;


import com.safesteps.backend.domain.routecalculator.RouteResponseDTO;
import com.safesteps.backend.domain.routecalculator.RouteCalculatorService;
import com.safesteps.backend.domain.routecalculator.RouteRequestDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Controlador de les direccions (@) de l'API.
// S'encarrega de rebre les peticions del client i retornar les respostes adequades.
@RestController

// De moment -> http://localhost:8080
// @ base -> /api/v1
@RequestMapping("/api/v1")
public class ApiAddressController {

    @Autowired
    RouteCalculatorService routeCalculatorService;

    @PostMapping("/calculate-route")
    public RouteResponseDTO calculateRoute(@RequestBody RouteRequestDTO request) {
        return routeCalculatorService.getBestRoute(
                request.getOrigin(),
                request.getDestination()
        );

    }
}
