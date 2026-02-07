package dev.felipeflohr.dbeavermcp.module.dbeaver.model.datasources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NullMarked;

@NullMarked
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DBeaverConnectionConfigurationSSHTunnelHandlerDTO {
    private boolean enabled;
    private DBeaverConnectionConfigurationSSHTunnelPropertiesDTO properties;
}
