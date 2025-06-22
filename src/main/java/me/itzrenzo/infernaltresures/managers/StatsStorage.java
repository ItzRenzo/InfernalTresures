package me.itzrenzo.infernaltresures.managers;

import me.itzrenzo.infernaltresures.models.Rarity;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for different storage backends (YML, SQLite, MySQL)
 */
public interface StatsStorage {
    
    /**
     * Initialize the storage system
     */
    CompletableFuture<Void> initialize();
    
    /**
     * Close the storage system and cleanup resources
     */
    CompletableFuture<Void> close();
    
    /**
     * Load player stats from storage
     */
    CompletableFuture<StatsManager.PlayerStats> loadPlayerStats(UUID uuid);
    
    /**
     * Save player stats to storage
     */
    CompletableFuture<Void> savePlayerStats(UUID uuid, StatsManager.PlayerStats stats);
    
    /**
     * Load all player stats from storage
     */
    CompletableFuture<java.util.Map<UUID, StatsManager.PlayerStats>> loadAllPlayerStats();
    
    /**
     * Save all player stats to storage
     */
    CompletableFuture<Void> saveAllPlayerStats(java.util.Map<UUID, StatsManager.PlayerStats> allStats);
    
    /**
     * Check if the storage system is available and working
     */
    boolean isAvailable();
    
    /**
     * Get the storage type name
     */
    String getStorageType();
}