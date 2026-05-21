package com.safesteps.backend.domain.chats.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class SharedRouteRequestDTO {
    @NotNull(message = "L'origen no pot ser nul")
    @Schema(description = "Origin Latitude", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double originLat;

    @NotNull(message = "L'origen no pot ser nul")
    @Schema(description = "Origin Longitude", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double originLng;

    @NotNull(message = "El destí no pot ser nul")
    @Schema(description = "Destination Latitude", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double destLat;

    @NotNull(message = "El destí no pot ser nul")
    @Schema(description = "Destination Longitude", requiredMode = Schema.RequiredMode.REQUIRED)
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