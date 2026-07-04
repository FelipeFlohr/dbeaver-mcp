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
    private static final int DEFAULT_HTTP_PORT = 8790;

    @SuppressWarnings("UnnecessaryModifier")
    public static void main(String[] args) {
        int httpPort = resolveServerPort(args);
        boolean httpAvailable = NetworkUtils.isPortAvailable(httpPort);

        SpringApplication app = new SpringApplication(DbeaverMcpApplication.class);
        app.setWebApplicationType(httpAvailable ? WebApplicationType.SERVLET : WebApplicationType.NONE);

        log.info("Starting MCP. Port {}, HTTP available = {}", httpPort, httpAvailable);
        app.run(args);
    }

    /**
     * Resolves the effective HTTP port so the web-vs-stdio decision keys off the port this
     * instance would actually bind, not a fixed one. This lets several instances run as HTTP
     * servers on different ports. Precedence: {@code --server.port} arg, {@code SERVER_PORT}
     * env, {@code server.port} system property, else {@value #DEFAULT_HTTP_PORT}.
     */
    private static int resolveServerPort(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--server.port=")) {
                return parsePort(arg.substring("--server.port=".length()));
            }
            if (arg.equals("--server.port") && i + 1 < args.length) {
                return parsePort(args[i + 1]);
            }
        }
        String env = System.getenv("SERVER_PORT");
        if (env != null && !env.isBlank()) return parsePort(env);
        String sys = System.getProperty("server.port");
        if (sys != null && !sys.isBlank()) return parsePort(sys);
        return DEFAULT_HTTP_PORT;
    }

    private static int parsePort(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return DEFAULT_HTTP_PORT;
        }
    }
}
