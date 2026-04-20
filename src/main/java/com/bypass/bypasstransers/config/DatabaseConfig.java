package com.bypass.bypasstransers.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Bean
    @Primary
    public DataSource dataSource() {
        String dbUrl = System.getenv("DATABASE_URL");
        
        if (dbUrl == null || dbUrl.isEmpty()) {
            log.info("No DATABASE_URL found, falling back to localhost database configuration.");
            return DataSourceBuilder.create()
                .url(System.getenv("DB_URL") != null ? System.getenv("DB_URL") : "jdbc:postgresql://localhost:5432/bypass_records")
                .username(System.getenv("DB_USERNAME") != null ? System.getenv("DB_USERNAME") : "postgres")
                .password(System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : "")
                .driverClassName("org.postgresql.Driver")
                .build();
        }

        try {
            log.info("Found DATABASE_URL, attempting to parse...");
            
            // If user passed a full JDBC url directly, use it
            if (dbUrl.startsWith("jdbc:")) {
                return DataSourceBuilder.create()
                    .url(dbUrl)
                    .username(System.getenv("DB_USERNAME") != null ? System.getenv("DB_USERNAME") : "postgres")
                    .password(System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : "")
                    .driverClassName("org.postgresql.Driver")
                    .build();
            }

            // Parse Render's postgres:// or postgresql:// URL
            URI uri = new URI(dbUrl);
            String userInfo = uri.getUserInfo();
            String user = "postgres";
            String password = "";

            if (userInfo != null) {
                int colonIndex = userInfo.indexOf(":");
                if (colonIndex != -1) {
                    user = userInfo.substring(0, colonIndex);
                    password = userInfo.substring(colonIndex + 1);
                } else {
                    user = userInfo;
                }
            }

            int port = uri.getPort();
            if (port == -1) port = 5432;

            String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getPath() + "?sslmode=prefer";
            log.info("Successfully constructed JDBC URL for host: {}", uri.getHost());

            return DataSourceBuilder.create()
                .url(jdbcUrl)
                .username(user)
                .password(password)
                .driverClassName("org.postgresql.Driver")
                .build();

        } catch (Exception e) {
            log.error("Failed to parse DATABASE_URL: {}", e.getMessage(), e);
            throw new RuntimeException("Database configuration failed", e);
        }
    }
}
