package me.itzrenzo.infernaltresures.models;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public enum Rarity {
    COMMON(NamedTextColor.GREEN, "Common", 50, 30),
    RARE(NamedTextColor.BLUE, "Rare", 30, 45),
    EPIC(NamedTextColor.DARK_PURPLE, "Epic", 15, 60),
    LEGENDARY(NamedTextColor.GOLD, "Legendary", 4, 90),
    MYTHIC(NamedTextColor.RED, "Mythic", 1, 120);

    private final TextColor color;
    private final String displayName;
    private final int chance; // Spawn chance weight
    private final int despawnTime; // Despawn time in seconds

    Rarity(TextColor color, String displayName, int chance, int despawnTime) {
        this.color = color;
        this.displayName = displayName;
        this.chance = chance;
        this.despawnTime = despawnTime;
    }

    public TextColor getColor() {
        return color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getChance() {
        return chance;
    }

    public int getDespawnTime() {
        return despawnTime;
    }

    // Get total weight for random calculations
    public static int getTotalWeight() {
        int total = 0;
        for (Rarity rarity : values()) {
            total += rarity.getChance();
        }
        return total;
    }
}