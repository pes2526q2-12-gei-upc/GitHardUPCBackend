package com.safesteps.backend.domain.routecalculator;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor

/*
 * Clase per a representar les coordenades (latitud i longitud) d'un punt.
 * S'utilitza per a encapsular les coordenades d'origen i desti en les peticions i respostes del calcul de la ruta.
 * Les coordenades es representen en el sistema de referencia WGS84
 */

public class Coord {
    private Double lon;
    private Double lat;
}
