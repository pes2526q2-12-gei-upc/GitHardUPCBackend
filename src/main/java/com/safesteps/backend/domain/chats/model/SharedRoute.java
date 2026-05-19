package com.safesteps.backend.domain.chats.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;

@Entity
@Table(name = "shared_routes")
@Data
public class SharedRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "origin_lat", nullable = false)
    private Double originLat;

    @Column(name = "origin_lng", nullable = false)
    private Double originLng;

    @Column(name = "dest_lat", nullable = false)
    private Double destLat;

    @Column(name = "dest_lng", nullable = false)
    private Double destLng;

    @Column(name = "scheduled_date")
    private OffsetDateTime scheduledDate;

    @Column(name = "route_type")
    private String routeType;

    @Column(name = "distance_meters")
    private Double distanceMeters;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "origin_address")
    private String originAddress;

    @Column(name = "dest_address")
    private String destAddress;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;
}