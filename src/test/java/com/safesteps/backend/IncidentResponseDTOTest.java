package com.safesteps.backend;

import com.safesteps.backend.domain.incidents.dto.IncidentResponseDTO;
import com.safesteps.backend.domain.incidents.projections.IncidentDBProjection;
import org.geolatte.geom.G2D;
import org.geolatte.geom.Point;
import org.geolatte.geom.builder.DSL;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.geolatte.geom.crs.CoordinateReferenceSystems.WGS84;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class IncidentResponseDTOTest {
    @Test
    void fromProjToDTO() {
        IncidentDBProjection ip = new IncidentDBProjection() {
            @Override
            public Long getId() {return 10L;}
            @Override
            public String getUsername() {return "test1";}
            @Override
            public Long getUserLevel() {return 1L;}
            @Override
            public String getType() {return "TestType";}
            @Override
            public String getDescription() {return "TestDescription";}
            @Override
            public Point<G2D> getLocation() {return null;}
            @Override
            public Long getPositiveVotes() {return 2L;}
            @Override
            public Long getNegativeVotes() {return 4L;}
            @Override
            public Double getReliabilityIndex() {return -0.30;}
            @Override
            public String getStatus() {return "Success";}
            @Override
            public LocalDateTime getCreated() {return null;}
            @Override
            public LocalDateTime getUpdated() {return null;}
        };
        //
        IncidentResponseDTO dto = new IncidentResponseDTO(ip);
        assertEquals(10L, dto.getId().longValue());
        assertEquals("test1", dto.getAuthorName());
        assertEquals(1L, dto.getAuthorLevel().longValue());
        assertEquals("TestType", dto.getType());
        assertEquals("TestDescription", dto.getDescription());
        assertEquals(2L, dto.getPositiveVotes().longValue());
        assertEquals(4L, dto.getNegativeVotes().longValue());
        assertEquals(-0.30, dto.getReliabilityIndex());
        assertEquals("Success", dto.getStatus());
    }

    @Test
    void fromNullProjToDTO() {
        //
        IncidentResponseDTO dto = new IncidentResponseDTO(null);
        assertNull(dto.getId());
        assertNull(dto.getAuthorName());
        assertNull(dto.getAuthorLevel());
        assertNull(dto.getType());
        assertNull(dto.getDescription());
        assertNull(dto.getPositiveVotes());
        assertNull(dto.getNegativeVotes());
        assertNull(dto.getReliabilityIndex());
        assertNull(dto.getStatus());
    }

    @Test
    void fromProjToDTOTestNulls() {
        IncidentDBProjection ip = new IncidentDBProjection() {
            @Override
            public Long getId() {return 10L;}
            @Override
            public String getUsername() {return "test1";}
            @Override
            public Long getUserLevel() {return 1L;}
            @Override
            public String getType() {return "TestType";}
            @Override
            public String getDescription() {return "TestDescription";}
            @Override
            public Point<G2D> getLocation() {
                double longitude = 2.1734;
                double latitude = 41.3851;

                // Creem un punt en el sistema WGS84 (SRID 4326)
                return DSL.point(WGS84, DSL.g(longitude, latitude));
                }

            @Override
            public Long getPositiveVotes() {return null;}
            @Override
            public Long getNegativeVotes() {return null;}
            @Override
            public Double getReliabilityIndex() {return -0.30;}
            @Override
            public String getStatus() {return "Success";}
            @Override
            public LocalDateTime getCreated() {return null;}
            @Override
            public LocalDateTime getUpdated() {return null;}
        };
        //
        IncidentResponseDTO dto = new IncidentResponseDTO(ip);
        assertEquals(10L, dto.getId().longValue());
        assertEquals("test1", dto.getAuthorName());
        assertEquals(1L, dto.getAuthorLevel().longValue());
        assertEquals("TestType", dto.getType());
        assertEquals("TestDescription", dto.getDescription());
        assertEquals(0L, dto.getPositiveVotes().longValue());
        assertEquals(0L, dto.getNegativeVotes().longValue());
        assertEquals(-0.30, dto.getReliabilityIndex());
        assertEquals("Success", dto.getStatus());
        assertEquals(41.3851, dto.getCoordinates().getLat());
        assertEquals(2.1734, dto.getCoordinates().getLon());
    }
}
