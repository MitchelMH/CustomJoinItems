package com.planetcraft.customjoinitems;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import me.clip.placeholderapi.PlaceholderAPI;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class CustomJoinItemsPlugin extends JavaPlugin implements Listener {

    private Connection connection;
    private FileConfiguration config;

    private boolean databaseEnabled;
    private boolean debugEnabled;
    private String dbHost;
    private String dbPort;
    private String dbName;
    private String dbUser;
    private String dbPassword;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        loadDatabaseSettings();

        if (databaseEnabled) connectToDatabase();
        debugEnabled = config.getBoolean("debug", false);

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new TeleportBowListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerVisibilityListener(this), this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        if (getCommand("customjoinitems") != null) {
            getCommand("customjoinitems").setExecutor(new CustomJoinItemsCommands(this));
        }

        if (debugEnabled) getLogger().info("Modo debug activado.");
    }

    private void loadDatabaseSettings() {
        databaseEnabled = config.getBoolean("database.enabled", false);
        dbHost = config.getString("database.host");
        dbPort = config.getString("database.port");
        dbName = config.getString("database.name");
        dbUser = config.getString("database.user");
        dbPassword = config.getString("database.password");
    }

    private void connectToDatabase() {
        try {
            connection = DriverManager.getConnection("jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName, dbUser, dbPassword);
            logDebug("Conectado a la base de datos correctamente.");
        } catch (SQLException e) {
            getLogger().warning("Error al conectar a la base de datos: " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            getLogger().warning("Error al cerrar la base de datos: " + e.getMessage());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        giveItems(event.getPlayer());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Bukkit.getScheduler().runTaskLater(this, () -> giveItems(player), 1L);
    }

    private void giveItems(Player player) {
        player.getInventory().clear();
        List<String> items = config.getStringList("items");

        for (String itemKey : items) {
            String path = "item-settings." + itemKey;
            String materialName = config.getString(path + ".material");
            String displayNameRaw = config.getString(path + ".name");

            if (materialName == null || displayNameRaw == null) {
                logDebug("El item '" + itemKey + "' no tiene material o name definido. Se omite.");
                continue;
            }

            String displayName = ChatColor.translateAlternateColorCodes('&', PlaceholderAPI.setPlaceholders(player, displayNameRaw));
            List<String> loreRaw = config.getStringList(path + ".lore");
            List<String> lore = loreRaw != null ? loreRaw.stream().map(line -> ChatColor.translateAlternateColorCodes('&', PlaceholderAPI.setPlaceholders(player, line))).collect(Collectors.toList()) : null;

            String permission = config.getString(path + ".permission");
            int slot = config.getInt(path + ".slot", -1);

            if (slot == -1) {
                logDebug("El item '" + itemKey + "' no tiene slot definido. Se omite.");
                continue;
            }

            if (permission != null && !player.hasPermission(permission)) {
                continue;
            }

            try {
                Material material = Material.valueOf(materialName.toUpperCase());
                ItemStack item;

                if (material == Material.PLAYER_HEAD) {
                    item = new ItemStack(Material.PLAYER_HEAD, 1);
                    SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
                    if (skullMeta != null) {
                        skullMeta.setOwningPlayer(player);
                        skullMeta.setDisplayName(displayName);
                        if (lore != null) skullMeta.setLore(lore);
                        item.setItemMeta(skullMeta);
                    }
                } else {
                    item = new ItemStack(material, 1);
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName(displayName);
                        if (lore != null) meta.setLore(lore);
                        item.setItemMeta(meta);
                    }
                }

                player.getInventory().setItem(slot, item);
            } catch (IllegalArgumentException e) {
                logDebug("Material inválido: " + materialName);
            }
        }

        if (config.getBoolean("teleport-bow.enabled", false)) {
            int bowSlot = config.getInt("teleport-bow.slot", 4);
            ItemStack bow = new ItemStack(Material.BOW, 1);
            ItemMeta bowMeta = bow.getItemMeta();
            if (bowMeta != null) {
                bowMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', config.getString("teleport-bow.name")));
                bowMeta.setLore(config.getStringList("teleport-bow.lore").stream().map(line -> ChatColor.translateAlternateColorCodes('&', line)).collect(Collectors.toList()));
                bow.setItemMeta(bowMeta);
            }
            player.getInventory().setItem(bowSlot, bow);
            player.getInventory().setItem(35, new ItemStack(Material.ARROW, 1));
        }

        ItemStack visibilityItem = new ItemStack(Material.LIME_DYE);
        ItemMeta visibilityMeta = visibilityItem.getItemMeta();
        if (visibilityMeta != null) {
            visibilityMeta.setDisplayName(ChatColor.GREEN + "Mostrar Jugadores");
            visibilityItem.setItemMeta(visibilityMeta);
        }
        player.getInventory().setItem(8, visibilityItem);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !item.hasItemMeta()) return;

        String clickedName = item.getItemMeta().getDisplayName();
        for (String itemKey : config.getStringList("items")) {
            String path = "item-settings." + itemKey;
            String nameRaw = config.getString(path + ".name");
            if (nameRaw == null) continue;

            String name = ChatColor.translateAlternateColorCodes('&', PlaceholderAPI.setPlaceholders(player, nameRaw));

            if (clickedName.equals(name)) {
                String command = config.getString(path + ".command");
                String server = config.getString(path + ".server");

                if (command != null && !command.isEmpty()) {
                    String finalCommand = PlaceholderAPI.setPlaceholders(player, command.replace("%player%", player.getName()));
                    logDebug("Ejecutando comando como jugador (simulando chat): " + finalCommand);
                    player.chat("/" + finalCommand);
                } else if (server != null && !server.isEmpty()) {
                    logDebug("Ejecutando BungeeCord a servidor: " + server);
                    player.sendPluginMessage(this, "BungeeCord", createBungeeMessage(player.getName(), server));
                }
                event.setCancelled(true);

                if (databaseEnabled) saveClickToDatabase(player.getName(), name);
            }
        }
    }

    private void saveClickToDatabase(String playerName, String itemName) {
        if (!databaseEnabled) return;
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO clicks (player, item) VALUES (?, ?);")) {
            ps.setString(1, playerName);
            ps.setString(2, itemName);
            ps.executeUpdate();
        } catch (SQLException e) {
            getLogger().warning("Error al guardar en la base de datos: " + e.getMessage());
        }
    }

    private byte[] createBungeeMessage(String playerName, String server) {
        java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
        java.io.DataOutputStream out = new java.io.DataOutputStream(b);
        try {
            out.writeUTF("Connect");
            out.writeUTF(server);
        } catch (Exception e) {
            getLogger().warning("Error creando mensaje BungeeCord: " + e.getMessage());
        }
        return b.toByteArray();
    }

    private void logDebug(String message) {
        if (debugEnabled) getLogger().info("[Debug] " + message);
    }

    public void reloadDatabaseSettings() {
        reloadConfig();
        config = getConfig();
        loadDatabaseSettings();
        debugEnabled = config.getBoolean("debug", false);
        if (debugEnabled) getLogger().info("Modo debug activado tras recarga.");
    }
}