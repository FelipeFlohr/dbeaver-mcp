package dev.felipeflohr.dbeavermcp.module.dbeaver.model.datasources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NullMarked;

@NullMarked
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DBeaverConnectionDTO {
    private String provider;
    private String driver;
    private String name;
    private DBeaverConnectionConfigurationDTO configuration;
}
