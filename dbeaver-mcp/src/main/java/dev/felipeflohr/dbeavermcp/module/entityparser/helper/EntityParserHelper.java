package dev.felipeflohr.dbeavermcp.module.entityparser.helper;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import dev.felipeflohr.dbeavermcp.module.entityparser.enumerator.NamingStrategy;
import lombok.experimental.UtilityClass;

@UtilityClass
public class EntityParserHelper {
    public static final String ENTITY_ANNOTATION_NAME = "Entity";

    public boolean isEntity(ClassOrInterfaceDeclaration declaration) {
        return declaration.getAnnotationByName(ENTITY_ANNOTATION_NAME).isPresent();
    }

    public String extractStringValue(Expression expr) {
        if (expr instanceof StringLiteralExpr str) {
            return str.getValue();
        }
        return expr.toString().replace("\"", "");
    }

    public String toStrategy(String value, NamingStrategy strategy) {
        return switch (strategy) {
            case TO_SNAKE_CASE -> toSnakeCase(value);
            case TO_LOWER_CASE -> value.toLowerCase();
            case TO_UPPER_CASE -> value.toUpperCase();
            case DO_NOTHING -> value;
        };
    }

    private String toSnakeCase(String camelCase) {
        return camelCase
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2")
                .toLowerCase();
    }
}
