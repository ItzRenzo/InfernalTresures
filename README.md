# InfernalTreasures

A comprehensive Minecraft plugin that adds treasure hunting while mining! Find valuable treasures hidden in blocks as you mine, with biome-specific loot, multiple item system integrations, and highly customizable features.

## 🌟 Features

- 🎯 **Treasure Hunting**: Find treasures while mining specific blocks with configurable spawn chances
- 🌍 **Biome-Specific Loot**: Different treasures spawn based on the biome you're mining in
- ⭐ **Advanced Rarity System**: 5 rarity tiers (Common, Rare, Epic, Legendary, Mythic) with individual spawn chances
- 📦 **Interactive Treasure Barrels**: Treasures spawn as barrels with scattered loot inside
- 🏷️ **Custom Holograms**: Floating text above treasures (configurable per rarity tier)
- 🔧 **Highly Configurable**: Extensive configuration options for every aspect of the plugin
- 🎨 **Advanced Item System**: Support for enchantments, attributes, custom names, lore, and potion effects
- ⏰ **Smart Auto-Despawn**: Treasures automatically despawn after configurable times per rarity
- 🎵 **Visual & Audio Effects**: Optional sound and particle effects when treasures are discovered
- 🔗 **Plugin Integrations**: Native support for MMOItems and ExecutableItems
- 📊 **Debug System**: Comprehensive debug logging with categorized output
- ⚡ **Performance Optimized**: Efficient treasure spawning and management system

## 🔌 Plugin Integrations

### MMOItems Integration
- **Native Support**: Create MMOItems as treasure rewards
- **Dynamic Detection**: Automatically detects and integrates with MMOItems
- **Validation**: Validates MMOItem existence before adding to loot pools
- **Configuration**: Use `mmo_type` and `mmo_id` in biome loot tables

### ExecutableItems Integration  
- **SCore API**: Uses official SCore API for ExecutableItems integration
- **Automatic Retry**: Smart initialization system handles plugin load timing
- **Custom Items**: Support for complex ExecutableItems as treasure rewards
- **Configuration**: Use `executable_id` in biome loot tables

## 📥 Installation

