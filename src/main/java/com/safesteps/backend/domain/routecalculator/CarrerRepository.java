package com.safesteps.backend.domain.routecalculator;

import com.safesteps.backend.domain.routecalculator.projections.CoordDBProjection;
import com.safesteps.backend.domain.routecalculator.projections.RouteDBProjection;
import com.safesteps.backend.domain.routecalculator.projections.PoiDBProjection;
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
    List<RouteDBProjection> findPathWithPenalties(
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
    List<CoordDBProjection> getCoordsFromNodeIds(@Param("fids") Long[] fids);

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

    /*
     * Busca comissaries a menys de :radi metres de la ruta.
     * 1. Construeix la geometria lineal de la ruta (ST_MakeLine).
     * 2. Comprova la distància amb les coordenades UTM (25831) de les comissaries.
     */
    @Query(value =
            "WITH route_geom AS ( " +
                    "  SELECT ST_MakeLine(ST_SetSRID(ST_MakePoint(n.\"Coord_X\", n.\"Coord_Y\"), 25831) ORDER BY t.seq) as geom " +
                    "  FROM unnest(cast(:fids as bigint[])) WITH ORDINALITY AS t(fid, seq) " +
                    "  JOIN bcn_grafvial_nodes n ON n.\"FID\" = t.fid " +
                    ") " +
                    "SELECT c.name as name, c.geo_epgs_4326_lat as lat, c.geo_epgs_4326_lon as lon " +
                    "FROM bcn_comissaries c, route_geom rg " +
                    "WHERE ST_DWithin(ST_SetSRID(ST_MakePoint(c.geo_epgs_25831_x, c.geo_epgs_25831_y), 25831), rg.geom, :radi)",
            nativeQuery = true)
    List<PoiDBProjection> findComissariesNearRoute(@Param("fids") Long[] fids, @Param("radi") Double radiMetres);

    /*
     * Query per trobar les Fonts d'Aigua Pública prop de la ruta.
     * Utilitza ETRS89 (que projectem a 25831) per a la distància en metres.
     */
    @Query(value =
            "WITH route_geom AS ( " +
                    "  SELECT ST_MakeLine(ST_SetSRID(ST_MakePoint(n.\"Coord_X\", n.\"Coord_Y\"), 25831) ORDER BY t.seq) as geom " +
                    "  FROM unnest(cast(:fids as bigint[])) WITH ORDINALITY AS t(fid, seq) " +
                    "  JOIN bcn_grafvial_nodes n ON n.\"FID\" = t.fid " +
                    ") " +
                    "SELECT COALESCE(f.nom, 'Font pública') as name, f.latitud as lat, f.longitud as lon " +
                    "FROM bcn_fonts_beure f, route_geom rg " +
                    "WHERE ST_DWithin(ST_SetSRID(ST_MakePoint(f.x_etrs89, f.y_etrs89), 25831), rg.geom, :radi)",
            nativeQuery = true)
    List<PoiDBProjection> findFontsNearRoute(@Param("fids") Long[] fids, @Param("radi") Double radiMetres);

    /*
     * Query per trobar Bancs per seure prop de la ruta.
     * Utilitza ETRS89 (que projectem a 25831) per a la distància en metres.
     */
    @Query(value =
            "WITH route_geom AS ( " +
                    "  SELECT ST_MakeLine(ST_SetSRID(ST_MakePoint(n.\"Coord_X\", n.\"Coord_Y\"), 25831) ORDER BY t.seq) as geom " +
                    "  FROM unnest(cast(:fids as bigint[])) WITH ORDINALITY AS t(fid, seq) " +
                    "  JOIN bcn_grafvial_nodes n ON n.\"FID\" = t.fid " +
                    ") " +
                    "SELECT COALESCE(b.descripcio, 'Banc públic') as name, b.latitud as lat, b.longitud as lon " +
                    "FROM bcn_bancs b, route_geom rg " +
                    "WHERE ST_DWithin(ST_SetSRID(ST_MakePoint(b.x_etrs89, b.y_etrs89), 25831), rg.geom, :radi)",
            nativeQuery = true)
    List<PoiDBProjection> findBancsNearRoute(@Param("fids") Long[] fids, @Param("radi") Double radiMetres);

    /*
     * Query per trobar Càmeres de Seguretat prop de la ruta.
     * Forcem el nom a 'Càmera de seguretat' ja que no hi ha una descripció amigable a la BD.
     */
    @Query(value =
            "WITH route_geom AS ( " +
                    "  SELECT ST_MakeLine(ST_SetSRID(ST_MakePoint(n.\"Coord_X\", n.\"Coord_Y\"), 25831) ORDER BY t.seq) as geom " +
                    "  FROM unnest(cast(:fids as bigint[])) WITH ORDINALITY AS t(fid, seq) " +
                    "  JOIN bcn_grafvial_nodes n ON n.\"FID\" = t.fid " +
                    ") " +
                    "SELECT 'Càmera de seguretat' as name, c.latitud as lat, c.longitud as lon " +
                    "FROM bcn_cameres_seguretat c, route_geom rg " +
                    "WHERE ST_DWithin(ST_SetSRID(ST_MakePoint(c.x_etrs89, c.y_etrs89), 25831), rg.geom, :radi)",
            nativeQuery = true)
    List<PoiDBProjection> findCameresNearRoute(@Param("fids") Long[] fids, @Param("radi") Double radiMetres);

    /*
     * Query per trobar Escales Mecàniques prop de la ruta.
     * Utilitza les coordenades ETRS89 projectades a 25831 per a distàncies en metres.
     */
    @Query(value =
            "WITH route_geom AS ( " +
                    "  SELECT ST_MakeLine(ST_SetSRID(ST_MakePoint(n.\"Coord_X\", n.\"Coord_Y\"), 25831) ORDER BY t.seq) as geom " +
                    "  FROM unnest(cast(:fids as bigint[])) WITH ORDINALITY AS t(fid, seq) " +
                    "  JOIN bcn_grafvial_nodes n ON n.\"FID\" = t.fid " +
                    ") " +
                    "SELECT e.name as name, e.geo_epgs_4326_lat as lat, e.geo_epgs_4326_lon as lon " +
                    "FROM bcn_escales_mecaniques e, route_geom rg " +
                    "WHERE ST_DWithin(ST_SetSRID(ST_MakePoint(e.geo_epgs_25831_x, e.geo_epgs_25831_y), 25831), rg.geom, :radi)",
            nativeQuery = true)
    List<PoiDBProjection> findEscalesNearRoute(@Param("fids") Long[] fids, @Param("radi") Double radiMetres);

    /*
     * Query per trobar Arbrat Viari (ombra) prop de la ruta.
     * Radi EXTREMADAMENT curt (15m) per evitar col·lapsar el JSON amb milers d'arbres.
     */
    @Query(value =
            "WITH route_geom AS ( " +
                    "  SELECT ST_MakeLine(ST_SetSRID(ST_MakePoint(n.\"Coord_X\", n.\"Coord_Y\"), 25831) ORDER BY t.seq) as geom " +
                    "  FROM unnest(cast(:fids as bigint[])) WITH ORDINALITY AS t(fid, seq) " +
                    "  JOIN bcn_grafvial_nodes n ON n.\"FID\" = t.fid " +
                    ") " +
                    "SELECT COALESCE(a.cat_nom_catala, a.cat_nom_cientific, 'Arbre') as name, a.latitud as lat, a.longitud as lon " +
                    "FROM bcn_arbrat_viari a, route_geom rg " +
                    "WHERE ST_DWithin(ST_SetSRID(ST_MakePoint(a.x_etrs89, a.y_etrs89), 25831), rg.geom, :radi)",
            nativeQuery = true)
    List<PoiDBProjection> findArbresNearRoute(@Param("fids") Long[] fids, @Param("radi") Double radiMetres);

    /*
     * Query per trobar Arbrat de Zona (parcs i espais verds) prop de la ruta.
     * Radi de 15m per mantenir el rendiment i donar només l'ombra real del trajecte.
     */
    @Query(value =
            "WITH route_geom AS ( " +
                    "  SELECT ST_MakeLine(ST_SetSRID(ST_MakePoint(n.\"Coord_X\", n.\"Coord_Y\"), 25831) ORDER BY t.seq) as geom " +
                    "  FROM unnest(cast(:fids as bigint[])) WITH ORDINALITY AS t(fid, seq) " +
                    "  JOIN bcn_grafvial_nodes n ON n.\"FID\" = t.fid " +
                    ") " +
                    "SELECT COALESCE(a.cat_nom_catala, a.cat_nom_cientific, 'Arbre de parc') as name, a.latitud as lat, a.longitud as lon " +
                    "FROM bcn_arbrat_zona a, route_geom rg " +
                    "WHERE ST_DWithin(ST_SetSRID(ST_MakePoint(a.x_etrs89, a.y_etrs89), 25831), rg.geom, :radi)",
            nativeQuery = true)
    List<PoiDBProjection> findArbresZonaNearRoute(@Param("fids") Long[] fids, @Param("radi") Double radiMetres);
}