package me.itzrenzo.infernaltresures.models;

import org.bukkit.Material;
import org.bukkit.block.Biome;

import java.util.List;

/**
 * Represents a biome category that groups multiple related biomes together
 * with shared loot tables and GUI representation.
 */
public class BiomeCategory {
    
    private final String name;
    private final String description;
    private final Material material;
    private final List<Biome> biomes;
    private final String fileName;
    
    public BiomeCategory(String name, String description, Material material, List<Biome> biomes, String fileName) {
        this.name = name;
        this.description = description;
        this.material = material;
        this.biomes = biomes;
        this.fileName = fileName;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public Material getMaterial() {
        return material;
    }
    
    public List<Biome> getBiomes() {
        return biomes;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    /**
     * Check if this category contains the specified biome
     */
    public boolean containsBiome(Biome biome) {
        return biomes.contains(biome);
    }
    
    @Override
    public String toString() {
        return "BiomeCategory{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", material=" + material +
                ", biomes=" + biomes +
                ", fileName='" + fileName + '\'' +
                '}';
    }
}