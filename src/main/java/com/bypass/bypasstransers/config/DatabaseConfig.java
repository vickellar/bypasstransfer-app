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
        log.info("========================================");
        log.info("Database Configuration Starting...");
        log.info("========================================");
        
        // Priority 1: DATABASE_URL (Render's default)
        String dbUrl = System.getenv("DATABASE_URL");
        
        // Priority 2: DB_URL (for local development with postgresql:// URLs)
        if (dbUrl == null || dbUrl.isEmpty()) {
            dbUrl = System.getenv("DB_URL");
            if (dbUrl != null && !dbUrl.isEmpty()) {
                log.info("Using DB_URL environment variable");
            }
        }
        
        log.info("Database URL environment variable: {}", dbUrl != null ? "FOUND" : "NOT FOUND");
        
        if (dbUrl != null && !dbUrl.isEmpty()) {
            log.info("Database URL starts with: {}", dbUrl.substring(0, Math.min(30, dbUrl.length())) + "...");
        }
        
        if (dbUrl == null || dbUrl.isEmpty()) {
            log.warn("No DATABASE_URL or DB_URL found, falling back to localhost.");
            
            // Fallback to localhost for local development
            String fallbackUrl = "jdbc:postgresql://localhost:5432/bypass_records";
            String fallbackUser = System.getenv("DB_USERNAME") != null ? System.getenv("DB_USERNAME") : "postgres";
            String fallbackPass = System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : "";
            
            log.info("Using localhost: jdbc:postgresql://localhost:5432/bypass_records");
            
            return DataSourceBuilder.create()
                .url(fallbackUrl)
                .username(fallbackUser)
                .password(fallbackPass)
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

            // Normalize postgresql:// to postgres:// for Java URI parsing
            String normalizedUrl = dbUrl;
            if (dbUrl.startsWith("postgresql://")) {
                normalizedUrl = dbUrl.replaceFirst("postgresql://", "postgres://");
                log.info("Normalized postgresql:// to postgres://");
            }

            // Parse Render's postgres:// or postgresql:// URL
            URI uri = new URI(normalizedUrl);
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

            String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getPath() + "?sslmode=require&ssl=true";
            log.info("Successfully constructed JDBC URL for host: {}", uri.getHost());
            log.info("JDBC URL: {}", jdbcUrl.replace(password, "***"));

            DataSource dataSource = DataSourceBuilder.create()
                .url(jdbcUrl)
                .username(user)
                .password(password)
                .driverClassName("org.postgresql.Driver")
                .build();
            
            log.info("DataSource created successfully. Testing connection...");
            return dataSource;

        } catch (Exception e) {
            log.error("Failed to parse DATABASE_URL: {}", e.getMessage(), e);
            throw new RuntimeException("Database configuration failed", e);
        }
    }
}
