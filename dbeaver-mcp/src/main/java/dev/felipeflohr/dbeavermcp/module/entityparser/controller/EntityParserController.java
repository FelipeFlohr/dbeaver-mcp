package dev.felipeflohr.dbeavermcp.module.entityparser.controller;

import dev.felipeflohr.dbeavermcp.module.entityparser.enumerator.NamingStrategy;
import dev.felipeflohr.dbeavermcp.module.entityparser.model.ParsedEntityRecord;
import dev.felipeflohr.dbeavermcp.module.entityparser.service.EntityParserService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RequiredArgsConstructor
@NullMarked
@RestController
@RequestMapping("/entity-parser")
public class EntityParserController {
    private final EntityParserService service;

    @GetMapping("/file-path")
    public ResponseEntity<ParsedEntityRecord> entityByFilePath(
            @RequestParam("filePath") String filePath,
            @RequestParam(value = "className", required = false) @Nullable String className,
            @RequestParam("namingStrategy") NamingStrategy namingStrategy,
            @RequestParam(value = "sourceRoot", required = false) @Nullable String sourceRoot
    ) {
        return ResponseEntity.ok(service.getEntityByFilePath(filePath, className, namingStrategy, sourceRoot));
    }
}
