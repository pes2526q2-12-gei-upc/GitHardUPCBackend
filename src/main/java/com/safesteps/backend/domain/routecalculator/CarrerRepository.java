package com.safesteps.backend.domain.routecalculator;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarrerRepository extends JpaRepository<Carrer, Long> {

    /* Query per a trobar la ruta mes curta.
     Utilitza la funcio pgr_dijkstra de PostGIS per a calcular la ruta mes curta entre dos nodes.
     Tambe utilitza una vista (v_trams_nodes) que conté les dades dels carrers (id, source, target, longitud)
     creuant els carrers amb els nodes de la BD.
     */
    @Query(value =
            "SELECT node FROM pgr_dijkstra" +
                "(" +
                        "'SELECT fid as id, source, target, longitud AS cost " +
                        "FROM v_trams_nodes', " +
                ":originId, :destId, false" +
                ")",
            nativeQuery = true
    )

    List<Long> findShortestPath(
            @Param("originId") Long originId,
            @Param("destId") Long destId
    );

    /*
    Query per a trobar el node mes proper a unes coordenades donades (latitud i longitud).
     Utilitza les funcions de PostGIS per a calcular la distancia entre les coordenades donades i les coordenades dels nodes de la BD.
     Donada una entrada en lat. i long. retorna el FID del node mes proper a aquestes coordenades.
     Com la BD esta en UTM (EPSG:25831) i les coordenades d'entrada estan en WGS84 (EPSG:4326),
     es necessari transformar les coordenades d'entrada a UTM abans de calcular la distancia.
    * */
    @Query(value = "SELECT \"FID\" FROM bcn_grafvial_nodes " +
            "ORDER BY ST_Distance(" +
            "  ST_SetSRID(ST_MakePoint(\"Coord_X\", \"Coord_Y\"), 25831), " +
            "  ST_Transform(ST_SetSRID(ST_MakePoint(:lon, :lat), 4326), 25831)" +
            ") ASC LIMIT 1", nativeQuery = true)
    Long findNearestNode(@Param("lat") Double lat, @Param("lon") Double lon);
}