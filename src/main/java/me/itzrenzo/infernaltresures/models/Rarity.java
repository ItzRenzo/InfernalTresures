package me.itzrenzo.infernaltresures.models;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public enum Rarity {
    COMMON(NamedTextColor.GREEN, "Common", 50),
    RARE(NamedTextColor.BLUE, "Rare", 30),
    EPIC(NamedTextColor.DARK_PURPLE, "Epic", 15),
    LEGENDARY(NamedTextColor.GOLD, "Legendary", 4),
    MYTHIC(NamedTextColor.RED, "Mythic", 1);

    private final TextColor color;
    private final String displayName;
    private final int chance; // Spawn chance weight

    Rarity(TextColor color, String displayName, int chance) {
        this.color = color;
        this.displayName = displayName;
        this.chance = chance;
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

    // Get despawn time from configuration
    public int getDespawnTime() {
        return me.itzrenzo.infernaltresures.InfernalTresures.getInstance()
            .getConfigManager().getDespawnTime(this);
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