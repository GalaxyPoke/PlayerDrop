package com.playerguard.data;

import com.github.retrooper.packetevents.util.Vector3d;
import org.bukkit.boss.BossBar;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

public class PendingDrop {

    private final int entityId;
    private final UUID entityUuid;
    private final ItemStack item;
    private Vector3d position;
    private final long createTime;
    private BukkitTask timeoutTask;
    private BukkitTask animationTask;
    private BossBar bossBar;

    public PendingDrop(int entityId, UUID entityUuid, ItemStack item, Vector3d position, long createTime) {
        this.entityId = entityId;
        this.entityUuid = entityUuid;
        this.item = item.clone();
        this.position = position;
        this.createTime = createTime;
    }

    public int getEntityId() {
        return entityId;
    }

    public UUID getEntityUuid() {
        return entityUuid;
    }

    public ItemStack getItem() {
        return item.clone();
    }

    public Vector3d getPosition() {
        return position;
    }

    public void setPosition(Vector3d position) {
        this.position = position;
    }

    public long getCreateTime() {
        return createTime;
    }

    public BukkitTask getTimeoutTask() {
        return timeoutTask;
    }

    public void setTimeoutTask(BukkitTask timeoutTask) {
        this.timeoutTask = timeoutTask;
    }

    public BukkitTask getAnimationTask() {
        return animationTask;
    }

    public void setAnimationTask(BukkitTask animationTask) {
        this.animationTask = animationTask;
    }
    
    public BossBar getBossBar() {
        return bossBar;
    }
    
    public void setBossBar(BossBar bossBar) {
        this.bossBar = bossBar;
    }
}
