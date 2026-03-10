package com.safesteps.backend.domain.routecalculator;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/*
    * DTO per encapsular les dades de la resposta del calcul de la ruta.
    * De moment retorna:
    * - Una llista de coordenades (latitud i longitud) que representen la ruta calculada.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteResponseDTO {
    private List<Double[]> ruta; // Lista de coordenadas que representan la ruta calculada
}

