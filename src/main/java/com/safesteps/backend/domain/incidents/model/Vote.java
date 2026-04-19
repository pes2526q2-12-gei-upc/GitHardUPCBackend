package com.safesteps.backend.domain.incidents.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "votes")
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "id_incidence")
    private Long incidenceId;

    @Column(nullable = false, name = "id_user")
    private Long userId;

    @Column(nullable = false)
    private double score;

    @Column(nullable = false, name = "data_score")
    private double dataScore;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;



}
