package me.itzrenzo.infernaltresures.commands;

import me.itzrenzo.infernaltresures.InfernalTresures;
import me.itzrenzo.infernaltresures.managers.MessageManager;
import me.itzrenzo.infernaltresures.managers.StatsManager;
import me.itzrenzo.infernaltresures.models.Rarity;
import me.itzrenzo.infernaltresures.models.Treasure;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
        // Handle the /lootgui alias
        if (label.equalsIgnoreCase("lootgui")) {
            return handleLootGUICommand(sender);
        }
        
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "spawn" -> handleSpawnCommand(sender, args);
            case "reload" -> handleReloadCommand(sender);
            case "info" -> handleInfoCommand(sender);
            case "stats" -> handleStatsCommand(sender, args);
            case "loot" -> handleLootCommand(sender, args);
            case "luck" -> handleLuckCommand(sender, args);
            case "toggle" -> handleToggleCommand(sender, args);
            case "progression" -> handleProgressionCommand(sender, args);
            case "help" -> sendHelpMessage(sender);
            default -> {
                sender.sendMessage(Component.text("Unknown command. Use /treasure help for a list of commands.")
                    .color(NamedTextColor.RED));
                return false;
            }
        }
        
        return true;
    }
    
    private boolean handleLootGUICommand(CommandSender sender) {
        // Only players can use GUI
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.").color(NamedTextColor.RED));
            return true;
        }
        
        // Check permissions
        if (!player.hasPermission("infernaltresures.command.loot.gui")) {
            player.sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED));
            return true;
        }
        
        plugin.getLootGUIManager().openBiomeGUI(player);
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

        // Handle different subcommands: view stats or set stats
        if (args.length >= 3 && args[2].equalsIgnoreCase("set")) {
            // Handle stats setting: /treasure stats <player> set <stattype> <value>
            handleStatsSetCommand(sender, args);
            return;
        }

        // Original stats viewing functionality
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

    /**
     * Handle the stats set command: /treasure stats <player> set <stattype> <value>
     */
    private void handleStatsSetCommand(CommandSender sender, String[] args) {
        // Check permissions for setting stats
        if (!sender.hasPermission("infernaltresures.command.stats.set")) {
            sender.sendMessage(Component.text("You don't have permission to set player statistics.")
                .color(NamedTextColor.RED));
            return;
        }

        // Usage: /treasure stats <player> set <stattype> <value>
        if (args.length < 5) {
            sender.sendMessage(Component.text("Usage: /treasure stats <player> set <stattype> <value>")
                .color(NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Available stat types:").color(NamedTextColor.GRAY));
            sender.sendMessage(Component.text("  • blocksmined - Total blocks mined")
                .color(NamedTextColor.GRAY));
            sender.sendMessage(Component.text("  • totaltreasuresfound - Total treasures found")
                .color(NamedTextColor.GRAY));
            sender.sendMessage(Component.text("  • commontreasures - Common treasures found")
                .color(NamedTextColor.GRAY));
            sender.sendMessage(Component.text("  • raretreasures - Rare treasures found")
                .color(NamedTextColor.GRAY));
            sender.sendMessage(Component.text("  • epictreasures - Epic treasures found")
                .color(NamedTextColor.GRAY));
            sender.sendMessage(Component.text("  • legendarytreasures - Legendary treasures found")
                .color(NamedTextColor.GRAY));
            sender.sendMessage(Component.text("  • mythictreasures - Mythic treasures found")
                .color(NamedTextColor.GRAY));
            return;
        }

        // Get target player
        String targetName = args[1];
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetName);
        if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
            sender.sendMessage(Component.text("Player '" + targetName + "' not found.")
                .color(NamedTextColor.RED));
            return;
        }

        // Get stat type
        String statType = args[3].toLowerCase();
        
        // Parse value
        long value;
        try {
            value = Long.parseLong(args[4]);
            if (value < 0) {
                sender.sendMessage(Component.text("Value must be a positive number.")
                    .color(NamedTextColor.RED));
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid value. Please enter a valid number.")
                .color(NamedTextColor.RED));
            return;
        }

        // Apply the stat change
        boolean success = false;
        String statDisplayName = "";
        
        switch (statType) {
            case "blocksmined" -> {
                plugin.getStatsManager().setBlocksMined(offlinePlayer.getUniqueId(), value);
                statDisplayName = "blocks mined";
                success = true;
            }
            case "totaltreasuresfound" -> {
                plugin.getStatsManager().setTotalTreasuresFound(offlinePlayer.getUniqueId(), value);
                statDisplayName = "total treasures found";
                success = true;
            }
            case "commontreasures" -> {
                plugin.getStatsManager().setTreasuresByRarity(offlinePlayer.getUniqueId(), 
                    me.itzrenzo.infernaltresures.models.Rarity.COMMON, value);
                statDisplayName = "common treasures found";
                success = true;
            }
            case "raretreasures" -> {
                plugin.getStatsManager().setTreasuresByRarity(offlinePlayer.getUniqueId(), 
                    me.itzrenzo.infernaltresures.models.Rarity.RARE, value);
                statDisplayName = "rare treasures found";
                success = true;
            }
            case "epictreasures" -> {
                plugin.getStatsManager().setTreasuresByRarity(offlinePlayer.getUniqueId(), 
                    me.itzrenzo.infernaltresures.models.Rarity.EPIC, value);
                statDisplayName = "epic treasures found";
                success = true;
            }
            case "legendarytreasures" -> {
                plugin.getStatsManager().setTreasuresByRarity(offlinePlayer.getUniqueId(), 
                    me.itzrenzo.infernaltresures.models.Rarity.LEGENDARY, value);
                statDisplayName = "legendary treasures found";
                success = true;
            }
            case "mythictreasures" -> {
                plugin.getStatsManager().setTreasuresByRarity(offlinePlayer.getUniqueId(), 
                    me.itzrenzo.infernaltresures.models.Rarity.MYTHIC, value);
                statDisplayName = "mythic treasures found";
                success = true;
            }
            default -> {
                sender.sendMessage(Component.text("Invalid stat type: " + statType)
                    .color(NamedTextColor.RED));
                sender.sendMessage(Component.text("Valid types: blocksmined, totaltreasuresfound, commontreasures, raretreasures, epictreasures, legendarytreasures, mythictreasures")
                    .color(NamedTextColor.GRAY));
            }
        }

        if (success) {
            // Save stats to persist changes
            plugin.getStatsManager().saveStats();
            
            // Send success message
            sender.sendMessage(Component.text("✅ Successfully set ")
                .color(NamedTextColor.GREEN)
                .append(Component.text(offlinePlayer.getName()).color(NamedTextColor.YELLOW))
                .append(Component.text("'s ").color(NamedTextColor.GREEN))
                .append(Component.text(statDisplayName).color(NamedTextColor.AQUA))
                .append(Component.text(" to ").color(NamedTextColor.GREEN))
                .append(Component.text(String.valueOf(value)).color(NamedTextColor.WHITE)));

            // Notify target player if they're online
            if (offlinePlayer.isOnline()) {
                Player targetPlayer = offlinePlayer.getPlayer();
                targetPlayer.sendMessage(Component.text("📊 Your ")
                    .color(NamedTextColor.BLUE)
                    .append(Component.text(statDisplayName).color(NamedTextColor.AQUA))
                    .append(Component.text(" has been set to ").color(NamedTextColor.BLUE))
                    .append(Component.text(String.valueOf(value)).color(NamedTextColor.WHITE))
                    .append(Component.text(" by an administrator.").color(NamedTextColor.BLUE)));
            }
        }
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
        
        // Show enhanced luck information if player is online
        if (targetPlayer != null && targetPlayer.isOnline()) {
            if (stats.hasActiveLuck()) {
                long remainingSeconds = stats.getRemainingLuckSeconds();
                String luckDuration = formatDuration(remainingSeconds);
                sender.sendMessage(Component.text("🍀 Active Luck: ").color(NamedTextColor.GREEN)
                    .append(Component.text(String.format("%.1fx", stats.luckMultiplier)).color(NamedTextColor.GOLD))
                    .append(Component.text(" for ").color(NamedTextColor.GRAY))
                    .append(Component.text(luckDuration).color(NamedTextColor.WHITE)));
                
                // Show queued luck if it exists
                if (stats.hasQueuedLuck()) {
                    long queuedSeconds = stats.getQueuedLuckRemainingSeconds();
                    String queuedDuration = formatDuration(queuedSeconds);
                    sender.sendMessage(Component.text("📋 Queued Luck: ").color(NamedTextColor.YELLOW)
                        .append(Component.text(String.format("%.1fx", stats.queuedLuckMultiplier)).color(NamedTextColor.GOLD))
                        .append(Component.text(" for ").color(NamedTextColor.GRAY))
                        .append(Component.text(queuedDuration).color(NamedTextColor.WHITE))
                        .append(Component.text(" (activates when current expires)").color(NamedTextColor.GRAY)));
                }
            } else {
                // Check if there's queued luck that might activate
                if (stats.hasQueuedLuck()) {
                    long queuedSeconds = stats.getQueuedLuckRemainingSeconds();
                    String queuedDuration = formatDuration(queuedSeconds);
                    sender.sendMessage(Component.text("🍀 Treasure Luck: ").color(NamedTextColor.GREEN)
                        .append(Component.text("INACTIVE").color(NamedTextColor.GRAY)));
                    sender.sendMessage(Component.text("📋 Queued Luck: ").color(NamedTextColor.YELLOW)
                        .append(Component.text(String.format("%.1fx", stats.queuedLuckMultiplier)).color(NamedTextColor.GOLD))
                        .append(Component.text(" for ").color(NamedTextColor.GRAY))
                        .append(Component.text(queuedDuration).color(NamedTextColor.WHITE))
                        .append(Component.text(" (will activate soon)").color(NamedTextColor.GRAY)));
                } else {
                    sender.sendMessage(Component.text("🍀 Treasure Luck: ").color(NamedTextColor.GREEN)
                        .append(Component.text("INACTIVE").color(NamedTextColor.GRAY)));
                }
            }
        }
        
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
        
        if (sender.hasPermission("infernaltresures.command.loot.gui")) {
            sender.sendMessage(messageManager.getMessageComponent("help-loot"));
        }
        
        if (sender.hasPermission("infernaltresures.command.luck")) {
            sender.sendMessage(Component.text("/treasure luck <seconds> <player> [multiplier] - Give treasure luck").color(NamedTextColor.YELLOW));
        }
        
        if (sender.hasPermission("infernaltresures.command.toggle")) {
            sender.sendMessage(Component.text("/treasure toggle - Toggle treasure spawning on/off for yourself").color(NamedTextColor.YELLOW));
        }
        
        if (sender.hasPermission("infernaltresures.command.progression")) {
            sender.sendMessage(Component.text("/treasure progression [info|set <level>|debug <on|off>] - Manage loot progression").color(NamedTextColor.YELLOW));
        }
        
        sender.sendMessage(messageManager.getMessageComponent("help-help"));
    }
    
    private void handleLootCommand(CommandSender sender, String[] args) {
        // Only players can use GUI
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.").color(NamedTextColor.RED));
            return;
        }
        
        // Check basic loot permission
        if (!player.hasPermission("infernaltresures.command.loot")) {
            player.sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED));
            return;
        }
        
        // If no subcommand or "gui" subcommand, open the GUI
        if (args.length == 1 || args[1].equalsIgnoreCase("gui")) {
            // Check GUI permission
            if (!player.hasPermission("infernaltresures.command.loot.gui")) {
                player.sendMessage(Component.text("You don't have permission to use the loot GUI.").color(NamedTextColor.RED));
                return;
            }
            
            plugin.getLootGUIManager().openBiomeGUI(player);
        } else {
            // Future: Could add other loot subcommands here
            player.sendMessage(Component.text("Usage: /treasure loot [gui]").color(NamedTextColor.YELLOW));
        }
    }
    
    private void handleLuckCommand(CommandSender sender, String[] args) {
        // Check permissions
        if (!sender.hasPermission("infernaltresures.command.luck")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED));
            return;
        }
        
        // Usage: /treasure luck <seconds> <player> [multiplier]
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /treasure luck <seconds> <player> [multiplier]").color(NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Example: /treasure luck 300 Steve 2.0").color(NamedTextColor.GRAY));
            return;
        }
        
        // Parse duration in seconds
        long durationSeconds;
        try {
            durationSeconds = Long.parseLong(args[1]);
            if (durationSeconds < 1) {
                sender.sendMessage(Component.text("Duration must be at least 1 second.").color(NamedTextColor.RED));
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid duration. Please enter a number in seconds.").color(NamedTextColor.RED));
            return;
        }
        
        // Find target player
        Player targetPlayer = Bukkit.getPlayer(args[2]);
        if (targetPlayer == null) {
            sender.sendMessage(Component.text("Player '" + args[2] + "' not found or not online.").color(NamedTextColor.RED));
            return;
        }
        
        // Parse multiplier (default 2.0 if not specified)
        double multiplier = 2.0;
        if (args.length >= 4) {
            try {
                multiplier = Double.parseDouble(args[3]);
                if (multiplier < 1.0) {
                    sender.sendMessage(Component.text("Multiplier must be at least 1.0.").color(NamedTextColor.RED));
                    return;
                }
                if (multiplier > 10.0) {
                    sender.sendMessage(Component.text("Multiplier cannot exceed 10.0 for balance reasons.").color(NamedTextColor.RED));
                    return;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Invalid multiplier. Please enter a number.").color(NamedTextColor.RED));
                return;
            }
        }
        
        // Give luck to player
        plugin.getStatsManager().giveLuck(targetPlayer, durationSeconds, multiplier);
        
        // Format duration for display
        String durationDisplay = formatDuration(durationSeconds);
        
        // Send success messages
        sender.sendMessage(Component.text("Given ")
            .color(NamedTextColor.GREEN)
            .append(Component.text(targetPlayer.getName()).color(NamedTextColor.YELLOW))
            .append(Component.text(" treasure luck for ").color(NamedTextColor.GREEN))
            .append(Component.text(durationDisplay).color(NamedTextColor.YELLOW))
            .append(Component.text(" with ").color(NamedTextColor.GREEN))
            .append(Component.text(String.format("%.1fx", multiplier)).color(NamedTextColor.GOLD))
            .append(Component.text(" spawn rate.").color(NamedTextColor.GREEN)));
        
        targetPlayer.sendMessage(Component.text("✨ You have been blessed with treasure luck!").color(NamedTextColor.GOLD));
        targetPlayer.sendMessage(Component.text("Duration: ").color(NamedTextColor.YELLOW)
            .append(Component.text(durationDisplay).color(NamedTextColor.WHITE))
            .append(Component.text(" | Multiplier: ").color(NamedTextColor.YELLOW))
            .append(Component.text(String.format("%.1fx", multiplier)).color(NamedTextColor.GOLD)));
    }
    
    private void handleToggleCommand(CommandSender sender, String[] args) {
        // Only players can toggle their own treasure spawning
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.").color(NamedTextColor.RED));
            return;
        }
        
        // Check permissions
        if (!player.hasPermission("infernaltresures.command.toggle")) {
            player.sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED));
            return;
        }
        
        // Toggle treasure spawning for the player
        boolean newState = plugin.getStatsManager().toggleTreasureSpawning(player);
        
        // Send feedback message
        if (newState) {
            player.sendMessage(Component.text("✅ Treasure spawning has been ")
                .color(NamedTextColor.GREEN)
                .append(Component.text("ENABLED").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                .append(Component.text("! You will now find treasures while mining.").color(NamedTextColor.GREEN)));
        } else {
            player.sendMessage(Component.text("❌ Treasure spawning has been ")
                .color(NamedTextColor.YELLOW)
                .append(Component.text("DISABLED").color(NamedTextColor.RED).decorate(TextDecoration.BOLD))
                .append(Component.text("! You will no longer find treasures while mining.").color(NamedTextColor.YELLOW)));
        }
    }
    
    private void handleProgressionCommand(CommandSender sender, String[] args) {
        // Check permissions
        if (!sender.hasPermission("infernaltresures.command.progression")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED));
            return;
        }
        
        // Usage: /treasure progression [info|set <level>|debug <on|off>]
        if (args.length == 1) {
            // Show current progression info
            showProgressionInfo(sender);
            return;
        }
        
        String subcommand = args[1].toLowerCase();
        
        switch (subcommand) {
            case "info" -> showProgressionInfo(sender);
            case "set" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /treasure progression set <level>").color(NamedTextColor.YELLOW));
                    sender.sendMessage(Component.text("Valid levels: 1, 2, 3, 4").color(NamedTextColor.GRAY));
                    return;
                }
                
                try {
                    int level = Integer.parseInt(args[2]);
                    if (level < 1 || level > 4) {
                        sender.sendMessage(Component.text("Invalid level. Must be between 1 and 4.").color(NamedTextColor.RED));
                        return;
                    }
                    
                    int oldLevel = plugin.getConfigManager().getCurrentProgressionLevel();
                    plugin.getConfigManager().setCurrentProgressionLevel(level);
                    
                    sender.sendMessage(Component.text("✅ Progression level changed from ")
                        .color(NamedTextColor.GREEN)
                        .append(Component.text(oldLevel).color(NamedTextColor.YELLOW))
                        .append(Component.text(" to ").color(NamedTextColor.GREEN))
                        .append(Component.text(level).color(NamedTextColor.YELLOW))
                        .append(Component.text("!").color(NamedTextColor.GREEN)));
                    
                    showProgressionInfo(sender);
                    
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Invalid level. Please enter a number between 1 and 4.").color(NamedTextColor.RED));
                }
            }
            case "debug" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /treasure progression debug <on|off>").color(NamedTextColor.YELLOW));
                    return;
                }
                
                String debugState = args[2].toLowerCase();
                if (!debugState.equals("on") && !debugState.equals("off")) {
                    sender.sendMessage(Component.text("Debug state must be 'on' or 'off'.").color(NamedTextColor.RED));
                    return;
                }
                
                boolean enable = debugState.equals("on");
                plugin.getConfig().set("treasure.loot-progression.debug", enable);
                plugin.saveConfig();
                
                sender.sendMessage(Component.text("🔧 Progression debug logging ")
                    .color(NamedTextColor.BLUE)
                    .append(Component.text(enable ? "ENABLED" : "DISABLED")
                        .color(enable ? NamedTextColor.GREEN : NamedTextColor.RED))
                    .append(Component.text("!").color(NamedTextColor.BLUE)));
            }
            default -> {
                sender.sendMessage(Component.text("Usage: /treasure progression [info|set <level>|debug <on|off>]").color(NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("Examples:").color(NamedTextColor.GRAY));
                sender.sendMessage(Component.text("  /treasure progression info - Show current settings").color(NamedTextColor.GRAY));
                sender.sendMessage(Component.text("  /treasure progression set 3 - Set to level 3").color(NamedTextColor.GRAY));
                sender.sendMessage(Component.text("  /treasure progression debug on - Enable debug logging").color(NamedTextColor.GRAY));
            }
        }
    }
    
    private void showProgressionInfo(CommandSender sender) {
        int currentLevel = plugin.getConfigManager().getCurrentProgressionLevel();
        int currentSlots = plugin.getConfigManager().getCurrentProgressionSlots();
        String levelName = plugin.getConfigManager().getProgressionLevelName(currentLevel);
        String levelDescription = plugin.getConfigManager().getProgressionLevelDescription(currentLevel);
        boolean debugEnabled = plugin.getConfigManager().isProgressionDebugEnabled();
        
        sender.sendMessage(Component.text("=== Loot Progression System ===").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Current Level: ").color(NamedTextColor.YELLOW)
            .append(Component.text(currentLevel).color(NamedTextColor.WHITE))
            .append(Component.text(" (").color(NamedTextColor.GRAY))
            .append(Component.text(levelName).color(NamedTextColor.AQUA))
            .append(Component.text(")").color(NamedTextColor.GRAY)));
        
        sender.sendMessage(Component.text("Description: ").color(NamedTextColor.YELLOW)
            .append(Component.text(levelDescription).color(NamedTextColor.WHITE)));
        
        sender.sendMessage(Component.text("Barrel Slots Filled: ").color(NamedTextColor.YELLOW)
            .append(Component.text(currentSlots).color(NamedTextColor.WHITE))
            .append(Component.text("/27 slots").color(NamedTextColor.GRAY)));
        
        // Show all level options
        sender.sendMessage(Component.text("Available Levels:").color(NamedTextColor.AQUA));
        for (int level = 1; level <= 4; level++) {
            int slots = plugin.getConfigManager().getProgressionSlots(level);
            String name = plugin.getConfigManager().getProgressionLevelName(level);
            
            Component levelLine = Component.text("  ")
                .append(Component.text(level).color(level == currentLevel ? NamedTextColor.GREEN : NamedTextColor.WHITE))
                .append(Component.text(": ").color(NamedTextColor.GRAY))
                .append(Component.text(name).color(NamedTextColor.AQUA))
                .append(Component.text(" (").color(NamedTextColor.GRAY))
                .append(Component.text(slots + " slots").color(NamedTextColor.YELLOW))
                .append(Component.text(")").color(NamedTextColor.GRAY));
            
            if (level == currentLevel) {
                levelLine = levelLine.append(Component.text(" ← CURRENT").color(NamedTextColor.GREEN));
            }
            
            sender.sendMessage(levelLine);
        }
        
        sender.sendMessage(Component.text("Debug Logging: ").color(NamedTextColor.BLUE)
            .append(Component.text(debugEnabled ? "ENABLED" : "DISABLED")
                .color(debugEnabled ? NamedTextColor.GREEN : NamedTextColor.RED)));
    }
    
    /**
     * Format duration in seconds to a readable string
     */
    private String formatDuration(long seconds) {
        if (seconds < 60) {
            return seconds + " second" + (seconds != 1 ? "s" : "");
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            long remainingSeconds = seconds % 60;
            if (remainingSeconds == 0) {
                return minutes + " minute" + (minutes != 1 ? "s" : "");
            } else {
                return minutes + " minute" + (minutes != 1 ? "s" : "") + " " + 
                       remainingSeconds + " second" + (remainingSeconds != 1 ? "s" : "");
            }
        } else {
            long hours = seconds / 3600;
            long remainingMinutes = (seconds % 3600) / 60;
            if (remainingMinutes == 0) {
                return hours + " hour" + (hours != 1 ? "s" : "");
            } else {
                return hours + " hour" + (hours != 1 ? "s" : "") + " " + 
                       remainingMinutes + " minute" + (remainingMinutes != 1 ? "s" : "");
            }
        }
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
            
            if (sender.hasPermission("infernaltresures.command.luck")) {
                subcommands.add("luck");
            }
            
            if (sender.hasPermission("infernaltresures.command.toggle")) {
                subcommands.add("toggle");
            }
            
            if (sender.hasPermission("infernaltresures.command.progression")) {
                subcommands.add("progression");
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
            } else if (args[0].equalsIgnoreCase("luck") && sender.hasPermission("infernaltresures.command.luck")) {
                // Second argument of luck command: duration suggestions
                List<String> durations = List.of("60", "300", "600", "1800", "3600");
                for (String duration : durations) {
                    if (duration.startsWith(args[1])) {
                        completions.add(duration);
                    }
                }
            } else if (args[0].equalsIgnoreCase("progression") && sender.hasPermission("infernaltresures.command.progression")) {
                // Second argument of progression command: subcommands
                List<String> progressionSubcommands = List.of("info", "set", "debug");
                for (String subcommand : progressionSubcommands) {
                    if (subcommand.startsWith(args[1].toLowerCase())) {
                        completions.add(subcommand);
                    }
                }
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("luck") && sender.hasPermission("infernaltresures.command.luck")) {
                // Third argument of luck command: player name
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    String playerName = onlinePlayer.getName();
                    if (playerName.toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(playerName);
                    }
                }
            } else if (args[0].equalsIgnoreCase("stats") && sender.hasPermission("infernaltresures.command.stats.others")) {
                // Third argument of stats command: "set" subcommand
                if (args[1].toLowerCase().startsWith("set")) {
                    completions.add("set");
                }
            } else if (args[0].equalsIgnoreCase("progression") && sender.hasPermission("infernaltresures.command.progression")) {
                // Third argument of progression command: level (1-4) if setting level
                if (args[1].equalsIgnoreCase("set")) {
                    List<String> levels = List.of("1", "2", "3", "4");
                    for (String level : levels) {
                        if (level.startsWith(args[2])) {
                            completions.add(level);
                        }
                    }
                } else if (args[1].equalsIgnoreCase("debug")) {
                    // Third argument of debug subcommand: on/off
                    List<String> debugStates = List.of("on", "off");
                    for (String state : debugStates) {
                        if (state.startsWith(args[2].toLowerCase())) {
                            completions.add(state);
                        }
                    }
                }
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("luck") && sender.hasPermission("infernaltresures.command.luck")) {
                // Fourth argument of luck command: multiplier suggestions
                List<String> multipliers = List.of("1.5", "2.0", "2.5", "3.0", "5.0");
                for (String multiplier : multipliers) {
                    if (multiplier.startsWith(args[3])) {
                        completions.add(multiplier);
                    }
                }
            } else if (args[0].equalsIgnoreCase("stats") && args[2].equalsIgnoreCase("set") && sender.hasPermission("infernaltresures.command.stats.set")) {
                // Fourth argument of stats set command: stat type
                List<String> statTypes = List.of("blocksmined", "totaltreasuresfound", "commontreasures", "raretreasures", "epictreasures", "legendarytreasures", "mythictreasures");
                for (String statType : statTypes) {
                    if (statType.startsWith(args[3].toLowerCase())) {
                        completions.add(statType);
                    }
                }
            }
        } else if (args.length == 5) {
            if (args[0].equalsIgnoreCase("stats") && args[2].equalsIgnoreCase("set") && sender.hasPermission("infernaltresures.command.stats.set")) {
                // Fifth argument of stats set command: value suggestions
                List<String> valueSuggestions = List.of("0", "100", "500", "1000", "5000", "10000");
                for (String value : valueSuggestions) {
                    if (value.startsWith(args[4])) {
                        completions.add(value);
                    }
                }
            }
        }
        
        return completions;
    }
}