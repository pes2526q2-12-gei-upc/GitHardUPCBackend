package com.safesteps.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safesteps.backend.controller.IncidentController;
import com.safesteps.backend.domain.incidents.dto.*;
import com.safesteps.backend.domain.incidents.model.IncidentTypeEnum;
import com.safesteps.backend.domain.incidents.service.IncidentService;
import com.safesteps.backend.domain.incidents.service.IncidentVoteService;
import com.safesteps.backend.domain.routecalculator.Coord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import org.springframework.http.MediaType;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IncidentController.class)
class IncidentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IncidentService incidentService;

    @MockBean
    private IncidentVoteService voteService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getIncidents() throws Exception {
        mockMvc.perform(get("/api/v1/incidents"))
                .andExpect(status().isOk());
    }

    @Test
    void createIncident() throws Exception {
        Coord c = new Coord();
        c.setLon(2.1686);
        c.setLat(41.3874);
        IncidentRequestDTO req = new IncidentRequestDTO();

        req.setGoogleId("1L");
        req.setType(IncidentTypeEnum.ALTRES);
        req.setDescription("Test desc");
        req.setCoordinates(c);

        IncidentResponseDTO expectedResponse = new IncidentResponseDTO();
        when(incidentService.createIncident(any(IncidentRequestDTO.class))).thenReturn(expectedResponse);

        mockMvc.perform(post("/api/v1/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    void createIncident_BadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getIncident() throws Exception {
        IncidentResponseDTO exp = new IncidentResponseDTO();
        when(incidentService.findIncidentById(1L)).thenReturn(exp);
        mockMvc.perform(get("/api/v1/incidents/1"))
                .andExpect(status().isOk());
    }

    @Test
    void getIncident_NOTFOUND() throws Exception {
        when(incidentService.findIncidentById(99L)).thenReturn(null);

        mockMvc.perform(get("/api/v1/incidents/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateIncident() throws Exception {
        IncidentResponseDTO exp = new IncidentResponseDTO();

        Coord c = new Coord();
        c.setLon(2.1686);
        c.setLat(41.3874);
        IncidentRequestDTO req = new IncidentRequestDTO();

        req.setGoogleId("1L");
        req.setType(IncidentTypeEnum.ALTRES);
        req.setDescription("Test desc");
        req.setCoordinates(c);

        when(incidentService.editIncidentById(1L, req)).thenReturn(exp);

        mockMvc.perform(put("/api/v1/incidents/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void updateIncident_NOTFOUND() throws Exception {
        Coord c = new Coord();
        c.setLon(2.1686);
        c.setLat(41.3874);
        IncidentRequestDTO req = new IncidentRequestDTO();

        req.setGoogleId("1L");
        req.setType(IncidentTypeEnum.ALTRES);
        req.setDescription("Test desc");
        req.setCoordinates(c);

        when(incidentService.editIncidentById(1L, req)).thenReturn(null);
        mockMvc.perform(put("/api/v1/incidents/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }


    @Test
    void deleteIncident() throws Exception {
        when(incidentService.deleteIncidentById(1L)).thenReturn(true);
        mockMvc.perform(delete("/api/v1/incidents/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteIncidentNotFound() throws Exception {
        when(incidentService.deleteIncidentById(1L)).thenReturn(false);
        mockMvc.perform(delete("/api/v1/incidents/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getIncidentsByUserId() throws Exception {
        mockMvc.perform(get("/api/v1/incidents/users/1"))
                .andExpect(status().isOk());
        List<IncidentResponseDTO> exp = incidentService.getIncidentsByUserId("1L");
        assertNotNull(exp);
    }

    @Test
    void checkVoteNoValid() throws Exception {
        VoteRequestDTO exp = new VoteRequestDTO();
        exp.setGoogleId("1L");
        exp.setVoteScore(10);

        mockMvc.perform(post("/api/v1/incidents/1/votes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(exp)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkVoteValid1() throws Exception {
        VoteRequestDTO exp = new VoteRequestDTO();
        exp.setGoogleId("1L");
        exp.setVoteScore(1);

        mockMvc.perform(post("/api/v1/incidents/1/votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exp)))
                .andExpect(status().isOk());
    }

    @Test
    void checkVoteValid2() throws Exception {
        VoteRequestDTO exp = new VoteRequestDTO();
        exp.setGoogleId("1L");
        exp.setVoteScore(-1);

        mockMvc.perform(post("/api/v1/incidents/1/votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exp)))
                .andExpect(status().isOk());
    }

    @Test
    void checkVoteNull() throws Exception {
        VoteRequestDTO exp = new VoteRequestDTO();
        exp.setGoogleId("1L");

        mockMvc.perform(post("/api/v1/incidents/1/votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exp)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getInfoVotes_OK() throws Exception {
        when(incidentService.getVoteCount(any())).thenReturn(new VoteCountDTO());
        mockMvc.perform(get("/api/v1/incidents/1/count-votes"))
                .andExpect(status().isOk());
    }

    @Test
    void getInfoVotes_NULL() throws Exception {
        when(incidentService.getVoteCount(any())).thenReturn(null);

        mockMvc.perform(get("/api/v1/incidents/1/count-votes"))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_OK() throws Exception {
        when(voteService.deleteVoteByVoteId(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/incidents/votes/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_NotFound() throws Exception {
        when(voteService.deleteVoteByVoteId(1L)).thenReturn(false);

        mockMvc.perform(delete("/api/v1/incidents/votes/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteByUserIncidence_OK() throws Exception {
        when(voteService.deleteByUserAndIncidence(1L, "1")).thenReturn(true);

        mockMvc.perform(delete("/api/v1/incidents/1/users/1/vote"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteByUserIncidence_NOTFOUND() throws Exception {
        when(voteService.deleteByUserAndIncidence(1L, "1L")).thenReturn(false);

        mockMvc.perform(delete("/api/v1/incidents/1/users/1/vote"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUserVotes_OK() throws Exception {
        when(voteService.getUserVotes(any())).thenReturn(List.of(new VoteResponseDTO()));

        mockMvc.perform(get("/api/v1/incidents/votes/users/1"))
                .andExpect(status().isOk());
    }
}