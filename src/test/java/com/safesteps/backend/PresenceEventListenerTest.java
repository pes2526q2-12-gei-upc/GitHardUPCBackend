package com.safesteps.backend;

import com.safesteps.backend.domain.users.repository.UserRepository;
import com.safesteps.backend.notifications.PresenceEventListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PresenceEventListenerTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PresenceEventListener presenceEventListener;

    @Test
    void shouldUpdateStatusToOnlineOnConnect() {
        String googleId = "googleId";
        Principal principal = () -> googleId;

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setUser(principal);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        SessionConnectedEvent event = new SessionConnectedEvent(this, message, principal);

        presenceEventListener.handleWebSocketConnectListener(event);

        verify(userRepository, times(1)).updateOnlineStatus(googleId, true);
    }

    @Test
    void onConnectPrincipalNull() {
        String googleId = "googleId";

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setUser(null);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        SessionConnectedEvent event = new SessionConnectedEvent(this, message, null);

        presenceEventListener.handleWebSocketConnectListener(event);

        verify(userRepository, never()).updateOnlineStatus(googleId, true);
    }

    @Test
    void shouldUpdateStatusToOfflineOnDisconnect() {
        String googleId = "googleId";
        Principal principal = () -> googleId;

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        accessor.setUser(principal);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        SessionDisconnectEvent event = new SessionDisconnectEvent(this, message, "session1", CloseStatus.NORMAL);

        presenceEventListener.handleWebSocketDisconnectListener(event);

        verify(userRepository, times(1)).updateOnlineStatus(googleId, false);
    }

    @Test
    void onDisconnectNullPrincipal() {
        String googleId = "googleId";

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        accessor.setUser(null);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        SessionDisconnectEvent event = new SessionDisconnectEvent(this, message, "session1", CloseStatus.NORMAL);

        presenceEventListener.handleWebSocketDisconnectListener(event);

        verify(userRepository, never()).updateOnlineStatus(googleId, false);
    }
}
