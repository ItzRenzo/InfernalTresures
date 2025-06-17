package me.itzrenzo.infernaltresures.managers;

import me.itzrenzo.infernaltresures.InfernalTresures;
import me.itzrenzo.infernaltresures.models.Rarity;
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
        
        for (File biomeFile : biomeFiles) {
            loadBiomeLootTable(biomeFile);
        }
        
        plugin.getLogger().info("Loaded loot tables for " + lootTables.size() + " biomes");
    }
    
    private void loadBiomeLootTable(File biomeFile) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(biomeFile);
        
        // Get biome name from file name (remove .yml extension)
        String fileName = biomeFile.getName().replace(".yml", "");
        Biome biome = getBiomeFromFileName(fileName);
        
        if (biome == null) {
            plugin.getLogger().warning("Could not determine biome from file: " + fileName);
            return;
        }
        
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
        
        lootTables.put(biome, biomeLootTable);
        plugin.getLogger().info("Loaded loot table for biome: " + biome.name() + " with " + 
            biomeLootTable.values().stream().mapToInt(List::size).sum() + " total items");
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
                        plugin.getLogger().warning("Item missing 'material', 'mmo_type'/'mmo_id', or 'executable_id' fields");
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
            item.requiredBlocksMined = requiredBlocksObj instanceof Number ? ((Number) requiredBlocksObj).longValue() : 0;
            
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
        if (player != null) {
            playerBlocksMined = plugin.getStatsManager().getPlayerStats(player).totalBlocksMined;
        }
        
        // Generate loot items based on their individual chances and progression requirements
        for (LootItem lootItem : rarityLoot) {
            // Check if player meets progression requirement
            if (lootItem.requiredBlocksMined > 0 && playerBlocksMined < lootItem.requiredBlocksMined) {
                if (plugin.getConfigManager().isLootGenerationDebugEnabled()) {
                    plugin.getLogger().info("Player " + (player != null ? player.getName() : "unknown") + 
                        " doesn't meet progression requirement: " + playerBlocksMined + "/" + lootItem.requiredBlocksMined + 
                        " blocks mined for item " + (lootItem.material != null ? lootItem.material.name() : 
                        lootItem.isMMOItem ? lootItem.mmoType + "." + lootItem.mmoId : lootItem.executableId));
                }
                continue; // Skip this item
            }
            
            if (ThreadLocalRandom.current().nextDouble(100.0) < lootItem.chance) {
                ItemStack itemStack = createItemStack(lootItem);
                if (itemStack != null) {
                    loot.add(itemStack);
                }
            }
        }
        
        return loot;
    }
    
    // Keep the old method for backward compatibility (for cases where player is unknown)
    public List<ItemStack> generateLoot(Biome biome, Rarity rarity) {
        return generateLoot(biome, rarity, null);
    }
    
    private ItemStack createItemStack(LootItem lootItem) {
        try {
            // Determine amount
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
            
            // Handle MMOItems
            if (lootItem.isMMOItem) {
                ItemStack mmoItem = InfernalTresures.getInstance().getMMOItemsIntegration()
                    .createMMOItem(lootItem.mmoType, lootItem.mmoId, amount);
                
                if (mmoItem != null) {
                    // Apply custom display name and lore if specified
                    if (lootItem.displayName != null || (lootItem.lore != null && !lootItem.lore.isEmpty())) {
                        ItemBuilder builder = ItemBuilder.from(mmoItem); // Use the static from() method
                        
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
            
            // Add enchantments
            if (lootItem.enchantments != null) {
                for (EnchantmentData enchantData : lootItem.enchantments) {
                    if ("RANDOM".equalsIgnoreCase(enchantData.enchantment)) {
                        builder.addRandomEnchantment(enchantData.minLevel, enchantData.maxLevel);
                    } else {
                        Enchantment enchantment = ItemBuilder.getEnchantmentByName(enchantData.enchantment);
                        if (enchantment != null) {
                            int level = enchantData.level;
                            if (enchantData.maxLevel > enchantData.minLevel) {
                                level = ThreadLocalRandom.current().nextInt(enchantData.minLevel, enchantData.maxLevel + 1);
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
        
        // Progression requirement
        long requiredBlocksMined = 0; // Default: no requirement
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
}