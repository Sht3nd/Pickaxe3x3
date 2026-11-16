package ru.sht3nd.pickaxe3x3api.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import ru.sht3nd.pickaxe3x3api.Pickaxe3x3Api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PickaxeCommand implements CommandExecutor {
    private final Pickaxe3x3Api plugin;

    public PickaxeCommand(Pickaxe3x3Api plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("pickaxe3x3")) {
            return false;
        }
        if (!sender.hasPermission("pickaxe3x3.admin")) {
            sender.sendMessage(plugin.messages().get("no_permission"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.messages().get("usage"));
            return true;
        }

        Material material;
        String displayName;
        switch (args[0].trim().toLowerCase()) {
            case "diamond":
                material = Material.DIAMOND_PICKAXE;
                displayName = plugin.messages().get("diamond_pickaxe_name");
                break;
            case "netherite":
                material = Material.NETHERITE_PICKAXE;
                displayName = plugin.messages().get("netherite_pickaxe_name");
                break;
            default:
                sender.sendMessage(plugin.messages().get("unknown_pickaxe"));
                return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target != null && target.isOnline()) {
            givePickaxe(target, material, displayName);
        } else {
            sender.sendMessage(plugin.messages().get("player_not_found"));
        }
        return true;
    }

    private void givePickaxe(Player player, Material material, String displayName) {
        ItemStack pickaxe = new ItemStack(material);
        ItemMeta meta = pickaxe.getItemMeta();
        if (meta == null) {
            player.sendMessage(plugin.messages().get("error_creating_pickaxe"));
            return;
        }

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));

        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(plugin.pickaxeKey(), PersistentDataType.STRING, "3x3_pickaxe");

        applyEnchantments(meta);
        applyLore(meta);
        applyCustomModelData(meta, material);

        pickaxe.setItemMeta(meta);
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(pickaxe);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            player.sendMessage(plugin.messages().get("item_drop"));
        }


        String message = plugin.messages().get("received_pickaxe").replace("{pickaxe}", displayName);
        player.sendMessage(message);
    }

    private void applyEnchantments(ItemMeta meta) {
        if (plugin.getConfig().getBoolean("enchantments.enabled")) {
            for (Map<?, ?> entry : plugin.getConfig().getMapList("enchantments.list")) {
                Object enchantmentObj = entry.get("enchantment");
                Object levelObj = entry.get("level");
                if (enchantmentObj instanceof String) {
                    int level = 1;
                    if (levelObj instanceof Number) {
                        level = ((Number) levelObj).intValue();
                    }
                    if (level < 1) {
                        level = 1;
                    }

                    NamespacedKey key = parseMinecraftKey((String) enchantmentObj);
                    if (key == null) {
                        plugin.getLogger().warning("Invalid enchantment key: " + enchantmentObj);
                        continue;
                    }
                    Enchantment enchantment = Enchantment.getByKey(key);
                    if (enchantment == null) {
                        plugin.getLogger().warning("Unknown enchantment: " + enchantmentObj);
                    } else {
                        meta.addEnchant(enchantment, level, true);
                    }
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    private NamespacedKey parseMinecraftKey(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String input = raw.trim().toLowerCase();
        String namespace;
        String key;
        if (input.contains(":")) {
            String[] parts = input.split(":", 2);
            namespace = parts[0];
            key = parts[1];
        } else {
            namespace = "minecraft";
            key = input;
        }
        try {
            if ("minecraft".equals(namespace)) {
                return NamespacedKey.minecraft(key);
            } else {
                return new NamespacedKey(namespace, key);
            }
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void applyCustomModelData(ItemMeta meta, Material material) {
        if (!plugin.getConfig().getBoolean("custom_model_data.enabled", false)) {
            return;
        }

        int customModelData;
        if (material == Material.DIAMOND_PICKAXE) {
            customModelData = plugin.getConfig().getInt("custom_model_data.diamond", 1001);
        } else if (material == Material.NETHERITE_PICKAXE) {
            customModelData = plugin.getConfig().getInt("custom_model_data.netherite", 1002);
        } else {
            return;
        }

        meta.setCustomModelData(customModelData);
    }

    private void applyLore(ItemMeta meta) {
        if (plugin.getConfig().getBoolean("lore.enabled")) {
            List<String> loreStrings = new ArrayList<>();
            for (String line : plugin.getConfig().getStringList("lore.text")) {
                if (line != null) {
                    loreStrings.add(ChatColor.translateAlternateColorCodes('&', line));
                }
            }
            meta.setLore(loreStrings);
        }
    }
}