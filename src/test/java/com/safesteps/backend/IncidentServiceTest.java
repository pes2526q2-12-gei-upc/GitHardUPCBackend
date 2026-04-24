package com.safesteps.backend;

import com.safesteps.backend.domain.incidents.dto.IncidentRequestDTO;
import com.safesteps.backend.domain.incidents.dto.IncidentResponseDTO;
import com.safesteps.backend.domain.incidents.model.Incident;
import com.safesteps.backend.domain.incidents.model.IncidentTypeEnum;
import com.safesteps.backend.domain.incidents.projections.IncidentDBProjection;
import com.safesteps.backend.domain.incidents.repository.IncidentRepository;
import com.safesteps.backend.domain.incidents.service.IncidentService;
import com.safesteps.backend.domain.routecalculator.Coord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepo;

    @InjectMocks
    private IncidentService incidentService;

    @Test
    void getIncidents_DTO() {
        IncidentDBProjection p = mock(IncidentDBProjection.class);
        when(incidentRepo.all()).thenReturn(List.of(p));

        List<IncidentResponseDTO> result = incidentService.getIncidents();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(incidentRepo, times(1)).all();
    }

    @Test
    void getIncidents_Empty() {
        when(incidentRepo.all()).thenReturn(List.of());
        List<IncidentResponseDTO> r = incidentService.getIncidents();
        assertNotNull(r);
        assertEquals(0, r.size());
        verify(incidentRepo, times(1)).all();
    }



    @Test
    void createIncident_DTO() {
        Coord c = new Coord(); c.setLat(41.38); c.setLon(2.16);

        IncidentRequestDTO req = new IncidentRequestDTO();
        req.setGoogleId("1L");
        req.setType(IncidentTypeEnum.OBRES);
        req.setDescription("test");
        req.setCoordinates(c);

        Incident i = new Incident();
        i.setId(10L);

        IncidentDBProjection p = mock(IncidentDBProjection.class);

        when(incidentRepo.save(any(Incident.class))).thenReturn(i);
        when(incidentRepo.findIncidentWithUserById(10L)).thenReturn(Optional.of(p));

        IncidentResponseDTO result = incidentService.createIncident(req);

        assertNotNull(result);
        verify(incidentRepo, times(1)).save(any(Incident.class));
        verify(incidentRepo, times(1)).findIncidentWithUserById(10L);
    }


    @Test
    void createIncident_NoDescDTO() {
        Coord c = new Coord(); c.setLat(41.38); c.setLon(2.16);

        IncidentRequestDTO req = new IncidentRequestDTO();
        req.setGoogleId("1L");
        req.setType(IncidentTypeEnum.OBRES);
        req.setCoordinates(c);

        Incident i = new Incident();
        i.setId(10L);

        IncidentDBProjection p = mock(IncidentDBProjection.class);

        when(incidentRepo.save(any(Incident.class))).thenReturn(i);
        when(incidentRepo.findIncidentWithUserById(10L)).thenReturn(Optional.of(p));

        IncidentResponseDTO result = incidentService.createIncident(req);

        assertNotNull(result);
        verify(incidentRepo, times(1)).save(any(Incident.class));
        verify(incidentRepo, times(1)).findIncidentWithUserById(10L);
    }



    @Test
    void findIncidentById_DTO() {
        IncidentDBProjection p = mock(IncidentDBProjection.class);
        when(incidentRepo.findIncidentWithUserById(1L)).thenReturn(Optional.of(p));

        IncidentResponseDTO result = incidentService.findIncidentById(1L);

        assertNotNull(result);
    }

    @Test
    void findIncidentById_Null() {
        when(incidentRepo.findIncidentWithUserById(99L)).thenReturn(Optional.empty());

        IncidentResponseDTO result = incidentService.findIncidentById(99L);

        assertNull(result);
    }



    @Test
    void editIncidentById_DTO() {
        Coord mockCoord = mock(Coord.class);
        IncidentRequestDTO req = new IncidentRequestDTO();
        req.setGoogleId("1L");
        req.setType(IncidentTypeEnum.OBRES);
        req.setDescription("test");
        req.setCoordinates(mockCoord);

        Incident i = new Incident();
        i.setId(1L);

        IncidentDBProjection mockProjection = mock(IncidentDBProjection.class);

        when(incidentRepo.findById(1L)).thenReturn(Optional.of(i));
        when(incidentRepo.save(any(Incident.class))).thenReturn(i);
        when(incidentRepo.findIncidentWithUserById(1L)).thenReturn(Optional.of(mockProjection));

        IncidentResponseDTO result = incidentService.editIncidentById(1L, req);

        assertNotNull(result);
        assertEquals(IncidentTypeEnum.OBRES, i.getType());
        assertEquals("test", i.getDescription());
        verify(incidentRepo, times(1)).save(i);
    }

    @Test
    void editIncidentById_Null() {
        when(incidentRepo.findById(99L)).thenReturn(Optional.empty());
        IncidentRequestDTO req = new IncidentRequestDTO();

        IncidentResponseDTO result = incidentService.editIncidentById(99L, req);

        assertNull(result);
        verify(incidentRepo, never()).save(any());
    }



    @Test
    void deleteIncidentById_TRUE() {
        when(incidentRepo.existsById(1L)).thenReturn(true);

        boolean result = incidentService.deleteIncidentById(1L);

        assertTrue(result);
        verify(incidentRepo, times(1)).deleteById(1L);
    }

    @Test
    void deleteIncidentById_FALSE() {
        when(incidentRepo.existsById(99L)).thenReturn(false);

        boolean result = incidentService.deleteIncidentById(99L);

        assertFalse(result);
        verify(incidentRepo, never()).deleteById(anyLong());
    }

    @Test
    void getIncidentsByUserId_DTOS() {
        String googleId = "1L";
        IncidentDBProjection mockProjection = mock(IncidentDBProjection.class);

        when(incidentRepo.getAllByUserId(googleId)).thenReturn(List.of(mockProjection));

        List<IncidentResponseDTO> result = incidentService.getIncidentsByUserId(googleId);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(incidentRepo, times(1)).getAllByUserId(googleId);
    }

    @Test
    void getIncidentsByUserId_EMPTY() {
        String googleId = "99L";

        when(incidentRepo.getAllByUserId(googleId)).thenReturn(List.of());

        List<IncidentResponseDTO> result = incidentService.getIncidentsByUserId(googleId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(incidentRepo, times(1)).getAllByUserId(googleId);
    }
}