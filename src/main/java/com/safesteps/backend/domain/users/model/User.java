package com.safesteps.backend.domain.users.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.HashSet;
import java.util.Set;
import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "google_id", length = 100, unique = true, nullable = false)
    private String googleId;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(name = "picture_url")
    private String pictureUrl;

    @Column(length = 10)
    private String language;

    @Column(nullable = false)
    private Long points = 0L;

    @Column(nullable = false)
    private Long level = 1L;

    @Column(name = "is_anonymous", nullable = false)
    private Boolean isAnonymous = false;

    @Column(nullable = false)
    private double reputacio = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) default 'ACTIVE'")
    private UserStatus status = UserStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name="pending_rewards")
    private Long recompenses = 0L;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_prizes",
            joinColumns = @JoinColumn(name = "google_id", referencedColumnName = "google_id"),
            inverseJoinColumns = @JoinColumn(name = "id", referencedColumnName = "id")
    )
    private Set<Premi> premis = new HashSet<>();

    @Column(name="fcm_token")
    private String fcmToken;

    @Column(name="is_online", nullable = false)
    private Boolean isOnline = false;

    @Column(name="is_in_emergency", nullable = false)
    private Boolean isInEmergency = false;
}