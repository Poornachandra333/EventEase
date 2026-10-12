package com.eventease.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class TicketInventoryBackfill implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public TicketInventoryBackfill(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int migratedRows = jdbcTemplate.update("""
                INSERT IGNORE INTO ticket_inventory (ticket_type_id, reserved_quantity)
                SELECT id, total_quantity - available_quantity
                FROM ticket_types
                """);
        log.info("Initialized reservation inventory for {} existing ticket types", migratedRows);
    }
}
