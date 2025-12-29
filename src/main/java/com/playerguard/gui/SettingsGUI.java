package com.playerguard.gui;

import com.playerguard.PlayerDropGuard;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class SettingsGUI {

    public static final int TOGGLE_SLOT = 13;
    private static final int GUI_SIZE = 27;

    public static void open(Player player, PlayerDropGuard plugin) {
        boolean enabled = plugin.getPlayerSettingsManager().isProtectionEnabled(player.getUniqueId());
        
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, plugin.getConfigManager().getGuiTitle());
        
        // 创建切换按钮
        ItemStack toggleButton;
        if (enabled) {
            toggleButton = createItem(
                Material.LIME_STAINED_GLASS_PANE,
                ChatColor.GREEN + "丢弃保护: 开启",
                ChatColor.GRAY + "点击关闭丢弃保护",
                "",
                ChatColor.YELLOW + "当前状态:",
                ChatColor.GREEN + "✔ 丢弃物品时需要确认",
                ChatColor.GREEN + "✔ 可随时撤回未确认的物品"
            );
        } else {
            toggleButton = createItem(
                Material.RED_STAINED_GLASS_PANE,
                ChatColor.RED + "丢弃保护: 关闭",
                ChatColor.GRAY + "点击开启丢弃保护",
                "",
                ChatColor.YELLOW + "当前状态:",
                ChatColor.RED + "✖ 物品将直接丢弃",
                ChatColor.RED + "✖ 无法撤回已丢弃的物品"
            );
        }
        
        gui.setItem(TOGGLE_SLOT, toggleButton);
        
        // 用灰色玻璃板填充边框
        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < GUI_SIZE; i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, border);
            }
        }
        
        // 添加说明物品
        ItemStack infoItem = createItem(
            Material.BOOK,
            ChatColor.GOLD + "使用说明",
            ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━",
            ChatColor.WHITE + "Q键" + ChatColor.GRAY + " - 创建待确认掉落物/撤回",
            ChatColor.WHITE + "Shift+Q" + ChatColor.GRAY + " - 确认丢弃物品",
            ChatColor.WHITE + "双击Q" + ChatColor.GRAY + " - 打开此设置界面",
            ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━",
            ChatColor.YELLOW + "超时时间: " + ChatColor.WHITE + plugin.getConfigManager().getConfirmTimeout() + "秒"
        );
        gui.setItem(11, infoItem);
        
        // 添加状态指示器
        ItemStack statusItem = createItem(
            enabled ? Material.EMERALD : Material.REDSTONE,
            enabled ? ChatColor.GREEN + "保护已激活" : ChatColor.RED + "保护未激活",
            ChatColor.GRAY + "当前保护状态指示器"
        );
        gui.setItem(15, statusItem);
        
        player.openInventory(gui);
    }

    private static ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
