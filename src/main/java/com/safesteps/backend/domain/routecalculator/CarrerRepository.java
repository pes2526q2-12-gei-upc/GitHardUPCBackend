package com.safesteps.backend.domain.routecalculator;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarrerRepository extends JpaRepository<Carrer, Long> {

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

    @Query(value = "SELECT \"FID\" FROM bcn_grafvial_nodes " +
            "ORDER BY ST_Distance(" +
            "  ST_SetSRID(ST_MakePoint(\"Coord_X\", \"Coord_Y\"), 25831), " +
            "  ST_Transform(ST_SetSRID(ST_MakePoint(:lon, :lat), 4326), 25831)" +
            ") ASC LIMIT 1", nativeQuery = true)
    Long findNearestNode(@Param("lat") Double lat, @Param("lon") Double lon);
}