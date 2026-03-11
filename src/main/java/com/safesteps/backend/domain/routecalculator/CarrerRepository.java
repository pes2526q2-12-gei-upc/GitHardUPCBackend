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
            "SELECT di.node, COALESCE(v.longitud, 0.0) AS real_length, di.edge " +
                    "FROM pgr_dijkstra(" +
                    "'SELECT fid as id, source, target, longitud AS cost " +
                    "FROM v_trams_nodes', " +
                    ":originId, :destId, false" +
                    ") di " +
                    "LEFT JOIN v_trams_nodes v ON di.edge = v.fid " +
                    "WHERE di.node > 0 ORDER BY di.seq",
            nativeQuery = true
    )
    List<Object[]> findShortestPathWithCost(
            @Param("originId") Long originId,
            @Param("destId") Long destId
    );

    @Query(value =
            "SELECT di.node AS node, di.edge AS edge, di.seq AS seq, COALESCE(v.longitud, 0.0) AS cost " +
                    "FROM pgr_dijkstra(" +
                    "  'SELECT fid as id, source, target, " +
                    "          CASE WHEN fid = ANY(string_to_array(''' || :penalizedEdges || ''', '','')::bigint[]) THEN longitud * 1.25 ELSE longitud END AS cost " +
                    "   FROM v_trams_nodes', " +
                    "  :originId, :destId, false" +
                    ") di " +
                    "LEFT JOIN v_trams_nodes v ON di.edge = v.fid " +
                    "ORDER BY di.seq",
            nativeQuery = true)
    List<Object[]> findPathWithPenalties(
            @Param("originId") Long originId,
            @Param("destId") Long destId,
            @Param("penalizedEdges") String penalizedEdges
    );

    /*
    Query per a trobar les coordenades (latitud i longitud) a partir dels identificadors dels nodes.
     Utilitza les funcions de PostGIS per a transformar les coordenades dels nodes de la BD (UTM) a coordenades GPS (lat i long.) (WGS84).
     */
    @Query(value =
            "SELECT ST_X(geom_gps) AS lon, ST_Y(geom_gps) AS lat " +
                    "FROM (" +
                    "  SELECT t.seq, " +
                    "         ST_Transform(ST_SetSRID(ST_MakePoint(n.\"Coord_X\", n.\"Coord_Y\"), 25831), 4326) AS geom_gps " +
                    "  FROM unnest(cast(:fids as bigint[])) WITH ORDINALITY AS t(fid, seq) " +
                    "  JOIN bcn_grafvial_nodes n ON n.\"FID\" = t.fid " +
                    "  ORDER BY t.seq" +
                    ") AS sub",
            nativeQuery = true)
    List<Object[]> getCoordsFromNodeIds(@Param("fids") Long[] fids);

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