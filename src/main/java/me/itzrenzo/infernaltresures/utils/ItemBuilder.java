package me.itzrenzo.infernaltresures.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class ItemBuilder {
    private final ItemStack itemStack;
    private final ItemMeta itemMeta;
    
    public ItemBuilder(Material material) {
        this.itemStack = new ItemStack(material);
        this.itemMeta = itemStack.getItemMeta();
    }
    
    public ItemBuilder(Material material, int amount) {
        this.itemStack = new ItemStack(material, amount);
        this.itemMeta = itemStack.getItemMeta();
    }
    
    /**
     * Set the display name of the item
     */
    public ItemBuilder setDisplayName(String displayName) {
        if (displayName != null && itemMeta != null) {
            Component nameComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(displayName)
                .decoration(TextDecoration.ITALIC, false);
            itemMeta.displayName(nameComponent);
        }
        return this;
    }
    
    /**
     * Set the lore of the item
     */
    public ItemBuilder setLore(List<String> lore) {
        if (lore != null && itemMeta != null) {
            List<Component> loreComponents = new ArrayList<>();
            for (String line : lore) {
                Component loreComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(line)
                    .decoration(TextDecoration.ITALIC, false);
                loreComponents.add(loreComponent);
            }
            itemMeta.lore(loreComponents);
        }
        return this;
    }
    
    /**
     * Add enchantments to the item
     */
    public ItemBuilder addEnchantment(Enchantment enchantment, int level) {
        if (enchantment != null && itemMeta != null) {
            itemMeta.addEnchant(enchantment, level, true);
        }
        return this;
    }
    
    /**
     * Add multiple enchantments
     */
    public ItemBuilder addEnchantments(Map<Enchantment, Integer> enchantments) {
        if (enchantments != null && itemMeta != null) {
            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                itemMeta.addEnchant(entry.getKey(), entry.getValue(), true);
            }
        }
        return this;
    }
    
    /**
     * Add a random enchantment within level range
     */
    public ItemBuilder addRandomEnchantment(int minLevel, int maxLevel) {
        if (itemMeta != null) {
            List<Enchantment> applicableEnchants = new ArrayList<>();
            for (Enchantment enchant : org.bukkit.Registry.ENCHANTMENT) {
                if (enchant.canEnchantItem(itemStack)) {
                    applicableEnchants.add(enchant);
                }
            }
            
            if (!applicableEnchants.isEmpty()) {
                Enchantment randomEnchant = applicableEnchants.get(ThreadLocalRandom.current().nextInt(applicableEnchants.size()));
                int level = ThreadLocalRandom.current().nextInt(minLevel, maxLevel + 1);
                itemMeta.addEnchant(randomEnchant, level, true);
            }
        }
        return this;
    }
    
    /**
     * Add attribute modifiers
     */
    public ItemBuilder addAttribute(Attribute attribute, double amount, AttributeModifier.Operation operation) {
        return addAttribute(attribute, amount, operation, EquipmentSlot.HAND);
    }
    
    /**
     * Add attribute modifiers with specific slot
     */
    public ItemBuilder addAttribute(Attribute attribute, double amount, AttributeModifier.Operation operation, EquipmentSlot slot) {
        if (attribute != null && itemMeta != null) {
            EquipmentSlotGroup slotGroup = convertToSlotGroup(slot);
            NamespacedKey key = NamespacedKey.minecraft(attribute.getKey().getKey() + "_modifier");
            AttributeModifier modifier = new AttributeModifier(
                key,
                amount,
                operation,
                slotGroup
            );
            itemMeta.addAttributeModifier(attribute, modifier);
        }
        return this;
    }
    
    /**
     * Convert EquipmentSlot to EquipmentSlotGroup for the newer API
     */
    private EquipmentSlotGroup convertToSlotGroup(EquipmentSlot slot) {
        return switch (slot) {
            case HAND, OFF_HAND -> EquipmentSlotGroup.HAND;
            case FEET -> EquipmentSlotGroup.FEET;
            case LEGS -> EquipmentSlotGroup.LEGS;
            case CHEST -> EquipmentSlotGroup.CHEST;
            case HEAD -> EquipmentSlotGroup.HEAD;
            case BODY -> EquipmentSlotGroup.BODY;
            default -> EquipmentSlotGroup.ANY;
        };
    }
    
    /**
     * Add item flags
     */
    public ItemBuilder addItemFlags(ItemFlag... flags) {
        if (flags != null && itemMeta != null) {
            itemMeta.addItemFlags(flags);
        }
        return this;
    }
    
    /**
     * Set item as unbreakable
     */
    public ItemBuilder setUnbreakable(boolean unbreakable) {
        if (itemMeta != null) {
            itemMeta.setUnbreakable(unbreakable);
        }
        return this;
    }
    
    /**
     * Set custom model data
     */
    public ItemBuilder setCustomModelData(int customModelData) {
        if (itemMeta != null) {
            itemMeta.setCustomModelData(customModelData);
        }
        return this;
    }
    
    /**
     * Add potion effects (for potions and tipped arrows)
     */
    public ItemBuilder addPotionEffect(PotionEffectType effectType, int duration, int amplifier) {
        if (effectType != null && itemMeta instanceof PotionMeta potionMeta) {
            // For tipped arrows, we need to adjust the duration because Minecraft
            // applies a different duration calculation for arrows vs potions
            int adjustedDuration = duration;
            if (itemStack.getType() == Material.TIPPED_ARROW) {
                // Tipped arrows typically have their duration reduced by about 1/8th
                // To get the desired duration, we need to multiply by 8
                adjustedDuration = duration * 8;
            }
            
            PotionEffect effect = new PotionEffect(effectType, adjustedDuration, amplifier);
            potionMeta.addCustomEffect(effect, true);
            // Make sure to update the itemStack with the modified meta
            itemStack.setItemMeta(potionMeta);
        }
        return this;
    }
    
    /**
     * Set the amount of the item
     */
    public ItemBuilder setAmount(int amount) {
        itemStack.setAmount(Math.max(1, amount));
        return this;
    }
    
    /**
     * Hide all item flags
     */
    public ItemBuilder hideAllFlags() {
        if (itemMeta != null) {
            itemMeta.addItemFlags(ItemFlag.values());
        }
        return this;
    }
    
    /**
     * Build and return the final ItemStack
     */
    public ItemStack build() {
        if (itemMeta != null) {
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }
    
    /**
     * Create an ItemBuilder from an existing ItemStack
     */
    public static ItemBuilder from(ItemStack itemStack) {
        ItemBuilder builder = new ItemBuilder(itemStack.getType(), itemStack.getAmount());
        if (itemStack.hasItemMeta()) {
            builder.itemStack.setItemMeta(itemStack.getItemMeta());
        }
        return builder;
    }
    
    /**
     * Get enchantment by name (case-insensitive)
     */
    public static Enchantment getEnchantmentByName(String name) {
        try {
            NamespacedKey key = NamespacedKey.minecraft(name.toLowerCase());
            return org.bukkit.Registry.ENCHANTMENT.get(key);
        } catch (Exception e) {
            for (Enchantment enchant : org.bukkit.Registry.ENCHANTMENT) {
                if (enchant.getKey().getKey().equalsIgnoreCase(name)) {
                    return enchant;
                }
            }
            return null;
        }
    }
    
    /**
     * Get attribute by name (case-insensitive) 
     */
    public static Attribute getAttributeByName(String name) {
        try {
            NamespacedKey key = NamespacedKey.minecraft(name.toLowerCase());
            return org.bukkit.Registry.ATTRIBUTE.get(key);
        } catch (Exception e) {
            for (Attribute attribute : org.bukkit.Registry.ATTRIBUTE) {
                if (attribute.getKey().getKey().equalsIgnoreCase(name)) {
                    return attribute;
                }
            }
            return null;
        }
    }
    
    /**
     * Get AttributeModifier.Operation by name
     */
    public static AttributeModifier.Operation getOperationByName(String name) {
        try {
            return AttributeModifier.Operation.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return AttributeModifier.Operation.ADD_NUMBER; // Default
        }
    }
    
    /**
     * Get PotionEffectType by name
     */
    public static PotionEffectType getPotionEffectByName(String name) {
        try {
            NamespacedKey key = NamespacedKey.minecraft(name.toLowerCase());
            return org.bukkit.Registry.POTION_EFFECT_TYPE.get(key);
        } catch (Exception e) {
            for (PotionEffectType effectType : org.bukkit.Registry.POTION_EFFECT_TYPE) {
                if (effectType.getKey().getKey().equalsIgnoreCase(name)) {
                    return effectType;
                }
            }
            return null;
        }
    }
}