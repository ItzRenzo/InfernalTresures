# InfernalTreasures

A comprehensive Minecraft plugin that adds treasure hunting while mining! Find valuable treasures hidden in blocks as you mine, with biome-specific loot, multiple item system integrations, customizable GUI browsers, player luck system, and highly configurable features.

## 🌟 Features

- 🎯 **Treasure Hunting**: Find treasures while mining specific blocks with configurable spawn chances
- 🌍 **Biome-Specific Loot**: Different treasures spawn based on the biome you're mining in
- ⭐ **Advanced Rarity System**: 5 rarity tiers (Common, Rare, Epic, Legendary, Mythic) with individual spawn chances
- 📦 **Interactive Treasure Barrels**: Treasures spawn as barrels with scattered loot inside
- 🖥️ **Interactive Loot GUI**: Browse all available treasures by biome and rarity with detailed information
- 🎨 **Fully Customizable Menus**: Configure GUI titles, layouts, colors, and content through YAML files
- 🏷️ **Custom Holograms**: Floating text above treasures (configurable per rarity tier)
- 🍀 **Treasure Luck System**: Admin command to boost player treasure spawn rates temporarily
- ⚙️ **Player Toggle Control**: Players can enable/disable treasure spawning for themselves
- 📊 **Comprehensive Statistics**: Track blocks mined, treasures found, playtime, and luck status
- 🔧 **Highly Configurable**: Extensive configuration options for every aspect of the plugin
- 🎨 **Advanced Item System**: Support for enchantments, attributes, custom names, lore, and potion effects
- 📊 **Player Progression**: Track blocks mined and gate items behind progression requirements
- ⏰ **Smart Auto-Despawn**: Treasures automatically despawn after configurable times per rarity
- 🎵 **Visual & Audio Effects**: Optional sound and particle effects when treasures are discovered
- 🔗 **Plugin Integrations**: Native support for MMOItems and ExecutableItems
- 📊 **Debug System**: Comprehensive debug logging with categorized output
- ⚡ **Performance Optimized**: Efficient treasure spawning and management system

## 🖥️ Interactive Loot Browser

### GUI System Features
- **📋 Biome Selection**: Browse all available biomes with custom icons and descriptions
- **⭐ Rarity Browser**: View available rarities for each biome with item counts
- **🎁 Loot Display**: See ALL possible items with detailed statistics (no random disappearing!)
- **📊 Item Information**: View drop chances, amount ranges, progression requirements, and item types
- **🎨 Full Customization**: Configure every aspect through YAML menu files

### GUI Commands
| Command | Permission | Description |
|---------|------------|-------------|
| `/lootgui` | `infernaltresures.command.use` | Open the loot browser GUI |
| `/treasure loot gui` | `infernaltresures.command.use` | Alternative command for loot GUI |

### Menu Customization
Create your own menu themes by editing files in the `menus/` folder:

**`menus/biome-selection.yml`** - Customize the biome selection screen
**`menus/rarity-selection.yml`** - Configure rarity browsing interface  
**`menus/loot-display.yml`** - Customize loot item display and information

Example menu customization:
```yaml
# menus/loot-display.yml
loot-display:
  show-details:
    chance: true              # Show drop percentages
    amount-range: true        # Show min/max amounts
    required-blocks: true     # Show progression requirements
    item-type: true          # Show if it's MMOItem/ExecutableItem
    biome-source: true       # Show source biome
  
  format:
    chance: "&7Chance: &f{chance}%"
    amount-range: "&7Amount: &f{min_amount} - {max_amount}"
    required-blocks: "&7Required Blocks: &f{required_blocks}"
    # Fully customizable with color codes!
```

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
7. **NEW**: Customize the loot browser GUI by editing files in the `menus/` folder

## ⚙️ Configuration

### Main Config (`config.yml`)

