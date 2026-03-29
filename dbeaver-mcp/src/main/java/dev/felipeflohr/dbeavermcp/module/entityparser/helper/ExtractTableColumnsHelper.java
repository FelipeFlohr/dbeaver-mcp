package dev.felipeflohr.dbeavermcp.module.entityparser.helper;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserClassDeclaration;
import dev.felipeflohr.dbeavermcp.module.entityparser.enumerator.NamingStrategy;
import dev.felipeflohr.dbeavermcp.module.entityparser.model.EntityAttributeRecord;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.NullMarked;

import java.util.*;
import java.util.stream.Collectors;

@NullMarked
@UtilityClass
public class ExtractTableColumnsHelper {
    public Set<EntityAttributeRecord> extractTableColumns(ClassOrInterfaceDeclaration declaration, NamingStrategy namingStrategy) {
        Set<EntityAttributeRecord> columns = declaration.getExtendedTypes().stream()
                .filter(Objects::nonNull)
                .map(c -> {
                    try {
                        return c.resolve().asReferenceType().getTypeDeclaration()
                                .filter(JavaParserClassDeclaration.class::isInstance)
                                .map(JavaParserClassDeclaration.class::cast)
                                .map(JavaParserClassDeclaration::getWrappedNode)
                                .orElse(null);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .filter(d -> d.getAnnotationByName("MappedSuperclass").isPresent())
                .map(d -> extractTableColumns(d, namingStrategy))
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(HashSet::new));

        for (FieldDeclaration field : declaration.getFields()) {
            if (shouldSkipField(field)) continue;
            String fieldName = field.getVariable(0).getNameAsString();

            if (field.getAnnotationByName("Embedded").isPresent() || field.getAnnotationByName("EmbeddedId").isPresent()) {
                columns.addAll(collectEmbeddedColumns(field, namingStrategy));
            } else if (field.getAnnotationByName("ManyToOne").isPresent() || field.getAnnotationByName("OneToOne").isPresent()) {
                columns.addAll(resolveJoinColumns(field, namingStrategy));
            } else {
                String columnName = resolveColumnName(field, namingStrategy);
                columns.add(new EntityAttributeRecord(fieldName, columnName));
            }
        }
        return columns;
    }

    private Set<EntityAttributeRecord> resolveJoinColumns(FieldDeclaration field, NamingStrategy namingStrategy) {
        String fieldName = field.getVariable(0).getNameAsString();

        Optional<AnnotationExpr> joinColumnsAnnotation = field.getAnnotationByName("JoinColumns");
        if (joinColumnsAnnotation.isPresent()) {
            Set<EntityAttributeRecord> result = new HashSet<>();
            List<AnnotationExpr> joinColumnAnnotations = extractNestedAnnotations(joinColumnsAnnotation.get());
            for (AnnotationExpr jc : joinColumnAnnotations) {
                if (jc instanceof NormalAnnotationExpr normal) {
                    normal.getPairs().stream()
                            .filter(p -> "name".equals(p.getNameAsString()))
                            .map(p -> EntityParserHelper.extractStringValue(p.getValue()))
                            .findFirst().ifPresent(colName -> result.add(new EntityAttributeRecord(fieldName, colName)));
                }
            }
            if (!result.isEmpty()) return result;
        }

        String columnName = resolveJoinColumnName(field, namingStrategy);
        return Set.of(new EntityAttributeRecord(fieldName, columnName));
    }

    private String resolveJoinColumnName(FieldDeclaration field, NamingStrategy namingStrategy) {
        Optional<String> explicit = getAnnotationAttribute(field, "JoinColumn", "name");
        if (explicit.isPresent()) return explicit.get();

        String fieldName = field.getVariable(0).getNameAsString();
        return EntityParserHelper.toStrategy(fieldName, namingStrategy) + "_id";
    }

    private boolean shouldSkipField(FieldDeclaration field) {
        if (field.isStatic()) return true;
        if (field.isTransient()) return true;
        if (field.getAnnotationByName("Transient").isPresent()) return true;
        if (field.getAnnotationByName("OneToMany").isPresent()) return true;
        if (field.getAnnotationByName("ManyToMany").isPresent()) return true;
        return field.getAnnotationByName("ElementCollection").isPresent();
    }

    private String resolveColumnName(FieldDeclaration field, NamingStrategy namingStrategy) {
        Optional<String> explicit = getAnnotationAttribute(field, "Column", "name");
        if (explicit.isPresent()) return explicit.get();

        String fieldName = field.getVariable(0).getNameAsString();
        return EntityParserHelper.toStrategy(fieldName, namingStrategy);
    }

    private Set<EntityAttributeRecord> collectEmbeddedColumns(FieldDeclaration field, NamingStrategy namingStrategy) {
        Optional<ClassOrInterfaceDeclaration> resolved = resolveFieldType(field);
        if (resolved.isEmpty()) {
            String fieldName = field.getVariable(0).getNameAsString();
            String columnName = resolveColumnName(field, namingStrategy);
            return Set.of(new EntityAttributeRecord(fieldName, columnName));
        }

        ClassOrInterfaceDeclaration embeddable = resolved.get();
        Map<String, String> overrides = extractAttributeOverrides(field);
        Set<EntityAttributeRecord> columns = new HashSet<>();

        for (FieldDeclaration embField : embeddable.getFields()) {
            if (shouldSkipField(embField)) continue;

            String embFieldName = embField.getVariable(0).getNameAsString();

            if (overrides.containsKey(embFieldName)) {
                columns.add(new EntityAttributeRecord(embFieldName, overrides.get(embFieldName)));
            } else {
                columns.add(new EntityAttributeRecord(embFieldName, resolveColumnName(embField, namingStrategy)));
            }
        }

        return columns;
    }

    private Map<String, String> extractAttributeOverrides(FieldDeclaration field) {
        Map<String, String> overrides = new HashMap<>();

        field.getAnnotationByName("AttributeOverride")
                .ifPresent(a -> parseAttributeOverride(a, overrides));

        field.getAnnotationByName("AttributeOverrides").ifPresent(a -> {
            List<AnnotationExpr> nested = extractNestedAnnotations(a);
            nested.forEach(ann -> parseAttributeOverride(ann, overrides));
        });

        return overrides;
    }

    private List<AnnotationExpr> extractNestedAnnotations(AnnotationExpr containerAnnotation) {
        List<AnnotationExpr> result = new ArrayList<>();

        if (containerAnnotation instanceof NormalAnnotationExpr normal) {
            normal.getPairs().stream()
                    .filter(p -> "value".equals(p.getNameAsString()))
                    .filter(p -> p.getValue() instanceof ArrayInitializerExpr)
                    .map(p -> (ArrayInitializerExpr) p.getValue())
                    .flatMap(arr -> arr.getValues().stream())
                    .filter(AnnotationExpr.class::isInstance)
                    .map(AnnotationExpr.class::cast)
                    .forEach(result::add);
        }

        if (containerAnnotation instanceof SingleMemberAnnotationExpr single
                && single.getMemberValue() instanceof ArrayInitializerExpr array) {
            array.getValues().stream()
                    .filter(AnnotationExpr.class::isInstance)
                    .map(AnnotationExpr.class::cast)
                    .forEach(result::add);
        }

        return result;
    }

    private void parseAttributeOverride(AnnotationExpr ann, Map<String, String> overrides) {
        if (!(ann instanceof NormalAnnotationExpr normal)) return;

        String name = null;
        String columnName = null;

        for (var pair : normal.getPairs()) {
            if ("name".equals(pair.getNameAsString())) {
                name = EntityParserHelper.extractStringValue(pair.getValue());
            } else if ("column".equals(pair.getNameAsString()) && pair.getValue() instanceof NormalAnnotationExpr colAnn) {
                columnName = colAnn.getPairs().stream()
                        .filter(p -> "name".equals(p.getNameAsString()))
                        .map(p -> EntityParserHelper.extractStringValue(p.getValue()))
                        .findFirst()
                        .orElse(null);
            }
        }

        if (name != null && columnName != null) {
            overrides.put(name, columnName);
        }
    }

    private Optional<ClassOrInterfaceDeclaration> resolveFieldType(FieldDeclaration field) {
        try {
            return field.getVariable(0).getType().resolve()
                    .asReferenceType().getTypeDeclaration()
                    .filter(JavaParserClassDeclaration.class::isInstance)
                    .map(JavaParserClassDeclaration.class::cast)
                    .map(JavaParserClassDeclaration::getWrappedNode);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Optional<String> getAnnotationAttribute(FieldDeclaration field, String annotationName, String attributeName) {
        return field.getAnnotationByName(annotationName)
                .flatMap(a -> {
                    if (a instanceof NormalAnnotationExpr normal) {
                        return normal.getPairs().stream()
                                .filter(p -> p.getNameAsString().equals(attributeName))
                                .map(p -> EntityParserHelper.extractStringValue(p.getValue()))
                                .findFirst();
                    }
                    if (a instanceof SingleMemberAnnotationExpr single && "value".equals(attributeName)) {
                        return Optional.ofNullable(EntityParserHelper.extractStringValue(single.getMemberValue()));
                    }
                    return Optional.empty();
                });
    }
}
