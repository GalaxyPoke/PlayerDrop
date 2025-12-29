package com.playerguard.managers;

import com.playerguard.PlayerDropGuard;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerSettingsManager {

    private final PlayerDropGuard plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;
    private final ConcurrentHashMap<UUID, Boolean> playerSettings;

    public PlayerSettingsManager(PlayerDropGuard plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        this.playerSettings = new ConcurrentHashMap<>();
        loadData();
    }

    private void loadData() {
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("无法创建 playerdata.yml: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        
        // 加载所有玩家设置到内存
        if (dataConfig.contains("players")) {
            for (String uuidStr : dataConfig.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    boolean enabled = dataConfig.getBoolean("players." + uuidStr, 
                        plugin.getConfigManager().isDefaultEnabled());
                    playerSettings.put(uuid, enabled);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    public boolean isProtectionEnabled(UUID playerUuid) {
        return playerSettings.computeIfAbsent(playerUuid, 
            k -> plugin.getConfigManager().isDefaultEnabled());
    }

    public void setProtectionEnabled(UUID playerUuid, boolean enabled) {
        playerSettings.put(playerUuid, enabled);
        // 异步保存
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            dataConfig.set("players." + playerUuid.toString(), enabled);
            saveData();
        });
    }

    public void toggleProtection(UUID playerUuid) {
        boolean current = isProtectionEnabled(playerUuid);
        setProtectionEnabled(playerUuid, !current);
    }

    private synchronized void saveData() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存 playerdata.yml: " + e.getMessage());
        }
    }

    public void saveAll() {
        for (var entry : playerSettings.entrySet()) {
            dataConfig.set("players." + entry.getKey().toString(), entry.getValue());
        }
        saveData();
    }
    
    public int getTotalPlayers() {
        return playerSettings.size();
    }
    
    public int getEnabledPlayersCount() {
        return (int) playerSettings.values().stream().filter(b -> b).count();
    }
}
