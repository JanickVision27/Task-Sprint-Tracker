package com.tracker.backend.service.mail;

import com.tracker.backend.config.BrevoProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// * Checks the exact HTTP contract with Brevo so a provider-side change is caught in CI
// * instead of silently swallowing every welcome email.
class BrevoEmailSenderTest {

    private static final String API_URL = "https://api.brevo.com/v3/smtp/email";

    private MockRestServiceServer server;
    private BrevoEmailSender sender;
    private BrevoProperties properties;

    @BeforeEach
    void setUp() {
        properties = new BrevoProperties();
        properties.setApiKey("xkeysib-test-key");
        properties.setApiUrl(API_URL);
        properties.setSenderEmail("no-reply@tracker.test");
        properties.setSenderName("Task & Sprint Tracker");

        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        sender = new BrevoEmailSender(properties, builder);
    }

    @Test
    void sendHtml_PostsBrevoPayloadWithApiKeyHeader() {
        server.expect(requestTo(API_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "xkeysib-test-key"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sender.email").value("no-reply@tracker.test"))
                .andExpect(jsonPath("$.sender.name").value("Task & Sprint Tracker"))
                .andExpect(jsonPath("$.to[0].email").value("alice@test.com"))
                .andExpect(jsonPath("$.to[0].name").value("Alice"))
                .andExpect(jsonPath("$.subject").value("Welcome"))
                .andExpect(jsonPath("$.htmlContent").value("<p>hi</p>"))
                .andExpect(jsonPath("$.textContent").value("hi"))
                .andRespond(withSuccess("{\"messageId\":\"<abc@brevo>\"}", MediaType.APPLICATION_JSON));

        sender.sendHtml("alice@test.com", "Alice", "Welcome", "<p>hi</p>", "hi");

        server.verify();
    }

    @Test
    void sendHtml_WithoutConfiguration_SkipsTheHttpCall() {
        properties.setApiKey("");
        properties.setSenderEmail("");

        // * No server expectations registered: any HTTP call would fail the test.
        assertDoesNotThrow(() -> sender.sendHtml("alice@test.com", "Alice", "Welcome", "<p>hi</p>", "hi"));
    }

    @Test
    void sendHtml_WhenDisabled_SkipsTheHttpCall() {
        properties.setEnabled(false);

        assertDoesNotThrow(() -> sender.sendHtml("alice@test.com", "Alice", "Welcome", "<p>hi</p>", "hi"));
    }

    @Test
    void sendHtml_BrevoError_SurfacesAsRestClientException() {
        server.expect(requestTo(API_URL))
                .andRespond(withStatus(org.springframework.http.HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"unauthorized\",\"message\":\"Key not found\"}"));

        assertThrows(RestClientResponseException.class,
                () -> sender.sendHtml("alice@test.com", "Alice", "Welcome", "<p>hi</p>", "hi"));

        server.verify();
    }
}
