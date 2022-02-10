package com.github.jenya705.rip;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.text.MessageFormat;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Jenya705
 */
public class RipHandler implements Listener {

    private static final NamespacedKey chestKey = new NamespacedKey(Rip.getInstance(), "chest_key");
    private static final NamespacedKey ownerKey = new NamespacedKey(Rip.getInstance(), "owner_key");

    @EventHandler
    public void death(EntityDeathEvent event) {
        if (event.getDrops().isEmpty() || !(event.getEntity() instanceof Player player)) return;
        Block deathBlock = findAirLocation(new Location(
                player.getWorld(),
                (int) Math.round(player.getLocation().getX()),
                (int) Math.round(player.getLocation().getY()),
                (int) Math.round(player.getLocation().getZ())
        )).getBlock();
        deathBlock.setType(Material.CHEST);
        long currentTime = System.currentTimeMillis();
        Chest chest = (Chest) deathBlock.getState();
        Inventory virtualChestInventory = Bukkit.createInventory(
                chest, event.getDrops().size() % 9 == 0 ?
                        event.getDrops().size() : (event.getDrops().size() / 9 * 9 + 9)
        );
        for (ItemStack drop : event.getDrops()) virtualChestInventory.addItem(drop);
        Rip.getInstance().getInventories().put(deathBlock.getLocation(), virtualChestInventory);
        chest.getPersistentDataContainer().set(chestKey, PersistentDataType.LONG, currentTime);
        chest.getPersistentDataContainer().set(ownerKey, PersistentDataType.LONG_ARRAY,
                new long[]{player.getUniqueId().getMostSignificantBits(), player.getUniqueId().getLeastSignificantBits()});
        chest.update();
        Block signBlock = null;
        if (Rip.getInstance().config().isSpawnSign()) {
            Directional directional = (Directional) deathBlock.getBlockData();
            Location signLocation = RipUtils.getLocationToFace(deathBlock.getLocation(), directional.getFacing());
            signBlock = signLocation.getBlock();
            signBlock.setType(Material.OAK_WALL_SIGN);
            Sign sign = (Sign) signBlock.getState();
            sign.line(0, player.displayName());
            sign.update();
            WallSign signData = (WallSign) signBlock.getBlockData();
            signData.setFacing(directional.getFacing());
            signBlock.setBlockData(signData);
        }
        Component locationNotifyMessage = LegacyComponentSerializer.legacyAmpersand().deserialize(
                MessageFormat.format(
                        Rip.getInstance().config().getLocationNotifyMessage(),
                        player.getName(),
                        Integer.toString(deathBlock.getLocation().getBlockX()),
                        Integer.toString(deathBlock.getLocation().getBlockY()),
                        Integer.toString(deathBlock.getLocation().getBlockZ()),
                        deathBlock.getLocation().getWorld().getKey().getKey()
                )
        );
        Block finalSignBlock = signBlock;
        Bukkit.getScheduler().runTaskTimer(
                Rip.getInstance(),
                task -> {
                    if (!(deathBlock.getLocation().getBlock().getState() instanceof Chest currentChest)) {
                        task.cancel();
                        return;
                    }
                    Long deathBlockTime = currentChest.getPersistentDataContainer().get(chestKey, PersistentDataType.LONG);
                    if (!Objects.equals(deathBlockTime, currentTime)) {
                        task.cancel();
                        return;
                    }
                    player.sendMessage(locationNotifyMessage);
                },
                Rip.getInstance().config().getLocationNotifyTime().toSeconds() * 20,
                Rip.getInstance().config().getLocationNotifyTime().toSeconds() * 20
        );
        Bukkit.getScheduler().runTaskLater(
                Rip.getInstance(),
                () -> {
                    if (!(deathBlock.getLocation().getBlock().getState() instanceof Chest currentChest)) return;
                    Long deathBlockTime = currentChest.getPersistentDataContainer().get(chestKey, PersistentDataType.LONG);
                    if (!Objects.equals(deathBlockTime, currentTime)) return;
                    if (finalSignBlock != null) {
                        finalSignBlock.setType(Material.AIR);
                    }
                    deathBlock.setType(Material.AIR);
                    Rip.getInstance().getInventories().remove(chest.getLocation());
                },
                Rip.getInstance().config().getChestDespawnTime().toSeconds() * 20
        );
        Bukkit.getScheduler().runTaskLater(
                Rip.getInstance(),
                () -> {
                    if (!(deathBlock.getLocation().getBlock().getState() instanceof Chest currentChest)) return;
                    Long deathBlockTime = currentChest.getPersistentDataContainer().get(chestKey, PersistentDataType.LONG);
                    if (!Objects.equals(deathBlockTime, currentTime)) return;
                    Bukkit.getOnlinePlayers().stream()
                            .filter(it -> !it.equals(player))
                            .forEach(it -> it.sendMessage(locationNotifyMessage));
                },
                Rip.getInstance().config().getDeathNotifyTime().toSeconds() * 20
        );
        Bukkit.getScheduler().runTaskAsynchronously(
                Rip.getInstance(),
                () -> Rip.getInstance().database()
                        .update("INSERT INTO deaths (uuidmost, uuidleast, locx, locy, locz, world) VALUES (?, ?, ?, ?, ?, ?)",
                                player.getUniqueId().getMostSignificantBits(),
                                player.getUniqueId().getLeastSignificantBits(),
                                chest.getLocation().getBlockX(),
                                chest.getLocation().getBlockY(),
                                chest.getLocation().getBlockZ(),
                                chest.getLocation().getWorld().getName()
                        )
        );
        event.getDrops().clear();
    }

