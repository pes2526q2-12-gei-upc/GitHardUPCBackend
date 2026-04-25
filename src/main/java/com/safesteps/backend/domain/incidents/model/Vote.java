package com.safesteps.backend.domain.incidents.model;

import com.safesteps.backend.domain.incidents.projections.VoteDBProjection;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@Table(name = "votes")
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "incidence_id")
    private Long incidenceId;

    @Column(nullable = false, name = "google_id")
    private String googleId;

    @Column(nullable = false)
    private double score;

    @Column(nullable = false, name = "user_reliability")
    private double reliability;

    @Column(nullable = false, name = "data_score")
    private double dataScore;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Vote(VoteDBProjection v) {
        this.id = v.getId();
        this.incidenceId = v.getIncidenceId();
        this.googleId = v.getGoogleId();
        this.score = v.getScore();
        this.createdAt = v.getCreatedAt();
    }

}
