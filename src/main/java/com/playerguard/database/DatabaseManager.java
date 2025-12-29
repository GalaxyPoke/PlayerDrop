package com.playerguard.database;

import com.playerguard.PlayerDropGuard;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {

    private final PlayerDropGuard plugin;
    private Connection connection;
    private final String tablePrefix;

    public DatabaseManager(PlayerDropGuard plugin) {
        this.plugin = plugin;
        this.tablePrefix = plugin.getConfigManager().getDbTablePrefix();
    }

    public boolean connect() {
        if (!plugin.getConfigManager().isDatabaseEnabled()) {
            return false;
        }

        try {
            String host = plugin.getConfigManager().getDbHost();
            int port = plugin.getConfigManager().getDbPort();
            String database = plugin.getConfigManager().getDbDatabase();
            String username = plugin.getConfigManager().getDbUsername();
            String password = plugin.getConfigManager().getDbPassword();

            String url = "jdbc:mysql://" + host + ":" + port + "/" + database + 
                    "?useSSL=false&autoReconnect=true&useUnicode=true&characterEncoding=UTF-8";
            
            connection = DriverManager.getConnection(url, username, password);
            plugin.getLogger().info("已连接到MySQL数据库");
            
            createTables();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("无法连接到数据库: " + e.getMessage());
            return false;
        }
    }

    private void createTables() {
        String sql = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "settings (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "enabled BOOLEAN DEFAULT TRUE," +
                "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ")";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            plugin.getLogger().severe("创建数据库表失败: " + e.getMessage());
        }
    }

    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("已断开数据库连接");
            } catch (SQLException e) {
                plugin.getLogger().warning("断开数据库连接失败: " + e.getMessage());
            }
        }
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public CompletableFuture<Boolean> getProtectionEnabled(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isConnected()) {
                return plugin.getConfigManager().isDefaultEnabled();
            }

            String sql = "SELECT enabled FROM " + tablePrefix + "settings WHERE uuid = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getBoolean("enabled");
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("查询玩家设置失败: " + e.getMessage());
            }
            return plugin.getConfigManager().isDefaultEnabled();
        });
    }

    public CompletableFuture<Void> setProtectionEnabled(UUID playerUuid, boolean enabled) {
        return CompletableFuture.runAsync(() -> {
            if (!isConnected()) return;

            String sql = "INSERT INTO " + tablePrefix + "settings (uuid, enabled) VALUES (?, ?) " +
                    "ON DUPLICATE KEY UPDATE enabled = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());
                stmt.setBoolean(2, enabled);
                stmt.setBoolean(3, enabled);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("保存玩家设置失败: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<int[]> getStats() {
        return CompletableFuture.supplyAsync(() -> {
            if (!isConnected()) {
                return new int[]{0, 0};
            }

            int total = 0;
            int enabled = 0;

            String sql = "SELECT COUNT(*) as total, SUM(CASE WHEN enabled = true THEN 1 ELSE 0 END) as enabled FROM " + tablePrefix + "settings";
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    total = rs.getInt("total");
                    enabled = rs.getInt("enabled");
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("获取统计信息失败: " + e.getMessage());
            }

            return new int[]{total, enabled};
        });
    }
}
