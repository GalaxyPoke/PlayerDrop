package com.playerguard.managers;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.playerguard.PlayerDropGuard;
import com.playerguard.data.PendingDrop;
import com.playerguard.util.ItemUtils;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class VirtualDropManager {

    private final PlayerDropGuard plugin;
    private final ConcurrentHashMap<UUID, List<PendingDrop>> pendingDrops;
    private final ConcurrentHashMap<UUID, Long> lastDropTime;
    private final AtomicInteger entityIdCounter;

    public VirtualDropManager(PlayerDropGuard plugin) {
        this.plugin = plugin;
        this.pendingDrops = new ConcurrentHashMap<>();
        this.lastDropTime = new ConcurrentHashMap<>();
        this.entityIdCounter = new AtomicInteger(-1000);
    }

    public boolean checkDoubleClick(UUID playerUuid) {
        long now = System.currentTimeMillis();
        Long lastTime = lastDropTime.get(playerUuid);
        lastDropTime.put(playerUuid, now);
        
        if (lastTime != null) {
            long diff = now - lastTime;
            if (diff <= plugin.getConfigManager().getDoubleClickInterval()) {
                lastDropTime.remove(playerUuid);
                return true;
            }
        }
        return false;
    }

    public void createPendingDrop(Player player, ItemStack item) {
        UUID playerUuid = player.getUniqueId();
        int entityId = entityIdCounter.decrementAndGet();
        
        // 计算玩家前方的位置
        Location loc = player.getEyeLocation();
        Vector3d position = new Vector3d(
            loc.getX() + loc.getDirection().getX() * 1.5,
            loc.getY() - 0.5,
            loc.getZ() + loc.getDirection().getZ() * 1.5
        );
        
        // 创建虚拟物品实体数据包
        UUID entityUuid = UUID.randomUUID();
        WrapperPlayServerSpawnEntity spawnPacket = new WrapperPlayServerSpawnEntity(
            entityId, Optional.of(entityUuid), EntityTypes.ITEM,
            position, 0f, 0f, 0f, 0, Optional.empty()
        );
        
        // 仅向该玩家发送生成实体数据包
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, spawnPacket);
        
        // 创建带发光效果的物品元数据
        List<EntityData> metadata = new ArrayList<>();
        
        // 实体标志 - 设置发光效果 (0x40)
        byte flags = 0x40;
        metadata.add(new EntityData(0, EntityDataTypes.BYTE, flags));
        
        // 物品槽位数据
        com.github.retrooper.packetevents.protocol.item.ItemStack packetItem = 
            SpigotConversionUtil.fromBukkitItemStack(item);
        metadata.add(new EntityData(8, EntityDataTypes.ITEMSTACK, packetItem));
        
        WrapperPlayServerEntityMetadata metadataPacket = 
            new WrapperPlayServerEntityMetadata(entityId, metadata);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, metadataPacket);
        
        // 创建待确认掉落物记录
        PendingDrop pendingDrop = new PendingDrop(entityId, entityUuid, item, position, System.currentTimeMillis());
        
        // 添加到待确认列表
        pendingDrops.computeIfAbsent(playerUuid, k -> Collections.synchronizedList(new ArrayList<>()))
            .add(pendingDrop);
        
        // 调度超时任务
        BukkitTask timeoutTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            timeoutDrop(player, pendingDrop);
        }, plugin.getConfigManager().getConfirmTimeoutTicks());
        
        pendingDrop.setTimeoutTask(timeoutTask);
        
        // 发送ActionBar提示
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, 
            TextComponent.fromLegacyText(plugin.getConfigManager().getActionBarHint()));
        
        // 播放创建声音
        playSound(player, plugin.getConfigManager().getCreateSound());
        
        // 启动浮动效果的位置更新任务
        startFloatingAnimation(player, pendingDrop);
        
        // 记录日志
        if (plugin.getDropLogger() != null) {
            plugin.getDropLogger().logCreate(player, item);
        }
        
        // 创建Boss Bar倒计时
        if (plugin.getConfigManager().isBossBarEnabled()) {
            org.bukkit.boss.BossBar bossBar = org.bukkit.Bukkit.createBossBar(
                plugin.getConfigManager().getBossBarTitle(plugin.getConfigManager().getConfirmTimeout()),
                plugin.getConfigManager().getBossBarColor(),
                plugin.getConfigManager().getBossBarStyle()
            );
            bossBar.addPlayer(player);
            bossBar.setProgress(1.0);
            pendingDrop.setBossBar(bossBar);
        }
    }

    private void startFloatingAnimation(Player player, PendingDrop drop) {
        final long[] tickCounter = {0};
        BukkitTask animationTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || !hasPendingDrop(player.getUniqueId(), drop)) {
                return;
            }
            
            tickCounter[0]++;
            
            // 更新位置以保持在玩家前方
            Location loc = player.getEyeLocation();
            double newX = loc.getX() + loc.getDirection().getX() * 1.5;
            double newY = loc.getY() - 0.5 + Math.sin(System.currentTimeMillis() / 300.0) * 0.1;
            double newZ = loc.getZ() + loc.getDirection().getZ() * 1.5;
            
            drop.setPosition(new Vector3d(newX, newY, newZ));
            
            // 每5tick生成一次粒子效果
            if (tickCounter[0] % 5 == 0) {
                Location particleLoc = new Location(player.getWorld(), newX, newY, newZ);
                spawnParticles(player, particleLoc);
            }
            
            // 更新Boss Bar进度
            if (drop.getBossBar() != null) {
                long elapsed = System.currentTimeMillis() - drop.getCreateTime();
                long totalTime = plugin.getConfigManager().getConfirmTimeout() * 1000L;
                double progress = Math.max(0, 1.0 - (double) elapsed / totalTime);
                int timeLeft = (int) Math.ceil((totalTime - elapsed) / 1000.0);
                drop.getBossBar().setProgress(progress);
                drop.getBossBar().setTitle(plugin.getConfigManager().getBossBarTitle(Math.max(0, timeLeft)));
            }
            
            WrapperPlayServerEntityTeleport teleportPacket = new WrapperPlayServerEntityTeleport(
                drop.getEntityId(),
                drop.getPosition(),
                0f, 0f,
                false
            );
            
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, teleportPacket);
        }, 1L, 1L);
        
        drop.setAnimationTask(animationTask);
    }

    private boolean hasPendingDrop(UUID playerUuid, PendingDrop drop) {
        List<PendingDrop> drops = pendingDrops.get(playerUuid);
        return drops != null && drops.contains(drop);
    }

    public void confirmDrop(Player player) {
        UUID playerUuid = player.getUniqueId();
        List<PendingDrop> drops = pendingDrops.get(playerUuid);
        
        if (drops == null || drops.isEmpty()) {
            return;
        }
        
        // 获取最近的待确认掉落物
        PendingDrop drop = drops.remove(drops.size() - 1);
        cleanupDrop(player, drop);
        
        // 在玩家位置生成真实掉落物
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Location dropLoc = player.getLocation();
            org.bukkit.entity.Item droppedEntity = player.getWorld().dropItemNaturally(dropLoc, drop.getItem());
            // 设置捡起延迟，防止立刻被吸回
            droppedEntity.setPickupDelay(40); // 2秒
            
            String itemName = ItemUtils.getItemName(drop.getItem());
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                TextComponent.fromLegacyText(plugin.getConfigManager().getDroppedMessage(itemName)));
            
            // 播放确认声音
            playSound(player, plugin.getConfigManager().getConfirmSound());
            
            // 记录日志
            if (plugin.getDropLogger() != null) {
                plugin.getDropLogger().logConfirm(player, drop.getItem());
            }
        });
    }

    public void recallDrop(Player player) {
        UUID playerUuid = player.getUniqueId();
        List<PendingDrop> drops = pendingDrops.get(playerUuid);
        
        if (drops == null || drops.isEmpty()) {
            return;
        }
        
        // 获取最近的待确认掉落物
        PendingDrop drop = drops.remove(drops.size() - 1);
        cleanupDrop(player, drop);
        
        // 将物品返还到玩家背包
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(drop.getItem());
            if (!overflow.isEmpty()) {
                // 如果背包已满，掉落在玩家脚下
                for (ItemStack item : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                }
            }
            
            String itemName = ItemUtils.getItemName(drop.getItem());
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                TextComponent.fromLegacyText(plugin.getConfigManager().getRecalledMessage(itemName)));
            
            // 播放撤回声音
            playSound(player, plugin.getConfigManager().getRecallSound());
            
            // 记录日志
            if (plugin.getDropLogger() != null) {
                plugin.getDropLogger().logRecall(player, drop.getItem());
            }
        });
    }

    private void timeoutDrop(Player player, PendingDrop drop) {
        UUID playerUuid = player.getUniqueId();
        List<PendingDrop> drops = pendingDrops.get(playerUuid);
        
        if (drops == null || !drops.remove(drop)) {
            return;
        }
        
        cleanupDrop(player, drop);
        
        // 将物品返还到玩家背包
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(drop.getItem());
                if (!overflow.isEmpty()) {
                    for (ItemStack item : overflow.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), item);
                    }
                }
                
                String itemName = ItemUtils.getItemName(drop.getItem());
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText(plugin.getConfigManager().getTimeoutRecalledMessage(itemName)));
                
                // 播放超时声音
                playSound(player, plugin.getConfigManager().getTimeoutSound());
                
                // 记录日志
                if (plugin.getDropLogger() != null) {
                    plugin.getDropLogger().logTimeout(player, drop.getItem());
                }
            }
        });
    }

    private void cleanupDrop(Player player, PendingDrop drop) {
        // 取消任务
        if (drop.getTimeoutTask() != null) {
            drop.getTimeoutTask().cancel();
        }
        if (drop.getAnimationTask() != null) {
            drop.getAnimationTask().cancel();
        }
        
        // 移除Boss Bar
        if (drop.getBossBar() != null) {
            drop.getBossBar().removeAll();
        }
        
        // 发送销毁实体数据包
        if (player.isOnline()) {
            WrapperPlayServerDestroyEntities destroyPacket = 
                new WrapperPlayServerDestroyEntities(drop.getEntityId());
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, destroyPacket);
        }
    }

    public boolean hasPendingDrops(UUID playerUuid) {
        List<PendingDrop> drops = pendingDrops.get(playerUuid);
        return drops != null && !drops.isEmpty();
    }

    public void returnAllPendingItems() {
        for (Map.Entry<UUID, List<PendingDrop>> entry : pendingDrops.entrySet()) {
            UUID playerUuid = entry.getKey();
            Player player = plugin.getServer().getPlayer(playerUuid);
            
            if (player != null && player.isOnline()) {
                for (PendingDrop drop : entry.getValue()) {
                    cleanupDrop(player, drop);
                    HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(drop.getItem());
                    if (!overflow.isEmpty()) {
                        for (ItemStack item : overflow.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), item);
                        }
                    }
                }
            }
        }
        pendingDrops.clear();
    }

    public void handlePlayerQuit(Player player) {
        UUID playerUuid = player.getUniqueId();
        List<PendingDrop> drops = pendingDrops.remove(playerUuid);
        lastDropTime.remove(playerUuid);
        
        if (drops != null) {
            for (PendingDrop drop : drops) {
                if (drop.getTimeoutTask() != null) {
                    drop.getTimeoutTask().cancel();
                }
                if (drop.getAnimationTask() != null) {
                    drop.getAnimationTask().cancel();
                }
                
                // 将物品返还到玩家背包
                HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(drop.getItem());
                if (!overflow.isEmpty()) {
                    for (ItemStack item : overflow.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), item);
                    }
                }
            }
        }
    }
    
    public int getPendingDropCount(UUID playerUuid) {
        List<PendingDrop> drops = pendingDrops.get(playerUuid);
        return drops != null ? drops.size() : 0;
    }
    
    private void playSound(Player player, org.bukkit.Sound sound) {
        if (plugin.getConfigManager().isSoundEnabled() && sound != null) {
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        }
    }
    
    private void spawnParticles(Player player, Location location) {
        if (plugin.getConfigManager().isParticleEnabled()) {
            player.spawnParticle(
                plugin.getConfigManager().getParticleType(),
                location,
                plugin.getConfigManager().getParticleCount(),
                0.2, 0.2, 0.2, 0.01
            );
        }
    }
}