    @EventHandler
    public void blockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() == Material.OAK_WALL_SIGN) {
            WallSign wallSign = (WallSign) block.getBlockData();
            Block chestBlock = RipUtils.getLocationToFace(block.getLocation(), wallSign.getFacing(), -1).getBlock();
            if (chestBlock.getType() == Material.CHEST && block.getState() instanceof Chest chest &&
                    RipUtils.parseUUID(chest.getPersistentDataContainer()
                            .getOrDefault(ownerKey, PersistentDataType.LONG_ARRAY, new long[0])) == null
            ) {
                event.setCancelled(true);
            }
            return;
        }
        if (block.getType() != Material.CHEST) return;
        Inventory inventory = Rip.getInstance().getInventories().get(block.getLocation());
        if (inventory == null) return;
        if (!isOwn(event.getPlayer(), block)) {
            event.setCancelled(true);
            return;
        }
        Chest chest = (Chest) block.getState();
        event.setDropItems(false);
        dropItems(chest.getLocation(), inventory);
        if (Rip.getInstance().config().isSpawnSign() && chest.getBlockData() instanceof Directional directional) {
            Block signBlock = RipUtils.getLocationToFace(chest.getLocation(), directional.getFacing()).getBlock();
            if (signBlock.getType() == Material.OAK_WALL_SIGN) signBlock.setType(Material.AIR);
        }
    }

    @EventHandler
    public void chestOpen(InventoryOpenEvent event) {
        if (!(event.getInventory().getHolder() instanceof Chest chest) ||
                !(event.getPlayer() instanceof Player player)) return;
        Inventory inventory = Rip.getInstance().getInventories().get(chest.getLocation());
        if (inventory == null) return;
        ItemStack inUse = player.getInventory().getItemInMainHand();
        boolean canOpen = inUse.getItemMeta() != null &&
                inUse.getItemMeta().getCustomModelData() == Rip.getInstance().config().getCustomModelData();
        if (canOpen) {
            inUse.setType(Material.AIR);
            player.getInventory().setItemInMainHand(inUse);
        }
        if (canOpen || isOwn(player, chest)) {
            chest.open();
            Rip.getInstance().getInventories().remove(chest.getLocation());
            player.openInventory(inventory);
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void chestClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof Chest chest) ||
                !(event.getPlayer() instanceof Player player)) return;
        if (Rip.getInstance().config().isSpawnSign() && chest.getBlockData() instanceof Directional directional) {
            Block signBlock = RipUtils.getLocationToFace(chest.getLocation(), directional.getFacing()).getBlock();
            if (signBlock.getType() == Material.OAK_WALL_SIGN) signBlock.setType(Material.AIR);
        }
        dropItems(chest.getLocation(), event.getInventory());
        Rip.getInstance().getInventories().remove(chest.getLocation());
        chest.getBlock().setType(Material.AIR);
    }

    private static boolean isOwn(Player player, Block block) {
        return Objects.equals(getOwner(block), player.getUniqueId());
    }

    private static boolean isOwn(Player player, Chest chest) {
        return Objects.equals(player.getUniqueId(), getOwner(chest));
    }

    private static UUID getOwner(Block block) {
        if (block.getType() != Material.CHEST || !(block.getState() instanceof Chest chest)) return null;
        return getOwner(chest);
    }

    private static UUID getOwner(Chest chest) {
        return RipUtils.parseUUID(chest
                .getPersistentDataContainer()
                .getOrDefault(ownerKey, PersistentDataType.LONG_ARRAY, new long[0])
        );
    }

    private static void dropItems(Location location, Inventory inventory) {
        for (ItemStack item : inventory) {
            if (item == null || item.getType() == Material.AIR) continue;
            location.getWorld().dropItemNaturally(location, item);
        }
    }

    private static Location findAirLocation(Location location) {
        return findAirLocation(location, location);
    }

    private static Location findAirLocation(Location location, Location defaultValue) {
        int radius = Rip.getInstance().config().getTryingRadius();
        for (int y = 0; y < radius; ++y) {
            for (int z = 0; z < radius; ++z) {
                for (int x = 0; x < radius; ++x) {
                    Block block = location.add(x, y, z).getBlock();
                    if (block.getType().isAir()) return block.getLocation();
                }
            }
        }
        return defaultValue;
    }

}
