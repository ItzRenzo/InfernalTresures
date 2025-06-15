package me.itzrenzo.infernaltresures.commands;

import me.itzrenzo.infernaltresures.InfernalTresures;
import me.itzrenzo.infernaltresures.models.Rarity;
import me.itzrenzo.infernaltresures.models.Treasure;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TreasureCommand implements CommandExecutor, TabCompleter {
    
    private final InfernalTresures plugin;
    
    public TreasureCommand(InfernalTresures plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "spawn" -> handleSpawnCommand(sender, args);
            case "reload" -> handleReloadCommand(sender);
            case "info" -> handleInfoCommand(sender);
            case "help" -> sendHelpMessage(sender);
            default -> {
                sender.sendMessage(Component.text("Unknown command. Use /treasure help for a list of commands.")
                    .color(NamedTextColor.RED));
                return false;
            }
        }
        
        return true;
    }
    
    private void handleSpawnCommand(CommandSender sender, String[] args) {
        // Only players can spawn treasures
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.").color(NamedTextColor.RED));
            return;
        }
        
        // Check permissions
        if (!player.hasPermission("infernaltresures.command.spawn")) {
            player.sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED));
            return;
        }
        
        // Default to COMMON rarity if not specified
        Rarity rarity = Rarity.COMMON;
        
        // Parse rarity if specified
        if (args.length >= 2) {
            try {
                rarity = Rarity.valueOf(args[1].toUpperCase());
            } catch (IllegalArgumentException e) {
                player.sendMessage(Component.text("Invalid rarity. Valid options are: ")
                    .color(NamedTextColor.RED)
                    .append(Component.text(Arrays.stream(Rarity.values())
                        .map(Enum::name)
                        .collect(Collectors.joining(", ")))));
                return;
            }
        }
        
        // Get player's location and spawn treasure there
        Location location = player.getLocation().getBlock().getLocation().add(0.5, 0, 0.5);
        
        // Debug: Show what biome we're in
        Biome biome = player.getWorld().getBiome(location);
        player.sendMessage(Component.text("Current biome: " + biome.name()).color(NamedTextColor.YELLOW));
        
        // Create treasure at player's location
        Treasure treasure = new Treasure(location, rarity, biome);
        plugin.getTreasureManager().getActiveTreasures().put(treasure.getId(), treasure);
        
        player.sendMessage(Component.text("Spawned a ")
            .color(NamedTextColor.GREEN)
            .append(Component.text(rarity.getDisplayName()).color(rarity.getColor()))
            .append(Component.text(" treasure at your location.").color(NamedTextColor.GREEN)));
    }
    
    private void handleReloadCommand(CommandSender sender) {
        // Check permissions
        if (!sender.hasPermission("infernaltresures.command.reload")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED));
            return;
        }
        
        // Reload config and loot tables
        plugin.getConfigManager().reloadConfig();
        
        sender.sendMessage(Component.text("InfernalTreasures configuration and loot tables reloaded.").color(NamedTextColor.GREEN));
    }
    
    private void handleInfoCommand(CommandSender sender) {
        // Check permissions
        if (!sender.hasPermission("infernaltresures.command.info")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED));
            return;
        }
        
        int treasureCount = plugin.getTreasureManager().getActiveTreasures().size();
        int spawnChance = plugin.getConfigManager().getTreasureSpawnChance();
        
        sender.sendMessage(Component.text("=== InfernalTreasures Info ===").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Active treasures: ").color(NamedTextColor.YELLOW)
            .append(Component.text(treasureCount).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Spawn chance: ").color(NamedTextColor.YELLOW)
            .append(Component.text(spawnChance + "%").color(NamedTextColor.WHITE)));
    }
    
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(Component.text("=== InfernalTreasures Commands ===").color(NamedTextColor.GOLD));
        
        if (sender.hasPermission("infernaltresures.command.spawn")) {
            sender.sendMessage(Component.text("/treasure spawn [rarity] ").color(NamedTextColor.YELLOW)
                .append(Component.text("- Spawn a treasure at your location").color(NamedTextColor.WHITE)));
        }
        
        if (sender.hasPermission("infernaltresures.command.reload")) {
            sender.sendMessage(Component.text("/treasure reload ").color(NamedTextColor.YELLOW)
                .append(Component.text("- Reload the plugin configuration").color(NamedTextColor.WHITE)));
        }
        
        if (sender.hasPermission("infernaltresures.command.info")) {
            sender.sendMessage(Component.text("/treasure info ").color(NamedTextColor.YELLOW)
                .append(Component.text("- Show plugin information").color(NamedTextColor.WHITE)));
        }
        
        sender.sendMessage(Component.text("/treasure help ").color(NamedTextColor.YELLOW)
            .append(Component.text("- Show this help message").color(NamedTextColor.WHITE)));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First argument: subcommand
            List<String> subcommands = new ArrayList<>();
            
            if (sender.hasPermission("infernaltresures.command.spawn")) {
                subcommands.add("spawn");
            }
            
            if (sender.hasPermission("infernaltresures.command.reload")) {
                subcommands.add("reload");
            }
            
            if (sender.hasPermission("infernaltresures.command.info")) {
                subcommands.add("info");
            }
            
            subcommands.add("help");
            
            for (String subcommand : subcommands) {
                if (subcommand.startsWith(args[0].toLowerCase())) {
                    completions.add(subcommand);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("spawn") && 
                  sender.hasPermission("infernaltresures.command.spawn")) {
            // Second argument of spawn command: rarity
            for (Rarity rarity : Rarity.values()) {
                String rarityName = rarity.name().toLowerCase();
                if (rarityName.startsWith(args[1].toLowerCase())) {
                    completions.add(rarityName);
                }
            }
        }
        
        return completions;
    }
}