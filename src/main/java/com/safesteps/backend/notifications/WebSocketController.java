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
    Es molt probable que nomes ens quedem amb location.update i ja.
     */
    @MessageMapping("/chat.send")
    public void handleChat(@Payload String msg, Principal principal) {
        throw new NotImplementedException();
    }

    @MessageMapping("/group-chat.send")
    public void handleGroupChat(@Payload String msg, Principal principal) {
        throw new NotImplementedException();
    }

    @MessageMapping("/emergency.start")
    public void handleEmergency(@Payload String msg, Principal principal) {
        throw new NotImplementedException();
    }

    @MessageMapping("/location.update")
    public void handleLocation(@Payload String msg, Principal principal) {
        throw new NotImplementedException();
    }
}