package me.itzrenzo.infernaltresures.integrations;

import java.lang.reflect.Method;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import me.itzrenzo.infernaltresures.InfernalTresures;

/**
 * Integration with ExecutableItems plugin using SCore API with reflection fallback
 */
public class ExecutableItemsIntegration {
    
    private final InfernalTresures plugin;
    private boolean enabled = false;
    private Object executableItemsManager;
    private Method getExecutableItemMethod;
    private Method isValidIDMethod;
    private Method getExecutableItemIdsListMethod;
    
    public ExecutableItemsIntegration(InfernalTresures plugin) {
        this.plugin = plugin;
        checkExecutableItems();
    }
    
    private void checkExecutableItems() {
        Plugin executableItems = Bukkit.getPluginManager().getPlugin("ExecutableItems");
        Plugin sCore = Bukkit.getPluginManager().getPlugin("SCore");
        
        // Safe debug check with null protection
        boolean debugEnabled = false;
        try {
            debugEnabled = InfernalTresures.getInstance() != null && 
                          InfernalTresures.getInstance().getConfigManager() != null && 
                          InfernalTresures.getInstance().getConfigManager().isExecutableItemsDebugEnabled();
        } catch (Exception e) {
            // Ignore - debug not available yet
        }
        
        if (debugEnabled) {
            plugin.getLogger().info("=== ExecutableItems Integration Check ===");
            plugin.getLogger().info("SCore plugin found: " + (sCore != null));
            if (sCore != null) {
                plugin.getLogger().info("SCore enabled: " + sCore.isEnabled());
                plugin.getLogger().info("SCore version: " + sCore.getDescription().getVersion());
            }
            plugin.getLogger().info("ExecutableItems plugin found: " + (executableItems != null));
            if (executableItems != null) {
                plugin.getLogger().info("ExecutableItems enabled: " + executableItems.isEnabled());
                plugin.getLogger().info("ExecutableItems version: " + executableItems.getDescription().getVersion());
            }
        }
        
        // Check SCore first since ExecutableItems depends on it
        if (sCore == null) {
            plugin.getLogger().warning("SCore plugin not found - ExecutableItems requires SCore to function!");
            plugin.getLogger().warning("Please install SCore plugin: https://www.spigotmc.org/resources/score.84702/");
            return;
        }
        
        if (!sCore.isEnabled()) {
            plugin.getLogger().warning("SCore plugin is installed but not enabled - check console for SCore errors!");
            return;
        }
        
        if (executableItems == null) {
            plugin.getLogger().info("ExecutableItems not found - executable items will not be available");
            return;
        }
        
        if (!executableItems.isEnabled()) {
            plugin.getLogger().warning("ExecutableItems plugin is installed but not enabled!");
            plugin.getLogger().warning("This usually means:");
            plugin.getLogger().warning("1. SCore failed to load properly");
            plugin.getLogger().warning("2. ExecutableItems configuration has errors");
            plugin.getLogger().warning("3. Missing dependencies or version conflicts");
            plugin.getLogger().warning("Check your server console for ExecutableItems/SCore error messages.");
            return;
        }
        
        // Both plugins are enabled, try to integrate
        try {
            if (debugEnabled) {
                plugin.getLogger().info("Attempting to access ExecutableItems API...");
            }
            
            // Try to access ExecutableItems API using reflection (like the example)
            Class<?> executableItemsAPIClass = Class.forName("com.ssomar.score.api.executableitems.ExecutableItemsAPI");
            Method getManagerMethod = executableItemsAPIClass.getMethod("getExecutableItemsManager");
            executableItemsManager = getManagerMethod.invoke(null);
            
            if (debugEnabled) {
                plugin.getLogger().info("ExecutableItems manager obtained: " + (executableItemsManager != null));
            }
            
            // Get the manager interface methods
            Class<?> managerClass = executableItemsManager.getClass();
            getExecutableItemMethod = managerClass.getMethod("getExecutableItem", String.class);
            isValidIDMethod = managerClass.getMethod("isValidID", String.class);
            getExecutableItemIdsListMethod = managerClass.getMethod("getExecutableItemIdsList");
            
            if (debugEnabled) {
                plugin.getLogger().info("ExecutableItems API methods resolved successfully");
                
                // List available ExecutableItems for debugging
                try {
                    Object idsList = getExecutableItemIdsListMethod.invoke(executableItemsManager);
                    if (idsList instanceof java.util.List) {
                        java.util.List<?> list = (java.util.List<?>) idsList;
                        plugin.getLogger().info("Available ExecutableItems (" + list.size() + "): " + list);
                    }
                } catch (Exception e) {
                    plugin.getLogger().info("Could not list ExecutableItems: " + e.getMessage());
                }
            }
            
            enabled = true;
            plugin.getLogger().info("ExecutableItems integration enabled!");
        } catch (Exception e) {
            plugin.getLogger().warning("ExecutableItems found but API access failed: " + e.getMessage());
            if (debugEnabled) {
                e.printStackTrace();
            }
            enabled = false;
        }
    }
    
