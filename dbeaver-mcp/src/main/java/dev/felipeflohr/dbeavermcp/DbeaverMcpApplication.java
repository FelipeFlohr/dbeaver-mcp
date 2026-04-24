package dev.felipeflohr.dbeavermcp;

import dev.felipeflohr.dbeavermcp.util.NetworkUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;

@Slf4j
@RequiredArgsConstructor
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
})
public class DbeaverMcpApplication {
    private static final int HTTP_PORT = 8790;

    @SuppressWarnings("UnnecessaryModifier")
    public static void main(String[] args) {
        boolean httpAvailable = NetworkUtils.isPortAvailable(HTTP_PORT);

        SpringApplication app = new SpringApplication(DbeaverMcpApplication.class);
        app.setWebApplicationType(httpAvailable ? WebApplicationType.SERVLET : WebApplicationType.NONE);

        log.info("Starting MCP. HTTP available = {}", httpAvailable);
        app.run(args);
    }
}
