package com.playerguard.commands;

import com.playerguard.PlayerDropGuard;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DropGuardCommand implements CommandExecutor, TabCompleter {

    private final PlayerDropGuard plugin;

    public DropGuardCommand(PlayerDropGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                return handleReload(sender);
            case "toggle":
                return handleToggle(sender, args);
            case "status":
                return handleStatus(sender, args);
            case "stats":
                return handleStats(sender);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("dropguard.admin")) {
            sender.sendMessage("§c你没有权限执行此命令");
            return true;
        }

        plugin.getConfigManager().loadConfig();
        sender.sendMessage(plugin.getConfigManager().getReloadSuccessMessage());
        return true;
    }

    private boolean handleToggle(CommandSender sender, String[] args) {
        if (!sender.hasPermission("dropguard.admin")) {
            sender.sendMessage("§c你没有权限执行此命令");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§c用法: /dropguard toggle <玩家>");
            return true;
        }

        String playerName = args[1];
        Player target = Bukkit.getPlayer(playerName);

        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getPlayerNotFoundMessage(playerName));
            return true;
        }

        plugin.getPlayerSettingsManager().toggleProtection(target.getUniqueId());
        boolean enabled = plugin.getPlayerSettingsManager().isProtectionEnabled(target.getUniqueId());
        sender.sendMessage(plugin.getConfigManager().getToggledForPlayerMessage(target.getName(), enabled));
        return true;
    }

    private boolean handleStatus(CommandSender sender, String[] args) {
        Player target;

        if (args.length >= 2 && sender.hasPermission("dropguard.admin")) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(plugin.getConfigManager().getPlayerNotFoundMessage(args[1]));
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage("§c请指定玩家: /dropguard status <玩家>");
            return true;
        }

        boolean enabled = plugin.getPlayerSettingsManager().isProtectionEnabled(target.getUniqueId());
        boolean hasPending = plugin.getVirtualDropManager().hasPendingDrops(target.getUniqueId());

        sender.sendMessage("§6===== " + target.getName() + " 的丢弃保护状态 =====");
        sender.sendMessage("§7保护状态: " + (enabled ? "§a已开启" : "§c已关闭"));
        sender.sendMessage("§7待确认物品: " + (hasPending ? "§e有" : "§7无"));
        return true;
    }

    private boolean handleStats(CommandSender sender) {
        if (!sender.hasPermission("dropguard.admin")) {
            sender.sendMessage("§c你没有权限执行此命令");
            return true;
        }

        int totalPlayers = plugin.getPlayerSettingsManager().getTotalPlayers();
        int enabledPlayers = plugin.getPlayerSettingsManager().getEnabledPlayersCount();

        sender.sendMessage("§6===== PlayerDropGuard 统计 =====");
        sender.sendMessage("§7总玩家数: §f" + totalPlayers);
        sender.sendMessage("§7开启保护的玩家: §a" + enabledPlayers);
        sender.sendMessage("§7关闭保护的玩家: §c" + (totalPlayers - enabledPlayers));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6===== PlayerDropGuard 帮助 =====");
        sender.sendMessage("§e/dropguard reload §7- 重载配置");
        sender.sendMessage("§e/dropguard toggle <玩家> §7- 切换玩家保护状态");
        sender.sendMessage("§e/dropguard status [玩家] §7- 查看保护状态");
        sender.sendMessage("§e/dropguard stats §7- 查看统计信息");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("reload", "toggle", "status", "stats");
            for (String sub : subCommands) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("toggle") || args[0].equalsIgnoreCase("status"))) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(player.getName());
                }
            }
        }

        return completions;
    }
}
