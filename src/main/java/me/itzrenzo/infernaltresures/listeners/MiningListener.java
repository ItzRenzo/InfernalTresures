package me.itzrenzo.infernaltresures.listeners;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import me.itzrenzo.infernaltresures.InfernalTresures;
import me.itzrenzo.infernaltresures.models.Treasure;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class MiningListener implements Listener {
    
    private final InfernalTresures plugin;
    
    public MiningListener(InfernalTresures plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTreasureBarrelBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        
        // Only check if this is a barrel
        if (block.getType() != Material.BARREL) {
            return;
        }
        
        // Check if this barrel is a treasure barrel with loot
        Treasure treasure = getTreasureAtLocation(block);
        if (treasure != null && hasTreasureLoot(block)) {
            // Cancel the event to prevent breaking
            event.setCancelled(true);
            
            // Send message to player
            player.sendMessage(Component.text("⚠️ You cannot break this treasure barrel while it still contains loot!")
                .color(NamedTextColor.RED));
            player.sendMessage(Component.text("💎 Take all the items first, then you can break it.")
                .color(NamedTextColor.YELLOW));
            
            return;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        // Track block mining statistics (for all blocks mined, not just those that spawn treasures)
        if (player.getGameMode() != GameMode.CREATIVE) {
            plugin.getStatsManager().onBlockMined(player);
        }
        
        // Check if block breaking should spawn treasure
        if (!canSpawnTreasure(player, block)) {
            return;
        }
        
        // Try to spawn treasure using the BlockManager system
        boolean treasureSpawned = plugin.getTreasureManager().trySpawnTreasure(block, player);
        
        // Play sound and effects only if treasure was actually spawned
        if (treasureSpawned && plugin.getConfigManager().isMiningEffectEnabled()) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }
    }
    
    /**
     * Check if there's a treasure at the given block location
     */
    private Treasure getTreasureAtLocation(Block block) {
        for (Treasure treasure : plugin.getTreasureManager().getActiveTreasures().values()) {
            if (treasure.getLocation().equals(block.getLocation())) {
                return treasure;
            }
        }
        return null;
    }
    
    /**
     * Check if a treasure barrel has any loot remaining
     */
    private boolean hasTreasureLoot(Block block) {
        if (block.getType() != Material.BARREL) {
            return false;
        }
        
        try {
            if (block.getState() instanceof org.bukkit.block.Barrel barrel) {
                org.bukkit.inventory.Inventory inventory = barrel.getInventory();
                
                // Check if any slot contains an item
                for (ItemStack item : inventory.getContents()) {
                    if (item != null && item.getType() != Material.AIR) {
                        return true; // Found loot
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking treasure barrel contents: " + e.getMessage());
        }
        
        return false; // No loot found or error occurred
    }

    private boolean canSpawnTreasure(Player player, Block block) {
        // Ignore in creative mode
        if (player.getGameMode() == GameMode.CREATIVE) {
            return false;
        }
        
        // Check if treasures are allowed in this world
        String worldName = block.getWorld().getName();
        if (!plugin.getConfigManager().isTreasureAllowedInWorld(worldName)) {
            return false;
        }
        
        // Check if the block is configured in blocks.yml for treasure spawning
        return plugin.getBlockManager().isBlockConfigured(block.getType());
    }
}