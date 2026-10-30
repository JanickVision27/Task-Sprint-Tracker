package com.tracker.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * * Provides the RestClient.Builder used by the Brevo sender.
 *
 * * Declaring it explicitly keeps the email HTTP layer independent of the web
 * * auto-configuration (which is not loaded in every slice test) and gives the
 * * unit tests a place to plug in MockRestServiceServer.
 */
@Configuration
public class EmailClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
