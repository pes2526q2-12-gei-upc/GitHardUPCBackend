package com.safesteps.backend.domain.routecalculator;

import jakarta.persistence.*;
import lombok.Data;

/*
Classe que representa un carrer de la BD
FID -> id del carrer (PK)
source -> Es el nus inicial del carrer
target -> Es el nus final a on va a parar el carrer
longitud -> La distancia en metres del carrer (ve a ser el cost de l'aresta)
 */

@Entity
@Table(name = "v_trams_nodes") // El nom de la taula a la BD
@Data
public class Carrer {

    // FID -> Identificador unic de cada carrer a la base de dades.
    @Id
    @Column(name = "fid")
    private Long fid;

    // C_Nus_I -> Identificador del node d'origen del carrer.
    @Column(name = "source")
    private Long source;

    // C_Nus_F -> Identificador del node de desti del carrer.
    @Column(name = "target")
    private Long target;

    // Distancia -> Longitud del carrer (cost associat al carrer).
    @Column(name = "longitud")
    private Double longitud;
}