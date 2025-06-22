package me.itzrenzo.infernaltresures.managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.itzrenzo.infernaltresures.InfernalTresures;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * MySQL database storage implementation for player stats with connection pooling
 */
public class MysqlStatsStorage implements StatsStorage {
    
    private final InfernalTresures plugin;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final Map<String, Object> properties;
    private HikariDataSource dataSource;
    
    public MysqlStatsStorage(InfernalTresures plugin, String host, int port, String database, 
                           String username, String password, Map<String, Object> properties) {
        this.plugin = plugin;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.properties = properties;
    }
    
    @Override
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            try {
                // Load MySQL driver (updated for mysql-connector-j)
                Class.forName("com.mysql.cj.jdbc.Driver");
                
                // Configure HikariCP connection pool
                HikariConfig config = new HikariConfig();
                config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
                config.setUsername(username);
                config.setPassword(password);
                
                // Apply configuration from config.yml
                config.setMaximumPoolSize((Integer) properties.getOrDefault("maximum-pool-size", 10));
                config.setMinimumIdle((Integer) properties.getOrDefault("minimum-idle", 5));
                config.setConnectionTimeout((Long) properties.getOrDefault("connection-timeout", 30000L));
                config.setIdleTimeout((Long) properties.getOrDefault("idle-timeout", 600000L));
                config.setMaxLifetime((Long) properties.getOrDefault("max-lifetime", 1800000L));
                
                // Additional MySQL properties
                Map<String, Object> mysqlProps = (Map<String, Object>) properties.get("properties");
                if (mysqlProps != null) {
                    for (Map.Entry<String, Object> entry : mysqlProps.entrySet()) {
                        config.addDataSourceProperty(entry.getKey(), entry.getValue());
                    }
                }
                
                // SSL configuration
                Map<String, Object> sslProps = (Map<String, Object>) properties.get("ssl");
                if (sslProps != null) {
                    config.addDataSourceProperty("useSSL", sslProps.get("enabled"));
                    config.addDataSourceProperty("trustCertificateKeyStoreUrl", sslProps.get("trust-certificate"));
                }
                
                dataSource = new HikariDataSource(config);
                
                // Test connection and create table
                try (Connection connection = dataSource.getConnection()) {
                    createTable(connection);
                }
                
                plugin.getLogger().info("Connected to MySQL database: " + database);
            } catch (ClassNotFoundException e) {
                plugin.getLogger().severe("MySQL driver not found: " + e.getMessage());
                throw new RuntimeException(e);
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to connect to MySQL database: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }
    
