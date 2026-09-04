package com.tracker.backend.config;

import org.springframework.context.annotation.Configuration;                     // Marks this as a configuration class
import org.springframework.messaging.simp.config.MessageBrokerRegistry;           // Configures where messages are sent
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker; // Turns on WebSocket + STOMP
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;    // Registers the connection endpoint
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer; // The interface we implement
//! This enables our real-time two-way tunnel using the STOMP protocol
@Configuration
@EnableWebSocketMessageBroker 
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        //? 1. Messages FROM the server TO the frontend will be sent to URLs starting with "/topic"
        // Example: /topic/tasks - The frontend subscribes to this to get updates
        config.enableSimpleBroker("/topic");
        
        //? 2. Messages FROM the frontend TO the server will be sent to URLs starting with "/app"
        // (We won't use this much, but it's good practice to define it)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        //? 3. This is the actual "Door" where the frontend connects to open the tunnel
        //! SockJS is a fallback library that allows WebSockets to work even on older browsers or restrictive networks
        registry.addEndpoint("/ws").withSockJS();
    }
}