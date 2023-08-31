/* 
 * This file is part of CVTrade.
 * 
 * CVTrade Bukkit plugin for Minecraft Bukkit servers.
 * 
 * Copyright (C) 2021-2023 Matt Ciolkosz (https://github.com/mciolkosz/)
 * Copyright (C) 2021-2023 Cubeville (https://www.cubeville.org/)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cubeville.trade.bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class OfflineTrader {
    
    private static final String KEY_UUID = "uuid";
    private static final String KEY_NAME = "name";
    private static final String KEY_LOGOUT_TIME = "logout_time";
    private static final String KEY_COMPLETE_REASON = "complete_reason";
    private static final String KEY_ITEMS = "items";
    
    enum CompleteReason {
        OFFLINE_SELF("§cYour trade has been automatically cancelled because you were offline for too long."),
        OFFLINE_OTHER("§cYour trade has been automatically cancelled because the other player was offline for too long."),
        CANCELLED("§cYour trade has been cancelled because the other player cancelled the trade while you were offline."),
        REJECTED("§cYour trade has been cancelled because the other player rejected your trade offer while you were offline."),
        ACCEPTED("§aYour trade has been finished because the other player accepted your trade offer while you were offline."),
        ERROR("§cYour trade has been cancelled because there was a system error. Please report this to a server administrator.");
        
        private final String message;
        
        CompleteReason(@NotNull final String message) {
            this.message = message;
        }
        
        @NotNull
        String getMessage() {
            return this.message;
        }
    }
    
    private final UUID uniqueId;
    private final String name;
    private final long logoutTime;
    
    private CompleteReason completeReason;
    private Inventory inventory;
    
    OfflineTrader(@NotNull final Player player) {
        this(player.getUniqueId(), player.getName(), System.currentTimeMillis());
    }
    
    OfflineTrader(@NotNull final UUID uniqueId, @NotNull final String name, final long logoutTime) {
        
        this.uniqueId = uniqueId;
        this.name = name;
        this.logoutTime = logoutTime;
        
        this.completeReason = null;
        this.inventory = null;
    }
    
    OfflineTrader(@NotNull final Server server, @NotNull final Configuration config) throws IllegalArgumentException {
        
        final String uuid = config.getString(KEY_UUID, null);
        if (uuid == null) {
            throw new IllegalArgumentException("Cannot have null UUID for offline trader.");
        }
        final UUID uniqueId;
        try {
            uniqueId = UUID.fromString(uuid);
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException("Unable to parse UUID " + uuid + " for offline trader.", e);
        }
        
        final String name = config.getString(KEY_NAME, null);
        if (name == null) {
            throw new IllegalArgumentException("Cannot have null name for offline trader.");
        } else if (name.trim().isBlank()) {
            throw new IllegalArgumentException("Cannot have blank name for offline trader.");
        }
        
        final long logoutTime = config.getLong(KEY_LOGOUT_TIME, -1L);
        if (logoutTime <= 0L) {
            throw new IllegalArgumentException("Cannot have invalid logout time for offline trader.");
        }
        
        final String reasonName = config.getString(KEY_COMPLETE_REASON, null);
        if (reasonName != null && reasonName.trim().isEmpty()) {
            throw new IllegalArgumentException("Cannot have blank complete reason for offline trade.");
        }
        final CompleteReason completeReason;
        if (reasonName == null) {
            completeReason = null;
        } else {
            try {
                completeReason = CompleteReason.valueOf(reasonName.trim().toUpperCase());
            } catch (final IllegalArgumentException e) {
                throw new IllegalArgumentException("Complete reason " + reasonName.trim() + " is not a valid complete reason for offline trade.", e);
            }
        }
        
        final List<?> rawItems = config.getList(KEY_ITEMS, null);
        final Inventory inventory;
        if (rawItems == null) {
            inventory = null;
        } else {
            inventory = server.createInventory(null, 27);
            final List<Map<String, Object>> items = (List<Map<String, Object>>) rawItems;
            
            for (int index = 0; index < items.size() && index < 27; index++) {
                final Map<String, Object> item = items.get(index);
                ItemStack itemStack = null;
                if (item != null) {
                    itemStack = ItemStack.deserialize(item);
                }
                inventory.setItem(index, itemStack);
            }
        }
        
        this.uniqueId = uniqueId;
        this.name = name;
        this.logoutTime = logoutTime;
        this.completeReason = completeReason;
        this.inventory = inventory;
    }
    
    @NotNull
    UUID getUniqueId() {
        return this.uniqueId;
    }
    
    @NotNull
    String getName() {
        return this.name;
    }
    
    long getLogoutTime() {
        return this.logoutTime;
    }
    
    @Nullable
    CompleteReason getCompleteReason() {
        return this.completeReason;
    }
    
    void setCompleteReason(@Nullable final CompleteReason completeReason) {
        this.completeReason = completeReason;
    }
    
    @Nullable
    Inventory getInventory() {
        return this.inventory;
    }
    
    void setInventory(@Nullable final Inventory inventory) {
        
        if (inventory == null) {
            this.inventory = null;
            return;
        }
        
        this.inventory = Bukkit.getServer().createInventory(null, 27);
        
        final ItemStack[] items = inventory.getStorageContents();
        for (int slot = 0; slot < items.length; slot++) {
            this.inventory.setItem(slot, items[slot]);
        }
    }
    
    @NotNull
    FileConfiguration getConfig() {
        
        final FileConfiguration config = new YamlConfiguration();
        
        config.set(KEY_UUID, this.getUniqueId().toString());
        config.set(KEY_NAME, this.getName());
        config.set(KEY_LOGOUT_TIME, this.getLogoutTime());
        
        if (this.getCompleteReason() != null) {
            config.set(KEY_COMPLETE_REASON, this.getCompleteReason().name());
        }
        if (this.getInventory() != null) {
            final List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
            for (final ItemStack item : this.getInventory().getStorageContents()) {
                items.add(item == null ? null : item.serialize());
            }
            config.set(KEY_ITEMS, items);
        }
        
        return config;
    }
}
