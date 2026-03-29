package dev.felipeflohr.dbeavermcp.module.entityparser.service;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import dev.felipeflohr.dbeavermcp.module.entityparser.enumerator.NamingStrategy;
import dev.felipeflohr.dbeavermcp.module.entityparser.helper.ExtractTableColumnsHelper;
import dev.felipeflohr.dbeavermcp.module.entityparser.helper.ExtractTableNameHelper;
import dev.felipeflohr.dbeavermcp.module.entityparser.model.EntityAttributeRecord;
import dev.felipeflohr.dbeavermcp.module.entityparser.model.ParsedEntityRecord;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

@Slf4j
@NullMarked
@Service
class EntityParserServiceImpl implements EntityParserService {
    @Override
    public @Nullable ParsedEntityRecord getEntityByFilePath(String filePath, @Nullable String className, NamingStrategy namingStrategy, @Nullable String sourceRoot) {
        try {
            CompilationUnit compUnit = parseWithSymbolSolver(new File(filePath), sourceRoot);
            return compUnit.findAll(ClassOrInterfaceDeclaration.class).stream()
                    .filter(c -> !c.isInterface())
                    .map(declaration -> classDeclarationToParsedEntity(filePath, declaration, namingStrategy))
                    .filter(Objects::nonNull)
                    .filter(p -> p.filePath().equals(filePath) && (className == null || p.className().equals(className)))
                    .findFirst()
                    .orElse(null);
        } catch (ParseProblemException | FileNotFoundException e) {
            log.error("Not possible to parse file {}.", filePath, e);
            return null;
        }
    }

    @Nullable
    private ParsedEntityRecord classDeclarationToParsedEntity(String filePath, ClassOrInterfaceDeclaration declaration, NamingStrategy namingStrategy) {
        String className = declaration.getNameAsString();
        String databaseName = ExtractTableNameHelper.getEntityTableName(declaration, namingStrategy);
        if (databaseName == null) return null;
        Set<EntityAttributeRecord> attributes = ExtractTableColumnsHelper.extractTableColumns(declaration, namingStrategy);
        return new ParsedEntityRecord(className, filePath, databaseName, attributes);
    }

    private CompilationUnit parseWithSymbolSolver(File file, @Nullable String sourceRoot) throws FileNotFoundException {
        CombinedTypeSolver combinedSolver = new CombinedTypeSolver();
        combinedSolver.add(new ReflectionTypeSolver(false));

        File resolvedSourceRoot = sourceRoot != null ? new File(sourceRoot) : detectSourceRoot(file);
        if (resolvedSourceRoot != null && resolvedSourceRoot.isDirectory()) {
            combinedSolver.add(new JavaParserTypeSolver(resolvedSourceRoot));
        } else {
            File parent = file.getParentFile();
            if (parent != null) {
                combinedSolver.add(new JavaParserTypeSolver(parent));
            }
        }

        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedSolver);
        ParserConfiguration config = new ParserConfiguration().setSymbolResolver(symbolSolver);
        JavaParser parser = new JavaParser(config);

        ParseResult<CompilationUnit> result = parser.parse(file);
        if (result.isSuccessful() && result.getResult().isPresent()) {
            return result.getResult().get();
        }
        throw new ParseProblemException(result.getProblems());
    }

    @Nullable
    private File detectSourceRoot(File javaFile) {
        String path = javaFile.getAbsolutePath().replace('\\', '/');
        for (String marker : List.of("src/main/java", "src/test/java")) {
            int idx = path.indexOf(marker);
            if (idx >= 0) {
                return new File(path.substring(0, idx + marker.length()));
            }
        }
        return null;
    }
}
