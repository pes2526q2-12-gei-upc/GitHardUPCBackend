package com.safesteps.backend.domain.routecalculator;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@Schema(description = "DTO que encapsula la respuesta del cálculo de la ruta. Incluye la geometría (coordenadas), la distancia total y el tiempo estimado.")
public class RouteResponseDTO {

    private List<Route> routes;

    public RouteResponseDTO() {
        routes =  new ArrayList<>();
    }

}