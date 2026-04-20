package com.bypass.bypasstransers.config;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class RenderDatabaseConfig {

    @Bean
    public DataSource dataSource() {
        String dbUrl = System.getenv("JDBC_DATABASE_URL");
        if (dbUrl == null) {
            throw new IllegalStateException("JDBC_DATABASE_URL environment variable is not set");
        }

        // Render's URL format: postgresql://user:password@host:port/database
        // DataSourceBuilder.create() handles this format automatically
        return DataSourceBuilder.create()
                .url(dbUrl)
                .driverClassName("org.postgresql.Driver")
                .build();
    }
}
