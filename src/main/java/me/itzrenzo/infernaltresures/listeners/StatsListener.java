package me.itzrenzo.infernaltresures.listeners;

import me.itzrenzo.infernaltresures.InfernalTresures;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class StatsListener implements Listener {
    
    private final InfernalTresures plugin;
    
    public StatsListener(InfernalTresures plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getStatsManager().onPlayerJoin(event.getPlayer());
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getStatsManager().onPlayerLeave(event.getPlayer());
    }
}