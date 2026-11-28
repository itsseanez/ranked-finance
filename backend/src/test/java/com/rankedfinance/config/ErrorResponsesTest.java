package com.rankedfinance.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.rankedfinance.TestcontainersConfiguration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;

// Over real HTTP, because MockMvc never forwards an error to /error. If /error requires sign-in,
// every anonymous error (a CSRF 403, a 400, a 500) reaches the browser as an empty 401.
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class ErrorResponsesTest {

    @Value("${local.server.port}")
    int port;

    @Test
    void anonymousPostWithoutCsrfIsForbiddenNotUnauthorized() throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/setup"))
                        .header("Content-Type", "application/json")
                        .POST(BodyPublishers.ofString("{\"code\":\"x\"}"))
                        .build();

        HttpResponse<String> response =
                HttpClient.newHttpClient().send(request, BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.headers().firstValue("Content-Type"))
                .hasValueSatisfying(
                        type -> assertThat(type).startsWith("application/problem+json"));
        assertThat(response.body()).contains("\"error\":\"forbidden\"");
    }
}
