package org.example;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class Main {
    public static void main(String[] args) throws Exception {
        HikariConfig config = new HikariConfig(); //this is just a settings object.
        config.setJdbcUrl("jdbc:postgresql://localhost:5432/urlshortener");
        config.setUsername("devuser");
        config.setPassword("devpass");

        HikariDataSource dataSource = new HikariDataSource(config); //immediately opens a batch of real connections to Postgres and holds them ready.

        try (Connection conn = dataSource.getConnection(); //doesn't open a new connection, it borrows one from the pool that's already open.
             Statement stmt = conn.createStatement(); //a Statement is how you send SQL text to the database
                                                      //use PreparedStatement instead, not Statement — Statement concatenates raw strings into SQL, which is exactly how SQL injection happens.
             ResultSet rs = stmt.executeQuery("SELECT 1")) { //sends the query, gets back a cursor over the result rows. ResultSet doesn't hand you all the data at once — you step through it row by row with .next().

            if (rs.next()) { //.next() advances the cursor to the first row and returns true if a row exists.
                System.out.println("Connected. Result: " + rs.getInt(1)); //rs.getInt(1) reads column 1 (1-indexed, not 0-indexed
            }
        }

        dataSource.close();
    }
}