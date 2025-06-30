# InfernalTreasures

A comprehensive Minecraft plugin that adds treasure hunting while mining! Find valuable treasures hidden in blocks as you mine, with biome-specific loot, multiple item system integrations, customizable GUI browsers, player luck system, difficulty scaling, and highly configurable features with full comment preservation.

## 🌟 Features

- 🎯 **Treasure Hunting**: Find treasures while mining specific blocks with configurable spawn chances
- 🌍 **Biome-Specific Loot**: Different treasures spawn based on the biome you're mining in
- ⭐ **Advanced Rarity System**: 5 rarity tiers (Common, Rare, Epic, Legendary, Mythic) with individual spawn chances
- 🎚️ **Difficulty System**: 4 difficulty levels (Easy, Medium, Hard, Extreme) that scale treasure requirements
- 📦 **Interactive Treasure Barrels**: Treasures spawn as barrels with scattered loot inside
- 🖥️ **Interactive Loot GUI**: Browse all available treasures by biome and rarity with detailed information
- 🎨 **Fully Customizable Menus**: Configure GUI titles, layouts, colors, and content through YAML files
- 🏷️ **Custom Holograms**: Floating text above treasures (configurable per rarity tier)
- 🍀 **Treasure Luck System**: Admin command to boost player treasure spawn rates temporarily
- ⚙️ **Player Toggle Control**: Players can enable/disable treasure spawning for themselves
- 📊 **Comprehensive Statistics**: Track blocks mined, treasures found, playtime, and luck status
- 🔧 **Highly Configurable**: Extensive configuration options with **comment preservation**
- 🎨 **Advanced Item System**: Support for enchantments, attributes, custom names, lore, and potion effects
- 📊 **Player Progression**: Track blocks mined and gate items behind progression requirements
- ⏰ **Smart Auto-Despawn**: Treasures automatically despawn after configurable times per rarity
- 🎵 **Visual & Audio Effects**: Optional sound and particle effects when treasures are discovered
- 🔗 **Plugin Integrations**: Native support for MMOItems and ExecutableItems/ExecutableBlocks
- 📊 **Debug System**: Comprehensive debug logging with categorized output
- ⚡ **Performance Optimized**: Efficient treasure spawning and management system
- 💬 **Config Comment Preservation**: Beautiful, documented configs that maintain formatting and comments

## 🆕 Latest Features

### 🎚️ Difficulty System
**NEW!** Dynamic difficulty scaling that adjusts treasure requirements based on server progression:

**Difficulty Levels:**
- **Easy (1.0x)**: Default requirements - perfect for new servers
- **Medium (2.0x)**: Double the mining requirements - for established servers
- **Hard (3.0x)**: Triple requirements - challenging but rewarding
- **Extreme (4.0x)**: Quadruple requirements - for hardcore treasure hunters

**Smart Scaling:**
- **Number Requirements**: `required_blocks_mined: 1000` becomes `4000` on Extreme
- **Range Requirements**: `0-99999` becomes `0-24999` on Extreme (shorter availability window)
- **Plus Requirements**: `5000+` becomes `20000+` on Extreme
- **Configurable**: Enable/disable range scaling and customize multipliers

### 💬 Config Comment Preservation
**BREAKTHROUGH FEATURE!** Your beautiful, documented config files now stay beautiful forever:

✅ **Perfect Structure**: All sections, indentation, and formatting preserved  
✅ **Comment Preservation**: Every header, explanation, and comment maintained  
✅ **Smart Updates**: Only changed values are updated, everything else stays intact  
✅ **Professional Appearance**: Your configs remain clean and readable  

**Example: Changing difficulty from EASY to EXTREME:**
```yaml
# Before AND After - only the value changes!
# ========================================
#           DIFFICULTY SETTINGS  
# ========================================
treasure:
  difficulty:
    # Current difficulty setting: EASY, MEDIUM, HARD, EXTREME
    current: EXTREME  # ← Only this changes!
    
    # Multipliers for required_blocks_mined values
    multipliers:
      easy: 1.0 # No change (x1)
      # All comments and structure preserved perfectly!
```

### 🔗 Enhanced Plugin Integrations
- **ExecutableBlocks Support**: Full integration with ExecutableBlocks for advanced treasure types
- **Improved MMOItems**: Better error handling and validation
- **Smart Detection**: Automatic retry systems handle plugin load timing
- **Debug Integration**: Detailed logging for all integration events

## 🎮 Commands

### 🎚️ Difficulty Commands
| Command | Permission | Description |
|---------|------------|-------------|
| `/treasure difficulty info` | `infernaltresures.command.difficulty` | Show current difficulty and settings |
| `/treasure difficulty set <level>` | `infernaltresures.command.difficulty.set` | Set difficulty (EASY/MEDIUM/HARD/EXTREME) |
| `/treasure difficulty multipliers` | `infernaltresures.command.difficulty` | View all difficulty multipliers |
| `/treasure difficulty test <value>` | `infernaltresures.command.difficulty` | Test how difficulty affects a specific value |

