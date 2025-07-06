package me.itzrenzo.infernaltresures.managers;

import me.itzrenzo.infernaltresures.InfernalTresures;
import me.itzrenzo.infernaltresures.models.Rarity;
import me.itzrenzo.infernaltresures.models.BiomeCategory;
import me.itzrenzo.infernaltresures.utils.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;

public class LootGUIManager implements Listener {
    
    private final InfernalTresures plugin;
    private final Map<UUID, GUISession> activeSessions = new HashMap<>();
    
    public LootGUIManager(InfernalTresures plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    public void openBiomeGUI(Player player) {
        MenuManager menuManager = plugin.getMenuManager();
        
        Inventory gui = Bukkit.createInventory(null, menuManager.getBiomeSelectionSize(), 
            menuManager.getBiomeSelectionTitle());
        
        // Get all available biome categories
        Collection<BiomeCategory> biomeCategories = plugin.getLootManager().getBiomeCategories();
        
        int slot = 0;
        for (BiomeCategory category : biomeCategories) {
            if (slot >= menuManager.getMaxLootSlots()) break; // Use configurable max slots
            
            String categoryFileName = category.getFileName();
            Material categoryMaterial = category.getMaterial();
            String displayName = category.getName();
            
            // Build lore with category description and biomes list
            List<String> lore = new ArrayList<>();
            if (category.getDescription() != null && !category.getDescription().isEmpty()) {
                lore.add("&7" + category.getDescription());
                lore.add("");
            }
            
            // Add biomes in this category
            lore.add("&7Contains biomes:");
            for (Biome biome : category.getBiomes()) {
                lore.add("&8• " + formatBiomeName(biome));
            }
            lore.add("");
            lore.add("&eClick to explore!");
            
            ItemStack categoryItem = new ItemBuilder(categoryMaterial)
                .setDisplayName("&6&l" + displayName)
                .setLore(lore)
                .build();
            
            gui.setItem(slot, categoryItem);
            slot++;
        }
        
        // Add close button using menu config
        Material closeMaterial = menuManager.getNavigationMaterial("biome-selection", "close");
        int closeSlot = menuManager.getNavigationSlot("biome-selection", "close");
        String closeDisplayName = menuManager.getNavigationDisplayName("biome-selection", "close");
        List<String> closeLore = menuManager.getNavigationLore("biome-selection", "close");
        
        ItemStack closeItem = new ItemBuilder(closeMaterial)
            .setDisplayName(closeDisplayName)
            .setLore(closeLore)
            .build();
        gui.setItem(closeSlot, closeItem);
        
        // Store session
        activeSessions.put(player.getUniqueId(), new GUISession(GUIType.BIOME_SELECTION, null, null, null));
        
        player.openInventory(gui);
    }
    
    public void openRarityGUI(Player player, BiomeCategory category) {
        MenuManager menuManager = plugin.getMenuManager();
        
        Inventory gui = Bukkit.createInventory(null, menuManager.getRaritySelectionSize(), 
            menuManager.getRaritySelectionTitle(category.getName()));
        
        // Get available rarities for this category (check first biome as they all share the same loot table)
        Biome representativeBiome = category.getBiomes().get(0);
        Set<Rarity> availableRarities = getAvailableRarities(representativeBiome);
        
        // Store slot to rarity mapping for this session
        Map<Integer, Rarity> slotToRarityMap = new HashMap<>();
        
        int startSlot = menuManager.getRarityStartSlot();
        int spacing = menuManager.getRaritySlotSpacing();
        int slot = startSlot;
        
        for (Rarity rarity : Rarity.values()) {
            if (!availableRarities.contains(rarity)) continue;
            
            Material rarityMaterial = menuManager.getRarityMaterial(rarity);
            
            // Get loot count for this rarity
            int lootCount = getLootCount(representativeBiome, rarity);
            
            String displayName = menuManager.getRarityDisplayName(rarity);
            List<String> lore = menuManager.getRarityLore(rarity, lootCount);
            
            ItemStack rarityItem = new ItemBuilder(rarityMaterial)
                .setDisplayName(displayName)
                .setLore(lore)
                .build();
            
            gui.setItem(slot, rarityItem);
            slotToRarityMap.put(slot, rarity); // Store the mapping
            slot += spacing; // Space them out according to config
        }
        
        // Add navigation buttons using menu config
        Material backMaterial = menuManager.getNavigationMaterial("rarity-selection", "back");
        int backSlot = menuManager.getNavigationSlot("rarity-selection", "back");
        String backDisplayName = menuManager.getNavigationDisplayName("rarity-selection", "back");
        List<String> backLore = menuManager.getNavigationLore("rarity-selection", "back");
        
        ItemStack backItem = new ItemBuilder(backMaterial)
            .setDisplayName(backDisplayName)
            .setLore(backLore)
            .build();
        gui.setItem(backSlot, backItem);
        
        Material closeMaterial = menuManager.getNavigationMaterial("rarity-selection", "close");
        int closeSlot = menuManager.getNavigationSlot("rarity-selection", "close");
        String closeDisplayName = menuManager.getNavigationDisplayName("rarity-selection", "close");
        List<String> closeLore = menuManager.getNavigationLore("rarity-selection", "close");
        
        ItemStack closeItem = new ItemBuilder(closeMaterial)
            .setDisplayName(closeDisplayName)
            .setLore(closeLore)
            .build();
        gui.setItem(closeSlot, closeItem);
        
        // Store session with slot mapping
        RarityGUISession raritySession = new RarityGUISession(GUIType.RARITY_SELECTION, null, null, category, slotToRarityMap);
        activeSessions.put(player.getUniqueId(), raritySession);
        
        player.openInventory(gui);
    }
    
    public void openLootGUI(Player player, BiomeCategory category, Rarity rarity) {
        MenuManager menuManager = plugin.getMenuManager();
        
        Inventory gui = Bukkit.createInventory(null, menuManager.getLootDisplaySize(), 
            menuManager.getLootDisplayTitle(category.getName(), rarity.getDisplayName()));
        
        // Get all loot items for this category and rarity (use first biome as they share the same loot table)
        Biome representativeBiome = category.getBiomes().get(0);
        List<LootManager.LootItemDisplay> lootItems = plugin.getLootManager().getAllLootItems(representativeBiome, rarity);
        
        // Fill the GUI with loot items
        int slot = 0;
        for (LootManager.LootItemDisplay lootDisplay : lootItems) {
            if (slot >= menuManager.getMaxLootSlots()) break; // Leave space for navigation
            
            // Clone the item and add configurable lore
            ItemStack displayItem = lootDisplay.getItemStack().clone();
            ItemMeta meta = displayItem.getItemMeta();
            
            if (meta != null) {
                List<Component> lore = meta.lore();
                if (lore == null) lore = new ArrayList<>();
                
                // Add empty line separator
                lore.add(Component.empty());
                
                // Add configurable detail information
                addConfigurableLore(lore, lootDisplay, category, rarity, menuManager);
                
                meta.lore(lore);
                displayItem.setItemMeta(meta);
            }
            
            gui.setItem(slot, displayItem);
            slot++;
        }
        
        // Add navigation items using menu config
        Material backMaterial = menuManager.getNavigationMaterial("loot-display", "back");
        int backSlot = menuManager.getNavigationSlot("loot-display", "back");
        String backDisplayName = menuManager.getNavigationDisplayName("loot-display", "back");
        List<String> backLore = menuManager.getNavigationLore("loot-display", "back");
        
        ItemStack backItem = new ItemBuilder(backMaterial)
            .setDisplayName(backDisplayName)
            .setLore(backLore)
            .build();
        gui.setItem(backSlot, backItem);
        
        // Category info item
        Material categoryInfoMaterial = category.getMaterial();
        int categoryInfoSlot = menuManager.getNavigationSlot("loot-display", "biome-info");
        String categoryInfoDisplayName = menuManager.getNavigationDisplayName("loot-display", "biome-info");
        List<String> categoryInfoLore = menuManager.getNavigationLore("loot-display", "biome-info");
        
        // Replace placeholders in category info
        categoryInfoDisplayName = categoryInfoDisplayName.replace("{biome_name}", category.getName());
        categoryInfoLore = categoryInfoLore.stream()
            .map(line -> line.replace("{biome_name}", category.getName()))
            .map(line -> line.replace("{rarity_display_name}", rarity.getDisplayName()))
            .map(line -> line.replace("{total_items}", String.valueOf(lootItems.size())))
            .toList();
        
        ItemStack categoryItem = new ItemBuilder(categoryInfoMaterial)
            .setDisplayName(categoryInfoDisplayName)
            .setLore(categoryInfoLore)
            .build();
        gui.setItem(categoryInfoSlot, categoryItem);
        
        // Close button
        Material closeMaterial = menuManager.getNavigationMaterial("loot-display", "close");
        int closeSlot = menuManager.getNavigationSlot("loot-display", "close");
        String closeDisplayName = menuManager.getNavigationDisplayName("loot-display", "close");
        List<String> closeLore = menuManager.getNavigationLore("loot-display", "close");
        
        ItemStack closeItem = new ItemBuilder(closeMaterial)
            .setDisplayName(closeDisplayName)
            .setLore(closeLore)
            .build();
        gui.setItem(closeSlot, closeItem);
        
        // Store session
        activeSessions.put(player.getUniqueId(), new GUISession(GUIType.LOOT_DISPLAY, null, rarity, category));
        
        player.openInventory(gui);
    }
    
    /**
     * Add configurable lore to loot items based on menu configuration
     */
    private void addConfigurableLore(List<Component> lore, LootManager.LootItemDisplay lootDisplay, 
                                   BiomeCategory category, Rarity rarity, MenuManager menuManager) {
        
        // Add category source info
        if (menuManager.shouldShowBiomeSource()) {
            String format = menuManager.getBiomeSourceFormat();
            format = format.replace("{biome_name}", category.getName());
            lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(format)
                .decoration(TextDecoration.ITALIC, false));
        }
        
        // Add rarity info
        String rarityFormat = menuManager.getRarityInfoFormat();
        rarityFormat = rarityFormat.replace("{rarity_display_name}", rarity.getDisplayName());
        lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(rarityFormat)
            .decoration(TextDecoration.ITALIC, false));
        
