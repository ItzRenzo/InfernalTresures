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
        if (!config.isSet("treasure.effects.sound")) {
            config.set("treasure.effects.sound", true);
        }
        
        if (!config.isSet("treasure.effects.particles")) {
            config.set("treasure.effects.particles", true);
        }
        
        if (!config.isSet("treasure.hourly-limit")) {
            config.set("treasure.hourly-limit", 0);
        }
        
        // Set debug configuration defaults
        if (!config.isSet("debug.enabled")) {
            config.set("debug.enabled", false);
        }
        
        // Set treasure announcement defaults (per rarity)
        if (!config.isSet("treasure.announce-finds.common")) {
            config.set("treasure.announce-finds.common", false);
        }
        if (!config.isSet("treasure.announce-finds.rare")) {
            config.set("treasure.announce-finds.rare", false);
        }
        if (!config.isSet("treasure.announce-finds.epic")) {
            config.set("treasure.announce-finds.epic", false);
        }
        if (!config.isSet("treasure.announce-finds.legendary")) {
            config.set("treasure.announce-finds.legendary", true);
        }
        if (!config.isSet("treasure.announce-finds.mythic")) {
            config.set("treasure.announce-finds.mythic", true);
        }
        
        // Set despawn time defaults (per rarity, in seconds)
        if (!config.isSet("rarity.despawn-times.common")) {
            config.set("rarity.despawn-times.common", 300); // 5 minutes
        }
        if (!config.isSet("rarity.despawn-times.rare")) {
            config.set("rarity.despawn-times.rare", 420); // 7 minutes
        }
        if (!config.isSet("rarity.despawn-times.epic")) {
            config.set("rarity.despawn-times.epic", 600); // 10 minutes
        }
        if (!config.isSet("rarity.despawn-times.legendary")) {
            config.set("rarity.despawn-times.legendary", 900); // 15 minutes
        }
        if (!config.isSet("rarity.despawn-times.mythic")) {
            config.set("rarity.despawn-times.mythic", 1200); // 20 minutes
        }
        
        // Remove old single announce-finds setting if it exists
        if (config.isSet("treasure.announce-finds") && !config.isConfigurationSection("treasure.announce-finds")) {
            config.set("treasure.announce-finds", null);
        }
        
        // Remove old mining section if it exists
        if (config.isSet("mining")) {
            config.set("mining", null);
        }
        
        // Remove old treasure.spawn-chance if it exists
        if (config.isSet("treasure.spawn-chance")) {
            config.set("treasure.spawn-chance", null);
        }
        
        // Remove old treasure.enabled-blocks if it exists
        if (config.isSet("treasure.enabled-blocks")) {
            config.set("treasure.enabled-blocks", null);
        }
        
        if (!config.isSet("debug.categories.loot-generation")) {
            config.set("debug.categories.loot-generation", true);
        }
        
        if (!config.isSet("debug.categories.treasure-spawning")) {
            config.set("debug.categories.treasure-spawning", true);
        }
        
        if (!config.isSet("debug.categories.mmo-items")) {
            config.set("debug.categories.mmo-items", true);
        }
        
        if (!config.isSet("debug.categories.executable-items")) {
            config.set("debug.categories.executable-items", true);
        }
        
        if (!config.isSet("debug.categories.barrel-filling")) {
            config.set("debug.categories.barrel-filling", true);
        }
        
        if (!config.isSet("debug.categories.biome-detection")) {
            config.set("debug.categories.biome-detection", true);
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
        
        // Also reload loot tables, messages, blocks, stats, and menus
        if (plugin.getLootManager() != null) {
            plugin.getLootManager().reload();
        }
        
        if (plugin.getMessageManager() != null) {
            plugin.getMessageManager().reload();
        }
        
        if (plugin.getBlockManager() != null) {
            plugin.getBlockManager().reload();
        }
        
        if (plugin.getStatsManager() != null) {
            plugin.getStatsManager().reload();
        }
        
        if (plugin.getMenuManager() != null) {
            plugin.getMenuManager().reload();
        }
        
        plugin.getLogger().info("Configuration, loot tables, messages, blocks, stats, and menus reloaded.");
    }
    
    public boolean isMiningEffectEnabled() {
        return config.getBoolean("treasure.mining-effect", DEFAULT_MINING_EFFECT);
    }
    
    public boolean isSoundEffectEnabled() {
        return config.getBoolean("treasure.effects.sound", true);
    }
    
    public boolean isParticleEffectEnabled() {
        return config.getBoolean("treasure.effects.particles", true);
    }
    
    public int getHourlyLimit() {
        return config.getInt("treasure.hourly-limit", 0);
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
    
    // Debug configuration methods
    public boolean isDebugEnabled() {
        return config.getBoolean("debug.enabled", false);
    }
    
    public boolean isDebugCategoryEnabled(String category) {
        if (!isDebugEnabled()) {
            return false;
        }
        return config.getBoolean("debug.categories." + category, true);
    }
    
    public boolean isLootGenerationDebugEnabled() {
        return isDebugCategoryEnabled("loot-generation");
    }
    
    public boolean isTreasureSpawningDebugEnabled() {
        return isDebugCategoryEnabled("treasure-spawning");
    }
    
    public boolean isMMOItemsDebugEnabled() {
        return isDebugCategoryEnabled("mmo-items");
    }
    
    public boolean isExecutableItemsDebugEnabled() {
        return isDebugCategoryEnabled("executable-items");
    }
    
    public boolean isBarrelFillingDebugEnabled() {
        return isDebugCategoryEnabled("barrel-filling");
    }
    
    public boolean isBiomeDetectionDebugEnabled() {
        return isDebugCategoryEnabled("biome-detection");
    }
    
    // Treasure announcement configuration methods
    public boolean isTreasureAnnouncementEnabled(me.itzrenzo.infernaltresures.models.Rarity rarity) {
        return config.getBoolean("treasure.announce-finds." + rarity.name().toLowerCase(), 
            rarity == me.itzrenzo.infernaltresures.models.Rarity.LEGENDARY || 
            rarity == me.itzrenzo.infernaltresures.models.Rarity.MYTHIC);
    }
    
    // Backward compatibility - check if any rarity has announcements enabled
    public boolean isTreasureAnnouncementEnabled() {
        return isTreasureAnnouncementEnabled(me.itzrenzo.infernaltresures.models.Rarity.COMMON) ||
               isTreasureAnnouncementEnabled(me.itzrenzo.infernaltresures.models.Rarity.RARE) ||
               isTreasureAnnouncementEnabled(me.itzrenzo.infernaltresures.models.Rarity.EPIC) ||
               isTreasureAnnouncementEnabled(me.itzrenzo.infernaltresures.models.Rarity.LEGENDARY) ||
               isTreasureAnnouncementEnabled(me.itzrenzo.infernaltresures.models.Rarity.MYTHIC);
    }
    
    // Despawn time configuration methods
    public int getDespawnTime(me.itzrenzo.infernaltresures.models.Rarity rarity) {
        // Default despawn times if not configured
        int defaultTime = switch (rarity) {
            case COMMON -> 300;     // 5 minutes
            case RARE -> 420;       // 7 minutes
            case EPIC -> 600;       // 10 minutes
            case LEGENDARY -> 900;  // 15 minutes
            case MYTHIC -> 1200;    // 20 minutes
        };
        
        return config.getInt("rarity.despawn-times." + rarity.name().toLowerCase(), defaultTime);
    }
}