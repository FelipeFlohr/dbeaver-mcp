package dev.felipeflohr.dbeavermcp.module.dbeaver.model.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class DBeaverAuthConnectionDTO {
    private String user;

    @Nullable
    private String password;

    @Nullable
    @JsonProperty("oracle.logon-as")
    private String oracleLogonAs;
}