```yaml
# Treasure spawn settings
treasure:
  effects:
    sound: true             # Play sound when treasure is found
    particles: true         # Show particles when treasure is found
  hourly-limit: 0          # Max treasures per player per hour (0 = unlimited)
  
  # Loot progression system
  # Controls how many slots in the barrel are filled (barrel has 27 slots total)
  loot-progression:
    # Current progression level (1-4)
    current-level: 1
    
    # Slot configuration for each level
    levels:
      1:
        slots: 7     # Level 1: Only 7 slots are filled
        name: "Beginner"
        description: "Basic treasure progression"
      2:
        slots: 14    # Level 2: 14 slots are filled
        name: "Intermediate" 
        description: "Improved treasure progression"
      3:
        slots: 21    # Level 3: 21 slots are filled
        name: "Advanced"
        description: "Enhanced treasure progression"
      4:
        slots: 27    # Level 4: All 27 slots are filled (fastest progression)
        name: "Master"
        description: "Maximum treasure progression"
    
    # Enable debug logging for progression system
    debug: false
  
  # Per-rarity announcement settings
  announce-finds:
    common: false           # Announce common finds
    rare: false            # Announce rare finds  
    epic: false            # Announce epic finds
    legendary: true        # Announce legendary finds
    mythic: true          # Announce mythic finds

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

# Rarity despawn times (in seconds)
rarity:
  despawn-times:
    common: 300            # 5 minutes
    rare: 420              # 7 minutes
    epic: 600              # 10 minutes
    legendary: 900         # 15 minutes
    mythic: 1200           # 20 minutes

# Debug system
debug:
  enabled: false           # Enable debug logging
  categories:              # Specific debug categories
    loot-generation: true
    treasure-spawning: true
    mmo-items: true
    executable-items: true
    barrel-filling: true
    biome-detection: true
```

### Player Progression System

Gate valuable items behind progression requirements:

```yaml
# In any biome loot table
loot:
  LEGENDARY:
    - material: NETHERITE_INGOT
      min_amount: 2
      max_amount: 5
      chance: 10
      required_blocks_mined: 5000  # Player must mine 5000 blocks first
      display_name: "&6&lNether Master's Ingot"
      lore:
        - "&7Only the most experienced"
        - "&7miners can find this treasure."
```

### Menu Configuration (`menus/`)

**Full GUI Customization**: Every aspect of the loot browser can be customized!

```yaml
# menus/biome-selection.yml
gui:
  title: "&6&lTreasure Biomes"
  size: 54

biome-items:
  desert:
    material: SAND
    display-name: "&e&lDesert Treasures"
    lore:
      - "&7Hot sands hide ancient secrets"
      - "&7Click to explore desert loot!"
  # Configure each biome individually
  
navigation:
  close:
    material: BARRIER
    slot: 53
    display-name: "&c&lClose"
    lore: ["&7Click to close this menu"]
```

```yaml
# menus/loot-display.yml
loot-display:
  show-details:
    chance: true              # Show drop chances
    amount-range: true        # Show min/max amounts  
    single-amount: false      # Show amount when min=max
    required-blocks: true     # Show progression requirements
    item-type: true          # Show MMOItem/ExecutableItem info
    biome-source: true       # Show source biome

  format:
    chance: "&7Chance: &f{chance}%"
    amount-range: "&7Amount: &f{min_amount} - {max_amount}"
    required-blocks: "&7Required Blocks: &f{required_blocks}"
    no-requirement: "&7Required Blocks: &aNone"
    item-type: "&7Type: &f{item_type}"
    # All formats support full color codes!
```

## 🎮 Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/treasure help` | `infernaltresures.command.use` | Show help message |
| `/treasure spawn [rarity]` | `infernaltresures.command.spawn` | Spawn a treasure at your location |
| `/treasure reload` | `infernaltresures.command.reload` | Reload all configurations and menus |
| `/treasure info` | `infernaltresures.command.info` | Show plugin and integration status |
| `/treasure stats [player]` | `infernaltresures.command.stats` | View treasure hunting statistics |
| `/treasure stats <player> set <stattype> <value>` | `infernaltresures.command.stats.set` | Set specific statistics for a player |
| `/treasure luck <seconds> <player> [multiplier]` | `infernaltresures.command.luck` | Give temporary treasure luck to a player |
| `/treasure toggle` | `infernaltresures.command.toggle` | Toggle treasure spawning on/off for yourself |
| `/treasure progression [info\|set <level>\|debug <on\|off>]` | `infernaltresures.command.progression` | Manage loot progression system |
| `/lootgui` | `infernaltresures.command.loot.gui` | Open interactive loot browser |
| `/treasure loot gui` | `infernaltresures.command.loot` | Alternative loot browser command |

### 🍀 Treasure Luck System

Administrators can grant temporary treasure luck to players, significantly boosting their treasure spawn rates:

```bash
# Give Steve 2x treasure spawn rate for 5 minutes
/treasure luck 300 Steve 2.0

# Give Alice 3x treasure spawn rate for 30 minutes  
/treasure luck 1800 Alice 3.0

# Default multiplier is 2.0 if not specified
/treasure luck 600 Bob
```

