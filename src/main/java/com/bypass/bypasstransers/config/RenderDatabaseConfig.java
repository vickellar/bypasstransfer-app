package com.bypass.bypasstransers.config;

// This file is intentionally left as a no-op placeholder.
// On Render, the DataSource is configured via spring.datasource.* properties
// set from environment variables (DB_URL, DB_USERNAME, DB_PASSWORD or JDBC_DATABASE_URL).
// Spring Boot's auto-configuration handles this correctly without a manual bean.
//
// The JDBC_DATABASE_URL format conversion (postgres:// -> jdbc:postgresql://) is
// handled in BypasstransersApplication.loadDotEnv() before Spring starts.
