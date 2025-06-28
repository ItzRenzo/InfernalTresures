package me.itzrenzo.infernaltresures;

import org.bukkit.plugin.java.JavaPlugin;

import me.itzrenzo.infernaltresures.commands.TreasureCommand;
import me.itzrenzo.infernaltresures.integrations.ExecutableBlocksIntegration;
import me.itzrenzo.infernaltresures.integrations.ExecutableItemsIntegration;
import me.itzrenzo.infernaltresures.integrations.MMOItemsIntegration;
import me.itzrenzo.infernaltresures.listeners.MiningListener;
import me.itzrenzo.infernaltresures.listeners.StatsListener;
import me.itzrenzo.infernaltresures.managers.BlockManager;
import me.itzrenzo.infernaltresures.managers.ConfigManager;
import me.itzrenzo.infernaltresures.managers.LootGUIManager;
import me.itzrenzo.infernaltresures.managers.LootManager;
import me.itzrenzo.infernaltresures.managers.MenuManager;
import me.itzrenzo.infernaltresures.managers.MessageManager;
import me.itzrenzo.infernaltresures.managers.StatsManager;
import me.itzrenzo.infernaltresures.managers.TreasureManager;

public final class InfernalTresures extends JavaPlugin {
    
    private static InfernalTresures instance;
    private ConfigManager configManager;
    private MessageManager messageManager;
    private BlockManager blockManager;
    private TreasureManager treasureManager;
    private LootManager lootManager;
    private LootGUIManager lootGUIManager;
    private MenuManager menuManager;
    private StatsManager statsManager;
    private MMOItemsIntegration mmoItemsIntegration;
    private ExecutableItemsIntegration executableItemsIntegration;
    private ExecutableBlocksIntegration executableBlocksIntegration;

    @Override
    public void onEnable() {
        // Set instance
        instance = this;
        
        // Initialize managers first
        configManager = new ConfigManager(this);
        configManager.loadConfig();
        
        messageManager = new MessageManager(this);
        messageManager.loadMessages();
        
        blockManager = new BlockManager(this);
        blockManager.loadBlocks();
        
        lootManager = new LootManager(this);
        menuManager = new MenuManager(this);
        lootGUIManager = new LootGUIManager(this);
        treasureManager = new TreasureManager(this);
        statsManager = new StatsManager(this);
        
        // Initialize integrations after managers are ready
        mmoItemsIntegration = new MMOItemsIntegration(this);
        executableItemsIntegration = new ExecutableItemsIntegration(this);
        executableBlocksIntegration = new ExecutableBlocksIntegration(this);
        
        // Schedule delayed checks for integrations that might need more time to initialize
        getServer().getScheduler().runTaskLater(this, () -> {
            boolean reloadNeeded = false;
            
            // Retry ExecutableItems integration if it failed initially
            if (!executableItemsIntegration.isEnabled()) {
                getLogger().info("Retrying ExecutableItems integration after server startup...");
                executableItemsIntegration = new ExecutableItemsIntegration(this);
                if (executableItemsIntegration.isEnabled()) {
                    reloadNeeded = true;
                }
            }
            
            // Retry ExecutableBlocks integration if it failed initially
            if (!executableBlocksIntegration.isEnabled()) {
                getLogger().info("Retrying ExecutableBlocks integration after server startup...");
                executableBlocksIntegration = new ExecutableBlocksIntegration(this);
                if (executableBlocksIntegration.isEnabled()) {
                    reloadNeeded = true;
                }
            }
            
            // Retry MMOItems integration if it failed initially  
            if (!mmoItemsIntegration.isEnabled()) {
                getLogger().info("Retrying MMOItems integration after server startup...");
                mmoItemsIntegration = new MMOItemsIntegration(this);
                if (mmoItemsIntegration.isEnabled()) {
                    reloadNeeded = true;
                }
            }
            
            // Reload loot tables if any integration became available
            if (reloadNeeded) {
                getLogger().info("Reloading loot tables with updated integrations...");
                lootManager.reload();
            }
        }, 60L); // Wait 3 seconds (60 ticks) after server startup
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new MiningListener(this), this);
        getServer().getPluginManager().registerEvents(new StatsListener(this), this);
        
        // Register commands
        getCommand("treasure").setExecutor(new TreasureCommand(this));
        getCommand("lootgui").setExecutor(new TreasureCommand(this));
        
        getLogger().info("InfernalTreasures has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save stats and shutdown storage system
        if (statsManager != null) {
            statsManager.shutdown();
        }
        
        // Clean up any remaining treasures
        if (treasureManager != null) {
            treasureManager.cleanupAllTreasures();
        }
        
        // Save config if needed
        if (configManager != null) {
            configManager.saveConfig();
        }
        
        getLogger().info("InfernalTreasures has been disabled!");
    }
    
    public static InfernalTresures getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public TreasureManager getTreasureManager() {
        return treasureManager;
    }
    
    public LootManager getLootManager() {
        return lootManager;
    }
    
    public MessageManager getMessageManager() {
        return messageManager;
    }
    
    public BlockManager getBlockManager() {
        return blockManager;
    }
    
    public StatsManager getStatsManager() {
        return statsManager;
    }
    
    public MMOItemsIntegration getMMOItemsIntegration() {
        return mmoItemsIntegration;
    }
    
    public ExecutableItemsIntegration getExecutableItemsIntegration() {
        return executableItemsIntegration;
    }
    
    public ExecutableBlocksIntegration getExecutableBlocksIntegration() {
        return executableBlocksIntegration;
    }
    
    public LootGUIManager getLootGUIManager() {
        return lootGUIManager;
    }
    
    public MenuManager getMenuManager() {
        return menuManager;
    }
}
