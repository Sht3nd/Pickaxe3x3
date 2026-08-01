package ru.sht3nd.pickaxe3x3api.listener;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import ru.sht3nd.pickaxe3x3api.Pickaxe3x3Api;

import java.util.*;

public final class PickaxeListener implements Listener {
    private static final String TAG = "3x3_pickaxe";

    private final Pickaxe3x3Api plugin;
    private final boolean worldGuardPresent;
    private final Set<String> synthetic = new HashSet<>();
    private final Set<Material> unbreakable;
    private final boolean gentleEnabled;
    private final double gentleThresholdPercent;
    private final boolean useUnbreaking;

    public PickaxeListener(Pickaxe3x3Api plugin) {
        this.plugin = plugin;
        this.worldGuardPresent = Bukkit.getPluginManager().getPlugin("WorldGuard") != null;

        Set<Material> base = new HashSet<>(Arrays.asList(
                Material.BEDROCK,
                Material.BARRIER,
                Material.COMMAND_BLOCK,
                Material.STRUCTURE_BLOCK,
                Material.END_PORTAL_FRAME
        ));
        if (plugin.getConfig().getBoolean("unbreakable_blocks.enable", true)) {
            List<String> configList = plugin.getConfig().getStringList("unbreakable_blocks.type");
            for (String name : configList) {
                try {
                    Material mat = Material.valueOf(name.trim().toUpperCase());
                    base.add(mat);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material in unbreakable_blocks.type: " + name);
                }
            }
        }
        this.unbreakable = Collections.unmodifiableSet(base);

        this.gentleEnabled = plugin.getConfig().getBoolean("gentle_mode.enabled", false);
        int threshold = plugin.getConfig().getInt("gentle_mode.threshold", 10);
        if (threshold < 1) threshold = 1;
        if (threshold > 100) threshold = 100;
        this.gentleThresholdPercent = threshold / 100.0;

        this.useUnbreaking = plugin.getConfig().getBoolean("use_unbreaking", false);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        String centerKey = key(event.getBlock());
        if (!synthetic.remove(centerKey)) {
            if (!plugin.getConfig().getStringList("disabled_worlds").contains(player.getWorld().getName())) {
                ItemStack tool = player.getInventory().getItemInMainHand();
                if (tool.getType() != Material.AIR) {
                    ItemMeta meta = tool.getItemMeta();
                    if (meta != null) {
                        PersistentDataContainer data = meta.getPersistentDataContainer();
                        String tag = data.get(plugin.pickaxeKey(), PersistentDataType.STRING);
                        if (TAG.equals(tag)) {
                            Block center = event.getBlock();
                            if (unbreakable.contains(center.getType())) {
                                event.setCancelled(true);
                            } else {
                                if (cannotBreak(player, center)) {
                                    event.setCancelled(true);
                                    player.sendMessage(plugin.messages().get("cannot_break_here"));
                                } else {
                                    event.setCancelled(true);
                                    boolean legacy = plugin.getConfig().getBoolean("legacy", false);

                                    List<Block> around;
                                    if (gentleEnabled && isBelowThreshold(tool)) {
                                        around = new ArrayList<>();
                                        around.add(center);
                                    } else {
                                        around = legacy ? collectCube(center) : collectPlane(player, center);
                                        around.add(center);
                                    }

                                    int broken = breakBlocks(player, around, tool);
                                    if (broken > 0) {
                                        int damagePerBlock = (broken > 1 && plugin.getConfig().getBoolean("breakable_fast.enabled", false)) ? 9 : 1;
                                        int rawDamage = damagePerBlock * broken;
                                        int finalDamage = applyUnbreaking(tool, rawDamage);
                                        damageTool(player, tool, finalDamage);
                                        if (gentleEnabled && isBelowThreshold(tool)) {
                                            player.sendMessage(plugin.messages().get("gentle_mode_activated"));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack a = event.getInventory().getItem(0);
        ItemStack b = event.getInventory().getItem(1);
        if (a != null && b != null) {
            ItemMeta am = a.getItemMeta();
            ItemMeta bm = b.getItemMeta();
            if (am != null && bm != null) {
                boolean aTagged = TAG.equals(am.getPersistentDataContainer().get(plugin.pickaxeKey(), PersistentDataType.STRING));
                boolean bTagged = TAG.equals(bm.getPersistentDataContainer().get(plugin.pickaxeKey(), PersistentDataType.STRING));
                if (aTagged || bTagged) {
                    ItemStack result = event.getResult();
                    if (result != null && result.getType() != Material.AIR) {
                        ItemMeta rm = result.getItemMeta();
                        if (rm != null) {
                            rm.getPersistentDataContainer().set(plugin.pickaxeKey(), PersistentDataType.STRING, TAG);
                            result.setItemMeta(rm);
                            event.setResult(result);
                        }
                    }
                }
            }
        }
    }

    private List<Block> collectPlane(Player player, Block center) {
        BlockFace face = null;
        RayTraceResult rayResult = player.rayTraceBlocks(5.0);
        if (rayResult != null) {
            face = rayResult.getHitBlockFace();
        }
        if (face == null) {
            face = BlockFace.UP;
        }

        Set<Block> blocks = new HashSet<>();
        switch (face) {
            case UP:
            case DOWN:
                for (int dx = -1; dx <= 1; ++dx) {
                    for (int dz = -1; dz <= 1; ++dz) {
                        addIfValid(blocks, center.getRelative(dx, 0, dz), center);
                    }
                }
                break;
            case EAST:
            case WEST:
                for (int dy = -1; dy <= 1; ++dy) {
                    for (int dz = -1; dz <= 1; ++dz) {
                        addIfValid(blocks, center.getRelative(0, dy, dz), center);
                    }
                }
                break;
            case NORTH:
            case SOUTH:
            default:
                for (int dx = -1; dx <= 1; ++dx) {
                    for (int dy = -1; dy <= 1; ++dy) {
                        addIfValid(blocks, center.getRelative(dx, dy, 0), center);
                    }
                }
                break;
        }
        return new ArrayList<>(blocks);
    }

    private List<Block> collectCube(Block center) {
        Set<Block> blocks = new HashSet<>();
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dy = -1; dy <= 1; ++dy) {
                for (int dz = -1; dz <= 1; ++dz) {
                    addIfValid(blocks, center.getRelative(dx, dy, dz), center);
                }
            }
        }
        return new ArrayList<>(blocks);
    }

    private void addIfValid(Set<Block> blocks, Block candidate, Block center) {
        if (!candidate.equals(center) && !unbreakable.contains(candidate.getType())) {
            blocks.add(candidate);
        }
    }

    private int breakBlocks(Player player, List<Block> blocks, ItemStack tool) {
        int broken = 0;
        for (Block b : blocks) {
            if (b.getType() == Material.AIR || cannotBreak(player, b)) {
                continue;
            }

            BlockBreakEvent sub = new BlockBreakEvent(b, player);
            String k = key(b);
            synthetic.add(k);
            Bukkit.getPluginManager().callEvent(sub);
            if (sub.isCancelled()) {
                synthetic.remove(k);
                continue;
            }

            if (sub.isDropItems()) {
                boolean broke = b.breakNaturally(tool);
                synthetic.remove(k);
                if (broke) broken++;
            } else {
                b.setType(Material.AIR, false);
                int exp = sub.getExpToDrop();
                if (exp > 0) {
                    b.getWorld().spawn(b.getLocation().add(0.5, 0.5, 0.5),
                            ExperienceOrb.class, orb -> orb.setExperience(exp));
                }
                synthetic.remove(k);
                broken++;
            }
        }
        return broken;
    }

    private String key(Block b) {
        return b.getWorld().getUID() + ":" + b.getX() + ":" + b.getY() + ":" + b.getZ();
    }

    private boolean cannotBreak(Player player, Block block) {
        if (!worldGuardPresent) {
            return false;
        }
        if (!plugin.getConfig().getBoolean("regions.enabled", true)) {
            return false;
        }
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        ApplicableRegionSet regions = query.getApplicableRegions(BukkitAdapter.adapt(block.getLocation()));
        List<String> allowedRegions = plugin.getConfig().getStringList("regions.list");
        for (ProtectedRegion region : regions) {
            if (allowedRegions.contains(region.getId())) {
                return false;
            }
        }
        for (ProtectedRegion region : regions) {
            String id = region.getId();
            if (!"__global__".equals(id) && !region.getMembers().contains(player.getUniqueId()) && !region.getOwners().contains(player.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    private void damageTool(Player player, ItemStack tool, int amount) {
        if (plugin.getConfig().getBoolean("unbreakable_pickaxe", false)) {
            return;
        }
        ItemMeta meta = tool.getItemMeta();
        if (meta instanceof Damageable) {
            Damageable damageable = (Damageable) meta;
            int max = tool.getType().getMaxDurability();
            int next = damageable.getDamage() + amount;
            if (next < max) {
                damageable.setDamage(next);
                tool.setItemMeta(meta);
            } else {
                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            }
        }
    }

    private int applyUnbreaking(ItemStack tool, int damage) {
        if (!useUnbreaking) {
            return damage;
        }
        ItemMeta meta = tool.getItemMeta();
        if (meta == null) {
            return damage;
        }
        int level = meta.getEnchantLevel(Enchantment.DURABILITY);
        if (level <= 0) {
            return damage;
        }
        int reduced = (int) Math.round((double) damage / (level + 1));
        return Math.max(reduced, 1);
    }

    private boolean isBelowThreshold(ItemStack tool) {
        ItemMeta meta = tool.getItemMeta();
        if (meta instanceof Damageable) {
            Damageable d = (Damageable) meta;
            int max = tool.getType().getMaxDurability();
            if (max == 0) return false;
            int currentDurability = max - d.getDamage();
            return currentDurability <= (int)(max * gentleThresholdPercent);
        }
        return false;
    }
}