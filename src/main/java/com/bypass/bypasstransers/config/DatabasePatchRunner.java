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
        try {
            int updated = jdbcTemplate.update("UPDATE account SET version = 0 WHERE version IS NULL");
            if (updated > 0) {
                System.out.println("✅ Applied Database Patch: Initialized Optimistic Locking version to 0 for " + updated + " legacy account(s).");
            }
        } catch (Exception e) {
            // Ignore if account table doesn't exist
        }

        // Patch exchange_rate table to have an auto-increment sequence if missing (fixes PostgreSQL 500 ID null error)
        try {
            jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS exchange_rate_id_seq");
            jdbcTemplate.execute("ALTER TABLE exchange_rate ALTER COLUMN id SET DEFAULT nextval('exchange_rate_id_seq')");
            System.out.println("✅ Applied Database Patch: Fixed exchange_rate auto-increment.");
        } catch (Exception e) {
            // Ignore if it fails (e.g. sequence exists or table doesn't)
        }
    }
}
