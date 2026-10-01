package com.tracker.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;

//! This enables our real-time two-way tunnel using the STOMP protocol
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        //? 1. Messages FROM the server TO the frontend will be sent to URLs starting with "/topic"
        // Example: /topic/sprints/{sprintId}/tasks - each board receives only its sprint's updates.
        config.enableSimpleBroker("/topic");

        //? 2. Messages FROM the frontend TO the server will be sent to URLs starting with "/app"
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        //? 3. This is the actual "Door" where the frontend connects to open the tunnel
        //! SockJS is a fallback library that allows WebSockets to work even on older browsers or restrictive networks
        String[] origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(origins.length == 0 ? new String[]{"*"} : origins)
                .withSockJS();
    }
}