1. Download the latest release from the [releases page](../../releases)
2. Place the `InfernalTreasures-1.0.jar` file in your server's `plugins` folder
3. *Optional*: Install [MMOItems](https://www.spigotmc.org/resources/mmoitems.39267/) for advanced item support
4. *Optional*: Install [SCore](https://www.spigotmc.org/resources/score.84702/) and [ExecutableItems](https://www.spigotmc.org/resources/executableitems.77578/) for executable item support
5. Restart your server
6. Configure the plugin by editing the generated config files

## ⚙️ Configuration

### Main Config (`config.yml`)

```yaml
# Treasure spawn settings
treasure:
  spawn-chance: 15          # Base chance (0-100%) of finding treasure
  effects:
    sound: true             # Play sound when treasure is found
    particles: true         # Show particles when treasure is found
  announce-finds: true      # Announce rare finds to server
  hourly-limit: 50         # Max treasures per player per hour (0 = unlimited)

# Hologram configuration
holograms:
  enabled-rarities:
    common: false           # Show/hide holograms per rarity
    rare: false
    epic: false
    legendary: true         # Only show for legendary+
    mythic: true
  height: 1.5              # Height above barrel (blocks)
  visible-distance: 16     # Distance players can see holograms

# Mining restrictions
mining:
  enabled-blocks:           # Blocks that can spawn treasures
    - STONE
    - DEEPSLATE
    - COAL_ORE
    - IRON_ORE
    # ... more blocks
  require-pickaxe: true     # Only spawn treasures when using pickaxes
  min-y-level: -64         # Minimum Y level for spawns
  max-y-level: 320         # Maximum Y level for spawns

# Rarity chances
rarity:
  chances:
    COMMON: 60             # Percentage chances (should add to 100%)
    RARE: 25
    EPIC: 10
    LEGENDARY: 4
    MYTHIC: 1
  colors:                  # Color codes for display
    COMMON: "&f"
    RARE: "&9"
    EPIC: "&5"
    LEGENDARY: "&6"
    MYTHIC: "&c"

# Debug system
debug:
  enabled: true            # Enable debug logging
  categories:              # Specific debug categories
    loot-generation: true
    treasure-spawning: true
    mmo-items: true
    executable-items: true
    barrel-filling: true
    biome-detection: true
```

### Block Configuration (`blocks.yml`)

Configure block-specific treasure spawn chances:

```yaml
block-specific-chances:
  enabled: true             # Use block-specific chances
  fallback-chance: 1.0      # Default chance for unconfigured blocks
  debug: false              # Block-specific debug logging

blocks:
  STONE:
    COMMON: 2.0
    RARE: 0.5
    EPIC: 0.1
    LEGENDARY: 0.02
    MYTHIC: 0.005
  DIAMOND_ORE:
    COMMON: 5.0
    RARE: 2.0
    EPIC: 0.8
    LEGENDARY: 0.3
    MYTHIC: 0.1
  # ... more blocks
```

### Biome Loot Tables (`biomes/`)

Create sophisticated loot tables with multiple item types:

```yaml
# biomes/forest.yml
name: "Forest"
description: "Dense woodlands filled with natural treasures"

loot:
  COMMON:
    # Regular Minecraft item
    - material: OAK_LOG
      min_amount: 4
      max_amount: 12
      chance: 85
      display_name: "&6&lAncient Oak Log"
      lore:
        - "&7Wood from the oldest trees"
        - "&7in the enchanted forest."
    
    # ExecutableItem
    - executable_id: FOREST_WAND
      min_amount: 1
      max_amount: 1
      chance: 15
      display_name: "&a&lNature's Wand"
      lore:
        - "&7A mystical wand from the forest"

  RARE:
    # MMOItem
    - mmo_type: SWORD
      mmo_id: LONG_SWORD
      min_amount: 1
      max_amount: 1
      chance: 25
    
    # Advanced item with enchantments
    - material: BOW
      min_amount: 1
      max_amount: 1
      chance: 55
      display_name: "&6&lHunter's Bow"
      enchantments:
        - enchant: POWER
          level: 3
        - enchant: INFINITY
          level: 1
      attributes:
        - attribute: GENERIC_ATTACK_DAMAGE
          value: 2.0
          operation: ADD_NUMBER
```

### Message Configuration (`messages.yml`)

Customize all player-facing messages:

```yaml
treasures:
  found: "&6You found a %rarity% treasure!"
  despawn-warning: "&cYour treasure will despawn in 30 seconds!"
  despawned: "&7Your treasure has despawned."

commands:
  no-permission: "&cYou don't have permission to use this command!"
  reload-success: "&aInfernalTreasures configuration reloaded!"
  # ... more messages
```

## 🎮 Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/treasure help` | `infernaltresures.command.use` | Show help message |
| `/treasure spawn [rarity]` | `infernaltresures.command.spawn` | Spawn a treasure at your location |
| `/treasure reload` | `infernaltresures.command.reload` | Reload all configurations |
| `/treasure info` | `infernaltresures.command.info` | Show plugin and integration status |

## 🔐 Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `infernaltresures.command.use` | Access to basic commands | `true` |
| `infernaltresures.command.spawn` | Access to spawn command | `op` |
| `infernaltresures.command.reload` | Access to reload command | `op` |
| `infernaltresures.command.info` | Access to info command | `op` |

## 🌍 Supported Biomes

The plugin includes pre-configured biome loot tables for:

- **Desert** - Sand dune treasures and oasis artifacts
- **Forest** - Nature-themed items and druidic equipment  
- **Ocean** - Aquatic treasures and maritime artifacts
- **Plains** - Pastoral items and farming equipment
- **Mountains** (Windswept Hills) - Alpine treasures
- **Swamp** - Mystical bog artifacts
- **Jungle** - Tropical treasures and ancient artifacts
- **Taiga** - Cold-weather survival gear
- **Savanna** - Tribal artifacts and wildlife items
- **Badlands** - Desert mining equipment
- **Nether** (Nether Wastes) - Infernal artifacts
- **End** (The End) - Otherworldly treasures

*Custom biome configurations can be added by creating new YAML files in the `biomes/` folder.*

## ⭐ Rarity System

| Rarity | Color | Default Chance | Hologram | Despawn Time |
|--------|-------|----------------|----------|--------------|
| **Common** | &f(White) | 60% | Hidden | 5 minutes |
| **Rare** | &9(Blue) | 25% | Hidden | 7 minutes |
| **Epic** | &5(Purple) | 10% | Hidden | 10 minutes |
| **Legendary** | &6(Gold) | 4% | Visible | 15 minutes |
| **Mythic** | &c(Red) | 1% | Visible | 20 minutes |

## 🎨 Advanced Item Features

### Supported Item Types
- **Regular Minecraft Items** - Use `material` field
- **MMOItems** - Use `mmo_type` and `mmo_id` fields
- **ExecutableItems** - Use `executable_id` field

### Item Customization Options
- **Enchantments** - Custom enchantments with level ranges or random enchants
- **Attributes** - Modify item attributes (damage, speed, health, etc.)
- **Potion Effects** - Add potion effects to consumable items
- **Display Names** - Custom item names with color codes and formatting
- **Lore** - Multi-line item descriptions with color support
- **Unbreakable** - Make items unbreakable
- **Custom Model Data** - Support for resource pack models

### Example Advanced Item Configuration

```yaml
- material: NETHERITE_SWORD
  min_amount: 1
  max_amount: 1
  chance: 5
  display_name: "&c&lBlade of the Nether King"
  lore:
    - "&7Forged in the depths of the Nether"
    - "&7by ancient demonic smiths."
    - ""
    - "&c⚔ Legendary Weapon"
  enchantments:
    - enchant: SHARPNESS
      level: 7
    - enchant: FIRE_ASPECT
      level: 3
  attributes:
    - attribute: GENERIC_ATTACK_DAMAGE
      value: 15.0
      operation: ADD_NUMBER
    - attribute: GENERIC_ATTACK_SPEED
      value: -2.0
      operation: ADD_NUMBER
  unbreakable: true
  custom_model_data: 1001
```

## 🔧 Building from Source

### Prerequisites
- Java 21 or higher
- Maven 3.6 or higher
- Git

### Build Steps
```bash
# Clone the repository
git clone https://github.com/yourusername/InfernalTreasures.git
cd InfernalTreasures

# Compile and package
mvn clean package

# The compiled JAR will be in the target/ folder
```

## 📋 Requirements

- **Minecraft Server**: 1.21+ (Paper/Spigot)
- **Java**: 21 or higher
- **Optional Dependencies**:
  - MMOItems 6.10+ (for MMOItems integration)
  - SCore 5.25+ (required for ExecutableItems)
  - ExecutableItems 7.25+ (for ExecutableItems integration)

## 🐛 Troubleshooting

### Common Issues

**ExecutableItems not working:**
1. Ensure SCore is installed and enabled
2. Check that ExecutableItems loaded properly
3. Enable debug logging: `debug.categories.executable-items: true`
4. Verify your ExecutableItem IDs exist in ExecutableItems config

**MMOItems not working:**
1. Ensure MMOItems is installed and enabled  
2. Enable debug logging: `debug.categories.mmo-items: true`
3. Verify your MMOItem types and IDs exist

**Treasures not spawning:**
1. Check your `mining.enabled-blocks` configuration
2. Verify you're mining with a pickaxe (if `require-pickaxe: true`)
3. Enable debug logging: `debug.categories.treasure-spawning: true`

## 📞 Support

If you encounter issues or have suggestions:

1. **Check the [Wiki](../../wiki)** for detailed configuration guides
2. **Search [Issues](../../issues)** for existing solutions
3. **Create a new issue** with:
   - Plugin version
   - Server version (Paper/Spigot)
   - Error logs (with debug enabled)
   - Configuration files
4. **Join our Discord** (link if available)

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

### Development Guidelines
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes and test thoroughly
4. Follow the existing code style and conventions
5. Add/update documentation as needed
6. Submit a pull request with a clear description

### Code Style
- Use 4 spaces for indentation
- Follow Java naming conventions
- Add JavaDoc comments for public methods
- Keep methods focused and reasonably sized
- Use meaningful variable and method names

## 📈 Changelog

### Version 1.0.0
- ✨ **NEW**: Initial release with core treasure hunting system
- ✨ **NEW**: Biome-specific loot tables (Desert, Forest, Ocean, Plains)
- ✨ **NEW**: Advanced rarity system with 5 tiers
- ✨ **NEW**: MMOItems integration support
- ✨ **NEW**: ExecutableItems integration support
- ✨ **NEW**: Custom hologram system with per-rarity configuration
- ✨ **NEW**: Comprehensive command system with permissions
- ✨ **NEW**: Block-specific spawn chance configuration
- ✨ **NEW**: Advanced item customization (enchantments, attributes, effects)
- ✨ **NEW**: Debug system with categorized logging
- ✨ **NEW**: Auto-despawn system with configurable timers
- ✨ **NEW**: Sound and particle effects
- ✨ **NEW**: Message customization system
- ✨ **NEW**: Performance-optimized treasure management

## 🙏 Acknowledgments

- Thanks to the **MMOItems** team for their excellent API documentation
- Thanks to **Ssomar** for the SCore framework and ExecutableItems integration examples  
- Thanks to the **Paper** team for their enhanced server software
- Thanks to all beta testers and contributors

---

**Made with ❤️ for the Minecraft community**