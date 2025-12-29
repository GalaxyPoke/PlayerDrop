package com.playerguard.managers;

import com.playerguard.PlayerDropGuard;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final PlayerDropGuard plugin;
    private int confirmTimeout;
    private int doubleClickInterval;
    private String glowColor;
    private boolean defaultEnabled;
    
    // 声音配置
    private boolean soundEnabled;
    private Sound createSound;
    private Sound confirmSound;
    private Sound recallSound;
    private Sound timeoutSound;
    
    // 粒子配置
    private boolean particleEnabled;
    private Particle particleType;
    private int particleCount;
    
    // Boss Bar 配置
    private boolean bossBarEnabled;
    private org.bukkit.boss.BarColor bossBarColor;
    private org.bukkit.boss.BarStyle bossBarStyle;
    private String bossBarTitle;
    
    // 日志配置
    private boolean loggingEnabled;
    private String logFile;
    
    // 数据库配置
    private boolean databaseEnabled;
    private String dbType;
    private String dbHost;
    private int dbPort;
    private String dbDatabase;
    private String dbUsername;
    private String dbPassword;
    private String dbTablePrefix;
    
    // 消息配置
    private String actionBarHint;
    private String droppedMessage;
    private String recalledMessage;
    private String timeoutRecalledMessage;
    private String guiTitle;
    private String enabledMessage;
    private String disabledMessage;
    private String reloadSuccessMessage;
    private String playerNotFoundMessage;
    private String toggledForPlayerMessage;

    public ConfigManager(PlayerDropGuard plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        
        this.confirmTimeout = config.getInt("confirm-timeout", 5);
        this.doubleClickInterval = config.getInt("double-click-interval", 300);
        this.glowColor = config.getString("glow-color", "YELLOW");
        this.defaultEnabled = config.getBoolean("default-enabled", true);
        
        // 加载声音配置
        this.soundEnabled = config.getBoolean("sounds.enabled", true);
        this.createSound = parseSound(config.getString("sounds.create", "BLOCK_NOTE_BLOCK_PLING"));
        this.confirmSound = parseSound(config.getString("sounds.confirm", "ENTITY_EXPERIENCE_ORB_PICKUP"));
        this.recallSound = parseSound(config.getString("sounds.recall", "ENTITY_ITEM_PICKUP"));
        this.timeoutSound = parseSound(config.getString("sounds.timeout", "BLOCK_NOTE_BLOCK_BASS"));
        
        // 加载粒子配置
        this.particleEnabled = config.getBoolean("particles.enabled", true);
        this.particleType = parseParticle(config.getString("particles.type", "END_ROD"));
        this.particleCount = config.getInt("particles.count", 3);
        
        // 加载Boss Bar配置
        this.bossBarEnabled = config.getBoolean("bossbar.enabled", false);
        this.bossBarColor = parseBarColor(config.getString("bossbar.color", "YELLOW"));
        this.bossBarStyle = parseBarStyle(config.getString("bossbar.style", "SOLID"));
        this.bossBarTitle = colorize(config.getString("bossbar.title", "&e丢弃确认倒计时: &f%time%秒"));
        
        // 加载日志配置
        this.loggingEnabled = config.getBoolean("logging.enabled", true);
        this.logFile = config.getString("logging.file", "drops.log");
        
        // 加载数据库配置
        this.databaseEnabled = config.getBoolean("database.enabled", false);
        this.dbType = config.getString("database.type", "MySQL");
        this.dbHost = config.getString("database.host", "localhost");
        this.dbPort = config.getInt("database.port", 3306);
        this.dbDatabase = config.getString("database.database", "minecraft");
        this.dbUsername = config.getString("database.username", "root");
        this.dbPassword = config.getString("database.password", "");
        this.dbTablePrefix = config.getString("database.table-prefix", "dropguard_");
        
        // 加载消息配置
        this.actionBarHint = colorize(config.getString("messages.action-bar-hint", "&e Shift+Q确认丢弃 &7| &cQ撤回"));
        this.droppedMessage = colorize(config.getString("messages.dropped", "&a已丢弃 &f[%item%]"));
        this.recalledMessage = colorize(config.getString("messages.recalled", "&e已撤回 &f[%item%]"));
        this.timeoutRecalledMessage = colorize(config.getString("messages.timeout-recalled", "&7[%item%] &e已超时自动撤回"));
        this.guiTitle = colorize(config.getString("messages.gui-title", "&8丢弃保护设置"));
        this.enabledMessage = colorize(config.getString("messages.enabled", "&a保护已开启"));
        this.disabledMessage = colorize(config.getString("messages.disabled", "&c保护已关闭"));
        this.reloadSuccessMessage = colorize(config.getString("messages.reload-success", "&a配置已重载"));
        this.playerNotFoundMessage = colorize(config.getString("messages.player-not-found", "&c找不到玩家: %player%"));
        this.toggledForPlayerMessage = colorize(config.getString("messages.toggled-for-player", "&a已为 %player% %status% 丢弃保护"));
    }
    
    private Sound parseSound(String soundName) {
        try {
            return Sound.valueOf(soundName);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("无效的声音: " + soundName + ", 使用默认值");
            return Sound.BLOCK_NOTE_BLOCK_PLING;
        }
    }
    
    private Particle parseParticle(String particleName) {
        try {
            return Particle.valueOf(particleName);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("无效的粒子: " + particleName + ", 使用默认值");
            return Particle.END_ROD;
        }
    }
    
    private org.bukkit.boss.BarColor parseBarColor(String colorName) {
        try {
            return org.bukkit.boss.BarColor.valueOf(colorName);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("无效的Boss Bar颜色: " + colorName + ", 使用默认值");
            return org.bukkit.boss.BarColor.YELLOW;
        }
    }
    
    private org.bukkit.boss.BarStyle parseBarStyle(String styleName) {
        try {
            return org.bukkit.boss.BarStyle.valueOf(styleName);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("无效的Boss Bar样式: " + styleName + ", 使用默认值");
            return org.bukkit.boss.BarStyle.SOLID;
        }
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public int getConfirmTimeout() { return confirmTimeout; }
    public int getConfirmTimeoutTicks() { return confirmTimeout * 20; }
    public int getDoubleClickInterval() { return doubleClickInterval; }
    public String getGlowColor() { return glowColor; }
    public boolean isDefaultEnabled() { return defaultEnabled; }
    
    // 声音相关
    public boolean isSoundEnabled() { return soundEnabled; }
    public Sound getCreateSound() { return createSound; }
    public Sound getConfirmSound() { return confirmSound; }
    public Sound getRecallSound() { return recallSound; }
    public Sound getTimeoutSound() { return timeoutSound; }
    
    // 粒子相关
    public boolean isParticleEnabled() { return particleEnabled; }
    public Particle getParticleType() { return particleType; }
    public int getParticleCount() { return particleCount; }
    
    // Boss Bar相关
    public boolean isBossBarEnabled() { return bossBarEnabled; }
    public org.bukkit.boss.BarColor getBossBarColor() { return bossBarColor; }
    public org.bukkit.boss.BarStyle getBossBarStyle() { return bossBarStyle; }
    public String getBossBarTitle(int timeLeft) { return bossBarTitle.replace("%time%", String.valueOf(timeLeft)); }
    
    // 日志相关
    public boolean isLoggingEnabled() { return loggingEnabled; }
    public String getLogFile() { return logFile; }
    
    // 数据库相关
    public boolean isDatabaseEnabled() { return databaseEnabled; }
    public String getDbType() { return dbType; }
    public String getDbHost() { return dbHost; }
    public int getDbPort() { return dbPort; }
    public String getDbDatabase() { return dbDatabase; }
    public String getDbUsername() { return dbUsername; }
    public String getDbPassword() { return dbPassword; }
    public String getDbTablePrefix() { return dbTablePrefix; }

    // 消息相关
    public String getActionBarHint() { return actionBarHint; }
    public String getDroppedMessage(String itemName) { return droppedMessage.replace("%item%", itemName); }
    public String getRecalledMessage(String itemName) { return recalledMessage.replace("%item%", itemName); }
    public String getTimeoutRecalledMessage(String itemName) { return timeoutRecalledMessage.replace("%item%", itemName); }
    public String getGuiTitle() { return guiTitle; }
    public String getEnabledMessage() { return enabledMessage; }
    public String getDisabledMessage() { return disabledMessage; }
    public String getReloadSuccessMessage() { return reloadSuccessMessage; }
    public String getPlayerNotFoundMessage(String player) { return playerNotFoundMessage.replace("%player%", player); }
    public String getToggledForPlayerMessage(String player, boolean enabled) { 
        return toggledForPlayerMessage.replace("%player%", player).replace("%status%", enabled ? "开启" : "关闭"); 
    }
}
