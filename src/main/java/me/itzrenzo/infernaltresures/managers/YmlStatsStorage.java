package me.itzrenzo.infernaltresures.managers;

import me.itzrenzo.infernaltresures.InfernalTresures;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * YML file storage implementation for player stats
 */
public class YmlStatsStorage implements StatsStorage {
    
    private final InfernalTresures plugin;
    private final File statsFile;
    private FileConfiguration statsConfig;
    
    public YmlStatsStorage(InfernalTresures plugin) {
        this.plugin = plugin;
        this.statsFile = new File(plugin.getDataFolder(), "stats.yml");
    }
    
    @Override
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            if (!statsFile.exists()) {
                try {
                    statsFile.createNewFile();
                    plugin.getLogger().info("Created stats.yml file");
                } catch (IOException e) {
                    plugin.getLogger().severe("Failed to create stats.yml: " + e.getMessage());
                    throw new RuntimeException(e);
                }
            }
            statsConfig = YamlConfiguration.loadConfiguration(statsFile);
        });
    }
    
    @Override
    public CompletableFuture<Void> close() {
        return CompletableFuture.completedFuture(null); // No cleanup needed for YML
    }
    
    @Override
    public CompletableFuture<StatsManager.PlayerStats> loadPlayerStats(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            if (statsConfig == null) return new StatsManager.PlayerStats();
            
            String uuidString = uuid.toString();
            StatsManager.PlayerStats stats = new StatsManager.PlayerStats();
            
            stats.totalBlocksMined = statsConfig.getLong(uuidString + ".total-blocks-mined", 0);
            stats.commonTreasuresFound = statsConfig.getLong(uuidString + ".common-treasures-found", 0);
            stats.rareTreasuresFound = statsConfig.getLong(uuidString + ".rare-treasures-found", 0);
            stats.epicTreasuresFound = statsConfig.getLong(uuidString + ".epic-treasures-found", 0);
            stats.legendaryTreasuresFound = statsConfig.getLong(uuidString + ".legendary-treasures-found", 0);
            stats.mythicTreasuresFound = statsConfig.getLong(uuidString + ".mythic-treasures-found", 0);
            stats.minutesPlayed = statsConfig.getLong(uuidString + ".minutes-played", 0);
            
            // Load luck data
            stats.luckEndTime = statsConfig.getLong(uuidString + ".luck-end-time", 0);
            stats.luckMultiplier = statsConfig.getDouble(uuidString + ".luck-multiplier", 1.0);
            stats.queuedLuckEndTime = statsConfig.getLong(uuidString + ".queued-luck-end-time", 0);
            stats.queuedLuckMultiplier = statsConfig.getDouble(uuidString + ".queued-luck-multiplier", 1.0);
            stats.treasureSpawningEnabled = statsConfig.getBoolean(uuidString + ".treasure-spawning-enabled", true);
            
            return stats;
        });
    }
    
    @Override
    public CompletableFuture<Void> savePlayerStats(UUID uuid, StatsManager.PlayerStats stats) {
        return CompletableFuture.runAsync(() -> {
            if (statsConfig == null) return;
            
            String uuidString = uuid.toString();
            
            statsConfig.set(uuidString + ".total-blocks-mined", stats.totalBlocksMined);
            statsConfig.set(uuidString + ".common-treasures-found", stats.commonTreasuresFound);
            statsConfig.set(uuidString + ".rare-treasures-found", stats.rareTreasuresFound);
            statsConfig.set(uuidString + ".epic-treasures-found", stats.epicTreasuresFound);
            statsConfig.set(uuidString + ".legendary-treasures-found", stats.legendaryTreasuresFound);
            statsConfig.set(uuidString + ".mythic-treasures-found", stats.mythicTreasuresFound);
            statsConfig.set(uuidString + ".minutes-played", stats.minutesPlayed);
            
            // Save luck data
            statsConfig.set(uuidString + ".luck-end-time", stats.luckEndTime);
            statsConfig.set(uuidString + ".luck-multiplier", stats.luckMultiplier);
            statsConfig.set(uuidString + ".queued-luck-end-time", stats.queuedLuckEndTime);
            statsConfig.set(uuidString + ".queued-luck-multiplier", stats.queuedLuckMultiplier);
            statsConfig.set(uuidString + ".treasure-spawning-enabled", stats.treasureSpawningEnabled);
            
            try {
                statsConfig.save(statsFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save stats.yml: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Map<UUID, StatsManager.PlayerStats>> loadAllPlayerStats() {
        return CompletableFuture.supplyAsync(() -> {
            Map<UUID, StatsManager.PlayerStats> allStats = new HashMap<>();
            
            if (statsConfig == null) return allStats;
            
            for (String uuidString : statsConfig.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    StatsManager.PlayerStats stats = loadPlayerStats(uuid).join();
                    allStats.put(uuid, stats);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in stats file: " + uuidString);
                }
            }
            
            plugin.getLogger().info("Loaded statistics for " + allStats.size() + " players from YML");
            return allStats;
        });
    }
    
    @Override
    public CompletableFuture<Void> saveAllPlayerStats(Map<UUID, StatsManager.PlayerStats> allStats) {
        return CompletableFuture.runAsync(() -> {
            if (statsConfig == null) return;
            
            for (Map.Entry<UUID, StatsManager.PlayerStats> entry : allStats.entrySet()) {
                savePlayerStats(entry.getKey(), entry.getValue()).join();
            }
        });
    }
    
    @Override
    public boolean isAvailable() {
        return statsConfig != null;
    }
    
    @Override
    public String getStorageType() {
        return "YML";
    }
}