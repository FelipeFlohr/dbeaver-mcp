package dev.felipeflohr.dbeavermcp;

import dev.felipeflohr.dbeavermcp.util.NetworkUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

class DbeaverMcpApplicationMultiInstanceTest {
    private static final int HTTP_PORT = 8790;
    private static final String[] ARGS = {
            "--spring.ai.mcp.server.stdio=false",
            "--spring.main.banner-mode=off",
            "--logging.file.name=build/test-logs/multi-instance.log",
            "--dbeavermcp.dbeaver.config.data-sources-file-path=",
            "--dbeavermcp.dbeaver.config.credentials-config-file-path=",
            "--server.port=" + HTTP_PORT
    };

    private ConfigurableApplicationContext firstContext;
    private ConfigurableApplicationContext secondContext;

    @AfterEach
    void tearDown() {
        if (secondContext != null) secondContext.close();
        if (firstContext != null) firstContext.close();
    }

    @Test
    void shouldAllowTwoInstancesToRunSimultaneouslyWithOneServingHttp() throws Exception {
        waitForPortToBeAvailable();

        firstContext = startApplication();
        assertTrue(firstContext.isRunning(), "First context should be running");
        assertInstanceOf(WebApplicationContext.class, firstContext, "First context should be a web context");
        assertFalse(NetworkUtils.isPortAvailable(HTTP_PORT), "Port should be taken after first instance starts");

        secondContext = startApplication();
        assertTrue(secondContext.isRunning(), "Second context should be running");
        assertFalse(secondContext instanceof WebApplicationContext,
                "Second context should not be a web context since the port is taken");

        HttpResponse<String> response = sendHealthRequest();
        assertEquals(200, response.statusCode(), "Health endpoint should respond with 200");
        assertNotNull(response.body());
        assertTrue(response.body().contains("\"status\":\"UP\""),
                "Expected UP status but got: " + response.body());
    }

    private void waitForPortToBeAvailable() {
        try {
            await().atMost(30, SECONDS).pollInterval(Duration.ofMillis(500))
                    .until(() -> NetworkUtils.isPortAvailable(HTTP_PORT));
        } catch (org.awaitility.core.ConditionTimeoutException e) {
            Assumptions.abort("Port " + HTTP_PORT + " did not become free within 30 seconds");
        }
    }

    private ConfigurableApplicationContext startApplication() {
        boolean httpAvailable = NetworkUtils.isPortAvailable(HTTP_PORT);
        SpringApplication app = new SpringApplication(DbeaverMcpApplication.class);
        app.setWebApplicationType(httpAvailable ? WebApplicationType.SERVLET : WebApplicationType.NONE);
        return app.run(ARGS);
    }

    private HttpResponse<String> sendHealthRequest() throws Exception {
        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + HTTP_PORT + "/actuator/health"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }
}
