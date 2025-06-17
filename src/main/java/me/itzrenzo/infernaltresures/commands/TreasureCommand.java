package me.itzrenzo.infernaltresures.commands;

import me.itzrenzo.infernaltresures.InfernalTresures;
import me.itzrenzo.infernaltresures.managers.MessageManager;
import me.itzrenzo.infernaltresures.managers.StatsManager;
import me.itzrenzo.infernaltresures.models.Rarity;
import me.itzrenzo.infernaltresures.models.Treasure;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
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
            case "stats" -> handleStatsCommand(sender, args);
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
        if (InfernalTresures.getInstance().getConfigManager().isBiomeDetectionDebugEnabled()) {
            player.sendMessage(Component.text("Current biome: " + biome.name()).color(NamedTextColor.YELLOW));
        }
        
        // Create treasure at player's location
        Treasure treasure = new Treasure(location, rarity, biome);
        plugin.getTreasureManager().addTreasure(treasure);
        
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
        
        sender.sendMessage(Component.text("=== InfernalTreasures Info ===").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Active treasures: ").color(NamedTextColor.YELLOW)
            .append(Component.text(treasureCount).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Block-specific spawn chances configured in blocks.yml").color(NamedTextColor.GRAY));
    }
    
    private void handleStatsCommand(CommandSender sender, String[] args) {
        // Check basic stats permission
        if (!sender.hasPermission("infernaltresures.command.stats")) {
            sender.sendMessage(plugin.getMessageManager().getMessageComponent("no-permission"));
            return;
        }
        
        Player targetPlayer = null;
        String targetName = null;
        
        if (args.length >= 2) {
            // Check if sender has permission to view other players' stats
            if (!sender.hasPermission("infernaltresures.command.stats.others")) {
                sender.sendMessage(plugin.getMessageManager().getMessageComponent("no-permission-view-others"));
                return;
            }
            
            // Try to find the target player
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[1]);
            if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
                sender.sendMessage(plugin.getMessageManager().getMessageComponentWithPlaceholders(
                    "player-not-found", "{player}", args[1]));
                return;
            }
            
            targetPlayer = offlinePlayer.isOnline() ? offlinePlayer.getPlayer() : null;
            targetName = offlinePlayer.getName();
        } else {
            // Show sender's own stats
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getMessageManager().getMessageComponent("console-specify-player"));
                return;
            }
            targetPlayer = (Player) sender;
            targetName = sender.getName();
        }
        
        displayPlayerStats(sender, targetPlayer, targetName);
    }
    
    private void displayPlayerStats(CommandSender sender, Player targetPlayer, String targetName) {
        StatsManager.PlayerStats stats;
        
        if (targetPlayer != null) {
            // Player is online, get current stats
            stats = plugin.getStatsManager().getPlayerStats(targetPlayer);
        } else {
            // Player is offline, get stats by name lookup
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetName);
            stats = plugin.getStatsManager().getPlayerStats(offlinePlayer.getUniqueId());
        }
        
        // Calculate total treasures found
        long totalTreasures = stats.commonTreasuresFound + stats.rareTreasuresFound + 
                             stats.epicTreasuresFound + stats.legendaryTreasuresFound + 
                             stats.mythicTreasuresFound;
        
        // Get playtime (including current session if online)
        long totalMinutes = stats.minutesPlayed;
        if (targetPlayer != null && targetPlayer.isOnline()) {
            totalMinutes += plugin.getStatsManager().getCurrentSessionMinutes(targetPlayer);
        }
        
        // Convert minutes to hours and minutes for display
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        
        MessageManager messageManager = plugin.getMessageManager();
        
        // Display stats using configurable messages with proper color parsing
        sender.sendMessage(messageManager.getMessageComponentWithPlaceholders(
            "stats-header", "{player}", targetName));
        
        sender.sendMessage(messageManager.getMessageComponentWithPlaceholders(
            "stats-total-blocks-mined", "{count}", String.valueOf(stats.totalBlocksMined)));
        
        sender.sendMessage(messageManager.getMessageComponentWithPlaceholders(
            "stats-total-treasures-found", "{count}", String.valueOf(totalTreasures)));
        
        sender.sendMessage(messageManager.getMessageComponent("stats-treasure-breakdown"));
        
        sender.sendMessage(messageManager.getMessageComponentWithPlaceholders(
            "stats-common-treasures", "{count}", String.valueOf(stats.commonTreasuresFound)));
        
        sender.sendMessage(messageManager.getMessageComponentWithPlaceholders(
            "stats-rare-treasures", "{count}", String.valueOf(stats.rareTreasuresFound)));
        
        sender.sendMessage(messageManager.getMessageComponentWithPlaceholders(
            "stats-epic-treasures", "{count}", String.valueOf(stats.epicTreasuresFound)));
        
        sender.sendMessage(messageManager.getMessageComponentWithPlaceholders(
            "stats-legendary-treasures", "{count}", String.valueOf(stats.legendaryTreasuresFound)));
        
        sender.sendMessage(messageManager.getMessageComponentWithPlaceholders(
            "stats-mythic-treasures", "{count}", String.valueOf(stats.mythicTreasuresFound)));
        
        sender.sendMessage(messageManager.getMessageComponentWithPlaceholders(
            "stats-playtime", "{hours}", String.valueOf(hours), "{minutes}", String.valueOf(minutes)));
        
        if (targetPlayer != null && targetPlayer.isOnline()) {
            long sessionMinutes = plugin.getStatsManager().getCurrentSessionMinutes(targetPlayer);
            long sessionHours = sessionMinutes / 60;
            long sessionMins = sessionMinutes % 60;
            sender.sendMessage(messageManager.getMessageComponentWithPlaceholders(
                "stats-current-session", "{hours}", String.valueOf(sessionHours), "{minutes}", String.valueOf(sessionMins)));
        }
    }
    
    private void sendHelpMessage(CommandSender sender) {
        MessageManager messageManager = plugin.getMessageManager();
        sender.sendMessage(messageManager.getMessageComponent("help-header"));
        
        if (sender.hasPermission("infernaltresures.command.spawn")) {
            sender.sendMessage(messageManager.getMessageComponent("help-spawn"));
        }
        
        if (sender.hasPermission("infernaltresures.command.reload")) {
            sender.sendMessage(messageManager.getMessageComponent("help-reload"));
        }
        
        if (sender.hasPermission("infernaltresures.command.info")) {
            sender.sendMessage(messageManager.getMessageComponent("help-info"));
        }
        
        if (sender.hasPermission("infernaltresures.command.stats")) {
            sender.sendMessage(messageManager.getMessageComponent("help-stats"));
        }
        
        sender.sendMessage(messageManager.getMessageComponent("help-help"));
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
            
            if (sender.hasPermission("infernaltresures.command.stats")) {
                subcommands.add("stats");
            }
            
            subcommands.add("help");
            
            for (String subcommand : subcommands) {
                if (subcommand.startsWith(args[0].toLowerCase())) {
                    completions.add(subcommand);
                }
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("spawn") && sender.hasPermission("infernaltresures.command.spawn")) {
                // Second argument of spawn command: rarity
                for (Rarity rarity : Rarity.values()) {
                    String rarityName = rarity.name().toLowerCase();
                    if (rarityName.startsWith(args[1].toLowerCase())) {
                        completions.add(rarityName);
                    }
                }
            } else if (args[0].equalsIgnoreCase("stats") && sender.hasPermission("infernaltresures.command.stats.others")) {
                // Second argument of stats command: player name
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    String playerName = onlinePlayer.getName();
                    if (playerName.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(playerName);
                    }
                }
            }
        }
        
        return completions;
    }
}