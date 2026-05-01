package com.safesteps.backend.domain.routecalculator;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.locationtech.jts.geom.*;

@Data
@Schema(description = "Objeto que representa una coordenada geográfica dentro de Barcelona.")
@InsideBarcelona
public class Coord {

    @NotNull(message = "La latitud es obligatoria")
    @Schema(description = "Latitud de la coordenada", example = "41.3874", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double lat;
  
    @NotNull(message = "La longitud es obligatoria")
    @Schema(description = "Longitud de la coordenada", example = "2.1686", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double lon;
  
    public Point toPoint() {
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        return geometryFactory.createPoint(new Coordinate(this.lon, this.lat));
    }
}