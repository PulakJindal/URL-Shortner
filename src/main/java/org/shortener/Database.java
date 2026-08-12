package org.shortener;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import static org.shortener.EnvUtil.getEnvOrDefault;

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
    public static DataSource getDataSource(){
        return dataSource;
    }
}
