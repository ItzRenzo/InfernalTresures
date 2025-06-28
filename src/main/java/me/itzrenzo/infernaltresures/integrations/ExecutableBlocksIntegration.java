package me.itzrenzo.infernaltresures.integrations;

import java.util.Optional;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import com.ssomar.executableblocks.api.ExecutableBlocksAPI;

import me.itzrenzo.infernaltresures.InfernalTresures;

/**
 * Integration with ExecutableBlocks plugin using the official ExecutableBlocks API
 * Uses the same pattern as ExecutableItems integration for consistency
 */
public class ExecutableBlocksIntegration {
    
    private final InfernalTresures plugin;
    private boolean enabled = false;
    
    public ExecutableBlocksIntegration(InfernalTresures plugin) {
        this.plugin = plugin;
        checkExecutableBlocks();
    }
    
    private void checkExecutableBlocks() {
        Plugin executableBlocks = Bukkit.getPluginManager().getPlugin("ExecutableBlocks");
        Plugin sCore = Bukkit.getPluginManager().getPlugin("SCore");
        
        // Safe debug check with null protection
        boolean debugEnabled = false;
        try {
            debugEnabled = InfernalTresures.getInstance() != null && 
                          InfernalTresures.getInstance().getConfigManager() != null && 
                          InfernalTresures.getInstance().getConfigManager().isExecutableBlocksDebugEnabled();
        } catch (Exception e) {
            // Ignore - debug not available yet
        }
        
        if (debugEnabled) {
            plugin.getLogger().info("=== ExecutableBlocks Integration Check ===");
            plugin.getLogger().info("SCore plugin found: " + (sCore != null));
            if (sCore != null) {
                plugin.getLogger().info("SCore enabled: " + sCore.isEnabled());
                plugin.getLogger().info("SCore version: " + sCore.getPluginMeta().getVersion());
            }
            plugin.getLogger().info("ExecutableBlocks plugin found: " + (executableBlocks != null));
            if (executableBlocks != null) {
                plugin.getLogger().info("ExecutableBlocks enabled: " + executableBlocks.isEnabled());
                plugin.getLogger().info("ExecutableBlocks version: " + executableBlocks.getPluginMeta().getVersion());
            }
        }
        
        if (sCore != null && sCore.isEnabled() && executableBlocks != null && executableBlocks.isEnabled()) {
            try {
                // Test the ExecutableBlocks API
                ExecutableBlocksAPI.getExecutableBlocksManager();
                enabled = true;
                plugin.getLogger().info("[InfernalTreasures] ExecutableBlocks hooked successfully!");
                if (debugEnabled) {
                    plugin.getLogger().info("ExecutableBlocks API integration successful");
                }
            } catch (Exception e) {
                if (debugEnabled) {
                    plugin.getLogger().log(Level.WARNING, "Failed to hook ExecutableBlocks API: " + e.getMessage());
                    plugin.getLogger().log(Level.WARNING, "ExecutableBlocks integration error", e);
                }
            }
        } else {
            if (debugEnabled) {
                plugin.getLogger().info("ExecutableBlocks integration requirements not met");
                plugin.getLogger().info("- SCore present and enabled: " + (sCore != null && sCore.isEnabled()));
                plugin.getLogger().info("- ExecutableBlocks present and enabled: " + (executableBlocks != null && executableBlocks.isEnabled()));
            }
        }
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Check if an ExecutableBlock ID is valid
     * Uses the ExecutableBlocks API
     */
    public boolean isValidExecutableBlockId(String id) {
        if (!enabled || id == null || id.trim().isEmpty()) {
            return false;
        }
        
        try {
            return ExecutableBlocksAPI.getExecutableBlocksManager().getExecutableBlock(id).isPresent();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error checking ExecutableBlock ID validity: " + e.getMessage());
            return false;
        }
    }
    
    // Alias method for compatibility with LootManager
    public boolean isValidExecutableBlock(String id) {
        return isValidExecutableBlockId(id);
    }
    
    /**
     * Creates an ExecutableBlock ItemStack with the specified amount
     * Uses the ExecutableBlocks API
     */
    public ItemStack createExecutableBlock(String id, int amount) {
        if (!enabled || !isValidExecutableBlockId(id)) {
            return null;
        }
        
        try {
            return ExecutableBlocksAPI.getExecutableBlocksManager().getExecutableBlock(id).get()
                    .buildItem(amount, Optional.empty());
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error creating ExecutableBlock: " + e.getMessage());
            return null;
        }
    }
}