# InfernalTreasures

A Minecraft plugin that adds treasure hunting while mining! Find valuable treasures hidden in blocks as you mine, with biome-specific loot and customizable rarities.

## Features

- 🎯 **Treasure Hunting**: Find treasures while mining specific blocks
- 🌍 **Biome-Specific Loot**: Different treasures spawn based on the biome you're in
- ⭐ **Rarity System**: 5 rarity tiers (Common, Rare, Epic, Legendary, Mythic) with different spawn chances
- 📦 **Treasure Barrels**: Treasures spawn as barrels with scattered loot inside
- 🏷️ **Holograms**: Customizable floating text above treasures (configurable per rarity)
- 🔧 **Highly Configurable**: Customize spawn chances, enabled blocks, biome loot tables, and more
- 🎨 **Custom Items**: Support for enchantments, attributes, custom names, lore, and effects
- ⏰ **Auto-Despawn**: Treasures automatically despawn after a configurable time
- 🎵 **Effects**: Optional sound and particle effects when treasures are found

## Installation

1. Download the latest release from the [releases page](../../releases)
2. Place the `InfernalTreasures-1.0.jar` file in your server's `plugins` folder
3. Restart your server
4. Configure the plugin by editing the generated config files

## Configuration

### Main Config (`config.yml`)

```yaml
treasure:
  spawn-chance: 15          # Base chance (0-100%) of finding treasure
  effects:
    sound: true             # Play sound when treasure is found
    particles: true         # Show particles when treasure is found
  announce-finds: true      # Announce rare finds to server
  hourly-limit: 50         # Max treasures per player per hour (0 = unlimited)

holograms:
  enabled-rarities:
    common: false           # Show/hide holograms per rarity
    rare: true
    epic: true
    legendary: true
    mythic: true
  height: 1.5              # Height above barrel
  visible-distance: 16     # Distance players can see holograms
```

### Block Configuration (`blocks.yml`)

Configure which blocks can spawn treasures and their individual chances:

```yaml
block-specific-chances:
  enabled: true             # Use block-specific chances
  fallback-chance: 1.0      # Chance for unconfigured blocks
  debug: false              # Debug logging

blocks:
  STONE:
    COMMON: 2.0
    RARE: 0.5
    EPIC: 0.1
    LEGENDARY: 0.02
    MYTHIC: 0.005
```

### Biome Loot Tables (`biomes/`)

Each biome has its own loot table with items for each rarity:

```yaml
# biomes/ocean.yml
name: "Ocean"
description: "Deep waters hiding aquatic treasures"

loot:
  COMMON:
    - material: COD
      min_amount: 3
      max_amount: 8
      chance: 80
    - material: KELP
      min_amount: 4
      max_amount: 12
      chance: 70
```

## Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/treasure help` | `infernaltresures.command.help` | Show help message |
| `/treasure spawn [rarity]` | `infernaltresures.command.spawn` | Spawn a treasure at your location |
| `/treasure reload` | `infernaltresures.command.reload` | Reload configuration |
| `/treasure info` | `infernaltresures.command.info` | Show plugin information |

## Permissions

| Permission | Description |
|------------|-------------|
| `infernaltresures.command.help` | Access to help command |
| `infernaltresures.command.spawn` | Access to spawn command |
| `infernaltresures.command.reload` | Access to reload command |
| `infernaltresures.command.info` | Access to info command |

## Supported Biomes

- Desert
- Forest
- Ocean
- Plains
- Mountains (Windswept Hills)
- Swamp
- Jungle
- Taiga
- Savanna
- Badlands
- Nether (Nether Wastes)
- End (The End)

## Rarity System

| Rarity | Color | Default Chance | Despawn Time |
|--------|-------|----------------|--------------|
| Common | Gray | 45% | 5 minutes |
| Rare | Blue | 30% | 7 minutes |
| Epic | Purple | 15% | 10 minutes |
| Legendary | Orange | 8% | 15 minutes |
| Mythic | Red | 2% | 20 minutes |

## Custom Item Features

The plugin supports advanced item customization:

- **Enchantments**: Custom enchantments with level ranges
- **Attributes**: Modify item attributes (damage, speed, etc.)
- **Custom Effects**: Potion effects for consumable items
- **Display Names**: Custom item names with color codes
- **Lore**: Multi-line item descriptions
- **Unbreakable**: Make items unbreakable
- **Custom Model Data**: Support for resource packs

## Building from Source

1. Clone the repository
2. Make sure you have Maven installed
3. Run `mvn clean package`
4. The compiled JAR will be in the `target` folder

## Requirements

- Minecraft Server 1.20+
- Java 17+
- Spigot/Paper server

## Support

If you encounter any issues or have suggestions:

1. Check the [wiki](../../wiki) for detailed guides
2. Open an [issue](../../issues) on GitHub
3. Join our Discord server (if available)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## Changelog

### Version 1.0
- Initial release
- Basic treasure hunting system
- Biome-specific loot tables
- Configurable rarity system
- Hologram support
- Command system
- Block-specific spawn chances