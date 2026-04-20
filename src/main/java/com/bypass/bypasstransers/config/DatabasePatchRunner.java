package com.bypass.bypasstransers.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabasePatchRunner implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        // Automatically patch existing accounts to have a version of 0
        // This ensures the @Version Optimistic Locking mechanism doesn't throw StaleObjectStateException on legacy records.
        int updated = jdbcTemplate.update("UPDATE account SET version = 0 WHERE version IS NULL");
        if (updated > 0) {
            System.out.println("✅ Applied Database Patch: Initialized Optimistic Locking version to 0 for " + updated + " legacy account(s).");
        }
    }
}
