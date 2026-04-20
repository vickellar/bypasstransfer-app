package com.bypass.bypasstransers.config;

// This file is intentionally left as a no-op placeholder.
// DataSource configuration is fully handled by Spring Boot auto-configuration
// using spring.datasource.* properties in application.properties, which are
// populated from the .env file (for local dev) or environment variables (for prod/Render).
//
// Previously this class manually created a dataSource bean using System.getenv(),
// but that conflicted with Spring Boot's auto-configuration and did not read
// System properties set by the .env loader. The auto-configuration approach
// is simpler, more reliable, and supports profile-based overrides via property files.
