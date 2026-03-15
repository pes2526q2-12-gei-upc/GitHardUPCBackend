package com.safesteps.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safesteps.backend.controller.ApiAddressController;
import com.safesteps.backend.domain.routecalculator.Coord;
import com.safesteps.backend.domain.routecalculator.RouteCalculatorService;
import com.safesteps.backend.domain.routecalculator.RouteRequestDTO;
import com.safesteps.backend.domain.routecalculator.RouteResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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

    @InjectMocks
    private ApiAddressController apiAddressController;

    @BeforeEach
    void setUp() {

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        // Construïm el MockMvc injectant-li el nostre controlador i el validador
        mockMvc = MockMvcBuilders.standaloneSetup(apiAddressController)
                .setValidator(validator)
                .build();
    }

    @Test
    void calculateRoute_WithValidRequest() throws Exception {
        RouteRequestDTO request = new RouteRequestDTO();
        Coord org = new Coord(); org.setLat(41.38); org.setLon(2.16);
        Coord dest = new Coord(); dest.setLat(41.40); dest.setLon(2.17);
        request.setOrigin(org);
        request.setDestination(dest);
        request.setNRoutes(3);

        when(routeCalculatorService.getBestRoute(any(), any(), anyInt()))
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
}