    /**
     * Check if ExecutableItems is available
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Create an ExecutableItem from id using the SCore API approach
     * @param id The ExecutableItem ID
     * @param amount The stack size
     * @return ItemStack or null if not found
     */
    public ItemStack createExecutableItem(String id, int amount) {
        if (!enabled) {
            // Safe debug check
            try {
                if (InfernalTresures.getInstance() != null && 
                    InfernalTresures.getInstance().getConfigManager() != null && 
                    InfernalTresures.getInstance().getConfigManager().isExecutableItemsDebugEnabled()) {
                    plugin.getLogger().warning("ExecutableItems integration is not enabled - cannot create ExecutableItem: " + id);
                }
            } catch (Exception e) {
                // Ignore - debug not available
            }
            return null;
        }
        
        try {
            // Safe debug check
            boolean debugEnabled = false;
            try {
                debugEnabled = InfernalTresures.getInstance() != null && 
                              InfernalTresures.getInstance().getConfigManager() != null && 
                              InfernalTresures.getInstance().getConfigManager().isExecutableItemsDebugEnabled();
            } catch (Exception e) {
                // Ignore - debug not available
            }
            
            if (debugEnabled) {
                plugin.getLogger().info("Attempting to create ExecutableItem: " + id + " (amount: " + amount + ")");
            }
            
            // Get the ExecutableItem using reflection (following the example pattern)
            // ExecutableItemsAPI.getExecutableItemsManager().getExecutableItem(id)
            Object executableItemOpt = getExecutableItemMethod.invoke(executableItemsManager, id);
            
            if (executableItemOpt == null) {
                plugin.getLogger().warning("Unknown ExecutableItem: " + id);
                return null;
            }
            
            // Check if the Optional is present (like the example: .get())
            Method isPresentMethod = executableItemOpt.getClass().getMethod("isPresent");
            Boolean isPresent = (Boolean) isPresentMethod.invoke(executableItemOpt);
            
            if (!isPresent) {
                plugin.getLogger().warning("ExecutableItem not found: " + id);
                if (InfernalTresures.getInstance().getConfigManager().isExecutableItemsDebugEnabled()) {
                    plugin.getLogger().info("Make sure the item exists in your ExecutableItems configuration");
                }
                return null;
            }
            
            // Get the ExecutableItem from the Optional
            Method getMethod = executableItemOpt.getClass().getMethod("get");
            Object executableItem = getMethod.invoke(executableItemOpt);
            
            if (debugEnabled) {
                plugin.getLogger().info("Found ExecutableItem definition: " + id);
            }
            
            // Build the item using the buildItem method (like the example)
            // .buildItem(1, player != null ? Optional.of(player) : Optional.empty())
            Class<?> executableItemClass = executableItem.getClass();
            Method buildItemMethod = executableItemClass.getMethod("buildItem", int.class, Optional.class);
            
            // Build with amount and empty player optional (since we don't have a player context)
            ItemStack item = (ItemStack) buildItemMethod.invoke(executableItem, amount, Optional.empty());
            
            if (item != null) {
                if (debugEnabled) {
                    plugin.getLogger().info("Successfully created ExecutableItem: " + id + " -> " + item.getType());
                    plugin.getLogger().info("Requested amount: " + amount + ", Actual item amount: " + item.getAmount());
                }
            } else {
                plugin.getLogger().warning("ExecutableItem builder returned null for: " + id);
            }
            
            return item;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create ExecutableItem " + id + ": " + e.getMessage());
            // Safe debug check for stack trace
            try {
                if (InfernalTresures.getInstance() != null && 
                    InfernalTresures.getInstance().getConfigManager() != null && 
                    InfernalTresures.getInstance().getConfigManager().isExecutableItemsDebugEnabled()) {
                    e.printStackTrace();
                }
            } catch (Exception debugException) {
                // Ignore - debug not available
            }
            return null;
        }
    }
    
    /**
     * Check if an ExecutableItem exists using reflection
     */
    public boolean isValidExecutableItem(String id) {
        if (!enabled) {
            return false;
        }
        
        try {
            return (Boolean) isValidIDMethod.invoke(executableItemsManager, id);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get all available ExecutableItem IDs using reflection
     */
    public String[] getAvailableItems() {
        if (!enabled) {
            return new String[0];
        }
        
        try {
            Object idsList = getExecutableItemIdsListMethod.invoke(executableItemsManager);
            if (idsList instanceof java.util.List) {
                java.util.List<?> list = (java.util.List<?>) idsList;
                return list.toArray(new String[0]);
            }
            return new String[0];
        } catch (Exception e) {
            return new String[0];
        }
    }
}