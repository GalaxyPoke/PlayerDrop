package com.playerguard.listeners;

import com.playerguard.PlayerDropGuard;
import com.playerguard.gui.SettingsGUI;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GUIListener implements Listener {

    private final PlayerDropGuard plugin;

    public GUIListener(PlayerDropGuard plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        String title = event.getView().getTitle();
        
        if (!title.equals(plugin.getConfigManager().getGuiTitle())) {
            return;
        }
        
        event.setCancelled(true);
        
        int slot = event.getRawSlot();
        if (slot == SettingsGUI.TOGGLE_SLOT) {
            if (!player.hasPermission("dropguard.toggle")) {
                return;
            }
            
            plugin.getPlayerSettingsManager().toggleProtection(player.getUniqueId());
            boolean enabled = plugin.getPlayerSettingsManager().isProtectionEnabled(player.getUniqueId());
            
            // 记录日志
            if (plugin.getDropLogger() != null) {
                plugin.getDropLogger().logToggle(player, enabled);
            }
            
            // 更新GUI
            SettingsGUI.open(player, plugin);
            
            // 发送反馈
            String message = enabled ? 
                plugin.getConfigManager().getEnabledMessage() : 
                plugin.getConfigManager().getDisabledMessage();
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, 
                TextComponent.fromLegacyText(message));
        }
    }
}
