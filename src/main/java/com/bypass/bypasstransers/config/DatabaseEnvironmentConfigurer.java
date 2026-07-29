package com.bypass.bypasstransers.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Converts Render-style DATABASE_URL values into Spring Boot datasource properties
 * before the application context starts.
 */
public final class DatabaseEnvironmentConfigurer {

    private static final Logger log = LoggerFactory.getLogger(DatabaseEnvironmentConfigurer.class);

    private DatabaseEnvironmentConfigurer() {
    }

    public static void configure() {
        if (isSet("spring.datasource.url")) {
            log.info("spring.datasource.url already configured, skipping DATABASE_URL conversion");
            return;
        }

        String rawUrl = firstNonBlank(
                System.getenv("DATABASE_URL"),
                System.getenv("DB_URL"),
                System.getProperty("DATABASE_URL"),
                System.getProperty("DB_URL")
        );

        if (rawUrl == null || rawUrl.isBlank()) {
            if ("true".equalsIgnoreCase(System.getenv("RENDER"))) {
                log.error("DATABASE_URL is not set on Render. Link your PostgreSQL database to this web service.");
            } else {
                log.info("No DATABASE_URL or DB_URL found; using application.properties defaults");
            }
            return;
        }

        log.info("Configuring datasource from environment URL");

        if (rawUrl.startsWith("jdbc:")) {
            setIfAbsent("spring.datasource.url", rawUrl);
        } else {
            ParsedDatabaseUrl parsed = parseDatabaseUrl(rawUrl);
            setIfAbsent("spring.datasource.url", parsed.jdbcUrl());
            setIfAbsent("spring.datasource.username", parsed.username());
            setIfAbsent("spring.datasource.password", parsed.password());
        }

        setIfAbsent("spring.datasource.driver-class-name", "org.postgresql.Driver");
        log.info("Datasource configured for host in JDBC URL");
    }

    private static ParsedDatabaseUrl parseDatabaseUrl(String rawUrl) {
        try {
            String normalizedUrl = rawUrl;
            if (rawUrl.startsWith("postgresql://")) {
                normalizedUrl = rawUrl.replaceFirst("postgresql://", "postgres://");
            }

            URI uri = new URI(normalizedUrl);
            String user = "postgres";
            String password = "";

            String userInfo = uri.getUserInfo();
            if (userInfo != null) {
                int colonIndex = userInfo.indexOf(':');
                if (colonIndex != -1) {
                    user = decode(userInfo.substring(0, colonIndex));
                    password = decode(userInfo.substring(colonIndex + 1));
                } else {
                    user = decode(userInfo);
                }
            }

            int port = uri.getPort();
            if (port == -1) {
                port = 5432;
            }

            String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getPath()
                    + "?sslmode=require";

            return new ParsedDatabaseUrl(jdbcUrl, user, password);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse database URL from environment", e);
        }
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static boolean isSet(String key) {
        String value = System.getProperty(key);
        return value != null && !value.isBlank();
    }

    private static void setIfAbsent(String key, String value) {
        if (!isSet(key)) {
            System.setProperty(key, value);
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private record ParsedDatabaseUrl(String jdbcUrl, String username, String password) {
    }
}