**Features:**
- **Multiplier Range**: 1.0x to 10.0x spawn rate boost
- **Duration**: Any time in seconds (60s = 1min, 3600s = 1hr)
- **Real-time Application**: Affects all treasure spawning immediately
- **Stats Integration**: Shows active luck and remaining time in `/treasure stats`
- **Debug Logging**: See luck calculations in debug mode

### ⚙️ Player Control System

Players have full control over their treasure hunting experience:

```bash
# Toggle treasure spawning on/off for yourself
/treasure toggle
```

**Benefits:**
- **No Interruptions**: Mine without treasures spawning when you don't want them
- **Building Projects**: Focus on gathering blocks without treasure distractions  
- **Personal Preference**: Some players prefer traditional mining
- **Persistent Setting**: Choice is saved across sessions

### 📈 Loot Progression System

**NEW!** Control how much loot appears in treasure barrels with a configurable progression system:

```bash
# View current progression settings
/treasure progression info

# Set progression level (1-4)
treasure progression set 3

# Enable/disable debug logging
/treasure progression debug on
```

**Progression Levels:**
- **Level 1 (Beginner)**: 7 slots filled - Slower, controlled progression
- **Level 2 (Intermediate)**: 14 slots filled - Moderate loot amounts
- **Level 3 (Advanced)**: 21 slots filled - Generous loot rewards
- **Level 4 (Master)**: 27 slots filled - Maximum loot (full barrel)

**Features:**
- **Server-wide Control**: Set the progression level for all players
- **Smart Slot Filling**: Each slot gets a chance to roll for items from the biome's loot table
- **Item Chances Still Apply**: Individual item drop chances are still respected
- **Debug Logging**: See exactly what happens in each slot when debug is enabled
- **Instant Changes**: Progression changes apply immediately to new treasures

**Use Cases:**
- **Economy Control**: Start at Level 1 to prevent inflation, increase as server matures
- **Event Management**: Boost to Level 4 during special events for maximum rewards
- **Testing**: Use debug mode to see exactly how the system works

**Example Debug Output:**
```
=== LOOT PROGRESSION DEBUG ===
Current progression level: 3
Max slots to fill: 21
Available loot items for LEGENDARY: 8
Slot 1: Added NETHERITE_INGOT x3 (chance: 15.0%)
Slot 2: No item (chance missed: 25.0%)
Slot 3: Added ENCHANTED_BOOK x1 (chance: 40.0%)
...
Final loot count: 12/21 slots filled
=== END LOOT PROGRESSION DEBUG ===
```

## 📊 Statistics System

Track detailed treasure hunting progress with `/treasure stats`:

**Player Statistics Include:**
- 🔨 **Total Blocks Mined**: Lifetime mining progress
- 💎 **Treasures Found**: Breakdown by rarity (Common, Rare, Epic, Legendary, Mythic)
- ⏱️ **Playtime**: Total and current session time
- 🍀 **Active Luck**: Current luck multiplier and remaining duration
- ⚙️ **Treasure Toggle**: Current treasure spawning preference

**Admin Features:**
- View any player's statistics with `/treasure stats <player>`
- Monitor server-wide treasure activity
- Track player engagement and progression

### 🔧 Statistics Management System

**NEW!** Administrators can now directly modify player statistics using the stats set command:

```bash
# Set a player's total blocks mined
/treasure stats ItzRenzo set blocksmined 10000

# Set total treasures found
/treasure stats Steve set totaltreasuresfound 500

# Set specific rarity counts
/treasure stats Alice set commontreasures 100
/treasure stats Alice set raretreasures 50
/treasure stats Alice set epictreasures 20
/treasure stats Alice set legendarytreasures 10
/treasure stats Alice set mythictreasures 5
```

**Available Stat Types:**
- `blocksmined` - Total blocks mined by the player
- `totaltreasuresfound` - Total treasures found (automatically calculated from rarity counts)
- `commontreasures` - Number of common treasures found
- `raretreasures` - Number of rare treasures found  
- `epictreasures` - Number of epic treasures found
- `legendarytreasures` - Number of legendary treasures found
- `mythictreasures` - Number of mythic treasures found

**Features:**
- **Instant Updates**: Changes are applied immediately and saved to disk
- **Player Notification**: Online players are notified when their stats are modified
- **Admin Feedback**: Confirmation messages show exactly what was changed
- **Data Validation**: Values must be positive numbers, with helpful error messages
- **Tab Completion**: Full tab completion support for all stat types and values
- **Permission Control**: Requires `infernaltresures.command.stats.set` permission

