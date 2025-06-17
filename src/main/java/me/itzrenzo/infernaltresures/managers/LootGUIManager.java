package me.itzrenzo.infernaltresures.managers;

import me.itzrenzo.infernaltresures.InfernalTresures;
import me.itzrenzo.infernaltresures.models.Rarity;
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
    
    // Biome display materials
    private static final Map<Biome, Material> BIOME_MATERIALS = new HashMap<>();
    
    static {
        BIOME_MATERIALS.put(Biome.PLAINS, Material.GRASS_BLOCK);
        BIOME_MATERIALS.put(Biome.DESERT, Material.SAND);
        BIOME_MATERIALS.put(Biome.FOREST, Material.OAK_LOG);
        BIOME_MATERIALS.put(Biome.OCEAN, Material.WATER_BUCKET);
        BIOME_MATERIALS.put(Biome.TAIGA, Material.SPRUCE_LOG);
        BIOME_MATERIALS.put(Biome.SWAMP, Material.LILY_PAD);
        BIOME_MATERIALS.put(Biome.JUNGLE, Material.JUNGLE_LOG);
        BIOME_MATERIALS.put(Biome.SAVANNA, Material.ACACIA_LOG);
        BIOME_MATERIALS.put(Biome.BADLANDS, Material.TERRACOTTA);
        BIOME_MATERIALS.put(Biome.WINDSWEPT_HILLS, Material.STONE);
        BIOME_MATERIALS.put(Biome.NETHER_WASTES, Material.NETHERRACK);
        BIOME_MATERIALS.put(Biome.THE_END, Material.END_STONE);
    }
    
    public LootGUIManager(InfernalTresures plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    public void openBiomeGUI(Player player) {
        MenuManager menuManager = plugin.getMenuManager();
        
        Inventory gui = Bukkit.createInventory(null, menuManager.getBiomeSelectionSize(), 
            menuManager.getBiomeSelectionTitle());
        
        // Get all available biomes from the biomes folder
        Set<Biome> availableBiomes = getAvailableBiomes();
        
        int slot = 0;
        for (Biome biome : availableBiomes) {
            if (slot >= menuManager.getMaxLootSlots()) break; // Use configurable max slots
            
            String biomeName = getFileNameFromBiome(biome);
            Material biomeMaterial = menuManager.getBiomeMaterial(biomeName);
            String displayName = menuManager.getBiomeDisplayName(biomeName);
            List<String> lore = menuManager.getBiomeLore(biomeName);
            
            ItemStack biomeItem = new ItemBuilder(biomeMaterial)
                .setDisplayName(displayName)
                .setLore(lore)
                .build();
            
            gui.setItem(slot, biomeItem);
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
        activeSessions.put(player.getUniqueId(), new GUISession(GUIType.BIOME_SELECTION, null, null));
        
        player.openInventory(gui);
    }
    
    public void openRarityGUI(Player player, Biome biome) {
        Inventory gui = Bukkit.createInventory(null, 27, 
            Component.text("Rarities in " + formatBiomeName(biome)).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        
        // Get available rarities for this biome
        Set<Rarity> availableRarities = getAvailableRarities(biome);
        
        // Store slot to rarity mapping for this session
        Map<Integer, Rarity> slotToRarityMap = new HashMap<>();
        
        int slot = 10; // Start at a nice position
        for (Rarity rarity : Rarity.values()) {
            if (!availableRarities.contains(rarity)) continue;
            
            Material rarityMaterial = getRarityMaterial(rarity);
            
            // Get loot count for this rarity
            int lootCount = getLootCount(biome, rarity);
            
            ItemStack rarityItem = new ItemBuilder(rarityMaterial)
                .setDisplayName(rarity.getDisplayName())
                .setLore(Arrays.asList(
                    "&7Rarity: " + rarity.getDisplayName(),
                    "&7Available items: &f" + lootCount,
                    "",
                    "&eClick to view loot items!"
                ))
                .build();
            
            gui.setItem(slot, rarityItem);
            slotToRarityMap.put(slot, rarity); // Store the mapping
            slot += 2; // Space them out nicely
        }
        
        // Add back button
        ItemStack backItem = new ItemBuilder(Material.ARROW)
            .setDisplayName("&a&lBack to Biomes")
            .setLore(Arrays.asList("&7Return to biome selection"))
            .build();
        gui.setItem(22, backItem);
        
        // Add close button
        ItemStack closeItem = new ItemBuilder(Material.BARRIER)
            .setDisplayName("&c&lClose")
            .setLore(Arrays.asList("&7Click to close this menu"))
            .build();
        gui.setItem(26, closeItem);
        
        // Store session with slot mapping
        RarityGUISession raritySession = new RarityGUISession(GUIType.RARITY_SELECTION, biome, null, slotToRarityMap);
        activeSessions.put(player.getUniqueId(), raritySession);
        
        player.openInventory(gui);
    }
    
    public void openLootGUI(Player player, Biome biome, Rarity rarity) {
        MenuManager menuManager = plugin.getMenuManager();
        String biomeName = getFileNameFromBiome(biome);
        
        Inventory gui = Bukkit.createInventory(null, menuManager.getLootDisplaySize(), 
            menuManager.getLootDisplayTitle(biomeName, rarity.getDisplayName()));
        
        // Get all loot items for this biome and rarity (without applying chances)
        List<LootManager.LootItemDisplay> lootItems = plugin.getLootManager().getAllLootItems(biome, rarity);
        
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
                addConfigurableLore(lore, lootDisplay, biome, rarity, menuManager);
                
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
        
        // Biome info item
        Material biomeInfoMaterial = menuManager.getBiomeMaterial(biomeName);
        int biomeInfoSlot = menuManager.getNavigationSlot("loot-display", "biome-info");
        String biomeInfoDisplayName = menuManager.getNavigationDisplayName("loot-display", "biome-info");
        List<String> biomeInfoLore = menuManager.getNavigationLore("loot-display", "biome-info");
        
        // Replace placeholders in biome info
        biomeInfoDisplayName = biomeInfoDisplayName.replace("{biome_name}", formatBiomeName(biome));
        biomeInfoLore = biomeInfoLore.stream()
            .map(line -> line.replace("{biome_name}", formatBiomeName(biome)))
            .map(line -> line.replace("{rarity_display_name}", rarity.getDisplayName()))
            .map(line -> line.replace("{total_items}", String.valueOf(lootItems.size())))
            .toList();
        
        ItemStack biomeItem = new ItemBuilder(biomeInfoMaterial)
            .setDisplayName(biomeInfoDisplayName)
            .setLore(biomeInfoLore)
            .build();
        gui.setItem(biomeInfoSlot, biomeItem);
        
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
        activeSessions.put(player.getUniqueId(), new GUISession(GUIType.LOOT_DISPLAY, biome, rarity));
        
        player.openInventory(gui);
    }
    
    /**
     * Add configurable lore to loot items based on menu configuration
     */
    private void addConfigurableLore(List<Component> lore, LootManager.LootItemDisplay lootDisplay, 
                                   Biome biome, Rarity rarity, MenuManager menuManager) {
        
        // Add biome source info
        if (menuManager.shouldShowBiomeSource()) {
            String format = menuManager.getBiomeSourceFormat();
            format = format.replace("{biome_name}", formatBiomeName(biome));
            lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(format));
        }
        
        // Add rarity info
        String rarityFormat = menuManager.getRarityInfoFormat();
        rarityFormat = rarityFormat.replace("{rarity_display_name}", rarity.getDisplayName());
        lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(rarityFormat));
        
        // Add chance info
        if (menuManager.shouldShowChance()) {
            String format = menuManager.getChanceFormat();
            format = format.replace("{chance}", String.format("%.1f", lootDisplay.getChance()));
            lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(format));
        }
        
        // Add amount info
        if (lootDisplay.getMinAmount() == lootDisplay.getMaxAmount()) {
            // Single amount
            if (menuManager.shouldShowSingleAmount()) {
                String format = menuManager.getSingleAmountFormat();
                format = format.replace("{amount}", String.valueOf(lootDisplay.getMinAmount()));
                lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(format));
            }
        } else {
            // Amount range
            if (menuManager.shouldShowAmountRange()) {
                String format = menuManager.getAmountRangeFormat();
                format = format.replace("{min_amount}", String.valueOf(lootDisplay.getMinAmount()));
                format = format.replace("{max_amount}", String.valueOf(lootDisplay.getMaxAmount()));
                lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(format));
            }
        }
        
        // Add required blocks info
        if (menuManager.shouldShowRequiredBlocks()) {
            if (lootDisplay.getRequiredBlocksMined() > 0) {
                String format = menuManager.getRequiredBlocksFormat();
                format = format.replace("{required_blocks}", String.valueOf(lootDisplay.getRequiredBlocksMined()));
                lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(format));
            } else {
                String format = menuManager.getNoRequirementFormat();
                lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(format));
            }
        }
        
        // Add item type info
        if (menuManager.shouldShowItemType()) {
            String format = menuManager.getItemTypeFormat();
            format = format.replace("{item_type}", lootDisplay.getItemType());
            lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(format));
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
        File biomeFile = getBiomeFile(biome);
        
        if (biomeFile == null || !biomeFile.exists()) return rarities;
        
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
        File biomeFile = getBiomeFile(biome);
        
        if (biomeFile == null || !biomeFile.exists()) return 0;
        
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
            case RARITY_SELECTION -> handleRaritySelection(player, event.getSlot(), displayName, session.biome);
            case LOOT_DISPLAY -> handleLootDisplay(player, event.getSlot(), displayName, session.biome, session.rarity);
        }
    }
    
    private void handleBiomeSelection(Player player, int slot, String displayName) {
        if (displayName.contains("Close")) {
            player.closeInventory();
            return;
        }
        
        // Find the biome based on slot
        Set<Biome> availableBiomes = getAvailableBiomes();
        List<Biome> biomeList = new ArrayList<>(availableBiomes);
        
        if (slot < biomeList.size()) {
            Biome selectedBiome = biomeList.get(slot);
            openRarityGUI(player, selectedBiome);
        }
    }
    
    private void handleRaritySelection(Player player, int slot, String displayName, Biome biome) {
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
            
            if (selectedRarity != null && getAvailableRarities(biome).contains(selectedRarity)) {
                openLootGUI(player, biome, selectedRarity);
            }
        }
    }
    
    private void handleLootDisplay(Player player, int slot, String displayName, Biome biome, Rarity rarity) {
        if (displayName.contains("Close")) {
            player.closeInventory();
            return;
        }
        
        if (displayName.contains("Back")) {
            openRarityGUI(player, biome);
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
        final Biome biome;
        final Rarity rarity;
        
        GUISession(GUIType type, Biome biome, Rarity rarity) {
            this.type = type;
            this.biome = biome;
            this.rarity = rarity;
        }
    }
    
    private enum GUIType {
        BIOME_SELECTION,
        RARITY_SELECTION,
        LOOT_DISPLAY
    }
    
    private static class RarityGUISession extends GUISession {
        final Map<Integer, Rarity> slotToRarityMap;
        
        RarityGUISession(GUIType type, Biome biome, Rarity rarity, Map<Integer, Rarity> slotToRarityMap) {
            super(type, biome, rarity);
            this.slotToRarityMap = slotToRarityMap;
        }
    }
}