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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MiningListener implements Listener {
    
    private final InfernalTresures plugin;
    private Set<Material> enabledBlocks;
    
    public MiningListener(InfernalTresures plugin) {
        this.plugin = plugin;
        loadEnabledBlocks();
    }
    
    private void loadEnabledBlocks() {
        enabledBlocks = new HashSet<>();
        
        String[] configBlocks = plugin.getConfigManager().getEnabledBlocks();
        for (String blockName : configBlocks) {
            try {
                Material material = Material.valueOf(blockName.toUpperCase());
                enabledBlocks.add(material);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Unknown material in config: " + blockName);
            }
        }
        
        // Fallback to default blocks if none configured
        if (enabledBlocks.isEmpty()) {
            Arrays.asList(
                Material.STONE, Material.DEEPSLATE, Material.NETHERRACK, 
                Material.END_STONE, Material.DIORITE, Material.ANDESITE, 
                Material.GRANITE, Material.BLACKSTONE, Material.BASALT
            ).forEach(enabledBlocks::add);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        // Check if block breaking should spawn treasure
        if (!canSpawnTreasure(player, block)) {
            return;
        }
        
        // Try to spawn a treasure
        boolean spawned = plugin.getTreasureManager().trySpawnTreasure(block, player);
        
        // Play sound and effects if treasure was spawned
        if (spawned && plugin.getConfigManager().isMiningEffectEnabled()) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }
    }
    
    private boolean canSpawnTreasure(Player player, Block block) {
        // Ignore in creative mode
        if (player.getGameMode() == GameMode.CREATIVE) {
            return false;
        }
        
        // Check if the block type is in our list of enabled blocks
        return enabledBlocks.contains(block.getType());
    }
}