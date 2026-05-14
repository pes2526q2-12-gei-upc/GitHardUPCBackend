package com.safesteps.backend.notifications;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import java.security.Principal;


@Controller
public class WebSocketController {

    /*
    Per a fer la implementacio, canviar string msg pel dto corresponent
     */
    @MessageMapping("/location.update")
    public void handleLocation(@Payload String msg, Principal principal) {
        throw new NotImplementedException();
    }
}