package me.itzrenzo.infernaltresures.integrations;

import me.itzrenzo.infernaltresures.InfernalTresures;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

/**
 * Integration with MMOItems plugin
 */
public class MMOItemsIntegration {
    
    private final InfernalTresures plugin;
    private boolean enabled = false;
    
    public MMOItemsIntegration(InfernalTresures plugin) {
        this.plugin = plugin;
        checkMMOItems();
    }
    
    private void checkMMOItems() {
        if (Bukkit.getPluginManager().getPlugin("MMOItems") != null) {
            enabled = true;
            plugin.getLogger().info("MMOItems integration enabled!");
        } else {
            plugin.getLogger().info("MMOItems not found - custom items will not be available");
        }
    }
    
    /**
     * Check if MMOItems is available
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Create an MMOItem from type and id
     * @param type The MMOItems type (SWORD, ARMOR, etc.)
     * @param id The item ID
     * @param amount The stack size
     * @return ItemStack or null if not found
     */
    public ItemStack createMMOItem(String type, String id, int amount) {
        if (!enabled) {
            plugin.getLogger().warning("MMOItems integration is not enabled - cannot create MMOItem: " + type + "." + id);
            return null;
        }
        
        try {
            plugin.getLogger().info("Attempting to create MMOItem: " + type + "." + id + " (amount: " + amount + ")");
            
            Type mmoType = Type.get(type.toUpperCase());
            if (mmoType == null) {
                plugin.getLogger().warning("Unknown MMOItems type: " + type);
                plugin.getLogger().info("Available types should include: SWORD, AXE, HELMET, CHESTPLATE, etc.");
                return null;
            }
            
            plugin.getLogger().info("Found MMOItems type: " + mmoType.getId());
            
            MMOItem mmoItem = MMOItems.plugin.getMMOItem(mmoType, id.toUpperCase());
            if (mmoItem == null) {
                plugin.getLogger().warning("Unknown MMOItems item: " + type + "." + id);
                plugin.getLogger().info("Make sure the item exists in your MMOItems configuration");
                return null;
            }
            
            plugin.getLogger().info("Found MMOItem definition: " + mmoItem.getId());
            
            ItemStack item = mmoItem.newBuilder().build();
            if (item != null) {
                item.setAmount(Math.max(1, amount));
                plugin.getLogger().info("Successfully created MMOItem: " + type + "." + id + " -> " + item.getType());
            } else {
                plugin.getLogger().warning("MMOItem builder returned null for: " + type + "." + id);
            }
            
            return item;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create MMOItem " + type + "." + id + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Check if an item type and id exists in MMOItems
     */
    public boolean isValidMMOItem(String type, String id) {
        if (!enabled) {
            return false;
        }
        
        try {
            Type mmoType = Type.get(type.toUpperCase());
            if (mmoType == null) {
                return false;
            }
            
            return MMOItems.plugin.getMMOItem(mmoType, id.toUpperCase()) != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get all available MMOItems types
     */
    public String[] getAvailableTypes() {
        if (!enabled) {
            return new String[0];
        }
        
        try {
            // MMOItems Type doesn't have values() method, so we'll return common types
            return new String[]{
                "SWORD", "AXE", "PICKAXE", "SHOVEL", "HOE",
                "HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS", 
                "BOW", "CROSSBOW", "SHIELD",
                "ACCESSORY", "CONSUMABLE", "TOOL", "MATERIAL"
            };
        } catch (Exception e) {
            return new String[0];
        }
    }
}