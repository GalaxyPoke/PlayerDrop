package com.playerguard.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemUtils {

    public static String getItemName(ItemStack item) {
        if (item == null) {
            return "未知";
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return meta.getDisplayName();
        }
        
        // 格式化材料名称: DIAMOND_SWORD -> Diamond Sword
        String materialName = item.getType().name();
        String[] parts = materialName.split("_");
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(" ");
            sb.append(parts[i].charAt(0));
            sb.append(parts[i].substring(1).toLowerCase());
        }
        
        int amount = item.getAmount();
        if (amount > 1) {
            return sb.toString() + " x" + amount;
        }
        
        return sb.toString();
    }
}
