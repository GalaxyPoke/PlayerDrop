package com.playerguard.listeners;

import com.playerguard.PlayerDropGuard;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final PlayerDropGuard plugin;

    public PlayerListener(PlayerDropGuard plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 在玩家退出前返还所有待确认物品
        plugin.getVirtualDropManager().handlePlayerQuit(event.getPlayer());
    }
}
