package com.safesteps.backend.domain.incidents.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.locationtech.jts.geom.Point;
import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "google_id")
    private String googleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private IncidentTypeEnum type;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, columnDefinition = "geometry(Point, 4326)")
    private Point location;

    @Column(name = "positive_votes")
    private Integer positiveVotes = 0;

    @Column(name = "negative_votes")
    private Integer negativeVotes = 0;

    @Column(name = "reliability_index")
    private Double reliabilityIndex = 0.0;

    @Column(length = 20)
    private String status = IncidentStatusEnum.PENDING.name();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}