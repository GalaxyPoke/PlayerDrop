package com.playerguard;

import com.playerguard.commands.DropGuardCommand;
import com.playerguard.database.DatabaseManager;
import com.playerguard.hooks.PlaceholderAPIHook;
import com.playerguard.listeners.DropListener;
import com.playerguard.listeners.GUIListener;
import com.playerguard.listeners.PlayerListener;
import com.playerguard.logging.DropLogger;
import com.playerguard.managers.ConfigManager;
import com.playerguard.managers.PlayerSettingsManager;
import com.playerguard.managers.VirtualDropManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerDropGuard extends JavaPlugin {

    private static PlayerDropGuard instance;
    private ConfigManager configManager;
    private PlayerSettingsManager playerSettingsManager;
    private VirtualDropManager virtualDropManager;
    private DatabaseManager databaseManager;
    private DropLogger dropLogger;

    @Override
    public void onEnable() {
        instance = this;
        
        // 初始化管理器
        this.configManager = new ConfigManager(this);
        this.playerSettingsManager = new PlayerSettingsManager(this);
        this.virtualDropManager = new VirtualDropManager(this);
        
        // 初始化日志记录器
        this.dropLogger = new DropLogger(this);
        
        // 初始化数据库（如果启用）
        if (configManager.isDatabaseEnabled()) {
            this.databaseManager = new DatabaseManager(this);
            databaseManager.connect();
        }
        
        // 注册命令
        getCommand("pdg").setExecutor(new DropGuardCommand(this));
        getCommand("pdg").setTabCompleter(new DropGuardCommand(this));
        
        // 注册Bukkit事件监听器
        getServer().getPluginManager().registerEvents(new DropListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        
        // 注册PlaceholderAPI扩展
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderAPIHook(this).register();
            getLogger().info("已挂钩 PlaceholderAPI");
        }
        
        getLogger().info("PlayerDropGuard 已启用!");
    }

    @Override
    public void onDisable() {
        // 将所有待确认物品返还给玩家
        if (virtualDropManager != null) {
            virtualDropManager.returnAllPendingItems();
        }
        
        // 保存玩家设置
        if (playerSettingsManager != null) {
            playerSettingsManager.saveAll();
        }
        
        // 关闭数据库连接
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
        
        // 关闭日志记录器
        if (dropLogger != null) {
            dropLogger.shutdown();
        }
        
        getLogger().info("PlayerDropGuard 已禁用!");
    }

    public static PlayerDropGuard getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public PlayerSettingsManager getPlayerSettingsManager() {
        return playerSettingsManager;
    }

    public VirtualDropManager getVirtualDropManager() {
        return virtualDropManager;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public DropLogger getDropLogger() {
        return dropLogger;
    }
}
