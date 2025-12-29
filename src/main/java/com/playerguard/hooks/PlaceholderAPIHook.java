package com.playerguard.hooks;

import com.playerguard.PlayerDropGuard;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlaceholderAPIHook extends PlaceholderExpansion {

    private final PlayerDropGuard plugin;

    public PlaceholderAPIHook(PlayerDropGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "dropguard";
    }

    @Override
    public @NotNull String getAuthor() {
        return "PlayerDropGuard";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        switch (params.toLowerCase()) {
            case "enabled":
                // 返回玩家是否启用了保护
                return plugin.getPlayerSettingsManager().isProtectionEnabled(player.getUniqueId()) ? "是" : "否";
            
            case "enabled_raw":
                // 返回原始布尔值
                return String.valueOf(plugin.getPlayerSettingsManager().isProtectionEnabled(player.getUniqueId()));
            
            case "pending":
                // 返回是否有待确认物品
                return plugin.getVirtualDropManager().hasPendingDrops(player.getUniqueId()) ? "是" : "否";
            
            case "pending_count":
                // 返回待确认物品数量
                return String.valueOf(plugin.getVirtualDropManager().getPendingDropCount(player.getUniqueId()));
            
            case "status":
                // 返回状态文本
                boolean enabled = plugin.getPlayerSettingsManager().isProtectionEnabled(player.getUniqueId());
                return enabled ? "§a已开启" : "§c已关闭";
            
            case "timeout":
                // 返回超时时间
                return String.valueOf(plugin.getConfigManager().getConfirmTimeout());
            
            default:
                return null;
        }
    }
}
