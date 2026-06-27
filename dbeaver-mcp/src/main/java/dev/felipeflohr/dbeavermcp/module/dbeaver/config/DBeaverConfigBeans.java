package dev.felipeflohr.dbeavermcp.module.dbeaver.config;

import dev.felipeflohr.dbeaverconfig.DBeaverCipher;
import dev.felipeflohr.dbeaverconfig.DBeaverCipherImpl;
import dev.felipeflohr.dbeaverconfig.DBeaverDataSource;
import dev.felipeflohr.dbeaverconfig.DBeaverDataSourceImpl;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@NullMarked
@Configuration(proxyBeanMethods = false)
class DBeaverConfigBeans {

    @Bean
    DBeaverDataSource dBeaverDataSource(ObjectMapper objectMapper) {
        return new DBeaverDataSourceImpl(objectMapper);
    }

    @Bean
    DBeaverCipher dBeaverCipher(ObjectMapper objectMapper) {
        return new DBeaverCipherImpl(objectMapper);
    }
}