**Examples:**
```bash
# Check current difficulty settings
/treasure difficulty info

# Set server to extreme difficulty
/treasure difficulty set EXTREME

# See how difficulty affects specific values
/treasure difficulty test 5000
# Output: "5000 blocks becomes 20000 blocks on EXTREME difficulty"

# View all multiplier settings
/treasure difficulty multipliers
```

### 📊 Enhanced Stats Commands
| Command | Permission | Description |
|---------|------------|-------------|
| `/treasure stats [player]` | `infernaltresures.command.stats` | View treasure hunting statistics |
| `/treasure stats <player> set <stattype> <value>` | `infernaltresures.command.stats.set` | Set specific statistics for a player |
| `/treasure luck <seconds> <player> [multiplier]` | `infernaltresures.command.luck` | Give temporary treasure luck to a player |
| `/treasure toggle` | `infernaltresures.command.toggle` | Toggle treasure spawning on/off for yourself |
| `/treasure progression [info\|set <level>\|debug <on\|off>]` | `infernaltresures.command.progression` | Manage loot progression system |
| `/lootgui` | `infernaltresures.command.loot.gui` | Open interactive loot browser |
| `/treasure loot gui` | `infernaltresures.command.loot` | Alternative loot browser command |

### 🖥️ GUI Commands
| Command | Permission | Description |
|---------|------------|-------------|
| `/lootgui` | `infernaltresures.command.use` | Open the loot browser GUI |
| `/treasure loot gui` | `infernaltresures.command.use` | Alternative command for loot GUI |

### 📈 Progression Commands
| Command | Permission | Description |
|---------|------------|-------------|
| `/treasure progression [info\|set <level>\|debug <on\|off>]` | `infernaltresures.command.progression` | Manage loot progression system |

## ⚙️ Configuration

### 🎚️ Difficulty System Configuration

```yaml
treasure:
  # Difficulty system - affects required_blocks_mined values
  difficulty:
    # Current difficulty setting: EASY, MEDIUM, HARD, EXTREME
    current: EASY
    
    # Multipliers for required_blocks_mined values
    multipliers:
      easy: 1.0     # No change (x1)
      medium: 2.0   # Double requirements (x2)
      hard: 3.0     # Triple requirements (x3)
      extreme: 4.0  # Quadruple requirements (x4)
    
    # Whether to affect range-type requirements like "0-99999"
    # If true: "0-99999" becomes "0-24999" on extreme difficulty
    # If false: range-type requirements are not affected by difficulty
    affect-range-requirements: true
```

**How Difficulty Affects Treasure Requirements:**

| Original Requirement | Easy (1x) | Medium (2x) | Hard (3x) | Extreme (4x) |
|----------------------|-----------|-------------|-----------|--------------|
| `required_blocks_mined: 1000` | 1000 | 2000 | 3000 | 4000 |
| `required_blocks_mined: "5000+"` | 5000+ | 10000+ | 15000+ | 20000+ |
| `required_blocks_mined: "0-99999"` | 0-99999 | 0-49999 | 0-33333 | 0-24999 |

**Use Cases:**
- **New Servers**: Start on Easy for quick treasure discovery
- **Established Servers**: Medium/Hard for balanced progression
- **Hardcore Servers**: Extreme for maximum challenge
- **Events**: Temporarily lower difficulty for special events

### 💬 Config Comment Preservation

**Revolutionary Feature**: Your config files maintain their beautiful structure forever!

**What's Preserved:**
- ✅ All section headers and dividers
- ✅ Explanatory comments and documentation
- ✅ Indentation and formatting
- ✅ Empty lines and spacing
- ✅ Custom organization and structure

**Smart Update System:**
- Only changed values are updated
- All comments and structure remain intact
- Original template formatting preserved
- No more config recreation or comment loss

**Before vs After Example:**
```yaml
# ========================================
#         DIFFICULTY SETTINGS
# ========================================
# This system allows you to scale treasure
# requirements based on your server's needs.
# Perfect for balancing treasure hunting!

treasure:
  difficulty:
    # Current setting - affects ALL treasure requirements
    # Options: EASY, MEDIUM, HARD, EXTREME
    current: EASY  # ← Changes to: EXTREME
    
    # The multipliers applied to required_blocks_mined
    # Higher values = more blocks needed = harder game
    multipliers:
      easy: 1.0    # Vanilla experience
      medium: 2.0  # 2x harder
      hard: 3.0    # 3x harder  
      extreme: 4.0 # 4x harder (hardcore mode!)
```

