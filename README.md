![image](https://proxy.spigotmc.org/0026b4638f33036ae97d445c53e3427e95089387/68747470733a2f2f737069676f746d632e72752f6174746163686d656e74732f6d696e6563726166745f7469746c652d322d706e672e33323034372f)

⛏️ Pickaxe 3x3 — Mine a 3x3 Area with a Single Swing!
  
✨ Tired of breaking blocks one by one?
This plugin adds a special pickaxe that instantly mines a 3x3 cube (9 blocks) every time you hit a block. Just hold the pickaxe, dig as usual, and watch the area clear in seconds — no extra commands, no toggles, no magic tricks.

Everything is configurable: durability, enchantments, lore, per‑world and per‑region restrictions, and even a legacy mode for tricky server setups.
# 
📦 What the Plugin Does
⛏️ True 3x3 Mining
When you break a block, the plugin automatically mines the surrounding 8 blocks (a 3x3 square centred on the target). It respects enchantments like Silk Touch and Fortune — they apply to all 9 blocks at once.
# 
🔩 Flexible Durability System
Two separate config options let you control wear and tear:

- breakable_fast – when true, mining a 3x3 area costs 9 durability (1 per block). When false, only 1 durability is consumed per swing.

- unbreakable_pickaxe – set to true to make the tool indestructible (infinite durability). Perfect for donator perks or OP tools.
# 
🛡️ Full WorldGuard Support
The plugin checks region flags before breaking any block. If the player does not have building permission in the region, the 3x3 effect is cancelled and the pickaxe will not break blocks. You can also whitelist specific regions where the pickaxe should work regardless of overlapping protections.
# 
🚫 Disable in Specific Worlds
Add world names to disabled_worlds (e.g., arena, creative, spawn) and the pickaxe will behave like a normal tool there — no 3x3 effect.
# 
✨ Auto‑Enchantments on Give
In the enchantments section you can define a list of enchants that are automatically applied when a player receives the pickaxe via the command. Example: Efficiency V + Unbreaking III.
# 
📜 Custom Lore
Give the pickaxe a description that players see on hover. Enable lore in the config, write your lines (supports color codes with §), and the text will appear every time the tool is given.
# 
🕰️ Legacy Mode
Some server cores behave differently. If blocks don't break correctly or errors appear, enable legacy: true. This switches to the pre‑10.5.5 block‑removal method for better compatibility.
# 
🌐 Language
Set language to en or ru to translate all plugin messages.
# 
⚙️ Configuration in Detail
The config.yml file is located in plugins/Pickaxe3x3/ and contains comments in English (Russian version available). Here’s every key explained:

- language – en or ru. Controls plugin messages.

- legacy – true enables the old breaking method; false uses the new one.

- lore:
1. enabled – true adds the custom lore.
2. text – list of strings for the lore (supports § colour codes).

- disabled_worlds – list of world names where the 3x3 effect is completely disabled.

- enchantments:
1. enabled – true to apply enchants.
2. list – each entry has enchantment (e.g., minecraft:efficiency) and level.

- unbreakable_pickaxe – true makes the pickaxe unbreakable; false uses normal durability.

- regions:
1. enabled – true (strongly recommended) checks WorldGuard build flags.
2. list – regions where the pickaxe always works, even if overlapped.

- breakable_fast - true makes the pickaxe unbreakable; false uses normal durability.

- unbreakable_blocks - A new config section where you can specify blocks that a 3x3 pickaxe will never break. By default, bedrock, barriers, command blocks, structure blocks, and end portal frames are protected. Feel free to add any other materials—spawners, chests, shulkers, obsidian.

- gentle_mode – when enabled, the pickaxe stops working as 3x3 if its durability drops below a certain percentage. It will only break a single block (like a normal pickaxe). After repairing the tool (anvil, Mending), the 3x3 ability automatically returns.

- use_unbreaking – true makes the pickaxe respect the Unbreaking enchantment, reducing durability loss per use. Thanks to @EpicJosch for the idea.

Changes take effect after reloading the plugin or restarting the server.
# 
🔧 Commands & Permissions

- /pickaxe3x3 – gives the caller a fully configured 3x3 pickaxe (with enchants, lore, unbreakable settings as defined in the config).

- pickaxe3x3.admin – the only permission node; required to run the command.
# 
📋 Requirements
Minecraft 1.16+ (Paper, Spigot, Purpur and forks).

(Optional) WorldGuard for region support.

![bstats](https://proxy.spigotmc.org/1db96d276251e4f0a5f2f1fc157a0c1f4fa68f3d/68747470733a2f2f6273746174732e6f72672f7369676e6174757265732f62756b6b69742f5069636b6178652532303378332e737667)
#  
💬 Support & Feedback
Have a question, suggestion, or found a bug? Just post in the discussion section of this resource. I check regularly and try to help everyone.

This plugin uses bStats for anonymous server statistics. You can disable it in the bStats config if you prefer.
