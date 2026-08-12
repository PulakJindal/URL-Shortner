package org.shortener;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class UrlRepository {
    private final DataSource dataSource;
    Cache<String, UrlRecord> cache = Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    public UrlRepository(DataSource dataSource){
        this.dataSource = dataSource;
    }

    public Optional<String> insertIfNotExists(String shortCode, String longUrl, OffsetDateTime expireTime){
        String sql = "INSERT INTO urls (short_code, long_url, expires_at) VALUES (?, ?, ?) ON CONFLICT (short_code) DO NOTHING RETURNING short_code";
        try(Connection conn = dataSource.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, shortCode);
            stmt.setString(2, longUrl);
            if (expireTime != null) {
                stmt.setObject(3, expireTime);
            } else {
                stmt.setNull(3, java.sql.Types.TIMESTAMP_WITH_TIMEZONE);
            }

            try(ResultSet rs = stmt.executeQuery()){
                if(rs.next()) return Optional.of(rs.getString("short_code"));
                return Optional.empty();
            }
        }catch(SQLException e){
            if("23505".equals((e.getSQLState()))) return findByLongUrl(longUrl);
            throw new RuntimeException(e);
        }
    }

    public Optional<UrlRecord> findByShortCode(String shortCode){
        UrlRecord cacheData = cache.getIfPresent(shortCode);
        if(cacheData != null) return Optional.of(cacheData);
        String sql = "SELECT long_url, expires_at FROM urls WHERE short_code = ?";
        try(Connection conn = dataSource.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, shortCode);
            try(ResultSet rs = stmt.executeQuery()){
                if(rs.next()){
                    UrlRecord record = new UrlRecord();
                    record.longUrl = rs.getString("long_url");
                    record.expiresAt = rs.getObject("expires_at", OffsetDateTime.class);
                    cache.put(shortCode, record);
                    return Optional.of(record);
                }
                return Optional.empty();
            }
        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    public Optional<String> findByLongUrl(String longUrl) {
        String sql = "SELECT short_code FROM urls WHERE long_url = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, longUrl);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(rs.getString("short_code"));
                else return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public void incrementClickCount(String shortCode){
        String sql = "UPDATE urls SET click_count = click_count + 1 WHERE short_code = ?";
        try(Connection conn = dataSource.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, shortCode);
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
