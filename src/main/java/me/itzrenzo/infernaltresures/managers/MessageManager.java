package me.itzrenzo.infernaltresures.managers;

import me.itzrenzo.infernaltresures.InfernalTresures;
import me.itzrenzo.infernaltresures.models.Rarity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class MessageManager {
    private final InfernalTresures plugin;
    private FileConfiguration messagesConfig;
    private final Map<String, String> cachedMessages = new HashMap<>();
    
    public MessageManager(InfernalTresures plugin) {
        this.plugin = plugin;
    }
    
    public void loadMessages() {
        // Create messages.yml if it doesn't exist
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            try {
                InputStream inputStream = plugin.getResource("messages.yml");
                if (inputStream != null) {
                    Files.copy(inputStream, messagesFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    plugin.getLogger().info("Created messages.yml file");
                    inputStream.close();
                } else {
                    plugin.getLogger().warning("Could not find messages.yml in plugin jar");
                    return;
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create messages.yml: " + e.getMessage());
                return;
            }
        }
        
        // Load the messages configuration
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        cachedMessages.clear();
        
        plugin.getLogger().info("Loaded messages configuration");
    }
    
    public void reload() {
        loadMessages();
    }
    
    public String getMessage(String path) {
        if (messagesConfig == null) {
            return "Message not found: " + path;
        }
        
        return cachedMessages.computeIfAbsent(path, k -> 
            messagesConfig.getString("messages." + k, "Missing message: " + k));
    }
    
    public String getTreasureName(Rarity rarity, Biome biome) {
        if (messagesConfig == null) {
            return rarity.getDisplayName() + " " + formatBiomeName(biome) + " Treasure";
        }
        
        String template = messagesConfig.getString("treasure-names." + rarity.name().toLowerCase(), 
            "&{rarity_color}{rarity} {biome} Treasure");
        
        return replacePlaceholders(template, rarity, biome, null, null);
    }
    
    public Component getTreasureNameComponent(Rarity rarity, Biome biome) {
        String name = getTreasureName(rarity, biome);
        return LegacyComponentSerializer.legacyAmpersand().deserialize(name);
    }
    
    public Component getHologramText(Rarity rarity, Biome biome) {
        if (messagesConfig == null) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(
                "&{rarity_color}{rarity} {biome} Treasure".replace("{rarity_color}", getRarityColor(rarity))
                    .replace("{rarity}", rarity.getDisplayName())
                    .replace("{biome}", formatBiomeName(biome))
            );
        }
        
        String template = messagesConfig.getString("hologram.text", "&{rarity_color}{rarity} {biome} Treasure");
        String hologramText = replacePlaceholders(template, rarity, biome, null, null);
        return LegacyComponentSerializer.legacyAmpersand().deserialize(hologramText);
    }
    
    public String getFormattedMessage(String messagePath, Rarity rarity, Biome biome, Player player, Integer despawnTime) {
        String template = getMessage(messagePath);
        return replacePlaceholders(template, rarity, biome, player, despawnTime);
    }
    
    public Component getFormattedMessageComponent(String messagePath, Rarity rarity, Biome biome, Player player, Integer despawnTime) {
        String message = getFormattedMessage(messagePath, rarity, biome, player, despawnTime);
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    }
    
    private String replacePlaceholders(String text, Rarity rarity, Biome biome, Player player, Integer despawnTime) {
        if (text == null) return "";
        
        String result = text;
        
        // Rarity placeholders
        if (rarity != null) {
            result = result.replace("{rarity}", rarity.getDisplayName());
            result = result.replace("{rarity_color}", getRarityColor(rarity));
        }
        
        // Biome placeholders
        if (biome != null) {
            result = result.replace("{biome}", formatBiomeName(biome));
        }
        
        // Player placeholders
        if (player != null) {
            result = result.replace("{player}", player.getName());
        }
        
        // Time placeholders
        if (despawnTime != null) {
            result = result.replace("{despawn_time}", String.valueOf(despawnTime));
        }
        
        // Other placeholders
        result = result.replace("{count}", "0"); // Default, can be overridden
        result = result.replace("{chance}", "0"); // Default, can be overridden
        
        return result;
    }
    
    private String getRarityColor(Rarity rarity) {
        if (messagesConfig == null) {
            return switch (rarity) {
                case COMMON -> "&f";
                case RARE -> "&9";
                case EPIC -> "&5";
                case LEGENDARY -> "&6";
                case MYTHIC -> "&c";
            };
        }
        
        return messagesConfig.getString("formatting.rarity-colors." + rarity.name().toLowerCase(), "&f");
    }
    
    private String formatBiomeName(Biome biome) {
        if (messagesConfig == null) {
            return defaultFormatBiomeName(biome);
        }
        
        // Check for custom biome name
        String customName = messagesConfig.getString("biome-names." + biome.name().toLowerCase());
        if (customName != null) {
            return customName;
        }
        
        return defaultFormatBiomeName(biome);
    }
    
    private String defaultFormatBiomeName(Biome biome) {
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
    
    // Convenience methods for specific message types
    public Component getTreasureFoundMessage(Rarity rarity, Integer despawnTime) {
        return getFormattedMessageComponent("treasure-found", rarity, null, null, despawnTime);
    }
    
    public Component getTreasureAnnouncementMessage(String playerName, Rarity rarity, Biome biome) {
        String template = getMessage("treasure-announcement");
        String message = template.replace("{player}", playerName)
                                .replace("{rarity_color}", getRarityColor(rarity))
                                .replace("{rarity}", rarity.getDisplayName())
                                .replace("{biome}", formatBiomeName(biome));
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    }
    
    public String getBiomeDisplayName(Biome biome) {
        return formatBiomeName(biome);
    }
    
    public String getMessageWithCount(String messagePath, int count) {
        String message = getMessage(messagePath);
        return message.replace("{count}", String.valueOf(count));
    }
    
    public String getMessageWithChance(String messagePath, int chance) {
        String message = getMessage(messagePath);
        return message.replace("{chance}", String.valueOf(chance));
    }
    
    // Helper method to get a message as a Component with color code parsing
    public Component getMessageComponent(String path) {
        String message = getMessage(path);
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    }
    
    // Helper method to get a message with placeholder replacement as a Component
    public Component getMessageComponentWithPlaceholders(String path, String... replacements) {
        String message = getMessage(path);
        
        // Apply placeholder replacements in pairs (placeholder, value)
        for (int i = 0; i < replacements.length - 1; i += 2) {
            String placeholder = replacements[i];
            String value = replacements[i + 1];
            message = message.replace(placeholder, value);
        }
        
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    }
}