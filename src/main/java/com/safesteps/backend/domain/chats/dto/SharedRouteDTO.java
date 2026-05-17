package com.safesteps.backend.domain.chats.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class SharedRouteDTO {
    @Schema(description = "ID of the shared route")
    private Long id;

    @Schema(description = "Origin Latitude")
    private Double originLat;

    @Schema(description = "Origin Longitude")
    private Double originLng;

    @Schema(description = "Destination Latitude")
    private Double destLat;

    @Schema(description = "Destination Longitude")
    private Double destLng;

    @Schema(description = "Scheduled date for planned routes")
    private OffsetDateTime scheduledDate;

    @Schema(description = "Route type", example = "SEGURETAT")
    private String routeType;

    @Schema(description = "Total distance of the route in meters")
    private Double distanceMeters;

    @Schema(description = "Estimated duration of the route in minutes")
    private Integer durationMinutes;

    @Schema(description = "Readable origin address")
    private String originAddress;

    @Schema(description = "Readable destination address")
    private String destAddress;
}