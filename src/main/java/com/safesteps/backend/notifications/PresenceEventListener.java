package com.safesteps.backend.notifications;

import com.safesteps.backend.domain.users.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

/**
 * Classe que s'encarrega de "loggejar" les connexions i desconnexions dels usuaris al WebSocket.
 * Quan un usuari es connecta, actualitza el seu estat a "online" a la base de dades.
 * Quan es desconnecta, actualitza el seu estat a "offline".
 */
@Component
public class PresenceEventListener {

    private final UserRepository userRepository;
    private static final Logger logger = LoggerFactory.getLogger(PresenceEventListener.class);


    public PresenceEventListener(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        // front ha de passar el googleId com a header
        Principal principal = sha.getUser();
        if (principal == null) {
            logger.info("User hasn't been logged");
            return;
        }

        String googleId = principal.getName();
        userRepository.updateOnlineStatus(googleId, true);
        logger.info("Usuari connectat i online: {}", googleId);
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());

        Principal principal = sha.getUser();
        if (principal != null) {
            String googleId = principal.getName();
            userRepository.updateOnlineStatus(googleId, false);
            logger.info("Usuari desconnectat: {}", googleId);
        }
    }
}