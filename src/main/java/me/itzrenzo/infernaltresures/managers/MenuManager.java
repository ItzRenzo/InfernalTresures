package me.itzrenzo.infernaltresures.managers;

import me.itzrenzo.infernaltresures.InfernalTresures;
import me.itzrenzo.infernaltresures.models.Rarity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

public class MenuManager {
    
    private final InfernalTresures plugin;
    private FileConfiguration biomeSelectionConfig;
    private FileConfiguration raritySelectionConfig;
    private FileConfiguration lootDisplayConfig;
    
    // Menu file names
    private static final String[] MENU_FILES = {
        "biome-selection.yml",
        "rarity-selection.yml", 
        "loot-display.yml"
    };
    
    public MenuManager(InfernalTresures plugin) {
        this.plugin = plugin;
        setupMenusFolder();
        loadMenuConfigs();
    }
    
    /**
     * Create menus folder and copy menu configuration files
     */
    private void setupMenusFolder() {
        File menusFolder = new File(plugin.getDataFolder(), "menus");
        
        // Create menus folder if it doesn't exist
        if (!menusFolder.exists()) {
            menusFolder.mkdirs();
            plugin.getLogger().info("Created menus folder.");
        }
        
        // Copy each menu file if it doesn't exist
        for (String fileName : MENU_FILES) {
            File menuFile = new File(menusFolder, fileName);
            
            if (!menuFile.exists()) {
                try {
                    InputStream inputStream = plugin.getResource("menus/" + fileName);
                    if (inputStream != null) {
                        Files.copy(inputStream, menuFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        plugin.getLogger().info("Created menu config: " + fileName);
                        inputStream.close();
                    } else {
                        plugin.getLogger().warning("Could not find menu resource: menus/" + fileName);
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to create menu config " + fileName + ": " + e.getMessage());
                }
            }
        }
    }
    
    private void loadMenuConfigs() {
        File menusFolder = new File(plugin.getDataFolder(), "menus");
        
        biomeSelectionConfig = YamlConfiguration.loadConfiguration(new File(menusFolder, "biome-selection.yml"));
        raritySelectionConfig = YamlConfiguration.loadConfiguration(new File(menusFolder, "rarity-selection.yml"));
        lootDisplayConfig = YamlConfiguration.loadConfiguration(new File(menusFolder, "loot-display.yml"));
        
        plugin.getLogger().info("Loaded menu configurations.");
    }
    
    public void reload() {
        loadMenuConfigs();
        plugin.getLogger().info("Menu configurations reloaded.");
    }
    
    // Biome Selection Menu Methods
    public Component getBiomeSelectionTitle() {
        String title = biomeSelectionConfig.getString("gui.title", "&6&lTreasure Biomes");
        return LegacyComponentSerializer.legacyAmpersand().deserialize(title);
    }
    
    public int getBiomeSelectionSize() {
        return biomeSelectionConfig.getInt("gui.size", 54);
    }
    
    public Material getBiomeMaterial(String biomeName) {
        ConfigurationSection biomeItems = biomeSelectionConfig.getConfigurationSection("biome-items");
        if (biomeItems != null && biomeItems.contains(biomeName.toLowerCase() + ".material")) {
            String materialName = biomeItems.getString(biomeName.toLowerCase() + ".material");
            try {
                return Material.valueOf(materialName.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material for biome " + biomeName + ": " + materialName);
            }
        }
        
        // Return default material based on biome
        return getDefaultBiomeMaterial(biomeName);
    }
    
    public String getBiomeDisplayName(String biomeName) {
        ConfigurationSection biomeItems = biomeSelectionConfig.getConfigurationSection("biome-items");
        if (biomeItems != null && biomeItems.contains(biomeName.toLowerCase() + ".display-name")) {
            return biomeItems.getString(biomeName.toLowerCase() + ".display-name");
        }
        
        // Use default format
        String format = biomeItems.getString("default.display-name-format", "&6&l{biome_name}");
        return format.replace("{biome_name}", formatBiomeName(biomeName));
    }
    
    public List<String> getBiomeLore(String biomeName) {
        ConfigurationSection biomeItems = biomeSelectionConfig.getConfigurationSection("biome-items");
        if (biomeItems != null && biomeItems.contains(biomeName.toLowerCase() + ".lore")) {
            return biomeItems.getStringList(biomeName.toLowerCase() + ".lore");
        }
        
        // Use default lore
        return biomeItems.getStringList("default.lore");
    }
    
    // Rarity Selection Menu Methods
    public Component getRaritySelectionTitle(String biomeName) {
        String title = raritySelectionConfig.getString("gui.title", "&6&lRarities in {biome_name}");
        title = title.replace("{biome_name}", formatBiomeName(biomeName));
        return LegacyComponentSerializer.legacyAmpersand().deserialize(title);
    }
    
    public int getRaritySelectionSize() {
        return raritySelectionConfig.getInt("gui.size", 27);
    }
    
    public Material getRarityMaterial(Rarity rarity) {
        ConfigurationSection rarityItems = raritySelectionConfig.getConfigurationSection("rarity-items");
        if (rarityItems != null && rarityItems.contains(rarity.name().toLowerCase() + ".material")) {
            String materialName = rarityItems.getString(rarity.name().toLowerCase() + ".material");
            try {
                return Material.valueOf(materialName.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material for rarity " + rarity + ": " + materialName);
            }
        }
        
        // Return default material based on rarity
        return getDefaultRarityMaterial(rarity);
    }
    
    public String getRarityDisplayName(Rarity rarity) {
        ConfigurationSection rarityItems = raritySelectionConfig.getConfigurationSection("rarity-items");
        if (rarityItems != null && rarityItems.contains(rarity.name().toLowerCase() + ".display-name")) {
            return rarityItems.getString(rarity.name().toLowerCase() + ".display-name");
        }
        
        // Use default format
        String format = rarityItems.getString("default.display-name-format", "{rarity_display_name}");
        return format.replace("{rarity_display_name}", rarity.getDisplayName());
    }
    
    public List<String> getRarityLore(Rarity rarity, int lootCount) {
        ConfigurationSection rarityItems = raritySelectionConfig.getConfigurationSection("rarity-items");
        List<String> lore;
        
        if (rarityItems != null && rarityItems.contains(rarity.name().toLowerCase() + ".lore")) {
            lore = rarityItems.getStringList(rarity.name().toLowerCase() + ".lore");
        } else {
            // Use default lore
            lore = rarityItems.getStringList("default.lore");
        }
        
        // Replace placeholders
        return lore.stream()
            .map(line -> line.replace("{rarity_display_name}", rarity.getDisplayName()))
            .map(line -> line.replace("{loot_count}", String.valueOf(lootCount)))
            .toList();
    }
    
    public int getRarityStartSlot() {
        return raritySelectionConfig.getInt("layout.rarity-start-slot", 10);
    }
    
    public int getRaritySlotSpacing() {
        return raritySelectionConfig.getInt("layout.rarity-slot-spacing", 2);
    }
    
    // Loot Display Menu Methods
    public Component getLootDisplayTitle(String biomeName, String rarityDisplayName) {
        String title = lootDisplayConfig.getString("gui.title", "{rarity_display_name} Loot - {biome_name}");
        title = title.replace("{biome_name}", formatBiomeName(biomeName))
                    .replace("{rarity_display_name}", rarityDisplayName);
        return LegacyComponentSerializer.legacyAmpersand().deserialize(title);
    }
    
    public int getLootDisplaySize() {
        return lootDisplayConfig.getInt("gui.size", 54);
    }
    
    public List<String> getLootAdditionalLore() {
        return lootDisplayConfig.getStringList("loot-display.additional-lore");
    }
    
    public int getMaxLootSlots() {
        return lootDisplayConfig.getInt("loot-display.max-loot-slots", 45);
    }
    
    // Loot Display Detail Configuration Methods
    public boolean shouldShowChance() {
        return lootDisplayConfig.getBoolean("loot-display.show-details.chance", true);
    }
    
    public boolean shouldShowAmountRange() {
        return lootDisplayConfig.getBoolean("loot-display.show-details.amount-range", true);
    }
    
    public boolean shouldShowSingleAmount() {
        return lootDisplayConfig.getBoolean("loot-display.show-details.single-amount", false);
    }
    
    public boolean shouldShowRequiredBlocks() {
        return lootDisplayConfig.getBoolean("loot-display.show-details.required-blocks", true);
    }
    
    public boolean shouldShowItemType() {
        return lootDisplayConfig.getBoolean("loot-display.show-details.item-type", true);
    }
    
    public boolean shouldShowBiomeSource() {
        return lootDisplayConfig.getBoolean("loot-display.show-details.biome-source", true);
    }
    
    // Loot Display Format Methods
    public String getChanceFormat() {
        return lootDisplayConfig.getString("loot-display.format.chance", "&7Chance: &f{chance}%");
    }
    
    public String getAmountRangeFormat() {
        return lootDisplayConfig.getString("loot-display.format.amount-range", "&7Amount: &f{min_amount} - {max_amount}");
    }
    
    public String getSingleAmountFormat() {
        return lootDisplayConfig.getString("loot-display.format.single-amount", "&7Amount: &f{amount}");
    }
    
    public String getRequiredBlocksFormat() {
        return lootDisplayConfig.getString("loot-display.format.required-blocks", "&7Required Blocks: &f{required_blocks}");
    }
    
    public String getNoRequirementFormat() {
        return lootDisplayConfig.getString("loot-display.format.no-requirement", "&7Required Blocks: &aNone");
    }
    
    public String getItemTypeFormat() {
        return lootDisplayConfig.getString("loot-display.format.item-type", "&7Type: &f{item_type}");
    }
    
    public String getBiomeSourceFormat() {
        return lootDisplayConfig.getString("loot-display.format.biome-source", "&7Found in: &f{biome_name}");
    }
    
    public String getRarityInfoFormat() {
        return lootDisplayConfig.getString("loot-display.format.rarity-info", "&7Rarity: {rarity_display_name}");
    }
    
    // Navigation Methods
    public Material getNavigationMaterial(String menuType, String buttonType) {
        String path = "navigation." + buttonType + ".material";
        String materialName = null;
        
        switch (menuType) {
            case "biome-selection" -> materialName = biomeSelectionConfig.getString(path);
            case "rarity-selection" -> materialName = raritySelectionConfig.getString(path);
            case "loot-display" -> materialName = lootDisplayConfig.getString(path);
        }
        
        if (materialName != null) {
            try {
                return Material.valueOf(materialName.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid navigation material: " + materialName);
            }
        }
        
        return Material.BARRIER; // Default fallback
    }
    
    public int getNavigationSlot(String menuType, String buttonType) {
        String path = "navigation." + buttonType + ".slot";
        
        return switch (menuType) {
            case "biome-selection" -> biomeSelectionConfig.getInt(path, 53);
            case "rarity-selection" -> raritySelectionConfig.getInt(path, 26);
            case "loot-display" -> lootDisplayConfig.getInt(path, 53);
            default -> 53;
        };
    }
    
    public String getNavigationDisplayName(String menuType, String buttonType) {
        String path = "navigation." + buttonType + ".display-name";
        
        return switch (menuType) {
            case "biome-selection" -> biomeSelectionConfig.getString(path, "&c&lClose");
            case "rarity-selection" -> raritySelectionConfig.getString(path, "&c&lClose");
            case "loot-display" -> lootDisplayConfig.getString(path, "&c&lClose");
            default -> "&c&lClose";
        };
    }
    
    public List<String> getNavigationLore(String menuType, String buttonType) {
        String path = "navigation." + buttonType + ".lore";
        
        return switch (menuType) {
            case "biome-selection" -> biomeSelectionConfig.getStringList(path);
            case "rarity-selection" -> raritySelectionConfig.getStringList(path);
            case "loot-display" -> lootDisplayConfig.getStringList(path);
            default -> List.of("&7Click to close this menu");
        };
    }
    
    // Helper Methods
    private Material getDefaultBiomeMaterial(String biomeName) {
        return switch (biomeName.toLowerCase()) {
            case "plains" -> Material.GRASS_BLOCK;
            case "desert" -> Material.SAND;
            case "forest" -> Material.OAK_LOG;
            case "ocean" -> Material.WATER_BUCKET;
            case "taiga" -> Material.SPRUCE_LOG;
            case "swamp" -> Material.LILY_PAD;
            case "jungle" -> Material.JUNGLE_LOG;
            case "savanna" -> Material.ACACIA_LOG;
            case "badlands" -> Material.TERRACOTTA;
            case "mountains" -> Material.STONE;
            case "nether" -> Material.NETHERRACK;
            case "end" -> Material.END_STONE;
            default -> Material.GRASS_BLOCK;
        };
    }
    
    private Material getDefaultRarityMaterial(Rarity rarity) {
        return switch (rarity) {
            case COMMON -> Material.IRON_INGOT;
            case RARE -> Material.GOLD_INGOT;
            case EPIC -> Material.DIAMOND;
            case LEGENDARY -> Material.EMERALD;
            case MYTHIC -> Material.NETHERITE_INGOT;
        };
    }
    
    private String formatBiomeName(String biomeName) {
        String name = biomeName.toLowerCase().replace("_", " ");
        String[] words = name.split(" ");
        StringBuilder formatted = new StringBuilder();
        
        for (String word : words) {
            if (formatted.length() > 0) formatted.append(" ");
            formatted.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
        }
        
        return formatted.toString();
    }
}