**Result**: Only `current: EASY` becomes `current: EXTREME` - everything else stays perfect!

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

## 🎨 Advanced Biome Configuration

### Difficulty-Aware Loot Tables

Configure treasures that respond to difficulty settings:

```yaml
# biomes/desert.yml
loot:
  LEGENDARY:
    - material: NETHERITE_INGOT
      min_amount: 1
      max_amount: 3
      chance: 15
      # This scales with difficulty automatically!
      required_blocks_mined: 5000  # Easy: 5000, Medium: 10000, Hard: 15000, Extreme: 20000
      display_name: "&6Desert Master's Ingot"
      lore:
        - "&7Only experienced miners can find this"
        - "&7in the harsh desert conditions."
        
    - material: DIAMOND_BLOCK
      min_amount: 2
      max_amount: 5
      chance: 25
      # Range requirements also scale!
      required_blocks_mined: "1000-9999"  # Extreme: "1000-2499" (shorter window)
      display_name: "&bDesert Diamond Cache"
      lore:
        - "&7A cache of diamonds hidden by"
        - "&7ancient desert civilizations."
```

### 🔗 Enhanced Integration Examples

**ExecutableBlocks Integration:**
```yaml
- executable_block_id: "custom_desert_shrine"
  min_amount: 1
  max_amount: 1
  chance: 5
  required_blocks_mined: 10000
  display_name: "&6&lDesert Shrine Block"
  lore:
    - "&7A mystical shrine block that can"
    - "&7be placed to create desert magic!"
```

**MMOItems Integration:**
```yaml
- mmo_type: "SWORD"
  mmo_id: "DESERT_BLADE"
  min_amount: 1
  max_amount: 1
  chance: 8
  required_blocks_mined: 7500
  # MMOItems handle their own display names and lore
```

## 🛠️ Advanced Admin Tools

### 🎚️ Difficulty Management

**Server Progression Planning:**
```bash
# Start new server on easy mode
/treasure difficulty set EASY

# Monitor player engagement and progression
/treasure stats TopPlayer

# Gradually increase difficulty as players progress
/treasure difficulty set MEDIUM  # After 1 month
/treasure difficulty set HARD     # After 3 months  
/treasure difficulty set EXTREME  # For endgame content
```

**Testing and Balancing:**
```bash
# Test how difficulty affects specific requirements
/treasure difficulty test 5000
/treasure difficulty test "0-99999"
/treasure difficulty test "10000+"

# View all current multipliers
/treasure difficulty multipliers

# Check current difficulty impact
/treasure difficulty info
```

### 💬 Config Management

**No More Config Loss!** The revolutionary comment preservation system means:

- ✅ **Safe Updates**: Change difficulty without losing your beautiful configs
- ✅ **Documentation Preserved**: All your custom comments stay intact
- ✅ **Professional Appearance**: Configs remain clean and organized
- ✅ **Version Control Friendly**: Only actual changes show in git diffs

**Perfect for:**
- **Server Networks**: Maintain consistent, documented configs across servers
- **Team Management**: Comments help team members understand settings
- **Version Control**: Clean diffs show only actual configuration changes
- **Professional Setups**: Beautiful configs that stay beautiful

## 🐛 Troubleshooting

### Difficulty System Issues

**Difficulty not applying:**
1. Check current difficulty: `/treasure difficulty info`
2. Verify config syntax is correct (no extra spaces/characters)
3. Enable debug logging: `debug.categories.loot-generation: true`
4. Check if `affect-range-requirements` is set as desired

**Config comments lost:**
- This should no longer happen with the new preservation system!
- If it does, please report as a bug with your config template

**ExecutableBlocks not working:**
1. Ensure ExecutableBlocks plugin is installed and enabled
2. Verify your ExecutableBlock IDs exist
3. Enable debug: `debug.categories.executable-blocks: true`

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

### Version 1.1.0 (Latest)
- ✨ **NEW**: Complete difficulty system with 4 scaling levels
- ✨ **NEW**: Revolutionary config comment preservation system
- ✨ **NEW**: ExecutableBlocks integration for advanced treasure types
- 🔧 **IMPROVED**: Enhanced MMOItems integration with better error handling
- 🔧 **IMPROVED**: Smart config merging preserves all formatting and comments
- 🔧 **IMPROVED**: Difficulty scaling affects both number and range requirements
- 🔧 **IMPROVED**: Advanced admin commands for difficulty management
- 🔧 **IMPROVED**: Enhanced debug logging for all systems
- 🔧 **IMPROVED**: Better plugin integration detection and retry systems
- 🐛 **FIXED**: Config file structure mixing issues
- 🐛 **FIXED**: Comment loss during config updates
- 🐛 **FIXED**: Compilation errors from deprecated Bukkit APIs

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