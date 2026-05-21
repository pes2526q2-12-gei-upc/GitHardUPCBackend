package com.safesteps.backend.domain.users.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "prizes")
public class Premi {
    @Id
    private String id;

    @Column(nullable = false, name = "name")
    private String name;

    @Column(name = "url")
    private String url;

    @Column(name = "oddity")
    private String oddity;

}
