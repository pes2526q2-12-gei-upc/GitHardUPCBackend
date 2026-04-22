package com.safesteps.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safesteps.backend.controller.ApiAddressController;
import com.safesteps.backend.domain.routecalculator.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ExtendWith(MockitoExtension.class)
class ApiAddressControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private RouteCalculatorService routeCalculatorService;

    @Mock
    private RouteEvaluationSafetyService routeEvaluationSafetyService;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        ApiAddressController apiAddressController = new ApiAddressController(routeCalculatorService, routeEvaluationSafetyService);

        mockMvc = MockMvcBuilders.standaloneSetup(apiAddressController)
                .setValidator(validator)
                .build();
    }

    @Test
    void calculateRoute_WithValidRequest() throws Exception {
        RouteRequestDTO request = new RouteRequestDTO();
        Coord org = new Coord(); org.setLat(41.38); org.setLon(2.16);
        Coord dest = new Coord(); dest.setLat(41.40); dest.setLon(2.17);
        Filtre filtre = new Filtre();
        request.setOrigin(org);
        request.setDestination(dest);
        request.setNRoutes(3);
        request.setFiltre(filtre);

        when(routeCalculatorService.getBestRoute(any(), any(), anyInt(), any()))
                .thenReturn(new RouteResponseDTO());

        mockMvc.perform(post("/api/v1/calculate-route")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void calculateRoute_WithTooManyRoutes() throws Exception {
        RouteRequestDTO request = new RouteRequestDTO();
        Coord org = new Coord(); org.setLat(41.38); org.setLon(2.16);
        Coord dest = new Coord(); dest.setLat(41.40); dest.setLon(2.17);
        request.setOrigin(org);
        request.setDestination(dest);
        request.setNRoutes(10); // màxim 5

        mockMvc.perform(post("/api/v1/calculate-route")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void calculateRoute_WithTooFewRoutes() throws Exception {
        RouteRequestDTO request = new RouteRequestDTO();
        Coord org = new Coord(); org.setLat(41.38); org.setLon(2.16);
        Coord dest = new Coord(); dest.setLat(41.40); dest.setLon(2.17);
        request.setOrigin(org);
        request.setDestination(dest);
        request.setNRoutes(0); // minim 1

        mockMvc.perform(post("/api/v1/calculate-route")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void calculateRoute_WithFiltreOverOne() throws Exception {
        RouteRequestDTO request = new RouteRequestDTO();
        Coord org = new Coord(); org.setLat(41.38); org.setLon(2.16);
        Coord dest = new Coord(); dest.setLat(41.40); dest.setLon(2.17);
        Filtre filtre = new Filtre();
        filtre.setSeguretat(1.5f); // màxim 1
        request.setOrigin(org);
        request.setDestination(dest);
        request.setNRoutes(1); // minim 1
        request.setFiltre(filtre);

        mockMvc.perform(post("/api/v1/calculate-route")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void calculateRoute_WithFiltreUnderZero() throws Exception {
        RouteRequestDTO request = new RouteRequestDTO();
        Coord org = new Coord(); org.setLat(41.38); org.setLon(2.16);
        Coord dest = new Coord(); dest.setLat(41.40); dest.setLon(2.17);
        Filtre filtre = new Filtre();
        filtre.setSeguretat(-1f); // màxim 1
        request.setOrigin(org);
        request.setDestination(dest);
        request.setNRoutes(1); // minim 1
        request.setFiltre(filtre);

        mockMvc.perform(post("/api/v1/calculate-route")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void calculateRoute_WithNoFilter() throws Exception {
        RouteRequestDTO request = new RouteRequestDTO();
        Coord org = new Coord(); org.setLat(41.38); org.setLon(2.16);
        Coord dest = new Coord(); dest.setLat(41.40); dest.setLon(2.17);
        request.setOrigin(org);
        request.setDestination(dest);
        request.setNRoutes(1); // minim 1

        mockMvc.perform(post("/api/v1/calculate-route")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void evaluateRouteSecurity_NullRoutePoints() throws Exception {
        mockMvc.perform(post("/api/v1/evaluate-route-security")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void evaluateRouteSecurity_NoRoutePoints2() throws Exception {
        String s = "{\"routePoints\": null}";
        mockMvc.perform(post("/api/v1/evaluate-route-security")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(s))
                .andExpect(status().isBadRequest());
    }

    @Test
    void evaluateRouteSecurity_NoRoutePoints() throws Exception {
        String s = "{\"routePoints\": [" +
                "{\"lon\": 2.16, \"lat\": 41.38}" +
                "]}";
        mockMvc.perform(post("/api/v1/evaluate-route-security")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(s))
                .andExpect(status().isBadRequest());
    }

    @Test
    void evaluateRouteSecurity_OK() throws Exception {
        String s = "{\"routePoints\": [" +
                "{\"lon\": 2.16, \"lat\": 41.38}, " +
                "{\"lon\": 2.17, \"lat\": 41.38}" +
                "]}";

        mockMvc.perform(post("/api/v1/evaluate-route-security")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(s))
                .andExpect(status().isOk());
    }
}