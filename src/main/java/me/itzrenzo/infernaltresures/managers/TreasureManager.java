package me.itzrenzo.infernaltresures.managers;

import me.itzrenzo.infernaltresures.InfernalTresures;
import me.itzrenzo.infernaltresures.models.Rarity;
import me.itzrenzo.infernaltresures.models.Treasure;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class TreasureManager {
    
    private final InfernalTresures plugin;
    private final Map<UUID, Treasure> activeTreasures = new HashMap<>();
    
    public TreasureManager(InfernalTresures plugin) {
        this.plugin = plugin;
    }
    
    public Rarity getRandomRarity() {
        int totalWeight = Rarity.getTotalWeight();
        int randomWeight = ThreadLocalRandom.current().nextInt(totalWeight);
        
        int currentWeight = 0;
        for (Rarity rarity : Rarity.values()) {
            currentWeight += rarity.getChance();
            if (randomWeight < currentWeight) {
                return rarity;
            }
        }
        
        // Default to common if something went wrong
        return Rarity.COMMON;
    }
    
    public boolean trySpawnTreasure(Block minedBlock, Player player) {
        // Use BlockManager to determine if treasure should spawn and what rarity
        Rarity rarity = InfernalTresures.getInstance().getBlockManager().shouldSpawnTreasure(minedBlock.getType());
        
        if (rarity == null) {
            // Check if we should fall back to global spawn chance system
            if (!InfernalTresures.getInstance().getBlockManager().isUsingBlockSpecificChances()) {
                // Fall back to global system
                int chancePercent = plugin.getConfigManager().getTreasureSpawnChance();
                
                if (ThreadLocalRandom.current().nextInt(100) >= chancePercent) {
                    return false;
                }
                
                // Use global rarity distribution
                rarity = getRandomRarity();
            } else {
                // Block-specific system determined no treasure should spawn
                return false;
            }
        }
        
        // Make final variable for lambda expression
        final Rarity finalRarity = rarity;
        
        // Use the exact location of the broken block, but delay the spawning
        Location spawnLocation = minedBlock.getLocation().add(0.5, 0, 0.5); // Center the barrel in the block
        
        // Get biome
        Biome biome = minedBlock.getBiome();
        
        // Delay the treasure creation to ensure the block breaking event completes first
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Treasure treasure = new Treasure(spawnLocation, finalRarity, biome);
            activeTreasures.put(treasure.getId(), treasure);
        }, 1L); // 1 tick delay
        
        // Announce to player immediately
        Component message = InfernalTresures.getInstance().getMessageManager().getTreasureFoundMessage(finalRarity, finalRarity.getDespawnTime());
        player.sendMessage(message);
        
        // Check if we should announce this treasure to the server (configurable per rarity)
        if (shouldAnnounce(finalRarity)) {
            announceToServer(player, finalRarity, biome);
        }
        
        return true;
    }
    
    /**
     * Check if this rarity level should be announced to the server
     */
    private boolean shouldAnnounce(Rarity rarity) {
        return plugin.getConfigManager().isTreasureAnnouncementEnabled(rarity);
    }
    
    /**
     * Announce a rare treasure find to all players on the server
     */
    private void announceToServer(Player finder, Rarity rarity, Biome biome) {
        Component announcement = InfernalTresures.getInstance().getMessageManager()
            .getTreasureAnnouncementMessage(finder.getName(), rarity, biome);
        
        // Broadcast to all online players
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendMessage(announcement);
        }
        
        // Also log to console
        plugin.getLogger().info(String.format("%s found a %s treasure in %s!", 
            finder.getName(), rarity.name(), formatBiomeName(biome)));
    }
    
    /**
     * Format biome name for display
     */
    private String formatBiomeName(Biome biome) {
        return InfernalTresures.getInstance().getMessageManager().getBiomeDisplayName(biome);
    }
    
    public void removeTreasure(Treasure treasure) {
        activeTreasures.remove(treasure.getId());
    }
    
    public void addTreasure(Treasure treasure) {
        activeTreasures.put(treasure.getId(), treasure);
    }
    
    public void cleanupAllTreasures() {
        for (Treasure treasure : new ArrayList<>(activeTreasures.values())) {
            treasure.despawn();
        }
        activeTreasures.clear();
    }
    
    public Map<UUID, Treasure> getActiveTreasures() {
        return Collections.unmodifiableMap(activeTreasures);
    }
}