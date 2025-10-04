package com.spendwise.spendwise_backend.web;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class DbPingController {
    private final JdbcTemplate jdbc;

    public DbPingController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/api/db-ping")
    public Map<String, String> dbPing() {
        String version = jdbc.queryForObject("select version()", String.class);
        return Map.of("db", "ok", "version", version);
    }
}
