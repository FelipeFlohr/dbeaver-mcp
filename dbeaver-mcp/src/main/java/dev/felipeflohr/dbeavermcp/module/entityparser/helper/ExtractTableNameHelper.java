package dev.felipeflohr.dbeavermcp.module.entityparser.helper;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import dev.felipeflohr.dbeavermcp.module.entityparser.enumerator.NamingStrategy;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

@NullMarked
@UtilityClass
public class ExtractTableNameHelper {
    @Nullable
    public String getEntityTableName(ClassOrInterfaceDeclaration declaration, NamingStrategy namingStrategy) {
        if (!EntityParserHelper.isEntity(declaration)) return null;
        Optional<String> explicitTableName = declaration.getAnnotationByName("Table")
                .filter(NormalAnnotationExpr.class::isInstance)
                .map(NormalAnnotationExpr.class::cast)
                .flatMap(normal -> normal.getPairs().stream()
                        .filter(p -> "name".equals(p.getNameAsString()))
                        .map(p -> EntityParserHelper.extractStringValue(p.getValue()))
                        .findFirst());
        if (explicitTableName.isPresent()) return explicitTableName.get();

        String logicalName = declaration.getAnnotationByName(EntityParserHelper.ENTITY_ANNOTATION_NAME)
                .filter(NormalAnnotationExpr.class::isInstance)
                .map(NormalAnnotationExpr.class::cast)
                .flatMap(normal -> normal.getPairs().stream()
                        .filter(p -> "name".equals(p.getNameAsString()))
                        .map(p -> EntityParserHelper.extractStringValue(p.getValue()))
                        .findFirst())
                .orElse(declaration.getNameAsString());
        return EntityParserHelper.toStrategy(logicalName, namingStrategy);
    }
}