**Use Cases:**
- **Event Rewards**: Grant players treasure finds for participating in events
- **Migration**: Transfer stats from other plugins or previous systems
- **Testing**: Set specific values for testing progression features
- **Corrections**: Fix incorrect stats due to bugs or data issues
- **Competitions**: Reset or adjust stats for treasure hunting competitions

**Example Admin Workflow:**
```bash
# Check current stats
/treasure stats ItzRenzo

# Set mining progress for new VIP player
/treasure stats ItzRenzo set blocksmined 5000

# Grant event participation rewards
/treasure stats ItzRenzo set legendarytreasures 10
/treasure stats ItzRenzo set mythictreasures 2

# Verify changes
/treasure stats ItzRenzo
```

## 🔐 Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `infernaltresures.command.use` | Access to basic commands | `true` |
| `infernaltresures.command.spawn` | Access to spawn command | `op` |
| `infernaltresures.command.reload` | Access to reload command | `op` |
| `infernaltresures.command.info` | Access to info command | `op` |
| `infernaltresures.command.stats` | View your own statistics | `true` |
| `infernaltresures.command.stats.others` | View other players' statistics | `op` |
| `infernaltresures.command.stats.set` | Set/modify player statistics | `op` |
| `infernaltresures.command.luck` | Give treasure luck to players | `op` |
| `infernaltresures.command.toggle` | Toggle treasure spawning for yourself | `true` |
| `infernaltresures.command.progression` | Manage loot progression system | `op` |
| `infernaltresures.command.loot` | Access to loot commands | `true` |
| `infernaltresures.command.loot.gui` | Access to loot browser GUI | `true` |

## 🌍 Supported Biomes

The plugin includes pre-configured biome loot tables for **17 biomes**:

### Overworld Biomes
- **Desert** - Sand dune treasures and oasis artifacts
- **Forest** - Nature-themed items and druidic equipment  
- **Ocean** - Aquatic treasures and maritime artifacts
- **Plains** - Pastoral items and farming equipment
- **Mountains** (Windswept Hills) - Alpine treasures and mining equipment
- **Swamp** - Mystical bog artifacts and witch brewing supplies
- **Jungle** - Tropical treasures and ancient jungle artifacts
- **Taiga** - Cold-weather survival gear and forestry items
- **Savanna** - Tribal artifacts and wildlife equipment
- **Badlands** - Desert mining equipment and geological specimens

### Nether Biomes
- **Nether Wastes** - Classic infernal artifacts and fire-resistant gear
- **Soul Sand Valley** - Soul-themed items and undead artifacts
- **Crimson Forest** - Crimson fungus materials and hoglin gear
- **Warped Forest** - Warped fungus materials and enderman artifacts
- **Basalt Deltas** - Volcanic treasures and heat-resistant equipment

### End Biomes
- **The End** - Otherworldly treasures and dragon-themed artifacts

### Legacy Support
- **Nether** (General) - Broad nether treasures for compatibility

*Custom biome configurations can be added by creating new YAML files in the `biomes/` folder. The system automatically detects and loads new biome files!*

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
- ✨ **NEW**: 17 biome-specific loot tables with unique themes
- ✨ **NEW**: Advanced rarity system with 5 tiers and custom despawn times
- ✨ **NEW**: Interactive loot browser GUI with full customization
- ✨ **NEW**: Treasure luck system for temporary spawn rate boosts
- ✨ **NEW**: Player toggle system for treasure spawning control
- ✨ **NEW**: Comprehensive statistics tracking with luck integration
- ✨ **NEW**: MMOItems integration with dynamic detection
- ✨ **NEW**: ExecutableItems integration with SCore API
- ✨ **NEW**: Custom hologram system with per-rarity configuration
- ✨ **NEW**: Advanced command system with full tab completion
- ✨ **NEW**: Block-specific spawn chance configuration with luck multipliers
- ✨ **NEW**: Advanced item customization (enchantments, attributes, effects)
- ✨ **NEW**: Debug system with categorized logging and luck calculations
- ✨ **NEW**: Auto-despawn system with configurable timers per rarity
- ✨ **NEW**: Sound and particle effects with toggle options
- ✨ **NEW**: Message customization system with color code support
- ✨ **NEW**: Performance-optimized treasure management
- ✨ **NEW**: Automatic biome file detection and loading
- ✨ **NEW**: Player progression system with blocks mined requirements