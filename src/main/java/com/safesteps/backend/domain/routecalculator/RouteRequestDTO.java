package com.safesteps.backend.domain.routecalculator;

import lombok.Data;

// DTO per encapsular les dades de la petició del calcul de la ruta.
// Necessita tenir la longitud i latitud de l'origen i el desti.
// El frontend haura de passar aquestes dades:
@Data
public class RouteRequestDTO {
    private Coord origin;
    private Coord destination;
}