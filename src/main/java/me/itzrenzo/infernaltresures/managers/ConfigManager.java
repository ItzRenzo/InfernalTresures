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
        "badlands.yml",
        "basalt_deltas.yml", 
        "crimson_forest.yml",
        "desert.yml",
        "end.yml",
        "forest.yml",
        "jungle.yml",
        "mountains.yml",
        "nether.yml",
        "nether_wastes.yml",
        "ocean.yml",
        "plains.yml",
        "savanna.yml",
        "soul_sand_valley.yml",
        "swamp.yml",
        "taiga.yml",
        "warped_forest.yml"
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
    
    // Loot progression configuration methods
    public int getCurrentProgressionLevel() {
        return config.getInt("treasure.loot-progression.current-level", 1);
    }
    
    public void setCurrentProgressionLevel(int level) {
        if (level < 1 || level > 4) {
            throw new IllegalArgumentException("Progression level must be between 1 and 4");
        }
        config.set("treasure.loot-progression.current-level", level);
        plugin.saveConfig();
    }
    
    public int getProgressionSlots(int level) {
        return config.getInt("treasure.loot-progression.levels." + level + ".slots", 
            level == 1 ? 7 : level == 2 ? 14 : level == 3 ? 21 : 27);
    }
    
    public int getCurrentProgressionSlots() {
        return getProgressionSlots(getCurrentProgressionLevel());
    }
    
    public String getProgressionLevelName(int level) {
        return config.getString("treasure.loot-progression.levels." + level + ".name", 
            level == 1 ? "Beginner" : level == 2 ? "Intermediate" : level == 3 ? "Advanced" : "Master");
    }
    
    public String getProgressionLevelDescription(int level) {
        return config.getString("treasure.loot-progression.levels." + level + ".description",
            "Progression level " + level);
    }
    
    public boolean isProgressionDebugEnabled() {
        return config.getBoolean("treasure.loot-progression.debug", false);
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
    
    /**
     * Check if items should drop when barrel despawns
     */
    public boolean shouldDropItemsOnDespawn() {
        return config.getBoolean("treasure.despawn.drop-items-on-despawn", true);
    }
    
    // Per-rarity effect configuration methods
    public boolean isRarityEffectEnabled(me.itzrenzo.infernaltresures.models.Rarity rarity) {
        // Check global effects first
        if (!isSoundEffectEnabled() && !isParticleEffectEnabled()) {
            return false;
        }
        
        return config.getBoolean("treasure.rarity-effects." + rarity.name().toLowerCase() + ".enabled", true);
    }
    
    public String getRaritySound(me.itzrenzo.infernaltresures.models.Rarity rarity) {
        // Default sounds for each rarity
        String defaultSound = switch (rarity) {
            case COMMON -> "ENTITY_EXPERIENCE_ORB_PICKUP";
            case RARE -> "ENTITY_PLAYER_LEVELUP";
            case EPIC -> "ENTITY_ENDER_EYE_LAUNCH";
            case LEGENDARY -> "ENTITY_WITHER_SPAWN";
            case MYTHIC -> "ENTITY_ENDER_DRAGON_DEATH";
        };
        
        return config.getString("treasure.rarity-effects." + rarity.name().toLowerCase() + ".sound.type", defaultSound);
    }
    
    public float getRaritySoundVolume(me.itzrenzo.infernaltresures.models.Rarity rarity) {
        // Default volumes for each rarity
        float defaultVolume = switch (rarity) {
            case COMMON -> 0.8f;
            case RARE -> 1.0f;
            case EPIC -> 1.2f;
            case LEGENDARY -> 1.5f;
            case MYTHIC -> 2.0f;
        };
        
        return (float) config.getDouble("treasure.rarity-effects." + rarity.name().toLowerCase() + ".sound.volume", defaultVolume);
    }
    
    public float getRaritySoundPitch(me.itzrenzo.infernaltresures.models.Rarity rarity) {
        // Default pitches for each rarity
        float defaultPitch = switch (rarity) {
            case COMMON -> 1.0f;
            case RARE -> 1.2f;
            case EPIC -> 0.8f;
            case LEGENDARY -> 1.5f;
            case MYTHIC -> 0.5f;
        };
        
        return (float) config.getDouble("treasure.rarity-effects." + rarity.name().toLowerCase() + ".sound.pitch", defaultPitch);
    }
    
    public String getRarityParticle(me.itzrenzo.infernaltresures.models.Rarity rarity) {
        // Default particles for each rarity
        String defaultParticle = switch (rarity) {
            case COMMON -> "VILLAGER_HAPPY";
            case RARE -> "ENCHANTMENT_TABLE";
            case EPIC -> "PORTAL";
            case LEGENDARY -> "DRAGON_BREATH";
            case MYTHIC -> "END_ROD";
        };
        
        return config.getString("treasure.rarity-effects." + rarity.name().toLowerCase() + ".particles.type", defaultParticle);
    }
    
    public int getRarityParticleCount(me.itzrenzo.infernaltresures.models.Rarity rarity) {
        // Default particle counts for each rarity
        int defaultCount = switch (rarity) {
            case COMMON -> 15;
            case RARE -> 25;
            case EPIC -> 35;
            case LEGENDARY -> 50;
            case MYTHIC -> 75;
        };
        
        return config.getInt("treasure.rarity-effects." + rarity.name().toLowerCase() + ".particles.count", defaultCount);
    }
    
    public double getRarityParticleOffset(me.itzrenzo.infernaltresures.models.Rarity rarity) {
        // Default particle offsets for each rarity
        double defaultOffset = switch (rarity) {
            case COMMON -> 0.5;
            case RARE -> 0.8;
            case EPIC -> 1.0;
            case LEGENDARY -> 1.2;
            case MYTHIC -> 1.5;
        };
        
        return config.getDouble("treasure.rarity-effects." + rarity.name().toLowerCase() + ".particles.offset", defaultOffset);
    }
}