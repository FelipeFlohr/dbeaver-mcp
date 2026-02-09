package dev.felipeflohr.dbeavermcp.module.query.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableConnectionDTO {
    private String identifier;
    private String name;
    private String provider;
    private String url;

    @Nullable
    private String sshHost;

    @Nullable
    private Integer sshPort;
}
