package me.itzrenzo.infernaltresures.managers;

import me.itzrenzo.infernaltresures.InfernalTresures;
import me.itzrenzo.infernaltresures.models.Rarity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class StatsManager {
    
    private final InfernalTresures plugin;
    private final Map<UUID, PlayerStats> playerStats = new HashMap<>();
    private final Map<UUID, Long> playerJoinTimes = new HashMap<>();
    private StatsStorage storage;
    
    public StatsManager(InfernalTresures plugin) {
        this.plugin = plugin;
        initializeStorage();
        loadStats();
    }
    
    private void initializeStorage() {
        String storageType = plugin.getConfig().getString("database.type", "YML").toUpperCase();
        
        try {
            switch (storageType) {
                case "YML" -> {
                    storage = new YmlStatsStorage(plugin);
                    plugin.getLogger().info("Using YML file storage for player statistics");
                }
                case "SQLITE" -> {
                    String filename = plugin.getConfig().getString("database.sqlite.filename", "stats.db");
                    storage = new SqliteStatsStorage(plugin, filename);
                    plugin.getLogger().info("Using SQLite database storage for player statistics");
                }
                case "MYSQL" -> {
                    String host = plugin.getConfig().getString("database.mysql.host", "localhost");
                    int port = plugin.getConfig().getInt("database.mysql.port", 3306);
                    String database = plugin.getConfig().getString("database.mysql.database", "infernal_treasures");
                    String username = plugin.getConfig().getString("database.mysql.username", "root");
                    String password = plugin.getConfig().getString("database.mysql.password", "password");
                    
                    // Build properties map for MySQL configuration
                    Map<String, Object> properties = new HashMap<>();
                    
                    // Pool settings
                    Map<String, Object> poolConfig = new HashMap<>();
                    poolConfig.put("maximum-pool-size", plugin.getConfig().getInt("database.mysql.pool.maximum-pool-size", 10));
                    poolConfig.put("minimum-idle", plugin.getConfig().getInt("database.mysql.pool.minimum-idle", 5));
                    poolConfig.put("connection-timeout", plugin.getConfig().getLong("database.mysql.pool.connection-timeout", 30000));
                    poolConfig.put("idle-timeout", plugin.getConfig().getLong("database.mysql.pool.idle-timeout", 600000));
                    poolConfig.put("max-lifetime", plugin.getConfig().getLong("database.mysql.pool.max-lifetime", 1800000));
                    properties.putAll(poolConfig);
                    
                    // SSL settings
                    Map<String, Object> sslConfig = new HashMap<>();
                    sslConfig.put("enabled", plugin.getConfig().getBoolean("database.mysql.ssl.enabled", false));
                    sslConfig.put("trust-certificate", plugin.getConfig().getBoolean("database.mysql.ssl.trust-certificate", false));
                    properties.put("ssl", sslConfig);
                    
                    // Additional MySQL properties
                    Map<String, Object> mysqlProps = new HashMap<>();
                    mysqlProps.put("useSSL", plugin.getConfig().getBoolean("database.mysql.properties.useSSL", false));
                    mysqlProps.put("allowPublicKeyRetrieval", plugin.getConfig().getBoolean("database.mysql.properties.allowPublicKeyRetrieval", true));
                    mysqlProps.put("serverTimezone", plugin.getConfig().getString("database.mysql.properties.serverTimezone", "UTC"));
                    properties.put("properties", mysqlProps);
                    
                    storage = new MysqlStatsStorage(plugin, host, port, database, username, password, properties);
                    plugin.getLogger().info("Using MySQL database storage for player statistics");
                }
                default -> {
                    plugin.getLogger().warning("Invalid database type '" + storageType + "'. Falling back to YML storage.");
                    storage = new YmlStatsStorage(plugin);
                }
            }
            
            // Initialize the storage system
            storage.initialize().join();
            
            if (!storage.isAvailable()) {
                plugin.getLogger().severe("Failed to initialize " + storage.getStorageType() + " storage. Plugin may not function correctly.");
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error initializing storage system: " + e.getMessage());
            plugin.getLogger().warning("Falling back to YML storage due to initialization error.");
            storage = new YmlStatsStorage(plugin);
            storage.initialize().join();
        }
    }
    
    private void loadStats() {
        if (storage == null || !storage.isAvailable()) {
            plugin.getLogger().warning("Storage not available, cannot load stats");
            return;
        }
        
        storage.loadAllPlayerStats().thenAccept(loadedStats -> {
            playerStats.clear();
            playerStats.putAll(loadedStats);
            plugin.getLogger().info("Loaded statistics for " + playerStats.size() + " players from " + storage.getStorageType());
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("Error loading player stats: " + throwable.getMessage());
            return null;
        });
    }

    public void saveStats() {
        if (storage == null || !storage.isAvailable()) {
            plugin.getLogger().warning("Storage not available, cannot save stats");
            return;
        }
        
        storage.saveAllPlayerStats(new HashMap<>(playerStats)).exceptionally(throwable -> {
            plugin.getLogger().severe("Error saving player stats: " + throwable.getMessage());
            return null;
        });
    }
    
    public void savePlayerStatsAsync(UUID uuid, PlayerStats stats) {
        if (storage == null || !storage.isAvailable()) {
            return;
        }
        
        storage.savePlayerStats(uuid, stats).exceptionally(throwable -> {
            plugin.getLogger().severe("Error saving stats for player " + uuid + ": " + throwable.getMessage());
            return null;
        });
    }
    
    /**
     * Shutdown the storage system and save all data
     */
    public void shutdown() {
        saveStats();
        
        if (storage != null) {
            storage.close().thenRun(() -> {
                plugin.getLogger().info("Storage system shutdown complete");
            }).exceptionally(throwable -> {
                plugin.getLogger().warning("Error during storage shutdown: " + throwable.getMessage());
                return null;
            });
        }
    }
    
    public void onPlayerJoin(Player player) {
        UUID uuid = player.getUniqueId();
        playerJoinTimes.put(uuid, System.currentTimeMillis());
        
        // Initialize stats if player doesn't exist
        if (!playerStats.containsKey(uuid)) {
            playerStats.put(uuid, new PlayerStats());
        }
    }
    
    public void onPlayerLeave(Player player) {
        UUID uuid = player.getUniqueId();
        Long joinTime = playerJoinTimes.remove(uuid);
        
        if (joinTime != null) {
            // Calculate minutes played this session
            long sessionTime = System.currentTimeMillis() - joinTime;
            long minutesPlayed = sessionTime / (1000 * 60); // Convert to minutes
            
            // Add to total minutes played
            PlayerStats stats = getPlayerStats(uuid);
            stats.minutesPlayed += minutesPlayed;
        }
        
        // Save stats when player leaves
        saveStats();
    }
    
    public void onBlockMined(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerStats stats = getPlayerStats(uuid);
        stats.totalBlocksMined++;
    }
    
    public void onTreasureFound(Player player, Rarity rarity) {
        UUID uuid = player.getUniqueId();
        PlayerStats stats = getPlayerStats(uuid);
        
        switch (rarity) {
            case COMMON -> stats.commonTreasuresFound++;
            case RARE -> stats.rareTreasuresFound++;
            case EPIC -> stats.epicTreasuresFound++;
            case LEGENDARY -> stats.legendaryTreasuresFound++;
            case MYTHIC -> stats.mythicTreasuresFound++;
        }
    }
    
    public PlayerStats getPlayerStats(UUID uuid) {
        return playerStats.computeIfAbsent(uuid, k -> new PlayerStats());
    }
    
    public PlayerStats getPlayerStats(Player player) {
        return getPlayerStats(player.getUniqueId());
    }
    
    public long getTotalTreasuresFound(UUID uuid) {
        PlayerStats stats = getPlayerStats(uuid);
        return stats.commonTreasuresFound + stats.rareTreasuresFound + 
               stats.epicTreasuresFound + stats.legendaryTreasuresFound + 
               stats.mythicTreasuresFound;
    }
    
    public long getTotalTreasuresFound(Player player) {
        return getTotalTreasuresFound(player.getUniqueId());
    }
    
    // Method to get current session playtime for online players
    public long getCurrentSessionMinutes(Player player) {
        UUID uuid = player.getUniqueId();
        Long joinTime = playerJoinTimes.get(uuid);
        
        if (joinTime != null) {
            long sessionTime = System.currentTimeMillis() - joinTime;
            return sessionTime / (1000 * 60); // Convert to minutes
        }
        
        return 0;
    }
    
    // Method to get total playtime including current session
    public long getTotalPlaytimeMinutes(Player player) {
        PlayerStats stats = getPlayerStats(player);
        return stats.minutesPlayed + getCurrentSessionMinutes(player);
    }
    
    public void reload() {
        // Save current stats before reloading
        saveStats();
        
        // Clear current data
        playerStats.clear();
        
        // Reload from file
        loadStats();
        
        plugin.getLogger().info("Statistics reloaded");
    }
    
    /**
     * Set the total blocks mined for a player
     */
    public void setBlocksMined(UUID uuid, long blocks) {
        PlayerStats stats = getPlayerStats(uuid);
        stats.totalBlocksMined = blocks;
    }
    
    /**
     * Set the total treasures found for a player by recalculating from individual rarity counts
     */
    public void setTotalTreasuresFound(UUID uuid, long total) {
        PlayerStats stats = getPlayerStats(uuid);
        
        // Calculate current total
        long currentTotal = stats.commonTreasuresFound + stats.rareTreasuresFound + 
                           stats.epicTreasuresFound + stats.legendaryTreasuresFound + 
                           stats.mythicTreasuresFound;
        
        if (total == 0) {
            // Reset all rarity counts to 0
            stats.commonTreasuresFound = 0;
            stats.rareTreasuresFound = 0;
            stats.epicTreasuresFound = 0;
            stats.legendaryTreasuresFound = 0;
            stats.mythicTreasuresFound = 0;
        } else if (total != currentTotal) {
            // Distribute the difference proportionally across rarities
            // If current total is 0, set all to common treasures
            if (currentTotal == 0) {
                stats.commonTreasuresFound = total;
            } else {
                // Calculate proportional distribution
                double ratio = (double) total / currentTotal;
                stats.commonTreasuresFound = Math.round(stats.commonTreasuresFound * ratio);
                stats.rareTreasuresFound = Math.round(stats.rareTreasuresFound * ratio);
                stats.epicTreasuresFound = Math.round(stats.epicTreasuresFound * ratio);
                stats.legendaryTreasuresFound = Math.round(stats.legendaryTreasuresFound * ratio);
                stats.mythicTreasuresFound = Math.round(stats.mythicTreasuresFound * ratio);
                
                // Adjust for rounding errors by adding difference to common treasures
                long newTotal = stats.commonTreasuresFound + stats.rareTreasuresFound + 
                              stats.epicTreasuresFound + stats.legendaryTreasuresFound + 
                              stats.mythicTreasuresFound;
                long difference = total - newTotal;
                stats.commonTreasuresFound += difference;
            }
        }
    }
    
    /**
     * Set treasures found for a specific rarity
     */
    public void setTreasuresByRarity(UUID uuid, Rarity rarity, long count) {
        PlayerStats stats = getPlayerStats(uuid);
        
        switch (rarity) {
            case COMMON -> stats.commonTreasuresFound = count;
            case RARE -> stats.rareTreasuresFound = count;
            case EPIC -> stats.epicTreasuresFound = count;
            case LEGENDARY -> stats.legendaryTreasuresFound = count;
            case MYTHIC -> stats.mythicTreasuresFound = count;
        }
    }
    
    /**
     * Enhanced luck system with stacking support
     * @param player The player to give luck to
     * @param durationSeconds How long the luck should last in seconds
     * @param multiplier The luck multiplier (e.g., 2.0 = double spawn rate)
     */
    public void giveLuck(Player player, long durationSeconds, double multiplier) {
        PlayerStats stats = getPlayerStats(player);
        long currentTime = System.currentTimeMillis();
        long newDuration = durationSeconds * 1000; // Convert to milliseconds
        
        // Check if player has active luck
        if (stats.hasActiveLuck()) {
            // Player has active luck - apply stacking logic
            
            if (Math.abs(stats.luckMultiplier - multiplier) < 0.01) {
                // Same multiplier (within 0.01 tolerance) - add time to existing luck
                stats.luckEndTime += newDuration;
                
                plugin.getLogger().info("Player " + player.getName() + " received additional " + 
                    formatDuration(durationSeconds) + " of " + String.format("%.1fx", multiplier) + 
                    " luck (same multiplier, time accumulated)");
                
            } else if (multiplier > stats.luckMultiplier) {
                // Higher multiplier - override current luck and save it for later
                long remainingTime = stats.luckEndTime - currentTime;
                
                // Save current luck as queued luck
                stats.queuedLuckEndTime = currentTime + newDuration + remainingTime;
                stats.queuedLuckMultiplier = stats.luckMultiplier;
                
                // Apply new higher luck immediately
                stats.luckEndTime = currentTime + newDuration;
                stats.luckMultiplier = multiplier;
                
                plugin.getLogger().info("Player " + player.getName() + " received " + 
                    String.format("%.1fx", multiplier) + " luck for " + formatDuration(durationSeconds) + 
                    " (higher multiplier overriding " + String.format("%.1fx", stats.queuedLuckMultiplier) + 
                    ", saved for later)");
                
            } else {
                // Lower multiplier - queue it to activate after current luck expires
                if (stats.queuedLuckMultiplier < multiplier || stats.queuedLuckEndTime <= stats.luckEndTime) {
                    // Replace queued luck if new one is better or no queue exists
                    stats.queuedLuckEndTime = stats.luckEndTime + newDuration;
                    stats.queuedLuckMultiplier = multiplier;
                    
                    plugin.getLogger().info("Player " + player.getName() + " received " + 
                        String.format("%.1fx", multiplier) + " luck queued for " + formatDuration(durationSeconds) + 
                        " (will activate after current " + String.format("%.1fx", stats.luckMultiplier) + " expires)");
                } else {
                    // Add time to existing queue if same multiplier
                    if (Math.abs(stats.queuedLuckMultiplier - multiplier) < 0.01) {
                        stats.queuedLuckEndTime += newDuration;
                        
                        plugin.getLogger().info("Player " + player.getName() + " received additional " + 
                            formatDuration(durationSeconds) + " added to queued " + String.format("%.1fx", multiplier) + " luck");
                    } else {
                        plugin.getLogger().info("Player " + player.getName() + " already has better luck queued, " +
                            String.format("%.1fx", multiplier) + " luck ignored");
                    }
                }
            }
        } else {
            // No active luck - check if queued luck should activate
            stats.processLuckQueue();
            
            // Apply new luck directly
            stats.luckEndTime = currentTime + newDuration;
            stats.luckMultiplier = multiplier;
            
            plugin.getLogger().info("Player " + player.getName() + " received " + 
                String.format("%.1fx", multiplier) + " luck for " + formatDuration(durationSeconds));
        }
    }
    
    /**
     * Helper method to format duration for logging
     */
    private String formatDuration(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        } else {
            return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
        }
    }
    
    /**
     * Remove luck from a player
     */
    public void removeLuck(Player player) {
        PlayerStats stats = getPlayerStats(player);
        stats.luckEndTime = 0;
        stats.luckMultiplier = 1.0;
    }
    
    /**
     * Get the effective treasure spawn rate multiplier for a player
     */
    public double getLuckMultiplier(Player player) {
        PlayerStats stats = getPlayerStats(player);
        return stats.getEffectiveLuckMultiplier();
    }
    
    /**
     * Toggle treasure spawning for a player
     * @param player The player to toggle treasure spawning for
     * @return The new state (true = enabled, false = disabled)
     */
    public boolean toggleTreasureSpawning(Player player) {
        PlayerStats stats = getPlayerStats(player);
        stats.treasureSpawningEnabled = !stats.treasureSpawningEnabled;
        return stats.treasureSpawningEnabled;
    }
    
    /**
     * Set treasure spawning state for a player
     * @param player The player to set treasure spawning for
     * @param enabled Whether treasure spawning should be enabled
     */
    public void setTreasureSpawning(Player player, boolean enabled) {
        PlayerStats stats = getPlayerStats(player);
        stats.treasureSpawningEnabled = enabled;
    }
    
    /**
     * Check if treasure spawning is enabled for a player
     * @param player The player to check
     * @return True if treasure spawning is enabled for this player
     */
    public boolean isTreasureSpawningEnabled(Player player) {
        PlayerStats stats = getPlayerStats(player);
        return stats.treasureSpawningEnabled;
    }

    public static class PlayerStats {
        public long totalBlocksMined = 0;
        public long commonTreasuresFound = 0;
        public long rareTreasuresFound = 0;
        public long epicTreasuresFound = 0;
        public long legendaryTreasuresFound = 0;
        public long mythicTreasuresFound = 0;
        public long minutesPlayed = 0;
        
        // Enhanced luck system with stacking
        public long luckEndTime = 0; // When current luck expires (System.currentTimeMillis())
        public double luckMultiplier = 1.0; // Current active luck multiplier
        
        // Queued luck system
        public long queuedLuckEndTime = 0; // When queued luck expires
        public double queuedLuckMultiplier = 1.0; // Queued luck multiplier
        
        // Treasure spawning toggle
        public boolean treasureSpawningEnabled = true; // Default enabled
        
        public long getTreasuresByRarity(Rarity rarity) {
            return switch (rarity) {
                case COMMON -> commonTreasuresFound;
                case RARE -> rareTreasuresFound;
                case EPIC -> epicTreasuresFound;
                case LEGENDARY -> legendaryTreasuresFound;
                case MYTHIC -> mythicTreasuresFound;
            };
        }
        
        /**
         * Check if player currently has active luck (without processing queue)
         */
        private boolean hasActiveLuckRaw() {
            return System.currentTimeMillis() < luckEndTime;
        }
        
        /**
         * Check if player currently has active luck
         */
        public boolean hasActiveLuck() {
            processLuckQueue(); // Process queue first, then check
            return hasActiveLuckRaw();
        }
        
        /**
         * Get remaining luck time in seconds
         */
        public long getRemainingLuckSeconds() {
            processLuckQueue(); // Process queue first
            if (!hasActiveLuckRaw()) {
                return 0;
            }
            return (luckEndTime - System.currentTimeMillis()) / 1000;
        }
        
        /**
         * Get effective luck multiplier (1.0 if no active luck)
         */
        public double getEffectiveLuckMultiplier() {
            processLuckQueue(); // Process queue first
            return hasActiveLuckRaw() ? luckMultiplier : 1.0;
        }
        
        /**
         * Check if player has queued luck waiting
         */
        public boolean hasQueuedLuck() {
            long currentTime = System.currentTimeMillis();
            return queuedLuckEndTime > currentTime && queuedLuckMultiplier > 1.0;
        }
        
        /**
         * Get queued luck remaining time in seconds
         */
        public long getQueuedLuckRemainingSeconds() {
            if (!hasQueuedLuck()) {
                return 0;
            }
            return (queuedLuckEndTime - System.currentTimeMillis()) / 1000;
        }
        
        /**
         * Process the queued luck effect, activating it if the current luck has expired
         * NOTE: This method uses hasActiveLuckRaw() to avoid infinite recursion
         */
        public void processLuckQueue() {
            long currentTime = System.currentTimeMillis();
            
            // If current luck has expired and we have queued luck
            // Use hasActiveLuckRaw() here to avoid infinite recursion
            if (!hasActiveLuckRaw() && hasQueuedLuck()) {
                // Calculate remaining time for queued luck
                long queuedRemainingTime = queuedLuckEndTime - currentTime;
                
                if (queuedRemainingTime > 0) {
                    // Activate queued luck
                    luckMultiplier = queuedLuckMultiplier;
                    luckEndTime = currentTime + queuedRemainingTime;
                    
                    // Reset queued luck
                    queuedLuckMultiplier = 1.0;
                    queuedLuckEndTime = 0;
                    
                    InfernalTresures.getInstance().getLogger().info("Activated queued luck: " + 
                        String.format("%.1fx", luckMultiplier) + " for " + (queuedRemainingTime / 1000) + " seconds");
                } else {
                    // Queued luck has also expired, clear it
                    queuedLuckMultiplier = 1.0;
                    queuedLuckEndTime = 0;
                }
            }
        }
    }
}