        // Add chance info
        if (menuManager.shouldShowChance()) {
            String format = menuManager.getChanceFormat();
            format = format.replace("{chance}", String.format("%.1f", lootDisplay.getChance()));
            lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(format)
                .decoration(TextDecoration.ITALIC, false));
        }
        
        // Add amount info
        if (lootDisplay.getMinAmount() == lootDisplay.getMaxAmount()) {
            // Single amount
            if (menuManager.shouldShowSingleAmount()) {
                String format = menuManager.getSingleAmountFormat();
                format = format.replace("{amount}", String.valueOf(lootDisplay.getMinAmount()));
                lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(format)
                    .decoration(TextDecoration.ITALIC, false));
            }
        } else {
            // Amount range
            if (menuManager.shouldShowAmountRange()) {
                String format = menuManager.getAmountRangeFormat();
                format = format.replace("{min_amount}", String.valueOf(lootDisplay.getMinAmount()));
                format = format.replace("{max_amount}", String.valueOf(lootDisplay.getMaxAmount()));
                lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(format)
                    .decoration(TextDecoration.ITALIC, false));
            }
        }
        
        // Add required blocks info
        if (menuManager.shouldShowRequiredBlocks()) {
            String rangeDisplay = lootDisplay.getRequiredBlocksMinedRange();
            if (rangeDisplay != null && !rangeDisplay.equals("0+")) {
                String format = menuManager.getRequiredBlocksFormat();
                format = format.replace("{required_blocks}", rangeDisplay);
                lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(format)
                    .decoration(TextDecoration.ITALIC, false));
            } else {
                String format = menuManager.getNoRequirementFormat();
                lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(format)
                    .decoration(TextDecoration.ITALIC, false));
            }
        }
        
        // Add item type info
        if (menuManager.shouldShowItemType()) {
            String format = menuManager.getItemTypeFormat();
            format = format.replace("{item_type}", lootDisplay.getItemType());
            lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(format)
                .decoration(TextDecoration.ITALIC, false));
        }
    }
    
    private Set<Biome> getAvailableBiomes() {
        Set<Biome> biomes = new HashSet<>();
        File biomesFolder = new File(plugin.getDataFolder(), "biomes");
        
        if (!biomesFolder.exists()) return biomes;
        
        File[] biomeFiles = biomesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (biomeFiles == null) return biomes;
        
        for (File biomeFile : biomeFiles) {
            String fileName = biomeFile.getName().replace(".yml", "");
            Biome biome = getBiomeFromFileName(fileName);
            if (biome != null) {
                biomes.add(biome);
            }
        }
        
        return biomes;
    }
    
    private Set<Rarity> getAvailableRarities(Biome biome) {
        Set<Rarity> rarities = new HashSet<>();
        
        // Instead of looking for individual biome files, use the biome category system
        BiomeCategory category = plugin.getLootManager().getBiomeCategory(biome);
        if (category == null) {
            plugin.getLogger().warning("No biome category found for biome: " + biome.name());
            return rarities;
        }
        
        File biomeFile = new File(new File(plugin.getDataFolder(), "biomes"), category.getFileName() + ".yml");
        
        if (!biomeFile.exists()) {
            plugin.getLogger().warning("Biome file does not exist: " + biomeFile.getPath());
            return rarities;
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(biomeFile);
        ConfigurationSection lootSection = config.getConfigurationSection("loot");
        
        if (lootSection != null) {
            for (Rarity rarity : Rarity.values()) {
                if (lootSection.contains(rarity.name()) && 
                    !lootSection.getMapList(rarity.name()).isEmpty()) {
                    rarities.add(rarity);
                }
            }
        }
        
        return rarities;
    }
    
    private int getLootCount(Biome biome, Rarity rarity) {
        // Use the biome category system instead of individual biome mapping
        BiomeCategory category = plugin.getLootManager().getBiomeCategory(biome);
        if (category == null) {
            return 0;
        }
        
        File biomeFile = new File(new File(plugin.getDataFolder(), "biomes"), category.getFileName() + ".yml");
        
        if (!biomeFile.exists()) return 0;
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(biomeFile);
        ConfigurationSection lootSection = config.getConfigurationSection("loot");
        
        if (lootSection != null) {
            return lootSection.getMapList(rarity.name()).size();
        }
        
        return 0;
    }
    
    private List<ItemStack> getLootItems(Biome biome, Rarity rarity, Player player) {
        // Use the existing LootManager to generate the loot items
        return plugin.getLootManager().generateLoot(biome, rarity, player);
    }
    
    private File getBiomeFile(Biome biome) {
        File biomesFolder = new File(plugin.getDataFolder(), "biomes");
        String fileName = getFileNameFromBiome(biome);
        
        if (fileName != null) {
            return new File(biomesFolder, fileName + ".yml");
        }
        
        return null;
    }
    
    private String getFileNameFromBiome(Biome biome) {
        // Handle known biomes with explicit mapping
        String biomeName = biome.name();
        
        // Check for specific biome mappings first
        switch (biomeName) {
            case "DESERT":
                return "desert";
            case "FOREST":
                return "forest";
            case "OCEAN":
                return "ocean";
            case "PLAINS":
                return "plains";
            case "WINDSWEPT_HILLS":
                return "mountains";
            case "SWAMP":
                return "swamp";
            case "JUNGLE":
                return "jungle";
            case "TAIGA":
                return "taiga";
            case "SAVANNA":
                return "savanna";
            case "BADLANDS":
                return "badlands";
            case "NETHER_WASTES":
                return "nether";
            case "THE_END":
                return "end";
            default:
                // For any unknown biomes, return lowercase version of the name
                plugin.getLogger().info("Unknown biome encountered: " + biomeName + ", using lowercase name");
                return biomeName.toLowerCase();
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
                try {
                    yield Biome.valueOf(fileName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    yield null;
                }
            }
        };
    }
    
    private String formatBiomeName(Biome biome) {
        String name = biome.name().toLowerCase().replace("_", " ");
        String[] words = name.split(" ");
        StringBuilder formatted = new StringBuilder();
        
        for (String word : words) {
            if (formatted.length() > 0) formatted.append(" ");
            formatted.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
        }
        
        return formatted.toString();
    }
    
    private Material getRarityMaterial(Rarity rarity) {
        return switch (rarity) {
            case COMMON -> Material.IRON_INGOT;
            case RARE -> Material.GOLD_INGOT;
            case EPIC -> Material.DIAMOND;
            case LEGENDARY -> Material.EMERALD;
            case MYTHIC -> Material.NETHERITE_INGOT;
        };
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        // Check if this is one of our GUI inventories by checking the title
        String inventoryTitle = "";
        if (event.getView().title() instanceof Component titleComponent) {
            inventoryTitle = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(titleComponent);
        }
        
        GUISession session = activeSessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        
        // Only handle clicks in our GUI inventories
        if (!inventoryTitle.contains("Treasure Biomes") && 
            !inventoryTitle.contains("Rarities in") && 
            !inventoryTitle.contains("Loot")) {
            return;
        }
        
        // Always cancel the event to prevent any item movement in our GUIs
        event.setCancelled(true);
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        
        String displayName = "";
        if (clickedItem.hasItemMeta() && clickedItem.getItemMeta().displayName() != null) {
            Component displayNameComponent = clickedItem.getItemMeta().displayName();
            displayName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(displayNameComponent);
        }
        
        switch (session.type) {
            case BIOME_SELECTION -> handleBiomeSelection(player, event.getSlot(), displayName);
            case RARITY_SELECTION -> handleRaritySelection(player, event.getSlot(), displayName, session.category);
            case LOOT_DISPLAY -> handleLootDisplay(player, event.getSlot(), displayName, session.category, session.rarity);
        }
    }
    
    private void handleBiomeSelection(Player player, int slot, String displayName) {
        if (displayName.contains("Close")) {
            player.closeInventory();
            return;
        }
        
        // Find the biome category based on slot
        Collection<BiomeCategory> biomeCategories = plugin.getLootManager().getBiomeCategories();
        List<BiomeCategory> categoryList = new ArrayList<>(biomeCategories);
        
        if (slot < categoryList.size()) {
            BiomeCategory selectedCategory = categoryList.get(slot);
            openRarityGUI(player, selectedCategory);
        }
    }
    
    private void handleRaritySelection(Player player, int slot, String displayName, BiomeCategory category) {
        if (displayName.contains("Close")) {
            player.closeInventory();
            return;
        }
        
        if (displayName.contains("Back")) {
            openBiomeGUI(player);
            return;
        }
        
        // Get the session and check if it has slot mapping
        GUISession session = activeSessions.get(player.getUniqueId());
        if (session instanceof RarityGUISession raritySession) {
            // Use the stored slot-to-rarity mapping
            Rarity selectedRarity = raritySession.slotToRarityMap.get(slot);
            
            if (selectedRarity != null && getAvailableRarities(category.getBiomes().get(0)).contains(selectedRarity)) {
                openLootGUI(player, category, selectedRarity);
            }
        }
    }
    
    private void handleLootDisplay(Player player, int slot, String displayName, BiomeCategory category, Rarity rarity) {
        if (displayName.contains("Close")) {
            player.closeInventory();
            return;
        }
        
        if (displayName.contains("Back")) {
            openRarityGUI(player, category);
            return;
        }
        
        // Items in the loot display are just for viewing
        // Could add additional functionality here if needed
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            // Don't remove the session immediately - delay it slightly to allow for GUI transitions
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Check if the player still has a GUI open
                if (player.getOpenInventory().getTopInventory().getSize() == player.getInventory().getSize()) {
                    // Player closed the GUI completely, remove the session
                    activeSessions.remove(player.getUniqueId());
                    plugin.getLogger().info("Removed session for player " + player.getName() + " - GUI closed");
                }
            }, 1L); // Delay by 1 tick
        }
    }
    
    private static class GUISession {
        final GUIType type;
        final Biome biome; // Keep for backward compatibility
        final Rarity rarity;
        final BiomeCategory category;
        
        GUISession(GUIType type, Biome biome, Rarity rarity, BiomeCategory category) {
            this.type = type;
            this.biome = biome;
            this.rarity = rarity;
            this.category = category;
        }
    }
    
    private enum GUIType {
        BIOME_SELECTION,
        RARITY_SELECTION,
        LOOT_DISPLAY
    }
    
    private static class RarityGUISession extends GUISession {
        final Map<Integer, Rarity> slotToRarityMap;
        
        RarityGUISession(GUIType type, Biome biome, Rarity rarity, BiomeCategory category, Map<Integer, Rarity> slotToRarityMap) {
            super(type, biome, rarity, category);
            this.slotToRarityMap = slotToRarityMap;
        }
    }
}