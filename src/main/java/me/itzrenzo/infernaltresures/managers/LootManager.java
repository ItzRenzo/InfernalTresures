package me.itzrenzo.infernaltresures.managers;

import me.itzrenzo.infernaltresures.InfernalTresures;
import me.itzrenzo.infernaltresures.models.Rarity;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class LootManager {

    private final InfernalTresures plugin;
    private final Map<String, YamlConfiguration> biomeConfigs;

    public LootManager(InfernalTresures plugin) {
        this.plugin = plugin;
        this.biomeConfigs = new HashMap<>();
        loadBiomeConfigs();
    }
    
    /**
     * Load all biome configuration files from the plugin data folder
     */
    private void loadBiomeConfigs() {
        File biomesFolder = new File(plugin.getDataFolder(), "biomes");
        
        if (!biomesFolder.exists() || !biomesFolder.isDirectory()) {
            plugin.getLogger().warning("Biomes folder not found! Make sure to regenerate your config.");
            return;
        }
        
        // List of biome files to load
        String[] biomeFiles = {"plains.yml", "desert.yml", "forest.yml", "ocean.yml"};
        
        for (String fileName : biomeFiles) {
            try {
                File configFile = new File(biomesFolder, fileName);
                if (configFile.exists()) {
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                    String biomeName = fileName.replace(".yml", "").toUpperCase();
                    biomeConfigs.put(biomeName, config);
                    plugin.getLogger().info("Loaded biome config: " + fileName);
                } else {
                    plugin.getLogger().warning("Biome config file not found: " + fileName);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error loading biome config " + fileName + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        plugin.getLogger().info("Loaded " + biomeConfigs.size() + " biome configurations");
    }
    
    /**
     * Reload all biome configurations
     */
    public void reload() {
        biomeConfigs.clear();
        loadBiomeConfigs();
    }

    /**
     * Generate loot for a specific biome and rarity
     */
    public List<ItemStack> generateLoot(Biome biome, Rarity rarity) {
        List<ItemStack> loot = new ArrayList<>();
        
        // Debug: Log what biome and rarity we're generating for
        plugin.getLogger().info("=== LOOT GENERATION DEBUG ===");
        plugin.getLogger().info("Generating loot for biome: " + biome + ", rarity: " + rarity);
        
        // Get the appropriate config for this biome
        YamlConfiguration config = getBiomeConfig(biome);
        
        if (config == null) {
            plugin.getLogger().warning("No config found for biome: " + biome + ", using fallback loot");
            return generateFallbackLoot(rarity);
        }
        
        plugin.getLogger().info("Using biome config for: " + getBiomeConfigName(biome));
        
        // Get loot items for this rarity
        List<Map<?, ?>> lootItems = config.getMapList("loot." + rarity.name());
        
        plugin.getLogger().info("Found " + lootItems.size() + " potential loot items for " + rarity);
        
        if (lootItems.isEmpty()) {
            plugin.getLogger().warning("No loot defined for " + rarity + " in " + biome + ", using fallback");
            return generateFallbackLoot(rarity);
        }
        
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        // Process each potential loot item
        for (int idx = 0; idx < lootItems.size(); idx++) {
            Map<?, ?> itemData = lootItems.get(idx);
            try {
                String materialName = (String) itemData.get("material");
                Object minAmountObj = itemData.get("min_amount");
                Object maxAmountObj = itemData.get("max_amount");
                Object chanceObj = itemData.get("chance");
                
                plugin.getLogger().info("Processing item " + (idx + 1) + ": " + materialName);
                
                // Handle different number types
                int minAmount = convertToInt(minAmountObj, 1);
                int maxAmount = convertToInt(maxAmountObj, 1);
                int chance = convertToInt(chanceObj, 100);
                
                plugin.getLogger().info("- Amount: " + minAmount + "-" + maxAmount + ", Chance: " + chance + "%");
                
                // Check if this item should drop based on chance
                int roll = random.nextInt(100);
                plugin.getLogger().info("- Rolled: " + roll + " (needed < " + chance + ")");
                
                if (roll < chance) {
                    Material material = Material.valueOf(materialName.toUpperCase());
                    int amount = random.nextInt(minAmount, maxAmount + 1);
                    
                    ItemStack item = new ItemStack(material, amount);
                    loot.add(item);
                    
                    plugin.getLogger().info("✓ Added loot: " + material + " x" + amount);
                } else {
                    plugin.getLogger().info("✗ Item didn't drop (failed chance roll)");
                }
                
            } catch (Exception e) {
                plugin.getLogger().warning("Error processing loot item " + (idx + 1) + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // If no items were generated, add fallback loot
        if (loot.isEmpty()) {
            plugin.getLogger().warning("No loot generated from config, using fallback for " + rarity);
            return generateFallbackLoot(rarity);
        }
        
        plugin.getLogger().info("=== FINAL RESULT: Generated " + loot.size() + " items ===");
        return loot;
    }
    
    /**
     * Convert various number types to int
     */
    private int convertToInt(Object obj, int defaultValue) {
        if (obj == null) return defaultValue;
        if (obj instanceof Integer) return (Integer) obj;
        if (obj instanceof Double) return ((Double) obj).intValue();
        if (obj instanceof String) {
            try {
                return Integer.parseInt((String) obj);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    /**
     * Get the appropriate biome config based on the actual biome
     */
    private YamlConfiguration getBiomeConfig(Biome biome) {
        String biomeName = biome.name().toLowerCase();
        
        // Map similar biomes to our config files
        if (biomeName.contains("plains") || biomeName.contains("meadow") || biomeName.contains("sunflower")) {
            return biomeConfigs.get("PLAINS");
        }
        else if (biomeName.contains("desert")) {
            return biomeConfigs.get("DESERT");
        }
        else if (biomeName.contains("forest") || biomeName.contains("jungle") || biomeName.contains("taiga") || 
                 biomeName.contains("birch") || biomeName.contains("dark_forest")) {
            return biomeConfigs.get("FOREST");
        }
        else if (biomeName.contains("ocean") || biomeName.contains("beach") || biomeName.contains("river") || 
                 biomeName.contains("lake")) {
            return biomeConfigs.get("OCEAN");
        }
        
        // Default to plains if no match found
        return biomeConfigs.get("PLAINS");
    }
    
    /**
     * Generate fallback loot if config loading fails
     */
    private List<ItemStack> generateFallbackLoot(Rarity rarity) {
        List<ItemStack> fallback = new ArrayList<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        switch (rarity) {
            case COMMON -> {
                fallback.add(new ItemStack(Material.COAL, random.nextInt(3, 8)));
                fallback.add(new ItemStack(Material.STICK, random.nextInt(2, 6)));
            }
            case RARE -> {
                fallback.add(new ItemStack(Material.IRON_INGOT, random.nextInt(2, 6)));
                fallback.add(new ItemStack(Material.GOLD_INGOT, random.nextInt(1, 4)));
            }
            case EPIC -> {
                fallback.add(new ItemStack(Material.DIAMOND, random.nextInt(1, 4)));
                fallback.add(new ItemStack(Material.EMERALD, random.nextInt(2, 6)));
            }
            case LEGENDARY -> {
                fallback.add(new ItemStack(Material.DIAMOND, random.nextInt(3, 8)));
                fallback.add(new ItemStack(Material.NETHERITE_INGOT, random.nextInt(1, 2)));
            }
            case MYTHIC -> {
                fallback.add(new ItemStack(Material.NETHERITE_INGOT, random.nextInt(2, 5)));
                fallback.add(new ItemStack(Material.NETHER_STAR, 1));
            }
        }
        
        return fallback;
    }
    
    /**
     * Get the biome config name being used
     */
    private String getBiomeConfigName(Biome biome) {
        String biomeName = biome.name().toLowerCase();
        
        if (biomeName.contains("plains") || biomeName.contains("meadow") || biomeName.contains("sunflower")) {
            return "PLAINS";
        }
        else if (biomeName.contains("desert")) {
            return "DESERT";
        }
        else if (biomeName.contains("forest") || biomeName.contains("jungle") || biomeName.contains("taiga") || 
                 biomeName.contains("birch") || biomeName.contains("dark_forest")) {
            return "FOREST";
        }
        else if (biomeName.contains("ocean") || biomeName.contains("beach") || biomeName.contains("river") || 
                 biomeName.contains("lake")) {
            return "OCEAN";
        }
        
        return "PLAINS (default)";
    }
}