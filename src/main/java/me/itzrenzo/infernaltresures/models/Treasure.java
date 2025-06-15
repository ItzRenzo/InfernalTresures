package me.itzrenzo.infernaltresures.models;

import me.itzrenzo.infernaltresures.InfernalTresures;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.UUID;

public class Treasure {
    private final UUID id;
    private final Location location;
    private final Rarity rarity;
    private final Biome biome;
    private final BukkitTask despawnTask;
    private boolean claimed = false;
    
    public Treasure(Location location, Rarity rarity, Biome biome) {
        this.id = UUID.randomUUID();
        this.location = location;
        this.rarity = rarity;
        this.biome = biome;
        
        // Create the treasure barrel immediately
        spawnTreasure();
        
        // Schedule despawn
        this.despawnTask = Bukkit.getScheduler().runTaskLater(
            InfernalTresures.getInstance(), 
            this::despawn, 
            rarity.getDespawnTime() * 20L
        );
    }
    
    private void spawnTreasure() {
        // Place the barrel block
        Block block = location.getBlock();
        block.setType(Material.BARREL);
        
        // Generate loot using the LootManager
        List<ItemStack> loot = InfernalTresures.getInstance().getLootManager().generateLoot(biome, rarity);
        
        // Debug log
        InfernalTresures.getInstance().getLogger().info("Generated " + loot.size() + " items for " + rarity + " treasure");
        for (ItemStack item : loot) {
            InfernalTresures.getInstance().getLogger().info("- " + item.getType() + " x" + item.getAmount());
        }
        
        // Use the working direct inventory approach with barrels
        Bukkit.getScheduler().runTaskLater(InfernalTresures.getInstance(), () -> {
            fillBarrelDirectly(location.getBlock(), loot);
        }, 3L);
    }
    
    private void fillBarrelDirectly(Block barrelBlock, List<ItemStack> loot) {
        InfernalTresures.getInstance().getLogger().info("=== FILLING BARREL WITH SCATTERED ITEMS ===");
        
        if (barrelBlock.getType() != Material.BARREL) {
            InfernalTresures.getInstance().getLogger().warning("Block is not a barrel: " + barrelBlock.getType());
            return;
        }
        
        try {
            // Get the inventory directly from the barrel
            org.bukkit.inventory.Inventory inventory = ((org.bukkit.inventory.InventoryHolder) barrelBlock.getState()).getInventory();
            
            InfernalTresures.getInstance().getLogger().info("Got barrel inventory directly: " + inventory.getClass().getSimpleName());
            
            // Set custom name for the barrel
            if (barrelBlock.getState() instanceof org.bukkit.block.Barrel barrel) {
                Component name = Component.text(rarity.getDisplayName() + " " + formatBiomeName(biome) + " Treasure")
                    .color(rarity.getColor())
                    .decoration(TextDecoration.BOLD, true);
                barrel.customName(name);
                // Don't call update() here, just set the name
            }
            
            // Clear inventory
            inventory.clear();
            InfernalTresures.getInstance().getLogger().info("Cleared barrel inventory");
            
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
                    InfernalTresures.getInstance().getLogger().info("Scattered item to slot " + randomSlot + ": " + item.getType() + " x" + item.getAmount());
                }
            }
            
            // Skip the update() call since it was causing the items to disappear
            InfernalTresures.getInstance().getLogger().info("Items scattered in barrel, skipping update() call");
            
            // Verification after a short delay
            Bukkit.getScheduler().runTaskLater(InfernalTresures.getInstance(), () -> {
                try {
                    org.bukkit.inventory.Inventory checkInventory = ((org.bukkit.inventory.InventoryHolder) barrelBlock.getState()).getInventory();
                    int itemCount = 0;
                    InfernalTresures.getInstance().getLogger().info("=== BARREL VERIFICATION ===");
                    for (int i = 0; i < checkInventory.getSize(); i++) {
                        ItemStack item = checkInventory.getItem(i);
                        if (item != null && item.getType() != Material.AIR) {
                            itemCount++;
                            InfernalTresures.getInstance().getLogger().info("Slot " + i + ": " + item.getType() + " x" + item.getAmount());
                        }
                    }
                    InfernalTresures.getInstance().getLogger().info("=== BARREL CONTAINS: " + itemCount + " scattered items ===");
                } catch (Exception e) {
                    InfernalTresures.getInstance().getLogger().severe("Barrel verification failed: " + e.getMessage());
                    e.printStackTrace();
                }
            }, 1L);
            
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
            location.getBlock().setType(Material.AIR);
            InfernalTresures.getInstance().getTreasureManager().removeTreasure(this);
        }
        
        if (despawnTask != null && !despawnTask.isCancelled()) {
            despawnTask.cancel();
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