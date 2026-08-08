package org.example;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

public class Main {
     static void main() throws Exception {
        UrlRepository repo = new UrlRepository(Database.getDataSource());

        boolean inserted = repo.insert("test111", "https://anthropic.com");
         System.out.println("Inserted: " + inserted);

         Optional<String> found = repo.findByShortCode("test111");
         System.out.println("Found: "+ found.orElse("nothing"));
    }
}