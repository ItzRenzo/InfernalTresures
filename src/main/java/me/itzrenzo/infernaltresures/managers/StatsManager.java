package me.itzrenzo.infernaltresures.managers;

import me.itzrenzo.infernaltresures.InfernalTresures;
import me.itzrenzo.infernaltresures.models.Rarity;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StatsManager {
    
    private final InfernalTresures plugin;
    private final Map<UUID, PlayerStats> playerStats = new HashMap<>();
    private final Map<UUID, Long> playerJoinTimes = new HashMap<>();
    private File statsFile;
    private FileConfiguration statsConfig;
    
    public StatsManager(InfernalTresures plugin) {
        this.plugin = plugin;
        this.statsFile = new File(plugin.getDataFolder(), "stats.yml");
        loadStats();
    }
    
    private void loadStats() {
        if (!statsFile.exists()) {
            try {
                statsFile.createNewFile();
                plugin.getLogger().info("Created stats.yml file");
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create stats.yml: " + e.getMessage());
                return;
            }
        }
        
        statsConfig = YamlConfiguration.loadConfiguration(statsFile);
        
        // Load all player stats from file
        for (String uuidString : statsConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidString);
                PlayerStats stats = new PlayerStats();
                
                stats.totalBlocksMined = statsConfig.getLong(uuidString + ".total-blocks-mined", 0);
                stats.commonTreasuresFound = statsConfig.getLong(uuidString + ".common-treasures-found", 0);
                stats.rareTreasuresFound = statsConfig.getLong(uuidString + ".rare-treasures-found", 0);
                stats.epicTreasuresFound = statsConfig.getLong(uuidString + ".epic-treasures-found", 0);
                stats.legendaryTreasuresFound = statsConfig.getLong(uuidString + ".legendary-treasures-found", 0);
                stats.mythicTreasuresFound = statsConfig.getLong(uuidString + ".mythic-treasures-found", 0);
                stats.minutesPlayed = statsConfig.getLong(uuidString + ".minutes-played", 0);
                
                playerStats.put(uuid, stats);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in stats file: " + uuidString);
            }
        }
        
        plugin.getLogger().info("Loaded statistics for " + playerStats.size() + " players");
    }
    
    public void saveStats() {
        if (statsConfig == null) return;
        
        // Save all current player stats to file
        for (Map.Entry<UUID, PlayerStats> entry : playerStats.entrySet()) {
            String uuidString = entry.getKey().toString();
            PlayerStats stats = entry.getValue();
            
            statsConfig.set(uuidString + ".total-blocks-mined", stats.totalBlocksMined);
            statsConfig.set(uuidString + ".common-treasures-found", stats.commonTreasuresFound);
            statsConfig.set(uuidString + ".rare-treasures-found", stats.rareTreasuresFound);
            statsConfig.set(uuidString + ".epic-treasures-found", stats.epicTreasuresFound);
            statsConfig.set(uuidString + ".legendary-treasures-found", stats.legendaryTreasuresFound);
            statsConfig.set(uuidString + ".mythic-treasures-found", stats.mythicTreasuresFound);
            statsConfig.set(uuidString + ".minutes-played", stats.minutesPlayed);
        }
        
        try {
            statsConfig.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save stats.yml: " + e.getMessage());
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
     * Give luck to a player for a specified duration
     * @param player The player to give luck to
     * @param durationSeconds How long the luck should last in seconds
     * @param multiplier The luck multiplier (e.g., 2.0 = double spawn rate)
     */
    public void giveLuck(Player player, long durationSeconds, double multiplier) {
        PlayerStats stats = getPlayerStats(player);
        stats.luckEndTime = System.currentTimeMillis() + (durationSeconds * 1000);
        stats.luckMultiplier = multiplier;
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

    public static class PlayerStats {
        public long totalBlocksMined = 0;
        public long commonTreasuresFound = 0;
        public long rareTreasuresFound = 0;
        public long epicTreasuresFound = 0;
        public long legendaryTreasuresFound = 0;
        public long mythicTreasuresFound = 0;
        public long minutesPlayed = 0;
        
        // Luck system
        public long luckEndTime = 0; // When luck expires (System.currentTimeMillis())
        public double luckMultiplier = 1.0; // Default 1.0 = no luck bonus
        
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
         * Check if player currently has active luck
         */
        public boolean hasActiveLuck() {
            return System.currentTimeMillis() < luckEndTime;
        }
        
        /**
         * Get remaining luck time in seconds
         */
        public long getRemainingLuckSeconds() {
            if (!hasActiveLuck()) {
                return 0;
            }
            return (luckEndTime - System.currentTimeMillis()) / 1000;
        }
        
        /**
         * Get effective luck multiplier (1.0 if no active luck)
         */
        public double getEffectiveLuckMultiplier() {
            return hasActiveLuck() ? luckMultiplier : 1.0;
        }
    }
}