package com.safesteps.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safesteps.backend.controller.ApiAddressController;
import com.safesteps.backend.domain.routecalculator.*;
import com.safesteps.backend.domain.common.exception.BadRequestException;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ExtendWith(MockitoExtension.class)
class ApiAddressControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private RouteCalculatorService routeCalculatorService;

    @Mock
    private FiltreService filtreService;

    @InjectMocks
    private ApiAddressController apiAddressController;
    @Mock
    private RouteEvaluationSafetyService routeEvaluationSafetyService;

    @BeforeEach
    void setUp() {

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        // Construïm el MockMvc injectant-li el nostre controlador, el validador i el GlobalExceptionHandler
        mockMvc = MockMvcBuilders.standaloneSetup(apiAddressController)
                .setValidator(validator)
                .setControllerAdvice(new com.safesteps.backend.domain.common.exception.GlobalExceptionHandler())
                .build();
    }

    @Test
    void calculateRoute_WithValidRequest() throws Exception {
        RouteRequestDTO request = buildValidRequest(FiltreEnum.SEGURETAT);
        Filtre filtre = new Filtre(FiltreEnum.SEGURETAT);

        when(filtreService.getFiltre(null, FiltreEnum.SEGURETAT)).thenReturn(filtre);

        when(routeCalculatorService.getBestRoute(any(), any(), anyInt(), any()))
                .thenReturn(new RouteResponseDTO());

        mockMvc.perform(post("/api/v1/calculate-route")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(filtreService).getFiltre(null, FiltreEnum.SEGURETAT);
    }

    @Test
    void calculateRoute_WithTooManyRoutes() throws Exception {
        RouteRequestDTO request = buildValidRequest(FiltreEnum.SEGURETAT);
        request.setNRoutes(10); // màxim 5

        mockMvc.perform(post("/api/v1/calculate-route")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void calculateRoute_WithTooFewRoutes() throws Exception {
        RouteRequestDTO request = buildValidRequest(FiltreEnum.SEGURETAT);
        request.setNRoutes(0); // minim 1

        mockMvc.perform(post("/api/v1/calculate-route")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
    @Test
    void calculateRouteMissingFilter() throws Exception {
        RouteRequestDTO request = buildValidRequest(null);

        mockMvc.perform(post("/api/v1/calculate-route")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void calculateRoutePersonalitzatNoGoogleId() throws Exception {
        RouteRequestDTO request = buildValidRequest(FiltreEnum.PERSONALITZAT);

        when(filtreService.getFiltre(null, FiltreEnum.PERSONALITZAT))
                .thenThrow(new BadRequestException("Per als filtres personalitzats el googleId no pot ser null."));

        mockMvc.perform(post("/api/v1/calculate-route")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(filtreService).getFiltre(null, FiltreEnum.PERSONALITZAT);
    }

    @Test
    void calculateRouteClimaNoGoogleId() throws Exception {
        RouteRequestDTO request = buildValidRequest(FiltreEnum.CLIMA);
        request.setGoogleId(null);

        when(filtreService.getFiltre(null, FiltreEnum.CLIMA)).thenReturn(new Filtre(FiltreEnum.CLIMA));

        mockMvc.perform(post("/api/v1/calculate-route")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(filtreService).getFiltre(null, FiltreEnum.CLIMA);
    }

    @Test
    void calculateRouteConfortNoGoogleId() throws Exception {
        RouteRequestDTO request = buildValidRequest(FiltreEnum.CONFORT);
        request.setGoogleId(null);

        when(filtreService.getFiltre(null, FiltreEnum.CONFORT)).thenReturn(new Filtre(FiltreEnum.CONFORT));

        mockMvc.perform(post("/api/v1/calculate-route")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(filtreService).getFiltre(null, FiltreEnum.CONFORT);
    }

    @Test
    void calculateRouteSeguretatNoGoogleId() throws Exception {
        RouteRequestDTO request = buildValidRequest(FiltreEnum.SEGURETAT);
        request.setGoogleId(null);

        when(filtreService.getFiltre(null, FiltreEnum.SEGURETAT)).thenReturn(new  Filtre(FiltreEnum.SEGURETAT));

        mockMvc.perform(post("/api/v1/calculate-route")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(filtreService).getFiltre(null, FiltreEnum.SEGURETAT);
    }

    @Test
    void calculateRouteInvalidOriginLongitude() throws Exception {
        RouteRequestDTO request = buildValidRequest(FiltreEnum.SEGURETAT);
        request.getOrigin().setLon(1.5);

        mockMvc.perform(post("/api/v1/calculate-route")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

    }

    @Test
    void calculateRouteInvalidDestinationLatitude() throws Exception {
        RouteRequestDTO request = buildValidRequest(FiltreEnum.SEGURETAT);
        request.getDestination().setLat(42.0);

        mockMvc.perform(post("/api/v1/calculate-route")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

    }

    private RouteRequestDTO buildValidRequest(FiltreEnum filtre) {
        RouteRequestDTO request = new RouteRequestDTO();
        Coord org = new Coord();
        org.setLat(41.38);
        org.setLon(2.16);
        Coord dest = new Coord();
        dest.setLat(41.40);
        dest.setLon(2.17);
        request.setOrigin(org);
        request.setDestination(dest);
        request.setNRoutes(3);
        request.setFiltre(filtre);
        return request;
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