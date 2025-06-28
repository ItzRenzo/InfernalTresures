package me.itzrenzo.infernaltresures.managers;

import me.itzrenzo.infernaltresures.InfernalTresures;
import me.itzrenzo.infernaltresures.models.Rarity;
import me.itzrenzo.infernaltresures.models.BiomeCategory;
import me.itzrenzo.infernaltresures.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class LootManager {
    
    private final InfernalTresures plugin;
    private final Map<Biome, Map<Rarity, List<LootItem>>> lootTables = new HashMap<>();
    private final Map<String, BiomeCategory> biomeCategories = new HashMap<>();
    private final Map<Biome, BiomeCategory> biomeToCategory = new HashMap<>();
    
    public LootManager(InfernalTresures plugin) {
        this.plugin = plugin;
        loadLootTables();
    }
    
    private void loadLootTables() {
        File biomesFolder = new File(plugin.getDataFolder(), "biomes");
        
        if (!biomesFolder.exists()) {
            plugin.getLogger().warning("Biomes folder does not exist!");
            return;
        }
        
        File[] biomeFiles = biomesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (biomeFiles == null) {
            plugin.getLogger().warning("No biome files found!");
            return;
        }
        
        // Clear existing data
        lootTables.clear();
        biomeCategories.clear();
        biomeToCategory.clear();
        
        for (File biomeFile : biomeFiles) {
            loadBiomeLootTable(biomeFile);
        }
        
        plugin.getLogger().info("Loaded " + biomeCategories.size() + " biome categories with loot tables for " + lootTables.size() + " biomes");
    }
    
    private void loadBiomeLootTable(File biomeFile) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(biomeFile);
        
        // Get category information from the config
        String categoryName = config.getString("name");
        String description = config.getString("description", "");
        String materialName = config.getString("material");
        List<String> biomeNames = config.getStringList("biomes");
        
        if (categoryName == null) {
            plugin.getLogger().warning("Missing 'name' field in biome file: " + biomeFile.getName());
            return;
        }
        
        // Parse material
        Material material;
        if (materialName != null) {
            try {
                material = Material.valueOf(materialName.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material '" + materialName + "' in biome file: " + biomeFile.getName());
                material = getDefaultBiomeMaterial(categoryName);
            }
        } else {
            material = getDefaultBiomeMaterial(categoryName);
        }
        
        // Parse biomes list
        List<Biome> biomes = new ArrayList<>();
        if (biomeNames != null && !biomeNames.isEmpty()) {
            for (String biomeName : biomeNames) {
                try {
                    Biome biome = Biome.valueOf(biomeName.toUpperCase());
                    biomes.add(biome);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid biome '" + biomeName + "' in biome file: " + biomeFile.getName());
                }
            }
        } else {
            // Fallback to old system - try to get biome from filename
            String fileName = biomeFile.getName().replace(".yml", "");
            Biome biome = getBiomeFromFileName(fileName);
            if (biome != null) {
                biomes.add(biome);
            }
        }
        
        if (biomes.isEmpty()) {
            plugin.getLogger().warning("No valid biomes found in biome file: " + biomeFile.getName());
            return;
        }
        
        // Create biome category
        String fileName = biomeFile.getName().replace(".yml", "");
        BiomeCategory category = new BiomeCategory(categoryName, description, material, biomes, fileName);
        biomeCategories.put(fileName, category);
        
        // Map each biome to this category
        for (Biome biome : biomes) {
            biomeToCategory.put(biome, category);
        }
        
        // Load loot tables
        Map<Rarity, List<LootItem>> biomeLootTable = new HashMap<>();
        
        ConfigurationSection lootSection = config.getConfigurationSection("loot");
        if (lootSection != null) {
            for (Rarity rarity : Rarity.values()) {
                List<LootItem> rarityLoot = new ArrayList<>();
                
                // Get the list of items for this rarity
                List<Map<?, ?>> itemList = lootSection.getMapList(rarity.name());
                if (itemList != null && !itemList.isEmpty()) {
                    for (Map<?, ?> itemMap : itemList) {
                        LootItem lootItem = parseLootItemFromMap(itemMap);
                        if (lootItem != null) {
                            rarityLoot.add(lootItem);
                        }
                    }
                }
                
                biomeLootTable.put(rarity, rarityLoot);
            }
        }
        
        // Apply the same loot table to all biomes in this category
        for (Biome biome : biomes) {
            lootTables.put(biome, biomeLootTable);
        }
        
        plugin.getLogger().info("Loaded biome category '" + categoryName + "' for " + biomes.size() + " biomes with " + 
            biomeLootTable.values().stream().mapToInt(List::size).sum() + " total items");
    }
    
    /**
     * Get the biome category for a given biome
     */
    public BiomeCategory getBiomeCategory(Biome biome) {
        return biomeToCategory.get(biome);
    }
    
    /**
     * Get all biome categories
     */
    public Collection<BiomeCategory> getBiomeCategories() {
        return biomeCategories.values();
    }
    
    /**
     * Get biome category by file name
     */
    public BiomeCategory getBiomeCategoryByFileName(String fileName) {
        return biomeCategories.get(fileName);
    }
    
    /**
     * Get default material for a biome category based on its name
     */
    private Material getDefaultBiomeMaterial(String categoryName) {
        return switch (categoryName.toLowerCase()) {
            case "plains" -> Material.GRASS_BLOCK;
            case "desert" -> Material.SAND;
            case "forest" -> Material.OAK_LOG;
            case "ocean" -> Material.WATER_BUCKET;
            case "taiga" -> Material.SPRUCE_LOG;
            case "swamp" -> Material.LILY_PAD;
            case "jungle" -> Material.JUNGLE_LOG;
            case "savanna" -> Material.ACACIA_LOG;
            case "badlands" -> Material.TERRACOTTA;
            case "mountains", "windswept" -> Material.STONE;
            case "nether" -> Material.NETHERRACK;
            case "end" -> Material.END_STONE;
            default -> Material.GRASS_BLOCK;
        };
    }
    
    private LootItem parseLootItemFromMap(Map<?, ?> itemMap) {
        try {
            LootItem item = new LootItem();
            
            // Check if this is an ExecutableItem first
            String executableId = (String) itemMap.get("executable_id");
            
            if (executableId != null) {
                // This is an ExecutableItem
                item.isExecutableItem = true;
                item.executableId = executableId;
                
                // Verify ExecutableItem exists (only if ExecutableItems integration is enabled)
                if (InfernalTresures.getInstance().getExecutableItemsIntegration().isEnabled()) {
                    if (!InfernalTresures.getInstance().getExecutableItemsIntegration().isValidExecutableItem(executableId)) {
                        plugin.getLogger().warning("Invalid ExecutableItem: " + executableId);
                        return null;
                    }
                } else {
                    plugin.getLogger().warning("ExecutableItems not available, skipping ExecutableItem: " + executableId);
                    return null;
                }
            } else {
                // Check if this is an ExecutableBlock
                String executableBlockId = (String) itemMap.get("executable_block_id");
                
                if (executableBlockId != null) {
                    // This is an ExecutableBlock
                    item.isExecutableBlock = true;
                    item.executableBlockId = executableBlockId;
                    
                    // Verify ExecutableBlock exists (only if ExecutableBlocks integration is enabled)
                    if (InfernalTresures.getInstance().getExecutableBlocksIntegration().isEnabled()) {
                        if (!InfernalTresures.getInstance().getExecutableBlocksIntegration().isValidExecutableBlock(executableBlockId)) {
                            plugin.getLogger().warning("Invalid ExecutableBlock: " + executableBlockId);
                            return null;
                        }
                    } else {
                        plugin.getLogger().warning("ExecutableBlocks not available, skipping ExecutableBlock: " + executableBlockId);
                        return null;
                    }
                } else {
                    // Check if this is an MMOItem
                    String mmoType = (String) itemMap.get("mmo_type");
                    String mmoId = (String) itemMap.get("mmo_id");
                    
                    if (mmoType != null && mmoId != null) {
                        // This is an MMOItem
                        item.isMMOItem = true;
                        item.mmoType = mmoType;
                        item.mmoId = mmoId;
                        
                        // Verify MMOItem exists (only if MMOItems integration is enabled)
                        if (InfernalTresures.getInstance().getMMOItemsIntegration().isEnabled()) {
                            if (!InfernalTresures.getInstance().getMMOItemsIntegration().isValidMMOItem(mmoType, mmoId)) {
                                plugin.getLogger().warning("Invalid MMOItem: " + mmoType + "." + mmoId);
                                return null;
                            }
                        } else {
                            plugin.getLogger().warning("MMOItems not available, skipping MMOItem: " + mmoType + "." + mmoId);
                            return null;
                        }
                    } else {
                        // Regular Bukkit material
                        String materialName = (String) itemMap.get("material");
                        if (materialName == null) {
                            plugin.getLogger().warning("Item missing 'material', 'mmo_type'/'mmo_id', 'executable_id', or 'executable_block_id' fields");
                            return null;
                        }
                        
                        try {
                            Material material = Material.valueOf(materialName.toUpperCase());
                            item.material = material;
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Unknown material: " + materialName);
                            return null;
                        }
                    }
                }
            }
            
            // Safe casting for numeric values
            Object minAmountObj = itemMap.get("min_amount");
            item.minAmount = minAmountObj instanceof Number ? ((Number) minAmountObj).intValue() : 1;
            
            Object maxAmountObj = itemMap.get("max_amount");
            item.maxAmount = maxAmountObj instanceof Number ? ((Number) maxAmountObj).intValue() : item.minAmount;
            
            Object chanceObj = itemMap.get("chance");
            item.chance = chanceObj instanceof Number ? ((Number) chanceObj).doubleValue() : 100.0;
            
            item.displayName = (String) itemMap.get("display_name");
            
            // Handle lore - it might be a list of strings
            Object loreObj = itemMap.get("lore");
            if (loreObj instanceof List<?>) {
                item.lore = new ArrayList<>();
                for (Object loreItem : (List<?>) loreObj) {
                    if (loreItem instanceof String) {
                        item.lore.add((String) loreItem);
                    }
                }
            }
            
            Object unbreakableObj = itemMap.get("unbreakable");
            item.unbreakable = unbreakableObj instanceof Boolean ? (Boolean) unbreakableObj : false;
            
            Object customModelDataObj = itemMap.get("custom_model_data");
            item.customModelData = customModelDataObj instanceof Number ? ((Number) customModelDataObj).intValue() : -1;
            
            // Parse enchantments (only for regular items, MMOItems handle their own enchantments)
            if (!item.isMMOItem) {
                Object enchantmentsObj = itemMap.get("enchantments");
                if (enchantmentsObj instanceof List<?>) {
                    item.enchantments = new ArrayList<>();
                    for (Object enchantObj : (List<?>) enchantmentsObj) {
                        if (enchantObj instanceof Map<?, ?>) {
                            Map<?, ?> enchantMap = (Map<?, ?>) enchantObj;
                            EnchantmentData enchantData = new EnchantmentData();
                            enchantData.enchantment = (String) enchantMap.get("enchant");
                            
                            Object levelObj = enchantMap.get("level");
                            enchantData.level = levelObj instanceof Number ? ((Number) levelObj).intValue() : 1;
                            
                            Object minLevelObj = enchantMap.get("min_level");
                            enchantData.minLevel = minLevelObj instanceof Number ? ((Number) minLevelObj).intValue() : enchantData.level;
                            
                            Object maxLevelObj = enchantMap.get("max_level");
                            enchantData.maxLevel = maxLevelObj instanceof Number ? ((Number) maxLevelObj).intValue() : enchantData.level;
                            
                            item.enchantments.add(enchantData);
                        }
                    }
                }
                
                // Parse attributes (only for regular items)
                Object attributesObj = itemMap.get("attributes");
                if (attributesObj instanceof List<?>) {
                    item.attributes = new ArrayList<>();
                    for (Object attributeObj : (List<?>) attributesObj) {
                        if (attributeObj instanceof Map<?, ?>) {
                            Map<?, ?> attributeMap = (Map<?, ?>) attributeObj;
                            AttributeData attributeData = new AttributeData();
                            attributeData.attribute = (String) attributeMap.get("attribute");
                            
                            Object valueObj = attributeMap.get("value");
                            attributeData.value = valueObj instanceof Number ? ((Number) valueObj).doubleValue() : 0.0;
                            
                            Object operationObj = attributeMap.get("operation");
                            attributeData.operation = operationObj instanceof String ? (String) operationObj : "ADD_NUMBER";
                            
                            Object slotObj = attributeMap.get("slot");
                            attributeData.slot = slotObj instanceof String ? (String) slotObj : "HAND";
                            
                            item.attributes.add(attributeData);
                        }
                    }
                }
                
                // Parse custom effects (only for regular items)
                Object customEffectsObj = itemMap.get("custom_effects");
                if (customEffectsObj instanceof List<?>) {
                    item.customEffects = new ArrayList<>();
                    for (Object effectObj : (List<?>) customEffectsObj) {
                        if (effectObj instanceof Map<?, ?>) {
                            Map<?, ?> effectMap = (Map<?, ?>) effectObj;
                            CustomEffectData effectData = new CustomEffectData();
                            effectData.effect = (String) effectMap.get("effect");
                            
                            Object durationObj = effectMap.get("duration");
                            effectData.duration = durationObj instanceof Number ? ((Number) durationObj).intValue() : 600;
                            
                            Object amplifierObj = effectMap.get("amplifier");
                            effectData.amplifier = amplifierObj instanceof Number ? ((Number) amplifierObj).intValue() : 0;
                            
                            item.customEffects.add(effectData);
                        }
                    }
                }
            }
            
            // Parse progression requirement
            Object requiredBlocksObj = itemMap.get("required_blocks_mined");
            if (requiredBlocksObj instanceof Number) {
                // Old format: single number (backward compatibility)
                long requiredBlocks = ((Number) requiredBlocksObj).longValue();
                item.minRequiredBlocksMined = requiredBlocks;
                item.maxRequiredBlocksMined = Long.MAX_VALUE;
                item.requiredBlocksMinedRange = String.valueOf(requiredBlocks) + "+";
            } else if (requiredBlocksObj instanceof String) {
                // New format: range string like "0-999" or "1000-4999"
                String rangeStr = (String) requiredBlocksObj;
                item.requiredBlocksMinedRange = rangeStr;
                
                if (rangeStr.contains("-")) {
                    String[] parts = rangeStr.split("-", 2);
                    try {
                        item.minRequiredBlocksMined = Long.parseLong(parts[0].trim());
                        item.maxRequiredBlocksMined = Long.parseLong(parts[1].trim());
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("Invalid required_blocks_mined range format: " + rangeStr + ". Expected format: 'min-max' (e.g., '0-999')");
                        item.minRequiredBlocksMined = 0;
                        item.maxRequiredBlocksMined = Long.MAX_VALUE;
                        item.requiredBlocksMinedRange = "0+";
                    }
                } else {
                    // Single number as string
                    try {
                        long requiredBlocks = Long.parseLong(rangeStr);
                        item.minRequiredBlocksMined = requiredBlocks;
                        item.maxRequiredBlocksMined = Long.MAX_VALUE;
                        item.requiredBlocksMinedRange = requiredBlocks + "+";
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("Invalid required_blocks_mined format: " + rangeStr);
                        item.minRequiredBlocksMined = 0;
                        item.maxRequiredBlocksMined = Long.MAX_VALUE;
                        item.requiredBlocksMinedRange = "0+";
                    }
                }
            } else {
                // Default: no requirement
                item.minRequiredBlocksMined = 0;
                item.maxRequiredBlocksMined = Long.MAX_VALUE;
                item.requiredBlocksMinedRange = "0+";
            }
            
            // Parse Shulker Box items if applicable
            Object shulkerBoxObj = itemMap.get("shulker_box_items");
            if (shulkerBoxObj instanceof List<?>) {
                item.isShulkerBox = true;
                item.shulkerBoxItems = new ArrayList<>();
                for (Object shulkerItemObj : (List<?>) shulkerBoxObj) {
                    if (shulkerItemObj instanceof Map<?, ?>) {
                        Map<?, ?> shulkerItemMap = (Map<?, ?>) shulkerItemObj;
                        ShulkerBoxItem shulkerItem = new ShulkerBoxItem();
                        
                        // Parse material
                        String materialName = (String) shulkerItemMap.get("material");
                        if (materialName != null) {
                            try {
                                shulkerItem.material = Material.valueOf(materialName.toUpperCase());
                            } catch (IllegalArgumentException e) {
                                plugin.getLogger().warning("Invalid material '" + materialName + "' for Shulker Box item");
                            }
                        }
                        
                        // Parse min_amount, max_amount, and chance (like regular loot items)
                        Object shulkerMinAmountObj = shulkerItemMap.get("min_amount");
                        shulkerItem.minAmount = shulkerMinAmountObj instanceof Number ? ((Number) shulkerMinAmountObj).intValue() : 1;
                        
                        Object shulkerMaxAmountObj = shulkerItemMap.get("max_amount");
                        shulkerItem.maxAmount = shulkerMaxAmountObj instanceof Number ? ((Number) shulkerMaxAmountObj).intValue() : shulkerItem.minAmount;
                        
                        Object shulkerChanceObj = shulkerItemMap.get("chance");
                        shulkerItem.chance = shulkerChanceObj instanceof Number ? ((Number) shulkerChanceObj).doubleValue() : 100.0;
                        
                        // Parse legacy amount field for backward compatibility
                        Object amountObj = shulkerItemMap.get("amount");
                        if (amountObj instanceof Number && shulkerMinAmountObj == null && shulkerMaxAmountObj == null) {
                            // Only use legacy amount if min/max not specified
                            int legacyAmount = ((Number) amountObj).intValue();
                            shulkerItem.minAmount = legacyAmount;
                            shulkerItem.maxAmount = legacyAmount;
                        }
                        
                        // Parse progression requirement (same as main loot items)
                        Object shulkerRequiredBlocksObj = shulkerItemMap.get("required_blocks_mined");
                        if (shulkerRequiredBlocksObj instanceof Number) {
                            long requiredBlocks = ((Number) shulkerRequiredBlocksObj).longValue();
                            shulkerItem.minRequiredBlocksMined = requiredBlocks;
                            shulkerItem.maxRequiredBlocksMined = Long.MAX_VALUE;
                            shulkerItem.requiredBlocksMinedRange = String.valueOf(requiredBlocks) + "+";
                        } else if (shulkerRequiredBlocksObj instanceof String) {
                            String rangeStr = (String) shulkerRequiredBlocksObj;
                            shulkerItem.requiredBlocksMinedRange = rangeStr;
                            
                            if (rangeStr.contains("-")) {
                                String[] parts = rangeStr.split("-", 2);
                                try {
                                    shulkerItem.minRequiredBlocksMined = Long.parseLong(parts[0].trim());
                                    shulkerItem.maxRequiredBlocksMined = Long.parseLong(parts[1].trim());
                                } catch (NumberFormatException e) {
                                    plugin.getLogger().warning("Invalid required_blocks_mined format for Shulker Box item: " + rangeStr);
                                    shulkerItem.minRequiredBlocksMined = 0;
                                    shulkerItem.maxRequiredBlocksMined = Long.MAX_VALUE;
                                    shulkerItem.requiredBlocksMinedRange = "0+";
                                }
                            } else {
                                try {
                                    long requiredBlocks = Long.parseLong(rangeStr);
                                    shulkerItem.minRequiredBlocksMined = requiredBlocks;
                                    shulkerItem.maxRequiredBlocksMined = Long.MAX_VALUE;
                                    shulkerItem.requiredBlocksMinedRange = requiredBlocks + "+";
                                } catch (NumberFormatException e) {
                                    plugin.getLogger().warning("Invalid required_blocks_mined format for Shulker Box item: " + rangeStr);
                                    shulkerItem.minRequiredBlocksMined = 0;
                                    shulkerItem.maxRequiredBlocksMined = Long.MAX_VALUE;
                                    shulkerItem.requiredBlocksMinedRange = "0+";
                                }
                            }
                        }
                        
                        // Parse display name
                        shulkerItem.displayName = (String) shulkerItemMap.get("display_name");
                        
                        // Parse lore
                        Object shulkerLoreObj = shulkerItemMap.get("lore");
                        if (shulkerLoreObj instanceof List<?>) {
                            shulkerItem.lore = new ArrayList<>();
                            for (Object loreItem : (List<?>) shulkerLoreObj) {
                                if (loreItem instanceof String) {
                                    shulkerItem.lore.add((String) loreItem);
                                }
                            }
                        }
                        
                        // Check for ExecutableItem
                        String shulkerExecutableId = (String) shulkerItemMap.get("executable_id");
                        if (shulkerExecutableId != null) {
                            shulkerItem.isExecutableItem = true;
                            shulkerItem.executableId = shulkerExecutableId;
                        }
                        
                        // Check for ExecutableBlock
                        String shulkerExecutableBlockId = (String) shulkerItemMap.get("executable_block_id");
                        if (shulkerExecutableBlockId != null) {
                            shulkerItem.isExecutableBlock = true;
                            shulkerItem.executableBlockId = shulkerExecutableBlockId;
                        }
                        
                        // Check for MMOItem
                        String shulkerMmoType = (String) shulkerItemMap.get("mmo_type");
                        String shulkerMmoId = (String) shulkerItemMap.get("mmo_id");
                        if (shulkerMmoType != null && shulkerMmoId != null) {
                            shulkerItem.isMMOItem = true;
                            shulkerItem.mmoType = shulkerMmoType;
                            shulkerItem.mmoId = shulkerMmoId;
                        }
                        
                        item.shulkerBoxItems.add(shulkerItem);
                    }
                }
            }
            
            return item;
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse loot item: " + e.getMessage());
            return null;
        }
    }
    
    public List<ItemStack> generateLoot(Biome biome, Rarity rarity, org.bukkit.entity.Player player) {
        List<ItemStack> loot = new ArrayList<>();
        
        Map<Rarity, List<LootItem>> biomeLootTable = lootTables.get(biome);
        if (biomeLootTable == null) {
            plugin.getLogger().warning("No loot table found for biome: " + biome.name());
            return loot;
        }
        
        List<LootItem> rarityLoot = biomeLootTable.get(rarity);
        if (rarityLoot == null || rarityLoot.isEmpty()) {
            plugin.getLogger().warning("No loot found for rarity " + rarity + " in biome " + biome.name());
            return loot;
        }
        
        // Get player's total blocks mined for progression check
        long playerBlocksMined = 0;
        UUID playerUUID = null;
        if (player != null) {
            playerUUID = player.getUniqueId();
            // Use UUID to get stats to ensure we get the most current data
            playerBlocksMined = plugin.getStatsManager().getPlayerStats(playerUUID).totalBlocksMined;
        }
        
        // Get current progression level and slot count
        int maxSlots = plugin.getConfigManager().getCurrentProgressionSlots();
        
        if (plugin.getConfigManager().isProgressionDebugEnabled()) {
            plugin.getLogger().info("=== LOOT PROGRESSION DEBUG ===");
            plugin.getLogger().info("Player: " + (player != null ? player.getName() + " (UUID: " + playerUUID + ")" : "unknown"));
            plugin.getLogger().info("Player blocks mined: " + playerBlocksMined);
            plugin.getLogger().info("Current progression level: " + plugin.getConfigManager().getCurrentProgressionLevel());
            plugin.getLogger().info("Max slots to fill: " + maxSlots);
            plugin.getLogger().info("Available loot items for " + rarity + ": " + rarityLoot.size());
        }
        
        // Filter items based on progression requirements
        List<LootItem> availableItems = new ArrayList<>();
        for (LootItem lootItem : rarityLoot) {
            // Check if player is within the required blocks mined range
            if (playerBlocksMined < lootItem.minRequiredBlocksMined || playerBlocksMined > lootItem.maxRequiredBlocksMined) {
                if (plugin.getConfigManager().isProgressionDebugEnabled()) {
                    plugin.getLogger().info("Player " + (player != null ? player.getName() : "unknown") + 
                        " doesn't meet progression requirement: " + playerBlocksMined + " blocks mined not in range " + 
                        lootItem.requiredBlocksMinedRange + " for item " + (lootItem.material != null ? lootItem.material.name() : 
                        lootItem.isMMOItem ? lootItem.mmoType + "." + lootItem.mmoId : lootItem.executableId));
                }
                continue; // Skip this item
            }
            availableItems.add(lootItem);
        }
        
        if (availableItems.isEmpty()) {
            if (plugin.getConfigManager().isProgressionDebugEnabled()) {
                plugin.getLogger().info("No available items after progression filtering");
            }
            return loot;
        }
        
        // Roll for each slot
        for (int slot = 0; slot < maxSlots; slot++) {
            // Randomly select an item from available items
            LootItem selectedItem = availableItems.get(ThreadLocalRandom.current().nextInt(availableItems.size()));
            
            // Check if we should use chance-based system or guarantee fills
            boolean useChanceSystem = plugin.getConfigManager().useChanceBasedSystem();
            
            boolean shouldAddItem = true;
            if (useChanceSystem) {
                // Apply the item's individual chance
                shouldAddItem = ThreadLocalRandom.current().nextDouble(100.0) < selectedItem.chance;
            }
            // If not using chance system, shouldAddItem remains true (guaranteed fill)
            
            if (shouldAddItem) {
                ItemStack itemStack = createItemStack(selectedItem, player);
                if (itemStack != null) {
                    loot.add(itemStack);
                    
                    if (plugin.getConfigManager().isProgressionDebugEnabled()) {
                        plugin.getLogger().info("Slot " + (slot + 1) + ": Added " + itemStack.getType() + 
                            " x" + itemStack.getAmount() + (useChanceSystem ? " (chance: " + selectedItem.chance + "%)" : " (guaranteed)"));
                    }
                } else {
                    if (plugin.getConfigManager().isProgressionDebugEnabled()) {
                        plugin.getLogger().info("Slot " + (slot + 1) + ": Failed to create item");
                    }
                }
            } else {
                if (plugin.getConfigManager().isProgressionDebugEnabled()) {
                    plugin.getLogger().info("Slot " + (slot + 1) + ": No item (chance missed: " + selectedItem.chance + "%)");
                }
            }
        }
        
        if (plugin.getConfigManager().isProgressionDebugEnabled()) {
            plugin.getLogger().info("Final loot count: " + loot.size() + "/" + maxSlots + " slots filled");
            plugin.getLogger().info("=== END LOOT PROGRESSION DEBUG ===");
        }
        
        return loot;
    }
    
    /**
     * Generate loot for a player using their UUID (works even if player is offline)
     */
    public List<ItemStack> generateLootByUUID(Biome biome, Rarity rarity, UUID playerUUID) {
        List<ItemStack> loot = new ArrayList<>();
        
        Map<Rarity, List<LootItem>> biomeLootTable = lootTables.get(biome);
        if (biomeLootTable == null) {
            plugin.getLogger().warning("No loot table found for biome: " + biome.name());
            return loot;
        }
        
        List<LootItem> rarityLoot = biomeLootTable.get(rarity);
        if (rarityLoot == null || rarityLoot.isEmpty()) {
            plugin.getLogger().warning("No loot found for rarity " + rarity + " in biome " + biome.name());
            return loot;
        }
        
        // Get player's total blocks mined for progression check using UUID
        long playerBlocksMined = 0;
        if (playerUUID != null) {
            playerBlocksMined = plugin.getStatsManager().getPlayerStats(playerUUID).totalBlocksMined;
        }
        
        // Get current progression level and slot count
        int maxSlots = plugin.getConfigManager().getCurrentProgressionSlots();
        
        if (plugin.getConfigManager().isProgressionDebugEnabled()) {
            plugin.getLogger().info("=== LOOT PROGRESSION DEBUG (UUID) ===");
            plugin.getLogger().info("Player UUID: " + playerUUID);
            plugin.getLogger().info("Player blocks mined: " + playerBlocksMined);
            plugin.getLogger().info("Current progression level: " + plugin.getConfigManager().getCurrentProgressionLevel());
            plugin.getLogger().info("Max slots to fill: " + maxSlots);
            plugin.getLogger().info("Available loot items for " + rarity + ": " + rarityLoot.size());
        }
        
        // Filter items based on progression requirements
        List<LootItem> availableItems = new ArrayList<>();
        for (LootItem lootItem : rarityLoot) {
            // Check if player is within the required blocks mined range
            if (playerBlocksMined < lootItem.minRequiredBlocksMined || playerBlocksMined > lootItem.maxRequiredBlocksMined) {
                if (plugin.getConfigManager().isProgressionDebugEnabled()) {
                    plugin.getLogger().info("Player " + playerUUID + 
                        " doesn't meet progression requirement: " + playerBlocksMined + " blocks mined not in range " + 
                        lootItem.requiredBlocksMinedRange + " for item " + (lootItem.material != null ? lootItem.material.name() : 
                        lootItem.isMMOItem ? lootItem.mmoType + "." + lootItem.mmoId : lootItem.executableId));
                }
                continue; // Skip this item
            }
            availableItems.add(lootItem);
        }
        
        if (availableItems.isEmpty()) {
            if (plugin.getConfigManager().isProgressionDebugEnabled()) {
                plugin.getLogger().info("No available items after progression filtering");
            }
            return loot;
        }
        
        // Roll for each slot
        for (int slot = 0; slot < maxSlots; slot++) {
            // Randomly select an item from available items
            LootItem selectedItem = availableItems.get(ThreadLocalRandom.current().nextInt(availableItems.size()));
            
            // Check if we should use chance-based system or guarantee fills
            boolean useChanceSystem = plugin.getConfigManager().useChanceBasedSystem();
            
            boolean shouldAddItem = true;
            if (useChanceSystem) {
                // Apply the item's individual chance
                shouldAddItem = ThreadLocalRandom.current().nextDouble(100.0) < selectedItem.chance;
            }
            // If not using chance system, shouldAddItem remains true (guaranteed fill)
            
            if (shouldAddItem) {
                ItemStack itemStack = createItemStack(selectedItem, null); // Pass null for UUID-based generation
                if (itemStack != null) {
                    loot.add(itemStack);
                    
                    if (plugin.getConfigManager().isProgressionDebugEnabled()) {
                        plugin.getLogger().info("Slot " + (slot + 1) + ": Added " + itemStack.getType() + 
                            " x" + itemStack.getAmount() + (useChanceSystem ? " (chance: " + selectedItem.chance + "%)" : " (guaranteed)"));
                    }
                } else {
                    if (plugin.getConfigManager().isProgressionDebugEnabled()) {
                        plugin.getLogger().info("Slot " + (slot + 1) + ": Failed to create item");
                    }
                }
            } else {
                if (plugin.getConfigManager().isProgressionDebugEnabled()) {
                    plugin.getLogger().info("Slot " + (slot + 1) + ": No item (chance missed: " + selectedItem.chance + "%)");
                }
            }
        }
        
        if (plugin.getConfigManager().isProgressionDebugEnabled()) {
            plugin.getLogger().info("Final loot count: " + loot.size() + "/" + maxSlots + " slots filled");
            plugin.getLogger().info("=== END LOOT PROGRESSION DEBUG (UUID) ===");
        }
        
        return loot;
    }
    
    /**
     * Get all possible loot items for display in GUI (without applying chances or random amounts)
     */
    public List<LootItemDisplay> getAllLootItems(Biome biome, Rarity rarity) {
        List<LootItemDisplay> displayItems = new ArrayList<>();
        
        Map<Rarity, List<LootItem>> biomeLootTable = lootTables.get(biome);
        if (biomeLootTable == null) {
            plugin.getLogger().warning("No loot table found for biome: " + biome.name());
            return displayItems;
        }
        
        List<LootItem> rarityLoot = biomeLootTable.get(rarity);
        if (rarityLoot == null || rarityLoot.isEmpty()) {
            plugin.getLogger().warning("No loot found for rarity " + rarity + " in biome " + biome.name());
            return displayItems;
        }
        
        // Create display items for each loot item
        for (LootItem lootItem : rarityLoot) {
            ItemStack itemStack = createDisplayItemStack(lootItem);
            if (itemStack != null) {
                LootItemDisplay displayItem = new LootItemDisplay(itemStack, lootItem);
                displayItems.add(displayItem);
            }
        }
        
        return displayItems;
    }
    
    /**
     * Create an ItemStack for display purposes (no random amounts or chance application)
     */
    private ItemStack createDisplayItemStack(LootItem lootItem) {
        try {
            // Use minimum amount for display
            int amount = lootItem.minAmount;
            
            // Handle ExecutableItems
            if (lootItem.isExecutableItem) {
                ItemStack executableItem = InfernalTresures.getInstance().getExecutableItemsIntegration()
                    .createExecutableItem(lootItem.executableId, amount);
                
                if (executableItem != null) {
                    // Apply custom display name and lore if specified
                    if (lootItem.displayName != null || (lootItem.lore != null && !lootItem.lore.isEmpty())) {
                        ItemBuilder builder = ItemBuilder.from(executableItem);
                        
                        if (lootItem.displayName != null) {
                            builder.setDisplayName(lootItem.displayName);
                        }
                        
                        if (lootItem.lore != null && !lootItem.lore.isEmpty()) {
                            builder.setLore(lootItem.lore);
                        }
                        
                        return builder.build();
                    }
                    
                    return executableItem;
                } else {
                    plugin.getLogger().warning("Failed to create ExecutableItem for display: " + lootItem.executableId);
                    return null;
                }
            }
            
            // Handle ExecutableBlocks
            if (lootItem.isExecutableBlock) {
                ItemStack executableBlock = InfernalTresures.getInstance().getExecutableBlocksIntegration()
                    .createExecutableBlock(lootItem.executableBlockId, amount);
                
                if (executableBlock != null) {
                    // Apply custom display name and lore if specified
                    if (lootItem.displayName != null || (lootItem.lore != null && !lootItem.lore.isEmpty())) {
                        ItemBuilder builder = ItemBuilder.from(executableBlock);
                        
                        if (lootItem.displayName != null) {
                            builder.setDisplayName(lootItem.displayName);
                        }
                        
                        if (lootItem.lore != null && !lootItem.lore.isEmpty()) {
                            builder.setLore(lootItem.lore);
                        }
                        
                        return builder.build();
                    }
                    
                    return executableBlock;
                } else {
                    plugin.getLogger().warning("Failed to create ExecutableBlock for display: " + lootItem.executableBlockId);
                    return null;
                }
            }
            
            // Handle Shulker Box items
            if (lootItem.isShulkerBox) {
                // Use the configured material (supports all colored shulker boxes)
                Material shulkerMaterial = lootItem.material != null ? lootItem.material : Material.SHULKER_BOX;
                ItemStack shulkerBox = new ItemStack(shulkerMaterial, 1);
                
                // Set custom display name and lore for the Shulker Box itself if specified
                if (lootItem.displayName != null || (lootItem.lore != null && !lootItem.lore.isEmpty())) {
                    ItemBuilder builder = ItemBuilder.from(shulkerBox);
                    
                    if (lootItem.displayName != null) {
                        builder.setDisplayName(lootItem.displayName);
                    }
                    
                    if (lootItem.lore != null && !lootItem.lore.isEmpty()) {
                        builder.setLore(lootItem.lore);
                    }
                    
                    shulkerBox = builder.build();
                }
                
                // Set up the contained items using BlockStateMeta (for display purposes - show first few items)
                if (lootItem.shulkerBoxItems != null && !lootItem.shulkerBoxItems.isEmpty()) {
                    org.bukkit.inventory.meta.BlockStateMeta blockStateMeta = (org.bukkit.inventory.meta.BlockStateMeta) shulkerBox.getItemMeta();
                    org.bukkit.block.ShulkerBox shulkerBoxState = (org.bukkit.block.ShulkerBox) blockStateMeta.getBlockState();
                    org.bukkit.inventory.Inventory shulkerInventory = shulkerBoxState.getInventory();
                    
                    // Add contained items to the shulker box inventory (display purposes - show first few items)
                    for (ShulkerBoxItem shulkerItem : lootItem.shulkerBoxItems) {
                        ItemStack containedItem = null;
                        
                        // Handle ExecutableItems
                        if (shulkerItem.isExecutableItem) {
                            containedItem = InfernalTresures.getInstance().getExecutableItemsIntegration()
                                .createExecutableItem(shulkerItem.executableId, shulkerItem.minAmount);
                        }
                        // Handle ExecutableBlocks
                        else if (shulkerItem.isExecutableBlock) {
                            containedItem = InfernalTresures.getInstance().getExecutableBlocksIntegration()
                                .createExecutableBlock(shulkerItem.executableBlockId, shulkerItem.minAmount);
                        }
                        // Handle MMOItems
                        else if (shulkerItem.isMMOItem) {
                            containedItem = InfernalTresures.getInstance().getMMOItemsIntegration()
                                .createMMOItem(shulkerItem.mmoType, shulkerItem.mmoId, shulkerItem.minAmount);
                        }
                        // Handle material-based items
                        else if (shulkerItem.material != null) {
                            containedItem = new ItemStack(shulkerItem.material, shulkerItem.minAmount);
                        }
                        
                        // Apply custom display name and lore to contained item if specified
                        if (containedItem != null && (shulkerItem.displayName != null || (shulkerItem.lore != null && !shulkerItem.lore.isEmpty()))) {
                            ItemBuilder itemBuilder = ItemBuilder.from(containedItem);
                            
                            if (shulkerItem.displayName != null) {
                                itemBuilder.setDisplayName(shulkerItem.displayName);
                            }
                            
                            if (shulkerItem.lore != null && !shulkerItem.lore.isEmpty()) {
                                itemBuilder.setLore(shulkerItem.lore);
                            }
                            
                            containedItem = itemBuilder.build();
                        }
                        
                        // Add the item to the shulker box inventory
                        if (containedItem != null) {
                            shulkerInventory.addItem(containedItem);
                        }
                    }
                    
                    // Update the block state and apply it back to the item
                    blockStateMeta.setBlockState(shulkerBoxState);
                    shulkerBox.setItemMeta(blockStateMeta);
                }
                
                return shulkerBox;
            }
            
            // Handle MMOItems for regular items
            if (lootItem.isMMOItem) {
                ItemStack mmoItem = InfernalTresures.getInstance().getMMOItemsIntegration()
                    .createMMOItem(lootItem.mmoType, lootItem.mmoId, amount);
                
                if (mmoItem != null) {
                    // Apply custom display name and lore if specified
                    if (lootItem.displayName != null || (lootItem.lore != null && !lootItem.lore.isEmpty())) {
                        ItemBuilder builder = ItemBuilder.from(mmoItem);
                        
                        if (lootItem.displayName != null) {
                            builder.setDisplayName(lootItem.displayName);
                        }
                        
                        if (lootItem.lore != null && !lootItem.lore.isEmpty()) {
                            builder.setLore(lootItem.lore);
                        }
                        
                        return builder.build();
                    }
                    
                    return mmoItem;
                } else {
                    plugin.getLogger().warning("Failed to create MMOItem: " + lootItem.mmoType + "." + lootItem.mmoId);
                    return null;
                }
            }
            
            // Regular Bukkit item handling
            ItemBuilder builder = new ItemBuilder(lootItem.material, amount);
            
            // Set display name
            if (lootItem.displayName != null) {
                builder.setDisplayName(lootItem.displayName);
            }
            
            // Set lore
            if (lootItem.lore != null && !lootItem.lore.isEmpty()) {
                builder.setLore(lootItem.lore);
            }
            
            // Set unbreakable
            if (lootItem.unbreakable) {
                builder.setUnbreakable(true);
            }
            
            // Set custom model data
            if (lootItem.customModelData > -1) {
                builder.setCustomModelData(lootItem.customModelData);
            }
            
            // Add enchantments with random levels
            if (lootItem.enchantments != null) {
                for (EnchantmentData enchantData : lootItem.enchantments) {
                    if ("RANDOM".equalsIgnoreCase(enchantData.enchantment)) {
                        builder.addRandomEnchantment(enchantData.minLevel, enchantData.maxLevel);
                    } else {
                        Enchantment enchantment = ItemBuilder.getEnchantmentByName(enchantData.enchantment);
                        if (enchantment != null) {
                            int level = enchantData.level;
                            
                            // If min/max levels are specified, use random level between them
                            if (enchantData.maxLevel > enchantData.minLevel) {
                                level = ThreadLocalRandom.current().nextInt(enchantData.minLevel, enchantData.maxLevel + 1);
                            } else if (enchantData.minLevel > 0) {
                                level = enchantData.minLevel;
                            }
                            
                            builder.addEnchantment(enchantment, level);
                        }
                    }
                }
            }
            
            // Add attributes
            if (lootItem.attributes != null) {
                for (AttributeData attributeData : lootItem.attributes) {
                    Attribute attribute = ItemBuilder.getAttributeByName(attributeData.attribute);
                    if (attribute != null) {
                        AttributeModifier.Operation operation = ItemBuilder.getOperationByName(attributeData.operation);
                        EquipmentSlot slot = getEquipmentSlot(attributeData.slot);
                        builder.addAttribute(attribute, attributeData.value, operation, slot);
                    }
                }
            }
            
            // Add custom effects (for consumables)
            if (lootItem.customEffects != null) {
                for (CustomEffectData effectData : lootItem.customEffects) {
                    PotionEffectType effectType = ItemBuilder.getPotionEffectByName(effectData.effect);
                    if (effectType != null) {
                        builder.addPotionEffect(effectType, effectData.duration, effectData.amplifier);
                    }
                }
            }
            
            return builder.build();
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create ItemStack: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Create an ItemStack for actual loot generation (with random amounts and full functionality)
     */
    private ItemStack createItemStack(LootItem lootItem, org.bukkit.entity.Player player) {
        try {
            // Calculate random amount between min and max
            int amount = lootItem.minAmount;
            if (lootItem.maxAmount > lootItem.minAmount) {
                amount = ThreadLocalRandom.current().nextInt(lootItem.minAmount, lootItem.maxAmount + 1);
            }
            
            // Handle ExecutableItems
            if (lootItem.isExecutableItem) {
                ItemStack executableItem = InfernalTresures.getInstance().getExecutableItemsIntegration()
                    .createExecutableItem(lootItem.executableId, amount);
                
                if (executableItem != null) {
                    // Apply custom display name and lore if specified
                    if (lootItem.displayName != null || (lootItem.lore != null && !lootItem.lore.isEmpty())) {
                        ItemBuilder builder = ItemBuilder.from(executableItem);
                        
                        if (lootItem.displayName != null) {
                            builder.setDisplayName(lootItem.displayName);
                        }
                        
                        if (lootItem.lore != null && !lootItem.lore.isEmpty()) {
                            builder.setLore(lootItem.lore);
                        }
                        
                        return builder.build();
                    }
                    
                    return executableItem;
                } else {
                    plugin.getLogger().warning("Failed to create ExecutableItem: " + lootItem.executableId);
                    return null;
                }
            }
            
            // Handle ExecutableBlocks
            if (lootItem.isExecutableBlock) {
                ItemStack executableBlock = InfernalTresures.getInstance().getExecutableBlocksIntegration()
                    .createExecutableBlock(lootItem.executableBlockId, amount);
                
                if (executableBlock != null) {
                    // Apply custom display name and lore if specified
                    if (lootItem.displayName != null || (lootItem.lore != null && !lootItem.lore.isEmpty())) {
                        ItemBuilder builder = ItemBuilder.from(executableBlock);
                        
                        if (lootItem.displayName != null) {
                            builder.setDisplayName(lootItem.displayName);
                        }
                        
                        if (lootItem.lore != null && !lootItem.lore.isEmpty()) {
                            builder.setLore(lootItem.lore);
                        }
                        
                        return builder.build();
                    }
                    
                    return executableBlock;
                } else {
                    plugin.getLogger().warning("Failed to create ExecutableBlock: " + lootItem.executableBlockId);
                    return null;
                }
            }
            
            // Handle Shulker Box items
            if (lootItem.isShulkerBox) {
                // Use the configured material (supports all colored shulker boxes)
                Material shulkerMaterial = lootItem.material != null ? lootItem.material : Material.SHULKER_BOX;
                ItemStack shulkerBox = new ItemStack(shulkerMaterial, 1);
                
                // Set custom display name and lore for the Shulker Box itself if specified
                if (lootItem.displayName != null || (lootItem.lore != null && !lootItem.lore.isEmpty())) {
                    ItemBuilder builder = ItemBuilder.from(shulkerBox);
                    
                    if (lootItem.displayName != null) {
                        builder.setDisplayName(lootItem.displayName);
                    }
                    
                    if (lootItem.lore != null && !lootItem.lore.isEmpty()) {
                        builder.setLore(lootItem.lore);
                    }
                    
                    shulkerBox = builder.build();
                }
                
                // Set up the contained items using BlockStateMeta
                if (lootItem.shulkerBoxItems != null && !lootItem.shulkerBoxItems.isEmpty()) {
                    org.bukkit.inventory.meta.BlockStateMeta blockStateMeta = (org.bukkit.inventory.meta.BlockStateMeta) shulkerBox.getItemMeta();
                    org.bukkit.block.ShulkerBox shulkerBoxState = (org.bukkit.block.ShulkerBox) blockStateMeta.getBlockState();
                    org.bukkit.inventory.Inventory shulkerInventory = shulkerBoxState.getInventory();
                    
                    // Get Shulker Box slot configuration
                    int maxShulkerSlots = plugin.getConfigManager().getShulkerBoxSlots();
                    boolean useShulkerChanceSystem = plugin.getConfigManager().useShulkerBoxChanceBasedSystem();
                    
                    // Filter available Shulker Box items based on progression requirements
                    List<ShulkerBoxItem> availableShulkerItems = new ArrayList<>();
                    long playerBlocksMined = 0;
                    if (player != null) {
                        playerBlocksMined = plugin.getStatsManager().getPlayerStats(player.getUniqueId()).totalBlocksMined;
                    }
                    
                    for (ShulkerBoxItem shulkerItem : lootItem.shulkerBoxItems) {
                        // Check progression requirement for this shulker item
                        if (playerBlocksMined >= shulkerItem.minRequiredBlocksMined && playerBlocksMined <= shulkerItem.maxRequiredBlocksMined) {
                            availableShulkerItems.add(shulkerItem);
                        }
                    }
                    
                    if (plugin.getConfigManager().isProgressionDebugEnabled()) {
                        plugin.getLogger().info("=== SHULKER BOX LOOT DEBUG ===");
                        plugin.getLogger().info("Max Shulker Box slots: " + maxShulkerSlots);
                        plugin.getLogger().info("Available Shulker Box items: " + availableShulkerItems.size());
                        plugin.getLogger().info("Using chance system: " + useShulkerChanceSystem);
                    }
                    
                    // Fill Shulker Box slots
                    if (!availableShulkerItems.isEmpty()) {
                        Set<String> addedItems = new HashSet<>(); // Track added items to prevent stacking
                        
                        for (int slot = 0; slot < maxShulkerSlots; slot++) {
                            // Filter out items that have already been added (to prevent stacking)
                            List<ShulkerBoxItem> unaddedItems = availableShulkerItems.stream()
                                .filter(item -> {
                                    String itemKey = getItemKey(item);
                                    return !addedItems.contains(itemKey);
                                })
                                .collect(java.util.stream.Collectors.toList());
                            

                            if (unaddedItems.isEmpty()) {
                                // If no unique items left, allow repeats
                                unaddedItems = new ArrayList<>(availableShulkerItems);
                            }
                            
                            // Randomly select an item from available unique items
                            ShulkerBoxItem selectedShulkerItem = unaddedItems.get(ThreadLocalRandom.current().nextInt(unaddedItems.size()));
                            
                            boolean shouldAddShulkerItem = true;
                            if (useShulkerChanceSystem) {
                                // Apply the item's individual chance
                                shouldAddShulkerItem = ThreadLocalRandom.current().nextDouble(100.0) < selectedShulkerItem.chance;
                            }
                            // If not using chance system, shouldAddShulkerItem remains true (guaranteed fill)
                            
                            if (shouldAddShulkerItem) {
                                // Calculate random amount between min and max
                                int itemAmount = selectedShulkerItem.minAmount;
                                if (selectedShulkerItem.maxAmount > selectedShulkerItem.minAmount) {
                                    itemAmount = ThreadLocalRandom.current().nextInt(selectedShulkerItem.minAmount, selectedShulkerItem.maxAmount + 1);
                                }
                                
                                ItemStack containedItem = null;
                                
                                // Handle ExecutableItems
                                if (selectedShulkerItem.isExecutableItem) {
                                    containedItem = InfernalTresures.getInstance().getExecutableItemsIntegration()
                                        .createExecutableItem(selectedShulkerItem.executableId, itemAmount);
                                    
                                    if (plugin.getConfigManager().isProgressionDebugEnabled() && containedItem != null) {
                                        plugin.getLogger().info("Created ExecutableItem '" + selectedShulkerItem.executableId + 
                                            "' - Requested: " + itemAmount + ", Actual: " + containedItem.getAmount());
                                    }
                                }
                                // Handle ExecutableBlocks
                                else if (selectedShulkerItem.isExecutableBlock) {
                                    containedItem = InfernalTresures.getInstance().getExecutableBlocksIntegration()
                                        .createExecutableBlock(selectedShulkerItem.executableBlockId, itemAmount);
                                }
                                // Handle MMOItems
                                else if (selectedShulkerItem.isMMOItem) {
                                    containedItem = InfernalTresures.getInstance().getMMOItemsIntegration()
                                        .createMMOItem(selectedShulkerItem.mmoType, selectedShulkerItem.mmoId, itemAmount);
                                }
                                // Handle material-based items
                                else if (selectedShulkerItem.material != null) {
                                    containedItem = new ItemStack(selectedShulkerItem.material, itemAmount);
                                }
                                
                                // Apply custom display name and lore to contained item if specified
                                if (containedItem != null && (selectedShulkerItem.displayName != null || (selectedShulkerItem.lore != null && !selectedShulkerItem.lore.isEmpty()))) {
                                    ItemBuilder itemBuilder = ItemBuilder.from(containedItem);
                                    
                                    if (selectedShulkerItem.displayName != null) {
                                        itemBuilder.setDisplayName(selectedShulkerItem.displayName);
                                    }
                                    
                                    if (selectedShulkerItem.lore != null && !selectedShulkerItem.lore.isEmpty()) {
                                        itemBuilder.setLore(selectedShulkerItem.lore);
                                    }
                                    
                                    containedItem = itemBuilder.build();
                                }
                                
                                // Add the item to the shulker box inventory
                                if (containedItem != null) {
                                    shulkerInventory.addItem(containedItem);
                                    addedItems.add(getItemKey(selectedShulkerItem)); // Track this item as added
                                    
                                    if (plugin.getConfigManager().isProgressionDebugEnabled()) {
                                        plugin.getLogger().info("Shulker Slot " + (slot + 1) + ": Added " + containedItem.getType() + 
                                            " x" + containedItem.getAmount() + (useShulkerChanceSystem ? " (chance: " + selectedShulkerItem.chance + "%)" : " (guaranteed)"));
                                    }
                                } else {
                                    if (plugin.getConfigManager().isProgressionDebugEnabled()) {
                                        plugin.getLogger().info("Shulker Slot " + (slot + 1) + ": Failed to create item");
                                    }
                                }
                            } else {
                                if (plugin.getConfigManager().isProgressionDebugEnabled()) {
                                    plugin.getLogger().info("Shulker Slot " + (slot + 1) + ": No item (chance missed: " + selectedShulkerItem.chance + "%)");
                                }
                            }
                        }
                        
                        if (plugin.getConfigManager().isProgressionDebugEnabled()) {
                            plugin.getLogger().info("Final Shulker Box items: " + shulkerInventory.getContents().length + " items added");
                            plugin.getLogger().info("=== END SHULKER BOX LOOT DEBUG ===");
                        }
                    }
                    
                    // Update the block state and apply it back to the item
                    blockStateMeta.setBlockState(shulkerBoxState);
                    shulkerBox.setItemMeta(blockStateMeta);
                }
                
                return shulkerBox;
            }
            
            // Handle MMOItems for regular items
            if (lootItem.isMMOItem) {
                ItemStack mmoItem = InfernalTresures.getInstance().getMMOItemsIntegration()
                    .createMMOItem(lootItem.mmoType, lootItem.mmoId, amount);
                
                if (mmoItem != null) {
                    // Apply custom display name and lore if specified
                    if (lootItem.displayName != null || (lootItem.lore != null && !lootItem.lore.isEmpty())) {
                        ItemBuilder builder = ItemBuilder.from(mmoItem);
                        
                        if (lootItem.displayName != null) {
                            builder.setDisplayName(lootItem.displayName);
                        }
                        
                        if (lootItem.lore != null && !lootItem.lore.isEmpty()) {
                            builder.setLore(lootItem.lore);
                        }
                        
                        return builder.build();
                    }
                    
                    return mmoItem;
                } else {
                    plugin.getLogger().warning("Failed to create MMOItem: " + lootItem.mmoType + "." + lootItem.mmoId);
                    return null;
                }
            }
            
            // Regular Bukkit item handling
            ItemBuilder builder = new ItemBuilder(lootItem.material, amount);
            
            // Set display name
            if (lootItem.displayName != null) {
                builder.setDisplayName(lootItem.displayName);
            }
            
            // Set lore
            if (lootItem.lore != null && !lootItem.lore.isEmpty()) {
                builder.setLore(lootItem.lore);
            }
            
            // Set unbreakable
            if (lootItem.unbreakable) {
                builder.setUnbreakable(true);
            }
            
            // Set custom model data
            if (lootItem.customModelData > -1) {
                builder.setCustomModelData(lootItem.customModelData);
            }
            
            // Add enchantments with random levels
            if (lootItem.enchantments != null) {
                for (EnchantmentData enchantData : lootItem.enchantments) {
                    if ("RANDOM".equalsIgnoreCase(enchantData.enchantment)) {
                        builder.addRandomEnchantment(enchantData.minLevel, enchantData.maxLevel);
                    } else {
                        Enchantment enchantment = ItemBuilder.getEnchantmentByName(enchantData.enchantment);
                        if (enchantment != null) {
                            int level = enchantData.level;
                            
                            // If min/max levels are specified, use random level between them
                            if (enchantData.maxLevel > enchantData.minLevel) {
                                level = ThreadLocalRandom.current().nextInt(enchantData.minLevel, enchantData.maxLevel + 1);
                            } else if (enchantData.minLevel > 0) {
                                level = enchantData.minLevel;
                            }
                            
                            builder.addEnchantment(enchantment, level);
                        }
                    }
                }
            }
            
            // Add attributes
            if (lootItem.attributes != null) {
                for (AttributeData attributeData : lootItem.attributes) {
                    Attribute attribute = ItemBuilder.getAttributeByName(attributeData.attribute);
                    if (attribute != null) {
                        AttributeModifier.Operation operation = ItemBuilder.getOperationByName(attributeData.operation);
                        EquipmentSlot slot = getEquipmentSlot(attributeData.slot);
                        builder.addAttribute(attribute, attributeData.value, operation, slot);
                    }
                }
            }
            
            // Add custom effects (for consumables)
            if (lootItem.customEffects != null) {
                for (CustomEffectData effectData : lootItem.customEffects) {
                    PotionEffectType effectType = ItemBuilder.getPotionEffectByName(effectData.effect);
                    if (effectType != null) {
                        builder.addPotionEffect(effectType, effectData.duration, effectData.amplifier);
                    }
                }
            }
            
            return builder.build();
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create ItemStack: " + e.getMessage());
            return null;
        }
    }
    
    private EquipmentSlot getEquipmentSlot(String slotName) {
        try {
            return EquipmentSlot.valueOf(slotName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return EquipmentSlot.HAND; // Default
        }
    }
    
    /**
     * Generate a unique key for an item to prevent stacking in Shulker Boxes
     */
    private String getItemKey(ShulkerBoxItem item) {
        if (item.isExecutableItem) {
            return "exec_item:" + item.executableId;
        } else if (item.isExecutableBlock) {
            return "exec_block:" + item.executableBlockId;
        } else if (item.isMMOItem) {
            return "mmo:" + item.mmoType + "." + item.mmoId;
        } else if (item.material != null) {
            return "material:" + item.material.name();
        } else {
            return "unknown";
        }
    }
    
    private Biome getBiomeFromFileName(String fileName) {
        return switch (fileName.toLowerCase()) {
            case "desert" -> Biome.DESERT;
            case "forest" -> Biome.FOREST;
            case "ocean" -> Biome.OCEAN;
            case "plains" -> Biome.PLAINS;
            case "mountains" -> Biome.WINDSWEPT_HILLS;
            case "swamp" -> Biome.SWAMP;
            case "jungle" -> Biome.JUNGLE;
            case "taiga" -> Biome.TAIGA;
            case "savanna" -> Biome.SAVANNA;
            case "badlands" -> Biome.BADLANDS;
            case "nether" -> Biome.NETHER_WASTES;
            case "end" -> Biome.THE_END;
            default -> {
                // Try to match exact biome name
                try {
                    yield Biome.valueOf(fileName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Unknown biome file: " + fileName);
                    yield null;
                }
            }
        };
    }
    
    public void reload() {
        lootTables.clear();
        loadLootTables();
    }
    
    // Inner classes for data structures
    private static class LootItem {
        Material material;
        int minAmount;
        int maxAmount;
        double chance;
        String displayName;
        List<String> lore;
        boolean unbreakable;
        int customModelData;
        List<EnchantmentData> enchantments;
        List<AttributeData> attributes;
        List<CustomEffectData> customEffects;
        
        // MMOItem fields
        boolean isMMOItem;
        String mmoType;
        String mmoId;
        
        // ExecutableItem fields
        boolean isExecutableItem;
        String executableId;
        
        // ExecutableBlock fields
        boolean isExecutableBlock;
        String executableBlockId;
        
        // Shulker Box fields
        boolean isShulkerBox;
        List<ShulkerBoxItem> shulkerBoxItems;
        
        // Progression requirement - now supports ranges
        long minRequiredBlocksMined = 0; // Minimum blocks required (inclusive)
        long maxRequiredBlocksMined = Long.MAX_VALUE; // Maximum blocks allowed (inclusive)
        String requiredBlocksMinedRange = null; // Original string representation for display
    }
    
    private static class EnchantmentData {
        String enchantment;
        int level;
        int minLevel;
        int maxLevel;
    }
    
    private static class AttributeData {
        String attribute;
        double value;
        String operation;
        String slot;
    }
    
    private static class CustomEffectData {
        String effect;
        int duration;
        int amplifier;
    }
    
    private static class ShulkerBoxItem {
        Material material;
        int minAmount = 1;
        int maxAmount = 1;
        double chance = 100.0;
        String displayName;
        List<String> lore;
        
        // Progression requirement - same as main loot items
        long minRequiredBlocksMined = 0;
        long maxRequiredBlocksMined = Long.MAX_VALUE;
        String requiredBlocksMinedRange = "0+";
        
        // MMOItem fields
        boolean isMMOItem;
        String mmoType;
        String mmoId;
        
        // ExecutableItem fields
        boolean isExecutableItem;
        String executableId;
        
        // ExecutableBlock fields
        boolean isExecutableBlock;
        String executableBlockId;
    }
    
    /**
     * Container class for displaying loot items in GUI with their configuration data
     */
    public static class LootItemDisplay {
        private final ItemStack itemStack;
        private final LootItem lootItem;
        
        public LootItemDisplay(ItemStack itemStack, LootItem lootItem) {
            this.itemStack = itemStack;
            this.lootItem = lootItem;
        }
        
        public ItemStack getItemStack() {
            return itemStack;
        }
        
        public double getChance() {
            return lootItem.chance;
        }
        
        public int getMinAmount() {
            return lootItem.minAmount;
        }
        
        public int getMaxAmount() {
            return lootItem.maxAmount;
        }
        
        public long getRequiredBlocksMined() {
            return lootItem.minRequiredBlocksMined;
        }
        
        public String getRequiredBlocksMinedRange() {
            return lootItem.requiredBlocksMinedRange;
        }
        
        public boolean isMMOItem() {
            return lootItem.isMMOItem;
        }
        
        public boolean isExecutableItem() {
            return lootItem.isExecutableItem;
        }
        
        public boolean isExecutableBlock() {
            return lootItem.isExecutableBlock;
        }
        
        public boolean isShulkerBox() {
            return lootItem.isShulkerBox;
        }
        
        public String getItemType() {
            if (lootItem.isExecutableItem) {
                return "ExecutableItem: " + lootItem.executableId;
            } else if (lootItem.isExecutableBlock) {
                return "ExecutableBlock: " + lootItem.executableBlockId;
            } else if (lootItem.isMMOItem) {
                return "MMOItem: " + lootItem.mmoType + "." + lootItem.mmoId;
            } else if (lootItem.isShulkerBox) {
                return "ShulkerBox with " + lootItem.shulkerBoxItems.size() + " items";
            } else {
                return "Material: " + lootItem.material.name();
            }
        }
    }
}