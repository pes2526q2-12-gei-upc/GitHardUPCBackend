package com.safesteps.backend.notifications;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.socket.WebSocketHandler;

import java.security.Principal;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class MyHandshakeHandlerTest {

    private final MyHandshakeHandler handler = new MyHandshakeHandler();
    WebSocketHandler wsH = Mockito.mock(WebSocketHandler.class);

    @Test
    void determineUserGoogleId() {
        ServerHttpRequest request = new ServletServerHttpRequest(
                new MockHttpServletRequest("GET", "/ws-safesteps?googleId=12345")
        );

        Principal principal = handler.determineUser(request, wsH, new HashMap<>());

        assertNotNull(principal);
        assertEquals("12345", principal.getName());
    }

    @Test
    void determineUserNoGoogleId() {
        ServerHttpRequest request = new ServletServerHttpRequest(
                new MockHttpServletRequest("GET", "/ws-safesteps?googleId=")
        );

        Principal principal = handler.determineUser(request, wsH, new HashMap<>());

        assertNull(principal);
    }

    @Test
    void determineUserBadRequest() {
        ServerHttpRequest request = new ServletServerHttpRequest(
                new MockHttpServletRequest("GET", "/ws-safesteps?")
        );

        Principal principal = handler.determineUser(request, wsH, new HashMap<>());

        assertNull(principal);
    }

    @Test
    void determineUserBadRequest2() {
        assertThrows(Exception.class ,() -> handler.determineUser(null, null, null));
    }

    @Test
    void determineUserBadRequest3() {
        ServerHttpRequest request = new ServletServerHttpRequest(
                new MockHttpServletRequest("GET", "/ws-safesteps?")
        );

        assertThrows(Exception.class ,() -> handler.determineUser(request, null, null));
    }

    @Test
    void determineUserBadRequest5() {
        ServerHttpRequest request = new ServletServerHttpRequest(
                new MockHttpServletRequest("GET", "/ws-safesteps?")
        );

        assertThrows(Exception.class ,() -> handler.determineUser(request, wsH, null));
    }
}