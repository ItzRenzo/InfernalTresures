package me.itzrenzo.infernaltresures.models;

import me.itzrenzo.infernaltresures.InfernalTresures;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.UUID;

public class Treasure {
    private final UUID id;
    private final Location location;
    private final Rarity rarity;
    private final Biome biome;
    private final UUID finderId; // Player who found this treasure
    private final BukkitTask despawnTask;
    private boolean claimed = false;
    private ArmorStand hologram;
    
    public Treasure(Location location, Rarity rarity, Biome biome, org.bukkit.entity.Player finder) {
        this.id = UUID.randomUUID();
        this.location = location;
        this.rarity = rarity;
        this.biome = biome;
        this.finderId = finder != null ? finder.getUniqueId() : null;
        
        // Create the treasure barrel immediately
        spawnTreasure(finder);
        
        // Schedule despawn
        this.despawnTask = Bukkit.getScheduler().runTaskLater(
            InfernalTresures.getInstance(), 
            this::despawn, 
            rarity.getDespawnTime() * 20L
        );
    }
    
    // Backward compatibility constructor
    public Treasure(Location location, Rarity rarity, Biome biome) {
        this(location, rarity, biome, null);
    }
    
    private void spawnTreasure(org.bukkit.entity.Player finder) {
        // Place the barrel block
        Block block = location.getBlock();
        block.setType(Material.BARREL);
        
        // Create hologram above the barrel (only if enabled for this rarity)
        if (InfernalTresures.getInstance().getConfigManager().isHologramEnabledForRarity(rarity)) {
            createHologram();
        }
        
        // Generate loot using the LootManager with progression support
        // Use UUID-based generation to ensure we get correct stats even if player goes offline
        List<ItemStack> loot;
        if (finderId != null) {
            loot = InfernalTresures.getInstance().getLootManager().generateLootByUUID(biome, rarity, finderId);
        } else {
            // Fallback to regular method if no UUID (shouldn't happen in normal gameplay)
            loot = InfernalTresures.getInstance().getLootManager().generateLoot(biome, rarity, finder);
        }
        
        // Debug log
        if (InfernalTresures.getInstance().getConfigManager().isLootGenerationDebugEnabled()) {
            InfernalTresures.getInstance().getLogger().info("Generated " + loot.size() + " items for " + rarity + " treasure" +
                (finderId != null ? " for player UUID " + finderId : " (no player UUID)"));
            for (ItemStack item : loot) {
                InfernalTresures.getInstance().getLogger().info("- " + item.getType() + " x" + item.getAmount());
            }
        }
        
        // Use the working direct inventory approach with barrels
        Bukkit.getScheduler().runTaskLater(InfernalTresures.getInstance(), () -> {
            fillBarrelDirectly(location.getBlock(), loot);
        }, 3L);
    }
    
    private void createHologram() {
        // Get configurable hologram height
        double hologramHeight = InfernalTresures.getInstance().getConfigManager().getHologramHeight();
        
        // Create armor stand at configurable height above the barrel
        Location hologramLocation = location.clone().add(0, hologramHeight, 0);
        hologram = (ArmorStand) location.getWorld().spawnEntity(hologramLocation, EntityType.ARMOR_STAND);
        
        // Configure the armor stand as a hologram
        hologram.setVisible(false);           // Make armor stand invisible
        hologram.setGravity(false);          // No gravity
        hologram.setMarker(true);            // Make it a marker (no collision)
        hologram.setSmall(true);             // Make it small
        hologram.setBasePlate(false);        // Remove base plate
        hologram.setArms(false);             // Remove arms
        hologram.setCanPickupItems(false);   // Can't pickup items
        hologram.setRemoveWhenFarAway(false); // Don't despawn when players leave
        hologram.setInvulnerable(true);      // Make invulnerable
        
        // Set the hologram text using MessageManager
        Component hologramText = InfernalTresures.getInstance().getMessageManager().getHologramText(rarity, biome);
        hologram.customName(hologramText);
        hologram.setCustomNameVisible(true);
        
        if (InfernalTresures.getInstance().getConfigManager().isTreasureSpawningDebugEnabled()) {
            InfernalTresures.getInstance().getLogger().info("Created hologram for " + rarity + " treasure");
        }
    }
    
