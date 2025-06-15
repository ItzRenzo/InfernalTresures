package me.itzrenzo.infernaltresures.managers;

import me.itzrenzo.infernaltresures.InfernalTresures;
import org.bukkit.configuration.file.FileConfiguration;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class ConfigManager {
    private final InfernalTresures plugin;
    private FileConfiguration config;
    
    // Default values
    private static final int DEFAULT_TREASURE_SPAWN_CHANCE = 15; // 15% chance
    private static final boolean DEFAULT_MINING_EFFECT = true;
    
    // Biome files to copy
    private static final String[] BIOME_FILES = {
        "desert.yml",
        "forest.yml",
        "ocean.yml",
        "plains.yml"
    };
    
    public ConfigManager(InfernalTresures plugin) {
        this.plugin = plugin;
    }
    
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
        
        // Create and populate biomes folder
        setupBiomesFolder();
        
        // Add default values
        setDefaults();
        
        // Save back any changes
        plugin.saveConfig();
    }
    
    /**
     * Create biomes folder and copy biome configuration files
     */
    private void setupBiomesFolder() {
        File biomesFolder = new File(plugin.getDataFolder(), "biomes");
        
        // Create biomes folder if it doesn't exist
        if (!biomesFolder.exists()) {
            biomesFolder.mkdirs();
            plugin.getLogger().info("Created biomes folder.");
        }
        
        // Copy each biome file if it doesn't exist
        for (String fileName : BIOME_FILES) {
            File biomeFile = new File(biomesFolder, fileName);
            
            if (!biomeFile.exists()) {
                try {
                    InputStream inputStream = plugin.getResource("biomes/" + fileName);
                    if (inputStream != null) {
                        Files.copy(inputStream, biomeFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        plugin.getLogger().info("Created biome config: " + fileName);
                        inputStream.close();
                    } else {
                        plugin.getLogger().warning("Could not find biome config in jar: " + fileName);
                    }
                } catch (IOException e) {
                    plugin.getLogger().severe("Failed to copy biome config " + fileName + ": " + e.getMessage());
                }
            }
        }
    }
    
    private void setDefaults() {
        if (!config.isSet("treasure.spawn-chance")) {
            config.set("treasure.spawn-chance", DEFAULT_TREASURE_SPAWN_CHANCE);
        }
        
        if (!config.isSet("treasure.mining-effect")) {
            config.set("treasure.mining-effect", DEFAULT_MINING_EFFECT);
        }
        
        // Make sure this is loaded but don't set a default
        if (!config.isSet("treasure.enabled-blocks")) {
            config.set("treasure.enabled-blocks", new String[]{"STONE", "DEEPSLATE", "NETHERRACK", "END_STONE", 
                "DIORITE", "ANDESITE", "GRANITE", "BLACKSTONE", "BASALT", "TUFF", "CALCITE"});
        }
    }
    
    public void saveConfig() {
        plugin.saveConfig();
    }
    
    /**
     * Reload the config and associated loot tables
     */
    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        
        // Also reload loot tables and messages
        if (plugin.getLootManager() != null) {
            plugin.getLootManager().reload();
        }
        
        if (plugin.getMessageManager() != null) {
            plugin.getMessageManager().reload();
        }
        
        plugin.getLogger().info("Configuration, loot tables, and messages reloaded.");
    }
    
    public int getTreasureSpawnChance() {
        return config.getInt("treasure.spawn-chance", DEFAULT_TREASURE_SPAWN_CHANCE);
    }
    
    public boolean isMiningEffectEnabled() {
        return config.getBoolean("treasure.mining-effect", DEFAULT_MINING_EFFECT);
    }
    
    public String[] getEnabledBlocks() {
        return config.getStringList("treasure.enabled-blocks").toArray(new String[0]);
    }
    
    // Hologram configuration methods
    public boolean isHologramEnabledForRarity(me.itzrenzo.infernaltresures.models.Rarity rarity) {
        return config.getBoolean("holograms.enabled-rarities." + rarity.name().toLowerCase(), true);
    }
    
    public double getHologramHeight() {
        return config.getDouble("holograms.height", 1.5);
    }
    
    public int getHologramVisibleDistance() {
        return config.getInt("holograms.visible-distance", 16);
    }
}