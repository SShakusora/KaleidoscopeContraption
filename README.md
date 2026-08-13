# Kaleidoscope Contraption

[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21.1-green.svg)](https://minecraft.net)
[![NeoForge Version](https://img.shields.io/badge/NeoForge-21.1.219%2B-orange.svg)](https://neoforged.net/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A Create compatibility addon for [Kaleidoscope Cookery](https://www.curseforge.com/minecraft/mc-mods/kaleidoscope-cookery) and [Kaleidoscope Tavern](https://www.curseforge.com/minecraft/mc-mods/kaleidoscope-tavern). It brings their cooking, brewing, food, furniture, and decoration blocks to moving Create contraptions.

Cookery and Tavern are optional integrations. Kaleidoscope Contraption can load without either mod; install them only when you want the corresponding support.

---

## 📖 Overview

Kaleidoscope Contraption connects the block interaction and movement systems of Kaleidoscope Cookery and Kaleidoscope Tavern with Create contraptions. Mobile kitchens, food trucks, breweries, taverns, trains, elevators, and rotating restaurants can keep working while assembled and moving.

The integration includes contraption interaction behaviours, movement behaviours, multi-part block attachment checks, surface placement rules, and client-side state synchronization for supported blocks.

---

## ✨ Supported Integrations

### 🍳 Kaleidoscope Cookery

Supported Cookery categories include:

- Cooking appliances: Pot, Stockpot, Shawarma Spit, Steamer, Teapot, Millstone, and Trash Can
- Food blocks with bite and multi-part behaviour
- Tables, chairs, cook stools, chopping boards, kitchenware racks, fruit baskets, enamel basins, and oil pots
- Teacups and teapot contents, including contraption placement on tables and stoves
- Stove, table, and steamer surface placement rules
- Multi-part attachment and brittle-block checks for Shawarma Spits and large food blocks
- Client-side transient state preservation for pots, teapots, stockpots, chopping boards, and trash cans

### 🍺 Kaleidoscope Tavern

Supported Tavern categories include:

- Brewing equipment: Pressing Tub, Barrel, and Tap
- Drinks, Signature Cocktails, Shakers, Molotovs, potion bottles, bottles, and glassware
- Glassware Holders, Holders, Tilted Racks, Circular Racks, Cellar Cabinets, Bar Cabinets, and Glass Bar Cabinets
- Tavern Tables and Bar Counters with contraption surface placement
- Chalkboards and flower sandwich boards
- Sofas and bar stools, including seating while the contraption is moving
- Incense blocks, including moving incense effects and zombie-villager conversion support
- String Lights and Pendant Lamps, with multi-part attachment checks

---

## ⚙️ Create Contraption Support

Supported blocks retain their registered interactions while mounted on Create contraptions such as:

- Mechanical Bearings and other rotating contraptions
- Cart Assemblers and mobile food trucks or tavern trains
- Rope Pulleys and Elevator Pulleys
- Moving tables, counters, stoves, brewing equipment, and seating areas

The mod also handles block attachment checks, multi-block structures, placement on supported surfaces, contraption block updates, and client-side rendering state where required.

---

## 📋 Requirements

| Mod | Version | Required |
|-----|---------|----------|
| Minecraft | 1.21.1 | ✓ |
| Java | 21 | ✓ |
| Minecraft NeoForge | 21.1.219+ | ✓ |
| Create | 6.0.10–<6.1.0 | ✓ |
| Kaleidoscope Cookery | 1.4.1+ | Optional |
| Kaleidoscope Tavern | 1.2.0+ | Optional |

Cookery and Tavern support is activated automatically when the corresponding mod is present. Their absence does not prevent the base mod from loading.

---

## 🔧 Installation

1. Install **Minecraft NeoForge 1.21.1** (21.1.219 or higher).
2. Install **[Create](https://www.curseforge.com/minecraft/mc-mods/create)** 6.0.10 up to, but not including, 6.1.0.
3. Optionally install **[Kaleidoscope Cookery](https://www.curseforge.com/minecraft/mc-mods/kaleidoscope-cookery)** for cooking support.
4. Optionally install **[Kaleidoscope Tavern](https://www.curseforge.com/minecraft/mc-mods/kaleidoscope-tavern)** for brewing and tavern support.
5. Put **Kaleidoscope Contraption** in the `mods` folder and launch the game with Java 21.

Use versions of Cookery, Tavern, Create, and Kaleidoscope Contraption that target the same Minecraft version and mod loader.

---

## 🐛 Compatibility Notes

- Support is registration-based: blocks listed above have dedicated contraption behaviours; unsupported or newly added upstream blocks may require a future compatibility update.
- Cookery and Tavern are independent optional integrations. You can install either one, both, or neither.
- Multi-part blocks must be assembled with their required parts attached so Create can preserve the structure.
- Client and server should use the same Kaleidoscope Contraption version and the same optional integrations for multiplayer.

---

## 📜 License

This mod is licensed under the **MIT License**. Feel free to use it in your modpacks!

---

## 🙏 Credits

- **Shinonome Shakusora** - Developer of Kaleidoscope Contraption
- **Create Team** - For the Create mod and its contraption API
- **Kaleidoscope Cookery Team** - For Kaleidoscope Cookery
- **Kaleidoscope Tavern Team** - For Kaleidoscope Tavern

---

## 📥 Downloads

- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/kaleidoscope-contraption)
- [Modrinth](https://modrinth.com/mod/kaleidoscope-contraption)

---

*Build a food train, a mobile brewery, a floating restaurant, or a rotating buffet—the contraption is your kitchen and tavern.* 🚚🍜🍺
