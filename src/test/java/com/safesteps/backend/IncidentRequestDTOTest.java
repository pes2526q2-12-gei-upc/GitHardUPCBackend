package com.safesteps.backend;

import com.safesteps.backend.domain.incidents.dto.IncidentRequestDTO;
import com.safesteps.backend.domain.incidents.model.IncidentTypeEnum;
import com.safesteps.backend.domain.routecalculator.Coord;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class IncidentRequestDTOTest {

    @Test
    void testGettersAndSetters() {
        IncidentRequestDTO dto = new IncidentRequestDTO();
        
        Coord coord = new Coord();
        coord.setLat(41.3851);
        coord.setLon(2.1734);

        dto.setGoogleId("12345");
        dto.setType(IncidentTypeEnum.OBRES);
        dto.setDescription("Test Description");
        dto.setCoordinates(coord);

        assertEquals("12345", dto.getGoogleId());
        assertEquals(IncidentTypeEnum.OBRES, dto.getType());
        assertEquals("Test Description", dto.getDescription());
        assertEquals(41.3851, dto.getCoordinates().getLat());
        assertEquals(2.1734, dto.getCoordinates().getLon());
    }

    @Test
    void testEqualsAndHashCode() {
        IncidentRequestDTO dto1 = new IncidentRequestDTO();
        dto1.setGoogleId("123");
        dto1.setType(IncidentTypeEnum.SEGURETAT);
        
        IncidentRequestDTO dto2 = new IncidentRequestDTO();
        dto2.setGoogleId("123");
        dto2.setType(IncidentTypeEnum.SEGURETAT);
        
        IncidentRequestDTO dto3 = new IncidentRequestDTO();
        dto3.setGoogleId("456");
        
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
        assertNotEquals(dto1, dto3);
    }
}
