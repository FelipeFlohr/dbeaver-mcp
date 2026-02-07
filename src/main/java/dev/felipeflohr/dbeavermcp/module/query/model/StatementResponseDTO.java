package dev.felipeflohr.dbeavermcp.module.query.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Map;

@NullMarked
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatementResponseDTO {
    private String sql;
    private List<Map<String, Object>> response;
}
