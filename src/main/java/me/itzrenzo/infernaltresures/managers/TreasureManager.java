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
        // Check if player has treasure spawning enabled
        if (!plugin.getStatsManager().isTreasureSpawningEnabled(player)) {
            return false; // Player has disabled treasure spawning
        }
        
        // Use BlockManager to determine if treasure should spawn and what rarity (with luck applied)
        Rarity rarity = InfernalTresures.getInstance().getBlockManager().shouldSpawnTreasure(minedBlock.getType(), player);
        
        if (rarity == null) {
            // No treasure should spawn based on blocks.yml configuration
            return false;
        }
        
        // Track treasure found statistics
        plugin.getStatsManager().onTreasureFound(player, rarity);
        
        // Play rarity-specific sound and particle effects
        playTreasureEffects(player, rarity, minedBlock.getLocation());
        
        // Make final variable for lambda expression
        final Rarity finalRarity = rarity;
        
        // Use the exact location of the broken block, but delay the spawning
        Location spawnLocation = minedBlock.getLocation().add(0.5, 0, 0.5); // Center the barrel in the block
        
        // Get biome
        Biome biome = minedBlock.getBiome();
        
        // Delay the treasure creation to ensure the block breaking event completes first
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Treasure treasure = new Treasure(spawnLocation, finalRarity, biome, player);
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
     * Play rarity-specific sound and particle effects
     */
    private void playTreasureEffects(Player player, Rarity rarity, Location location) {
        // Check if this rarity has effects enabled
        if (!plugin.getConfigManager().isRarityEffectEnabled(rarity)) {
            return;
        }
        
        // Play sound effect if global sound is enabled
        if (plugin.getConfigManager().isSoundEffectEnabled()) {
            try {
                String soundName = plugin.getConfigManager().getRaritySound(rarity);
                float volume = plugin.getConfigManager().getRaritySoundVolume(rarity);
                float pitch = plugin.getConfigManager().getRaritySoundPitch(rarity);
                
                org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundName);
                player.playSound(player.getLocation(), sound, volume, pitch);
                
                if (plugin.getConfigManager().isTreasureSpawningDebugEnabled()) {
                    plugin.getLogger().info("Played " + rarity + " sound: " + soundName + 
                        " (volume: " + volume + ", pitch: " + pitch + ")");
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid sound for " + rarity + " rarity: " + 
                    plugin.getConfigManager().getRaritySound(rarity));
            }
        }
        
        // Play particle effect if global particles are enabled
        if (plugin.getConfigManager().isParticleEffectEnabled()) {
            try {
                String particleName = plugin.getConfigManager().getRarityParticle(rarity);
                int count = plugin.getConfigManager().getRarityParticleCount(rarity);
                double offset = plugin.getConfigManager().getRarityParticleOffset(rarity);
                
                org.bukkit.Particle particle = org.bukkit.Particle.valueOf(particleName);
                Location effectLocation = location.clone().add(0.5, 0.5, 0.5); // Center of block
                
                player.getWorld().spawnParticle(particle, effectLocation, count, offset, offset, offset, 0);
                
                if (plugin.getConfigManager().isTreasureSpawningDebugEnabled()) {
                    plugin.getLogger().info("Spawned " + rarity + " particles: " + particleName + 
                        " (count: " + count + ", offset: " + offset + ")");
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid particle for " + rarity + " rarity: " + 
                    plugin.getConfigManager().getRarityParticle(rarity));
            }
        }
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