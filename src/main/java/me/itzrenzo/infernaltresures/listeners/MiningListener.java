package me.itzrenzo.infernaltresures.listeners;

import me.itzrenzo.infernaltresures.InfernalTresures;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class MiningListener implements Listener {
    
    private final InfernalTresures plugin;
    
    public MiningListener(InfernalTresures plugin) {
        this.plugin = plugin;
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
    
    private boolean canSpawnTreasure(Player player, Block block) {
        // Ignore in creative mode
        if (player.getGameMode() == GameMode.CREATIVE) {
            return false;
        }
        
        // Check if the block is configured in blocks.yml for treasure spawning
        return plugin.getBlockManager().isBlockConfigured(block.getType());
    }
}