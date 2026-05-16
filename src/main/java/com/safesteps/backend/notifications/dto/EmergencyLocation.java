package com.safesteps.backend.notifications.dto;

import com.safesteps.backend.domain.routecalculator.Coord;
import lombok.Data;

@Data
public class EmergencyLocation {
    private String username;
    private Coord coord;

    public  EmergencyLocation(String username, Coord coord) {
        this.username = username;
        this.coord = coord;
    }
}
