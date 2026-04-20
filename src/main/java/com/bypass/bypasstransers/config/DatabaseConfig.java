package com.bypass.bypasstransers.config;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

@Configuration
@Profile("default") // Only active for default profile (local development)
public class DatabaseConfig {

    @Bean
    public DataSource dataSource() {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USERNAME");
        String dbPassword = System.getenv("DB_PASSWORD");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new IllegalStateException("Database environment variables are not set for default profile");
        }

        return DataSourceBuilder.create()
                .url(dbUrl)
                .username(dbUser)
                .password(dbPassword)
                .driverClassName("org.postgresql.Driver")
                .build();
    }
}
