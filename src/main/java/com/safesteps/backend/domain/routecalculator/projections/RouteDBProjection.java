package com.safesteps.backend.domain.routecalculator.projections;

/*
Classe per a facilitar la lectura de les dades de les queries
 */
public interface RouteDBProjection {
    Long getNode();
    Long getEdge();
    Integer getSeq();
    Double getCost();
}
