package com.safesteps.backend.domain.routecalculator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.locationtech.jts.geom.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class InsideBarcelonaValidator implements ConstraintValidator<InsideBarcelona, Coord> {

    // Bounding box de Barcelona como fallback cuando el servicio no está disponible
    // (p.ej. en tests unitarios sin contexto Spring completo)
    private static final Geometry FALLBACK_POLYGON;

    static {
        GeometryFactory gf = new GeometryFactory();
        LinearRing shell = gf.createLinearRing(new Coordinate[] {
                new Coordinate(2.05, 41.32),
                new Coordinate(2.23, 41.32),
                new Coordinate(2.23, 41.47),
                new Coordinate(2.05, 41.47),
                new Coordinate(2.05, 41.32)
        });
        FALLBACK_POLYGON = gf.createPolygon(shell);
    }

    @Autowired(required = false)
    @Nullable
    private BarcelonaBoundaryService boundaryService;

    @Override
    public void initialize(InsideBarcelona constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(Coord coord, ConstraintValidatorContext context) {
        if (coord == null || coord.getLat() == null || coord.getLon() == null) {
            return true; // Dejar que otras validaciones manejen null
        }
        if (boundaryService != null) {
            return boundaryService.isWithinBarcelona(coord.getLon(), coord.getLat());
        }
        // Fallback cuando no hay contexto Spring (tests unitarios)
        GeometryFactory gf = new GeometryFactory();
        Point point = gf.createPoint(new Coordinate(coord.getLon(), coord.getLat()));
        return FALLBACK_POLYGON.intersects(point);
    }
}
