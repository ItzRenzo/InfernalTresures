package me.itzrenzo.infernaltresures.managers;

import me.itzrenzo.infernaltresures.InfernalTresures;
import me.itzrenzo.infernaltresures.models.Rarity;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class BlockManager {
    private final InfernalTresures plugin;
    private FileConfiguration blocksConfig;
    private final Map<Material, Map<Rarity, Double>> blockChances = new HashMap<>();
    private boolean useBlockSpecificChances = true;
    private double fallbackChance = 1.0;
    private boolean debugBlockChances = false;
    
    public BlockManager(InfernalTresures plugin) {
        this.plugin = plugin;
    }
    
    public void loadBlocks() {
        // Create blocks.yml if it doesn't exist
        File blocksFile = new File(plugin.getDataFolder(), "blocks.yml");
        if (!blocksFile.exists()) {
            try {
                InputStream inputStream = plugin.getResource("blocks.yml");
                if (inputStream != null) {
                    Files.copy(inputStream, blocksFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    plugin.getLogger().info("Created blocks.yml file");
                    inputStream.close();
                } else {
                    plugin.getLogger().warning("Could not find blocks.yml in plugin jar");
                    return;
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create blocks.yml: " + e.getMessage());
                return;
            }
        }
        
        // Load the blocks configuration
        blocksConfig = YamlConfiguration.loadConfiguration(blocksFile);
        blockChances.clear();
        
        // Load global settings
        useBlockSpecificChances = blocksConfig.getBoolean("settings.use-block-specific-chances", true);
        fallbackChance = blocksConfig.getDouble("settings.fallback-chance", 1.0);
        debugBlockChances = blocksConfig.getBoolean("settings.debug-block-chances", false);
        
        // Load block configurations
        ConfigurationSection blocksSection = blocksConfig.getConfigurationSection("blocks");
        if (blocksSection != null) {
            int loadedBlocks = 0;
            for (String blockKey : blocksSection.getKeys(false)) {
                try {
                    Material material = Material.valueOf(blockKey.toUpperCase());
                    ConfigurationSection blockSection = blocksSection.getConfigurationSection(blockKey);
                    
                    if (blockSection != null) {
                        ConfigurationSection chancesSection = blockSection.getConfigurationSection("spawn-chances");
                        if (chancesSection != null) {
                            Map<Rarity, Double> rarityChances = new HashMap<>();
                            
                            for (Rarity rarity : Rarity.values()) {
                                String rarityKey = rarity.name().toLowerCase();
                                double chance = chancesSection.getDouble(rarityKey, 0.0);
                                rarityChances.put(rarity, chance);
                            }
                            
                            blockChances.put(material, rarityChances);
                            loadedBlocks++;
                            
                            if (debugBlockChances) {
                                plugin.getLogger().info("Loaded block config for " + material + ": " + rarityChances);
                            }
                        }
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material in blocks.yml: " + blockKey);
                }
            }
            
            plugin.getLogger().info("Loaded " + loadedBlocks + " block configurations");
        }
    }
    
    public void reload() {
        loadBlocks();
    }
    
    /**
     * Check if a treasure should spawn for the given block and return the rarity
     * @param material The block material that was mined
     * @param player The player who mined the block (for luck calculation)
     * @return The rarity of treasure to spawn, or null if no treasure should spawn
     */
    public Rarity shouldSpawnTreasure(Material material, Player player) {
        if (!useBlockSpecificChances) {
            // Fall back to global spawn chance system
            return null; // Let the existing system handle it
        }
        
        // Get player's luck multiplier
        double luckMultiplier = 1.0;
        if (player != null) {
            luckMultiplier = plugin.getStatsManager().getLuckMultiplier(player);
        }
        
        Map<Rarity, Double> chances = blockChances.get(material);
        if (chances == null) {
            // Block not configured, use fallback chance
            if (debugBlockChances) {
                plugin.getLogger().info("Block " + material + " not configured, using fallback chance: " + fallbackChance + "%" +
                    (luckMultiplier > 1.0 ? " (luck: " + String.format("%.1fx", luckMultiplier) + ")" : ""));
            }
            
            double effectiveChance = fallbackChance * luckMultiplier;
            if (ThreadLocalRandom.current().nextDouble(100.0) < effectiveChance) {
                // Use global rarity distribution
                return plugin.getTreasureManager().getRandomRarity();
            }
            return null;
        }
        
        // Check each rarity in order (starting with highest)
        for (Rarity rarity : new Rarity[]{Rarity.MYTHIC, Rarity.LEGENDARY, Rarity.EPIC, Rarity.RARE, Rarity.COMMON}) {
            double baseChance = chances.get(rarity);
            double effectiveChance = baseChance * luckMultiplier;
            double roll = ThreadLocalRandom.current().nextDouble(100.0);
            
            if (debugBlockChances) {
                plugin.getLogger().info("Block " + material + " " + rarity + " check: rolled " + 
                    String.format("%.4f", roll) + " vs " + String.format("%.4f", effectiveChance) + "%" +
                    " (base: " + baseChance + "%" + 
                    (luckMultiplier > 1.0 ? ", luck: " + String.format("%.1fx", luckMultiplier) + ")" : ")"));
            }
            
            if (roll < effectiveChance) {
                if (debugBlockChances) {
                    plugin.getLogger().info("Block " + material + " triggered " + rarity + " treasure!" +
                        (luckMultiplier > 1.0 ? " (boosted by luck)" : ""));
                }
                return rarity;
            }
        }
        
        return null; // No treasure spawned
    }
    
    /**
     * Backward compatibility method - calls the new method with null player
     */
    public Rarity shouldSpawnTreasure(Material material) {
        return shouldSpawnTreasure(material, null);
    }
    
    /**
     * Get the display name for a block
     */
    public String getBlockDisplayName(Material material) {
        if (blocksConfig == null) {
            return formatMaterialName(material);
        }
        
        String blockKey = material.name().toLowerCase();
        String displayName = blocksConfig.getString("blocks." + blockKey + ".display-name");
        
        if (displayName != null) {
            return displayName;
        }
        
        return formatMaterialName(material);
    }
    
    /**
     * Format material name for display
     */
    private String formatMaterialName(Material material) {
        String name = material.name().toLowerCase().replace("_", " ");
        String[] words = name.split(" ");
        StringBuilder formatted = new StringBuilder();
        
        for (String word : words) {
            if (!word.isEmpty()) {
                formatted.append(Character.toUpperCase(word.charAt(0)));
                formatted.append(word.substring(1));
                formatted.append(" ");
            }
        }
        
        return formatted.toString().trim();
    }
    
    /**
     * Get spawn chance for a specific block and rarity
     */
    public double getSpawnChance(Material material, Rarity rarity) {
        Map<Rarity, Double> chances = blockChances.get(material);
        if (chances == null) {
            return 0.0;
        }
        return chances.getOrDefault(rarity, 0.0);
    }
    
    /**
     * Check if block-specific chances are enabled
     */
    public boolean isUsingBlockSpecificChances() {
        return useBlockSpecificChances;
    }
    
    /**
     * Get the number of configured blocks
     */
    public int getConfiguredBlockCount() {
        return blockChances.size();
    }
    
    /**
     * Check if a material is configured for treasure spawning
     */
    public boolean isBlockConfigured(Material material) {
        return blockChances.containsKey(material);
    }
}