package com.playerguard.listeners;

import com.playerguard.PlayerDropGuard;
import com.playerguard.gui.SettingsGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

public class DropListener implements Listener {

    private final PlayerDropGuard plugin;

    public DropListener(PlayerDropGuard plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        
        // 检查绕过权限
        if (player.hasPermission("dropguard.bypass")) {
            return;
        }
        
        // 检查使用权限
        if (!player.hasPermission("dropguard.use")) {
            return;
        }
        
        // 检查该玩家是否启用了保护
        if (!plugin.getPlayerSettingsManager().isProtectionEnabled(player.getUniqueId())) {
            return;
        }
        
        // 检查双击以打开设置
        if (plugin.getVirtualDropManager().checkDoubleClick(player.getUniqueId())) {
            event.setCancelled(true);
            // 返还即将丢弃的物品
            ItemStack droppedItem = event.getItemDrop().getItemStack();
            player.getInventory().addItem(droppedItem);
            event.getItemDrop().remove();
            
            // 打开设置GUI
            if (player.hasPermission("dropguard.toggle")) {
                SettingsGUI.open(player, plugin);
            }
            return;
        }
        
        // 检查玩家是否潜行 (Shift+Q = 确认丢弃)
        if (player.isSneaking()) {
            // 检查是否有待确认的掉落物
            if (plugin.getVirtualDropManager().hasPendingDrops(player.getUniqueId())) {
                // 确认待处理的掉落物而不是丢弃新物品
                event.setCancelled(true);
                ItemStack droppedItem = event.getItemDrop().getItemStack();
                player.getInventory().addItem(droppedItem);
                event.getItemDrop().remove();
                
                plugin.getVirtualDropManager().confirmDrop(player);
            }
            // 如果没有待确认的掉落物，允许潜行时正常丢弃
            return;
        }
        
        // 普通丢弃 (不按Shift的Q) - 创建虚拟待确认掉落物
        event.setCancelled(true);
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        event.getItemDrop().remove();
        
        // 检查是否有待确认掉落物 - 不按Shift的Q会撤回它们
        if (plugin.getVirtualDropManager().hasPendingDrops(player.getUniqueId())) {
            // 撤回待确认的掉落物
            plugin.getVirtualDropManager().recallDrop(player);
            // 将触发此事件的物品返还到背包
            player.getInventory().addItem(droppedItem);
        } else {
            // 创建新的虚拟待确认掉落物
            plugin.getVirtualDropManager().createPendingDrop(player, droppedItem);
        }
    }
}
