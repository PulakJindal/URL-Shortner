package org.example;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Main {
    public static void main(String[] args) throws Exception {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost:5432/urlshortener");
        config.setUsername("devuser");
        config.setPassword("devpass");

        HikariDataSource dataSource = new HikariDataSource(config);

        // Insert with ON CONFLICT DO NOTHING
        String insertSql = "INSERT INTO urls (short_code, long_url) VALUES (?, ?) ON CONFLICT (short_code) DO NOTHING";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertSql)) {

            stmt.setString(1, "xyz789");
            stmt.setString(2, "https://openai.com");

            int rowsAffected = stmt.executeUpdate();
            System.out.println("Rows inserted: " + rowsAffected);
        }

        // Select it back
        String selectSql = "SELECT long_url FROM urls WHERE short_code = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(selectSql)) {

            stmt.setString(1, "xyz789");

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Found URL: " + rs.getString("long_url"));
                } else {
                    System.out.println("No URL found for that code.");
                }
            }
        }

        Runtime.getRuntime().addShutdownHook(new Thread(dataSource::close));
    }
}