package com.safesteps.backend.domain.routecalculator;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.WKBReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class BarcelonaBoundaryService {

    private static final Logger logger = LoggerFactory.getLogger(BarcelonaBoundaryService.class);

    private Geometry barcelonaBoundary;
    private boolean isUsingFallback = false;
    private static final GeometryFactory geometryFactory = new GeometryFactory();

    // Bounding box fallback en WGS84
    private static final Polygon FALLBACK_POLYGON = createFallbackPolygon();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static Polygon createFallbackPolygon() {
        GeometryFactory gf = new GeometryFactory();
        LinearRing shell = gf.createLinearRing(new Coordinate[]{
            new Coordinate(2.05, 41.32),  // SW
            new Coordinate(2.23, 41.32),  // SE
            new Coordinate(2.23, 41.47),  // NE
            new Coordinate(2.05, 41.47),  // NW
            new Coordinate(2.05, 41.32)   // Cerrar
        });
        return gf.createPolygon(shell);
    }

    @PostConstruct
    public void loadBarcelonaBoundary() {
        logger.info("Intentando cargar el polígono de límites de Barcelona desde la base de datos...");

        try {
            // Primero intentamos cargar desde la vista materializada
            if (tryLoadFromMaterializedView()) {
                logger.info("✓ Polígono de Barcelona cargado exitosamente desde vista materializada en memoria.");
                return;
            }
        } catch (Exception e) {
            logger.warn("No se pudo cargar desde vista materializada: {}", e.getMessage());
        }

        // Si falla, intentamos crearlo dinámicamente
        try {
            if (tryCreateAndLoadBoundaryDynamically()) {
                logger.info("✓ Polígono de Barcelona creado y cargado dinámicamente en memoria.");
                return;
            }
        } catch (Exception e) {
            logger.warn("No se pudo crear dinámicamente el polígono: {}", e.getMessage());
        }

        // Si todo falla, usar polígono fallback
        logger.warn("⚠ Usando polígono fallback (bounding box simple). La precisión será menor que la óptima.");
        barcelonaBoundary = FALLBACK_POLYGON;
        isUsingFallback = true;
    }

    private boolean tryLoadFromMaterializedView() throws Exception {
        String sql = "SELECT ST_AsBinary(boundary_geom) AS wkb FROM barcelona_boundary LIMIT 1";
        byte[] wkb = jdbcTemplate.queryForObject(sql, byte[].class);
        if (wkb != null) {
            WKBReader reader = new WKBReader();
            barcelonaBoundary = reader.read(wkb);
            return true;
        }
        return false;
    }

    private boolean tryCreateAndLoadBoundaryDynamically() throws Exception {
        logger.info("Intentando crear la vista materializada barcelona_boundary dinámicamente...");
        try {
            String createViewSQL =
                "CREATE MATERIALIZED VIEW IF NOT EXISTS barcelona_boundary AS " +
                "SELECT ST_Transform(ST_ConcaveHull(ST_Collect(geom), 0.99), 4326) AS boundary_geom " +
                "FROM bcn_grafvial_trams " +
                "WHERE geom IS NOT NULL;";

            jdbcTemplate.execute(createViewSQL);
            logger.info("Vista materializada barcelona_boundary creada exitosamente.");

            // Ahora intentamos cargar desde la vista recién creada
            return tryLoadFromMaterializedView();
        } catch (DataAccessException e) {
            logger.warn("No se pudo crear la vista (puede que bcn_grafvial_trams no tenga datos aún): {}", e.getMessage());
            return false;
        }
    }

    public Geometry getBarcelonaBoundary() {
        return barcelonaBoundary;
    }

    public boolean isWithinBarcelona(double lon, double lat) {
        if (barcelonaBoundary == null) {
            logger.warn("Polígono de Barcelona aún no cargado, usando fallback.");
            barcelonaBoundary = FALLBACK_POLYGON;
            isUsingFallback = true;
        }
        try {
            Point point = geometryFactory.createPoint(new Coordinate(lon, lat));
            return barcelonaBoundary.intersects(point);
        } catch (Exception e) {
            logger.error("Error al validar coordenada ({}, {}): {}", lon, lat, e.getMessage());
            return false;
        }
    }

    public boolean isUsingFallbackBoundary() {
        return isUsingFallback;
    }

    public void refresh() {
        logger.info("Refrescando el polígono de límites de Barcelona...");
        loadBarcelonaBoundary();
    }
}
