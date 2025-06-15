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
        // Check if treasure should spawn based on config chances
        int chancePercent = plugin.getConfigManager().getTreasureSpawnChance();
        
        if (ThreadLocalRandom.current().nextInt(100) >= chancePercent) {
            return false;
        }
        
        // Use the exact location of the broken block, but delay the spawning
        Location spawnLocation = minedBlock.getLocation().add(0.5, 0, 0.5); // Center the barrel in the block
        
        // Determine rarity and biome
        Rarity rarity = getRandomRarity();
        Biome biome = minedBlock.getBiome();
        
        // Delay the treasure creation to ensure the block breaking event completes first
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Treasure treasure = new Treasure(spawnLocation, rarity, biome);
            activeTreasures.put(treasure.getId(), treasure);
        }, 1L); // 1 tick delay
        
        // Announce to player immediately
        Component message = Component.text("You found a ")
            .append(Component.text(rarity.getDisplayName())
                .color(rarity.getColor())
                .decoration(TextDecoration.BOLD, true))
            .append(Component.text(" treasure! It will despawn in " + rarity.getDespawnTime() + " seconds."));
        
        player.sendMessage(message);
        
        return true;
    }
    
    public void removeTreasure(Treasure treasure) {
        activeTreasures.remove(treasure.getId());
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