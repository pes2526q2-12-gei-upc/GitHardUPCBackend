package com.safesteps.backend.notifications;

import com.safesteps.backend.domain.routecalculator.Coord;
import com.safesteps.backend.domain.users.service.UserService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import java.security.Principal;
import java.util.List;


@Controller
public class WebSocketController {

    private final NotificationService notificationService;
    private final UserService userService;

    public WebSocketController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @MessageMapping("/location.update")
    public void handleLocation(Principal principal, @Payload Coord coordenades) {
        if (principal == null) return;
        String googleId = principal.getName();
        boolean isInEmergency = userService.getUserStatusEmergency(googleId);
        if (isInEmergency) {
            List<String> users = userService.getEmergencyContactsGoogleIds(googleId);
            notificationService.sendLocationUpdate(users, coordenades);
        }
    }
}