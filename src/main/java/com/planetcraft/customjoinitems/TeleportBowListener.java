package com.planetcraft.customjoinitems;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

public class TeleportBowListener implements Listener {

    private final CustomJoinItemsPlugin plugin;

    public TeleportBowListener(CustomJoinItemsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow)) return;
        Arrow arrow = (Arrow) event.getEntity();
        if (!(arrow.getShooter() instanceof Player)) return;

        Player player = (Player) arrow.getShooter();
        FileConfiguration config = plugin.getConfig();

        if (!config.getBoolean("teleport-bow.enabled", false)) return;

        int slot = config.getInt("teleport-bow.slot", 4);
        ItemStack bowItem = player.getInventory().getItem(slot);
        if (bowItem == null || bowItem.getType() != Material.BOW) return;

        // Hacer que el arco no tenga desgaste
        ItemMeta meta = bowItem.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            bowItem.setItemMeta(meta);
        }

        // Teletransportar al jugador donde impactó la flecha, manteniendo la orientación
        Location loc = arrow.getLocation();
        float yaw = player.getLocation().getYaw();
        float pitch = player.getLocation().getPitch();
        Location teleportLoc = new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(), yaw, pitch);

        Bukkit.getScheduler().runTask(plugin, () -> player.teleport(teleportLoc));

        // Reponer la flecha automáticamente en el slot 35
        int arrowSlot = 35;
        PlayerInventory inv = player.getInventory();
        ItemStack currentArrow = inv.getItem(arrowSlot);
        if (currentArrow == null || currentArrow.getType() != Material.ARROW) {
            inv.setItem(arrowSlot, new ItemStack(Material.ARROW, 1));
        }
    }
}