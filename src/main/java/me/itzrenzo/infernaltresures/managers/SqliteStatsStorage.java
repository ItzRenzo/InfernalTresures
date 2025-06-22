package me.itzrenzo.infernaltresures.managers;

import me.itzrenzo.infernaltresures.InfernalTresures;

import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * SQLite database storage implementation for player stats
 */
public class SqliteStatsStorage implements StatsStorage {
    
    private final InfernalTresures plugin;
    private final String filename;
    private Connection connection;
    
    public SqliteStatsStorage(InfernalTresures plugin, String filename) {
        this.plugin = plugin;
        this.filename = filename;
    }
    
    @Override
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            try {
                File dbFile = new File(plugin.getDataFolder(), filename);
                String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
                
                // Load SQLite driver
                Class.forName("org.sqlite.JDBC");
                
                connection = DriverManager.getConnection(url);
                connection.setAutoCommit(true);
                
                // Create table if it doesn't exist
                createTable();
                
                plugin.getLogger().info("Connected to SQLite database: " + filename);
            } catch (ClassNotFoundException e) {
                plugin.getLogger().severe("SQLite driver not found: " + e.getMessage());
                throw new RuntimeException(e);
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to connect to SQLite database: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }
    
    private void createTable() throws SQLException {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS player_stats (
                uuid TEXT PRIMARY KEY,
                total_blocks_mined INTEGER DEFAULT 0,
                common_treasures_found INTEGER DEFAULT 0,
                rare_treasures_found INTEGER DEFAULT 0,
                epic_treasures_found INTEGER DEFAULT 0,
                legendary_treasures_found INTEGER DEFAULT 0,
                mythic_treasures_found INTEGER DEFAULT 0,
                minutes_played INTEGER DEFAULT 0,
                luck_end_time INTEGER DEFAULT 0,
                luck_multiplier REAL DEFAULT 1.0,
                queued_luck_end_time INTEGER DEFAULT 0,
                queued_luck_multiplier REAL DEFAULT 1.0,
                treasure_spawning_enabled INTEGER DEFAULT 1
            )
            """;
        
        try (Statement statement = connection.createStatement()) {
            statement.execute(createTableSQL);
        }
    }
    
    @Override
    public CompletableFuture<Void> close() {
        return CompletableFuture.runAsync(() -> {
            if (connection != null) {
                try {
                    connection.close();
                    plugin.getLogger().info("Closed SQLite database connection");
                } catch (SQLException e) {
                    plugin.getLogger().warning("Error closing SQLite connection: " + e.getMessage());
                }
            }
        });
    }
    
    @Override
    public CompletableFuture<StatsManager.PlayerStats> loadPlayerStats(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM player_stats WHERE uuid = ?";
            
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, uuid.toString());
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        StatsManager.PlayerStats stats = new StatsManager.PlayerStats();
                        
                        stats.totalBlocksMined = resultSet.getLong("total_blocks_mined");
                        stats.commonTreasuresFound = resultSet.getLong("common_treasures_found");
                        stats.rareTreasuresFound = resultSet.getLong("rare_treasures_found");
                        stats.epicTreasuresFound = resultSet.getLong("epic_treasures_found");
                        stats.legendaryTreasuresFound = resultSet.getLong("legendary_treasures_found");
                        stats.mythicTreasuresFound = resultSet.getLong("mythic_treasures_found");
                        stats.minutesPlayed = resultSet.getLong("minutes_played");
                        stats.luckEndTime = resultSet.getLong("luck_end_time");
                        stats.luckMultiplier = resultSet.getDouble("luck_multiplier");
                        stats.queuedLuckEndTime = resultSet.getLong("queued_luck_end_time");
                        stats.queuedLuckMultiplier = resultSet.getDouble("queued_luck_multiplier");
                        stats.treasureSpawningEnabled = resultSet.getBoolean("treasure_spawning_enabled");
                        
                        return stats;
                    } else {
                        return new StatsManager.PlayerStats();
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error loading player stats from SQLite: " + e.getMessage());
                return new StatsManager.PlayerStats();
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> savePlayerStats(UUID uuid, StatsManager.PlayerStats stats) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT OR REPLACE INTO player_stats (
                    uuid, total_blocks_mined, common_treasures_found, rare_treasures_found,
                    epic_treasures_found, legendary_treasures_found, mythic_treasures_found,
                    minutes_played, luck_end_time, luck_multiplier, queued_luck_end_time,
                    queued_luck_multiplier, treasure_spawning_enabled
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
            
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, uuid.toString());
                statement.setLong(2, stats.totalBlocksMined);
                statement.setLong(3, stats.commonTreasuresFound);
                statement.setLong(4, stats.rareTreasuresFound);
                statement.setLong(5, stats.epicTreasuresFound);
                statement.setLong(6, stats.legendaryTreasuresFound);
                statement.setLong(7, stats.mythicTreasuresFound);
                statement.setLong(8, stats.minutesPlayed);
                statement.setLong(9, stats.luckEndTime);
                statement.setDouble(10, stats.luckMultiplier);
                statement.setLong(11, stats.queuedLuckEndTime);
                statement.setDouble(12, stats.queuedLuckMultiplier);
                statement.setBoolean(13, stats.treasureSpawningEnabled);
                
                statement.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error saving player stats to SQLite: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Map<UUID, StatsManager.PlayerStats>> loadAllPlayerStats() {
        return CompletableFuture.supplyAsync(() -> {
            Map<UUID, StatsManager.PlayerStats> allStats = new HashMap<>();
            String sql = "SELECT * FROM player_stats";
            
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(sql)) {
                
                while (resultSet.next()) {
                    try {
                        UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                        StatsManager.PlayerStats stats = new StatsManager.PlayerStats();
                        
                        stats.totalBlocksMined = resultSet.getLong("total_blocks_mined");
                        stats.commonTreasuresFound = resultSet.getLong("common_treasures_found");
                        stats.rareTreasuresFound = resultSet.getLong("rare_treasures_found");
                        stats.epicTreasuresFound = resultSet.getLong("epic_treasures_found");
                        stats.legendaryTreasuresFound = resultSet.getLong("legendary_treasures_found");
                        stats.mythicTreasuresFound = resultSet.getLong("mythic_treasures_found");
                        stats.minutesPlayed = resultSet.getLong("minutes_played");
                        stats.luckEndTime = resultSet.getLong("luck_end_time");
                        stats.luckMultiplier = resultSet.getDouble("luck_multiplier");
                        stats.queuedLuckEndTime = resultSet.getLong("queued_luck_end_time");
                        stats.queuedLuckMultiplier = resultSet.getDouble("queued_luck_multiplier");
                        stats.treasureSpawningEnabled = resultSet.getBoolean("treasure_spawning_enabled");
                        
                        allStats.put(uuid, stats);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in SQLite database: " + resultSet.getString("uuid"));
                    }
                }
                
                plugin.getLogger().info("Loaded statistics for " + allStats.size() + " players from SQLite");
                return allStats;
            } catch (SQLException e) {
                plugin.getLogger().severe("Error loading all player stats from SQLite: " + e.getMessage());
                return allStats;
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> saveAllPlayerStats(Map<UUID, StatsManager.PlayerStats> allStats) {
        return CompletableFuture.runAsync(() -> {
            try {
                connection.setAutoCommit(false); // Start transaction
                
                for (Map.Entry<UUID, StatsManager.PlayerStats> entry : allStats.entrySet()) {
                    savePlayerStats(entry.getKey(), entry.getValue()).join();
                }
                
                connection.commit();
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                try {
                    connection.rollback();
                    connection.setAutoCommit(true);
                } catch (SQLException rollbackEx) {
                    plugin.getLogger().severe("Error rolling back SQLite transaction: " + rollbackEx.getMessage());
                }
                plugin.getLogger().severe("Error saving all player stats to SQLite: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }
    
    @Override
    public boolean isAvailable() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
    
    @Override
    public String getStorageType() {
        return "SQLite";
    }
}