package com.safesteps.backend.config;

import com.safesteps.backend.notifications.MyHandshakeHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Endpoints per a subscriure's
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        //front escolta d'aqui
        config.enableSimpleBroker("/topic", "/queue");
        //front envia a /app
        config.setApplicationDestinationPrefixes("/app");
        //prefix privat /user -> /user/queue/ ...
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-safesteps")
                .setAllowedOrigins("*")
                .setHandshakeHandler(new MyHandshakeHandler());
    }
}