package com.playerguard.logging;

import com.playerguard.PlayerDropGuard;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DropLogger {

    private final PlayerDropGuard plugin;
    private final File logFile;
    private final SimpleDateFormat dateFormat;
    private final ExecutorService executor;

    public DropLogger(PlayerDropGuard plugin) {
        this.plugin = plugin;
        this.logFile = new File(plugin.getDataFolder(), plugin.getConfigManager().getLogFile());
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        this.executor = Executors.newSingleThreadExecutor();
        
        // 确保日志文件存在
        if (!logFile.exists()) {
            try {
                logFile.getParentFile().mkdirs();
                logFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("无法创建日志文件: " + e.getMessage());
            }
        }
    }

    public void logCreate(Player player, ItemStack item) {
        if (!plugin.getConfigManager().isLoggingEnabled()) return;
        log(String.format("[创建] %s 创建了虚拟掉落物: %s x%d", 
            player.getName(), item.getType().name(), item.getAmount()));
    }

    public void logConfirm(Player player, ItemStack item) {
        if (!plugin.getConfigManager().isLoggingEnabled()) return;
        log(String.format("[确认] %s 确认丢弃: %s x%d", 
            player.getName(), item.getType().name(), item.getAmount()));
    }

    public void logRecall(Player player, ItemStack item) {
        if (!plugin.getConfigManager().isLoggingEnabled()) return;
        log(String.format("[撤回] %s 撤回了: %s x%d", 
            player.getName(), item.getType().name(), item.getAmount()));
    }

    public void logTimeout(Player player, ItemStack item) {
        if (!plugin.getConfigManager().isLoggingEnabled()) return;
        log(String.format("[超时] %s 的物品超时自动撤回: %s x%d", 
            player.getName(), item.getType().name(), item.getAmount()));
    }

    public void logToggle(Player player, boolean enabled) {
        if (!plugin.getConfigManager().isLoggingEnabled()) return;
        log(String.format("[设置] %s %s了丢弃保护", 
            player.getName(), enabled ? "开启" : "关闭"));
    }

    private void log(String message) {
        executor.submit(() -> {
            try (FileWriter fw = new FileWriter(logFile, true);
                 BufferedWriter bw = new BufferedWriter(fw);
                 PrintWriter out = new PrintWriter(bw)) {
                out.println("[" + dateFormat.format(new Date()) + "] " + message);
            } catch (IOException e) {
                plugin.getLogger().warning("写入日志失败: " + e.getMessage());
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
}
