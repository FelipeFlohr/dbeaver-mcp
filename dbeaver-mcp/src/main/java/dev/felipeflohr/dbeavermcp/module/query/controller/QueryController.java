package dev.felipeflohr.dbeavermcp.module.query.controller;

import dev.felipeflohr.dbeavermcp.exception.DBeaverMCPValidationException;
import dev.felipeflohr.dbeavermcp.module.query.factory.QueryServiceFactory;
import dev.felipeflohr.dbeavermcp.module.query.service.QueryService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@NullMarked
@RequiredArgsConstructor
@RestController
@RequestMapping(QueryController.REQUEST_MAPPING)
public class QueryController {
    public static final String REQUEST_MAPPING = "/query";
    private final QueryServiceFactory queryServiceFactory;

    @PostMapping
    public ResponseEntity<?> query(
            @RequestParam("connectionName") String connectionName,
            @RequestParam(value = "timeout", required = false) @Nullable Integer timeoutInSeconds,
            @RequestBody(required = false) @Nullable List<String> statements
    ) throws DBeaverMCPValidationException {
        if (statements == null || statements.isEmpty()) {
            return ResponseEntity.badRequest().body("No statements");
        }
        QueryService service = queryServiceFactory.getFromConnectionName(connectionName);
        return ResponseEntity.ok(service.executeReadOnlyStatements(statements, connectionName, timeoutInSeconds));
    }

    @PostMapping("/async")
    public ResponseEntity<?> queryAsync(
            @RequestParam("connectionName") String connectionName,
            @RequestParam(value = "timeout", required = false) @Nullable Integer timeoutInSeconds,
            @RequestBody(required = false) @Nullable List<String> statements
    ) throws DBeaverMCPValidationException {
        if (statements == null || statements.isEmpty()) {
            return ResponseEntity.badRequest().body("No statements");
        }
        QueryService service = queryServiceFactory.getFromConnectionName(connectionName);
        return ResponseEntity.ok(service.executeReadOnlyStatementsAsync(statements, connectionName, timeoutInSeconds).getAbsolutePath());
    }
}
