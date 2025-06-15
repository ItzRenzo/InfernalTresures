package me.itzrenzo.infernaltresures;

import me.itzrenzo.infernaltresures.commands.TreasureCommand;
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

    @Override
    public void onEnable() {
        // Set instance
        instance = this;
        
        // Initialize managers
        configManager = new ConfigManager(this);
        configManager.loadConfig();
        
        messageManager = new MessageManager(this);
        messageManager.loadMessages();
        
        blockManager = new BlockManager(this);
        blockManager.loadBlocks();
        
        lootManager = new LootManager(this);
        treasureManager = new TreasureManager(this);
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new MiningListener(this), this);
        
        // Register commands
        getCommand("treasure").setExecutor(new TreasureCommand(this));
        
        getLogger().info("InfernalTreasures has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save config if needed
        configManager.saveConfig();
        
        // Clean up any remaining treasures
        treasureManager.cleanupAllTreasures();
        
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
}
