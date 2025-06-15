package me.itzrenzo.infernaltresures;

import me.itzrenzo.infernaltresures.commands.TreasureCommand;
import me.itzrenzo.infernaltresures.integrations.ExecutableItemsIntegration;
import me.itzrenzo.infernaltresures.integrations.MMOItemsIntegration;
import me.itzrenzo.infernaltresures.listeners.MiningListener;
import me.itzrenzo.infernaltresures.managers.BlockManager;
import me.itzrenzo.infernaltresures.managers.ConfigManager;
import me.itzrenzo.infernaltresures.managers.LootManager;
import me.itzrenzo.infernaltresures.managers.MessageManager;
import me.itzrenzo.infernaltresures.managers.TreasureManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class InfernalTresures extends JavaPlugin {
    
    private static InfernalTresures instance;
    private ConfigManager configManager;
    private MessageManager messageManager;
    private BlockManager blockManager;
    private TreasureManager treasureManager;
    private LootManager lootManager;
    private MMOItemsIntegration mmoItemsIntegration;
    private ExecutableItemsIntegration executableItemsIntegration;

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
        treasureManager = new TreasureManager(this);
        
        // Initialize integrations after managers are ready
        mmoItemsIntegration = new MMOItemsIntegration(this);
        executableItemsIntegration = new ExecutableItemsIntegration(this);
        
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
        
        // Register commands
        getCommand("treasure").setExecutor(new TreasureCommand(this));
        
        getLogger().info("InfernalTreasures has been enabled!");
    }

    @Override
    public void onDisable() {
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
    
    public MMOItemsIntegration getMMOItemsIntegration() {
        return mmoItemsIntegration;
    }
    
    public ExecutableItemsIntegration getExecutableItemsIntegration() {
        return executableItemsIntegration;
    }
}
