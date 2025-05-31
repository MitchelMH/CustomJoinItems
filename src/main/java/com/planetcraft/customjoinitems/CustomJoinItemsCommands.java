package com.planetcraft.customjoinitems;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;

public class CustomJoinItemsCommands implements CommandExecutor {

    private final CustomJoinItemsPlugin plugin;

    public CustomJoinItemsCommands(CustomJoinItemsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§7Usa /customjoinitems help para ver los comandos.");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            plugin.reloadDatabaseSettings();
            sender.sendMessage("§aConfig y base de datos recargados correctamente.");
            return true;
        }

        if (args[0].equalsIgnoreCase("additem")) {
            if (args.length < 3) {
                sender.sendMessage("§cUso: /customjoinitems additem <id> <material>");
                return true;
            }
            String id = args[1];
            String material = args[2].toUpperCase();
            plugin.getConfig().set("item-settings." + id + ".material", material);
            plugin.getConfig().set("item-settings." + id + ".name", "§fNuevo Item");
            plugin.getConfig().set("item-settings." + id + ".lore", List.of("§7Este es un nuevo ítem."));
            plugin.getConfig().set("item-settings." + id + ".slot", 0);
            plugin.getConfig().set("item-settings." + id + ".permission", "customjoinitems.use");

            List<String> items = plugin.getConfig().getStringList("items");
            if (!items.contains(id)) {
                items.add(id);
                plugin.getConfig().set("items", items);
            }

            plugin.saveConfig();
            sender.sendMessage("§aÍtem " + id + " creado correctamente.");
            return true;
        }

        if (args[0].equalsIgnoreCase("setname")) {
            if (args.length < 3) {
                sender.sendMessage("§cUso: /customjoinitems setname <id> <nombre>");
                return true;
            }
            String id = args[1];
            String nombre = String.join(" ", List.of(args).subList(2, args.length));
            plugin.getConfig().set("item-settings." + id + ".name", nombre);
            plugin.saveConfig();
            sender.sendMessage("§aNombre actualizado para " + id + ".");
            return true;
        }

        if (args[0].equalsIgnoreCase("setlore")) {
            if (args.length < 3) {
                sender.sendMessage("§cUso: /customjoinitems setlore <id> <lore>");
                return true;
            }
            String id = args[1];
            String lore = String.join(" ", List.of(args).subList(2, args.length));
            plugin.getConfig().set("item-settings." + id + ".lore", List.of(lore));
            plugin.saveConfig();
            sender.sendMessage("§aLore actualizado para " + id + ".");
            return true;
        }

        if (args[0].equalsIgnoreCase("remove")) {
            if (args.length < 2) {
                sender.sendMessage("§cUso: /customjoinitems remove <id>");
                return true;
            }
            String id = args[1];
            plugin.getConfig().set("item-settings." + id, null);
            List<String> items = plugin.getConfig().getStringList("items");
            items.remove(id);
            plugin.getConfig().set("items", items);
            plugin.saveConfig();
            sender.sendMessage("§aÍtem " + id + " eliminado.");
            return true;
        }

        sender.sendMessage("§7Comando desconocido. Usa /customjoinitems help para ver los comandos disponibles.");
        return true;
    }
}