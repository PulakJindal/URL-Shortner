package org.example;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class UrlRepository {
    private final DataSource dataSource;

    public UrlRepository(DataSource dataSource){
        this.dataSource = dataSource;
    }

    public boolean insert(String shortCode, String longUrl){
        String sql = "INSERT INTO urls (short_code, long_url) VALUES (?, ?) ON CONFLICT (short_code) DO NOTHING";
        try(Connection conn = dataSource.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, shortCode);
            stmt.setString(2, longUrl);

            int rowAffected = stmt.executeUpdate();
            return rowAffected > 0;
        }catch(SQLException e){
            throw new RuntimeException((e));
        }
    }

    public Optional<String> findByShortCode(String shortCode){
        String sql = "SELECT long_url FROM urls WHERE short_code = ?";
        try(Connection conn = dataSource.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, shortCode);
            try(ResultSet rs = stmt.executeQuery()){
                if(rs.next()){
                    return Optional.of(rs.getString("long_url"));
                }
                return Optional.empty();
            }
        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }
}
