package com.planetcraft.customjoinitems;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerVisibilityListener implements Listener {

    private final CustomJoinItemsPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public PlayerVisibilityListener(CustomJoinItemsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        String name = meta.getDisplayName();
        if (name == null) return;

        FileConfiguration config = plugin.getConfig();
        int cooldownSeconds = config.getInt("cooldown-seconds", 5);
        String visibilityOnMessage = ChatColor.translateAlternateColorCodes('&', config.getString("messages.visibility-on", "&aAhora puedes ver a todos los jugadores."));
        String visibilityOffMessage = ChatColor.translateAlternateColorCodes('&', config.getString("messages.visibility-off", "&cHas ocultado a todos los jugadores."));

        long currentTime = System.currentTimeMillis();
        if (cooldowns.containsKey(player.getUniqueId())) {
            long lastUse = cooldowns.get(player.getUniqueId());
            long diff = (currentTime - lastUse) / 1000;
            if (diff < cooldownSeconds) {
                player.sendMessage(ChatColor.RED + "Debes esperar " + (cooldownSeconds - diff) + " segundos para usar esto nuevamente.");
                return;
            }
        }

        if (name.contains("Mostrar Jugadores")) {
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (!target.equals(player)) {
                    player.hidePlayer(plugin, target);
                }
            }
            item.setType(Material.RED_DYE);
            meta.setDisplayName(ChatColor.RED + "Ocultar Jugadores");
            item.setItemMeta(meta);
            player.sendMessage(visibilityOffMessage);
            cooldowns.put(player.getUniqueId(), currentTime);
        } else if (name.contains("Ocultar Jugadores")) {
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (!target.equals(player)) {
                    player.showPlayer(plugin, target);
                }
            }
            item.setType(Material.LIME_DYE);
            meta.setDisplayName(ChatColor.GREEN + "Mostrar Jugadores");
            item.setItemMeta(meta);
            player.sendMessage(visibilityOnMessage);
            cooldowns.put(player.getUniqueId(), currentTime);
        }
    }
}