    private void fillBarrelDirectly(Block barrelBlock, List<ItemStack> loot) {
        if (InfernalTresures.getInstance().getConfigManager().isBarrelFillingDebugEnabled()) {
            InfernalTresures.getInstance().getLogger().info("=== FILLING BARREL WITH SCATTERED ITEMS ===");
        }
        
        if (barrelBlock.getType() != Material.BARREL) {
            InfernalTresures.getInstance().getLogger().warning("Block is not a barrel: " + barrelBlock.getType());
            return;
        }
        
        try {
            // First, set the barrel name using a separate approach
            if (barrelBlock.getState() instanceof org.bukkit.block.Barrel barrel) {
                Component name = InfernalTresures.getInstance().getMessageManager().getTreasureNameComponent(rarity, biome);
                barrel.customName(name);
                barrel.update(true, false); // Update the barrel name only
                if (InfernalTresures.getInstance().getConfigManager().isBarrelFillingDebugEnabled()) {
                    InfernalTresures.getInstance().getLogger().info("Set barrel name to: " + InfernalTresures.getInstance().getMessageManager().getTreasureName(rarity, biome));
                }
            }
            
            // Small delay to ensure the name is set before filling
            Bukkit.getScheduler().runTaskLater(InfernalTresures.getInstance(), () -> {
                try {
                    // Get the inventory directly from the barrel
                    org.bukkit.inventory.Inventory inventory = ((org.bukkit.inventory.InventoryHolder) barrelBlock.getState()).getInventory();
                    
                    if (InfernalTresures.getInstance().getConfigManager().isBarrelFillingDebugEnabled()) {
                        InfernalTresures.getInstance().getLogger().info("Got barrel inventory directly: " + inventory.getClass().getSimpleName());
                    }
                    
                    // Clear inventory
                    inventory.clear();
                    if (InfernalTresures.getInstance().getConfigManager().isBarrelFillingDebugEnabled()) {
                        InfernalTresures.getInstance().getLogger().info("Cleared barrel inventory");
                    }
                    
                    // Create a list of available slots (0-26 for barrel)
                    java.util.List<Integer> availableSlots = new java.util.ArrayList<>();
                    for (int i = 0; i < inventory.getSize(); i++) {
                        availableSlots.add(i);
                    }
                    
                    // Shuffle the available slots to create scattered placement
                    java.util.Collections.shuffle(availableSlots);
                    
                    // Add items to random scattered slots
                    for (int i = 0; i < loot.size() && i < availableSlots.size(); i++) {
                        ItemStack item = loot.get(i);
                        if (item != null && item.getType() != Material.AIR) {
                            int randomSlot = availableSlots.get(i);
                            inventory.setItem(randomSlot, item.clone());
                            if (InfernalTresures.getInstance().getConfigManager().isBarrelFillingDebugEnabled()) {
                                InfernalTresures.getInstance().getLogger().info("Scattered item to slot " + randomSlot + ": " + item.getType() + " x" + item.getAmount());
                            }
                        }
                    }
                    
                    // Skip the update() call for inventory since it was causing the items to disappear
                    if (InfernalTresures.getInstance().getConfigManager().isBarrelFillingDebugEnabled()) {
                        InfernalTresures.getInstance().getLogger().info("Items scattered in barrel, skipping inventory update() call");
                    }
                    
                    // Verification after a short delay
                    Bukkit.getScheduler().runTaskLater(InfernalTresures.getInstance(), () -> {
                        try {
                            org.bukkit.inventory.Inventory checkInventory = ((org.bukkit.inventory.InventoryHolder) barrelBlock.getState()).getInventory();
                            int itemCount = 0;
                            
                            if (InfernalTresures.getInstance().getConfigManager().isBarrelFillingDebugEnabled()) {
                                InfernalTresures.getInstance().getLogger().info("=== BARREL VERIFICATION ===");
                                
                                // Check barrel name
                                if (barrelBlock.getState() instanceof org.bukkit.block.Barrel checkBarrel) {
                                    Component currentName = checkBarrel.customName();
                                    InfernalTresures.getInstance().getLogger().info("Barrel name: " + (currentName != null ? "SET" : "NOT SET"));
                                }
                            }
                            
                            for (int i = 0; i < checkInventory.getSize(); i++) {
                                ItemStack item = checkInventory.getItem(i);
                                if (item != null && item.getType() != Material.AIR) {
                                    itemCount++;
                                    if (InfernalTresures.getInstance().getConfigManager().isBarrelFillingDebugEnabled()) {
                                        InfernalTresures.getInstance().getLogger().info("Slot " + i + ": " + item.getType() + " x" + item.getAmount());
                                    }
                                }
                            }
                            
                            if (InfernalTresures.getInstance().getConfigManager().isBarrelFillingDebugEnabled()) {
                                InfernalTresures.getInstance().getLogger().info("=== BARREL CONTAINS: " + itemCount + " scattered items ===");
                            }
                        } catch (Exception e) {
                            InfernalTresures.getInstance().getLogger().severe("Barrel verification failed: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }, 1L);
                    
                } catch (Exception e) {
                    InfernalTresures.getInstance().getLogger().severe("Barrel inventory filling failed: " + e.getMessage());
                    e.printStackTrace();
                }
            }, 1L); // Small delay before filling inventory
            
        } catch (Exception e) {
            InfernalTresures.getInstance().getLogger().severe("Direct barrel filling failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private String formatBiomeName(Biome biome) {
        String biomeName = biome.toString().replace("_", " ").toLowerCase();
        String[] words = biomeName.split(" ");
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
    
    public void despawn() {
        if (!claimed && location.getBlock().getType() == Material.BARREL) {
            // Check if items should drop when barrel despawns
            if (InfernalTresures.getInstance().getConfigManager().shouldDropItemsOnDespawn()) {
                dropBarrelContents();
            }
            
            location.getBlock().setType(Material.AIR);
            InfernalTresures.getInstance().getTreasureManager().removeTreasure(this);
        }
        
        // Remove hologram
        if (hologram != null && !hologram.isDead()) {
            hologram.remove();
        }
        
        if (despawnTask != null && !despawnTask.isCancelled()) {
            despawnTask.cancel();
        }
    }
    
    /**
     * Drop all items from the barrel inventory when it despawns
     */
    private void dropBarrelContents() {
        try {
            Block barrelBlock = location.getBlock();
            if (barrelBlock.getType() == Material.BARREL && 
                barrelBlock.getState() instanceof org.bukkit.block.Barrel barrel) {
                
                org.bukkit.inventory.Inventory inventory = barrel.getInventory();
                Location dropLocation = location.clone().add(0.5, 1.0, 0.5); // Drop above barrel center
                
                int droppedItems = 0;
                
                // Drop all non-null items from the barrel
                for (ItemStack item : inventory.getContents()) {
                    if (item != null && item.getType() != Material.AIR) {
                        location.getWorld().dropItemNaturally(dropLocation, item);
                        droppedItems++;
                    }
                }
                
                // Clear the inventory to prevent duplication
                inventory.clear();
                
                // Debug logging
                if (InfernalTresures.getInstance().getConfigManager().isTreasureSpawningDebugEnabled()) {
                    InfernalTresures.getInstance().getLogger().info(
                        "Dropped " + droppedItems + " items from despawning " + rarity + " treasure barrel");
                }
            }
        } catch (Exception e) {
            InfernalTresures.getInstance().getLogger().warning(
                "Failed to drop items from despawning barrel: " + e.getMessage());
        }
    }
    
    public void markClaimed() {
        this.claimed = true;
    }
    
    // Getters
    public UUID getId() { return id; }
    public Location getLocation() { return location; }
    public Rarity getRarity() { return rarity; }
    public Biome getBiome() { return biome; }
    public boolean isClaimed() { return claimed; }
}