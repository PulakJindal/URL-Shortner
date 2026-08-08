package org.example;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public class Database {
    private static final HikariDataSource dataSource;

    static{
        HikariConfig config = new HikariConfig();  //describing how you want to connect, not connecting.
        config.setJdbcUrl(getEnvOrDefault("DB_URL", "jdbc:postgresql://localhost:5433/urlshortener"));
        config.setUsername(getEnvOrDefault("DB_USER", "DefaultUser"));
        config.setPassword(getEnvOrDefault("DB_PASSWORD", "DefaultPass"));

        dataSource = new HikariDataSource(config); //opens a batch of real connections to Postgres and holds them ready

        Runtime.getRuntime().addShutdownHook(new Thread(dataSource::close));
    }
    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return value != null ? value : defaultValue;
    }
    public static DataSource getDataSource(){
        return dataSource;
    }
}
