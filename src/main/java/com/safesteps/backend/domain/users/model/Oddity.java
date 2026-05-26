package com.safesteps.backend.domain.users.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "oddities")
public class Oddity {
    @Id
    private String id;

    @Column(name = "probability")
    private float probability;

    @Column(name = "percentage_lvl_compensation")
    private float percentageLvlCompensation;
}
