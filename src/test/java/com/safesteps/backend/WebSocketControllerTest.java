package com.safesteps.backend;

import com.safesteps.backend.domain.routecalculator.Coord;
import com.safesteps.backend.domain.users.dto.UserResponseDTO;
import com.safesteps.backend.domain.users.service.UserService;
import com.safesteps.backend.notifications.NotificationService;
import com.safesteps.backend.notifications.WebSocketController;
import com.safesteps.backend.notifications.dto.EmergencyLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketControllerTest {

    @Mock
    private NotificationService notificationService;
    @Mock private UserService userService;
    @InjectMocks
    private WebSocketController webSocketController;

    private String googleId;
    private Coord coords;
    private Principal principal;

    @BeforeEach
    void setUp() {
        googleId = "googleId";
        coords = new Coord();
        coords.setLat(41.40338);
        coords.setLon(2.17403);
        principal = () -> googleId;
    }


    @Test
    void sendLocation_OKInEmergency() {
        UserResponseDTO userDTO = new UserResponseDTO();
        userDTO.setIsInEmergency(true);
        userDTO.setUsername("user");

        List<String> contacts = List.of("contact1", "contact2");

        when(userService.getUserByGoogleId(googleId)).thenReturn(userDTO);
        when(userService.getEmergencyContactsGoogleIds(googleId)).thenReturn(contacts);

        webSocketController.handleLocation(principal, coords);

        verify(notificationService).sendLocationUpdate(eq(contacts), any(EmergencyLocation.class));
    }

    @Test
    void sendLocation_OKNotInEmergency() {
        UserResponseDTO userDTO = new UserResponseDTO();
        userDTO.setIsInEmergency(false);
        userDTO.setUsername("user");

        when(userService.getUserByGoogleId(googleId)).thenReturn(userDTO);

        webSocketController.handleLocation(principal, coords);

        verify(notificationService, never()).sendLocationUpdate(any(), any());
    }

    @Test
    void sendLocation_PrincipalCaigut() {
        UserResponseDTO userDTO = new UserResponseDTO();
        userDTO.setIsInEmergency(true);
        userDTO.setUsername("user");

        webSocketController.handleLocation(null, coords);

        verify(notificationService, never()).sendLocationUpdate(any(), any());
        verify(userService, never()).getUserByGoogleId(any());
    }
}