    private void createTable(Connection connection) throws SQLException {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS player_stats (
                uuid VARCHAR(36) PRIMARY KEY,
                total_blocks_mined BIGINT DEFAULT 0,
                common_treasures_found BIGINT DEFAULT 0,
                rare_treasures_found BIGINT DEFAULT 0,
                epic_treasures_found BIGINT DEFAULT 0,
                legendary_treasures_found BIGINT DEFAULT 0,
                mythic_treasures_found BIGINT DEFAULT 0,
                minutes_played BIGINT DEFAULT 0,
                luck_end_time BIGINT DEFAULT 0,
                luck_multiplier DOUBLE DEFAULT 1.0,
                queued_luck_end_time BIGINT DEFAULT 0,
                queued_luck_multiplier DOUBLE DEFAULT 1.0,
                treasure_spawning_enabled BOOLEAN DEFAULT TRUE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;
        
        try (Statement statement = connection.createStatement()) {
            statement.execute(createTableSQL);
        }
    }
    
    @Override
    public CompletableFuture<Void> close() {
        return CompletableFuture.runAsync(() -> {
            if (dataSource != null) {
                dataSource.close();
                plugin.getLogger().info("Closed MySQL database connection pool");
            }
        });
    }
    
    @Override
    public CompletableFuture<StatsManager.PlayerStats> loadPlayerStats(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM player_stats WHERE uuid = ?";
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
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
                plugin.getLogger().severe("Error loading player stats from MySQL: " + e.getMessage());
                return new StatsManager.PlayerStats();
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> savePlayerStats(UUID uuid, StatsManager.PlayerStats stats) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO player_stats (
                    uuid, total_blocks_mined, common_treasures_found, rare_treasures_found,
                    epic_treasures_found, legendary_treasures_found, mythic_treasures_found,
                    minutes_played, luck_end_time, luck_multiplier, queued_luck_end_time,
                    queued_luck_multiplier, treasure_spawning_enabled
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    total_blocks_mined = VALUES(total_blocks_mined),
                    common_treasures_found = VALUES(common_treasures_found),
                    rare_treasures_found = VALUES(rare_treasures_found),
                    epic_treasures_found = VALUES(epic_treasures_found),
                    legendary_treasures_found = VALUES(legendary_treasures_found),
                    mythic_treasures_found = VALUES(mythic_treasures_found),
                    minutes_played = VALUES(minutes_played),
                    luck_end_time = VALUES(luck_end_time),
                    luck_multiplier = VALUES(luck_multiplier),
                    queued_luck_end_time = VALUES(queued_luck_end_time),
                    queued_luck_multiplier = VALUES(queued_luck_multiplier),
                    treasure_spawning_enabled = VALUES(treasure_spawning_enabled)
                """;
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
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
                plugin.getLogger().severe("Error saving player stats to MySQL: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Map<UUID, StatsManager.PlayerStats>> loadAllPlayerStats() {
        return CompletableFuture.supplyAsync(() -> {
            Map<UUID, StatsManager.PlayerStats> allStats = new HashMap<>();
            String sql = "SELECT * FROM player_stats";
            
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement();
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
                        plugin.getLogger().warning("Invalid UUID in MySQL database: " + resultSet.getString("uuid"));
                    }
                }
                
                plugin.getLogger().info("Loaded statistics for " + allStats.size() + " players from MySQL");
                return allStats;
            } catch (SQLException e) {
                plugin.getLogger().severe("Error loading all player stats from MySQL: " + e.getMessage());
                return allStats;
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> saveAllPlayerStats(Map<UUID, StatsManager.PlayerStats> allStats) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO player_stats (
                    uuid, total_blocks_mined, common_treasures_found, rare_treasures_found,
                    epic_treasures_found, legendary_treasures_found, mythic_treasures_found,
                    minutes_played, luck_end_time, luck_multiplier, queued_luck_end_time,
                    queued_luck_multiplier, treasure_spawning_enabled
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    total_blocks_mined = VALUES(total_blocks_mined),
                    common_treasures_found = VALUES(common_treasures_found),
                    rare_treasures_found = VALUES(rare_treasures_found),
                    epic_treasures_found = VALUES(epic_treasures_found),
                    legendary_treasures_found = VALUES(legendary_treasures_found),
                    mythic_treasures_found = VALUES(mythic_treasures_found),
                    minutes_played = VALUES(minutes_played),
                    luck_end_time = VALUES(luck_end_time),
                    luck_multiplier = VALUES(luck_multiplier),
                    queued_luck_end_time = VALUES(queued_luck_end_time),
                    queued_luck_multiplier = VALUES(queued_luck_multiplier),
                    treasure_spawning_enabled = VALUES(treasure_spawning_enabled)
                """;

            try (Connection connection = dataSource.getConnection()) {
                connection.setAutoCommit(false); // Start transaction
                
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    for (Map.Entry<UUID, StatsManager.PlayerStats> entry : allStats.entrySet()) {
                        UUID uuid = entry.getKey();
                        StatsManager.PlayerStats stats = entry.getValue();
                        
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
                        
                        statement.addBatch();
                    }
                    
                    statement.executeBatch();
                    connection.commit();
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error saving all player stats to MySQL: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }
    
    @Override
    public boolean isAvailable() {
        return dataSource != null && !dataSource.isClosed();
    }
    
    @Override
    public String getStorageType() {
        return "MySQL";
    }
}