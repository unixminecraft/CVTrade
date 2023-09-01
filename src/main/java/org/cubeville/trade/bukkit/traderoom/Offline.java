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

package org.cubeville.trade.bukkit.traderoom;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Offline {
    
    private static final String KEY_UUID = "uuid";
    private static final String KEY_NAME = "name";
    private static final String KEY_LOGOUT_TIME = "logout_time";
    private static final String KEY_COMPLETE_REASON = "complete_reason";
    private static final String KEY_ITEMS = "items";
    private static final String KEY_TELEPORT = "teleport";
    
    private final UUID uniqueId;
    private final String name;
    private final long logoutTime;
    
    private CompleteReason reason;
    private Inventory inventory;
    private Location teleport;
    
    public Offline(@NotNull final Trader trader) {
        
        this.uniqueId = trader.getUniqueId();
        this.name = trader.getName();
        this.logoutTime = Math.max(trader.getLogoutTime(), 0L);
        
        this.reason = null;
        this.inventory = null;
        this.teleport = null;
    }
    
    public Offline(@NotNull final ConfigurationSection config) throws IllegalArgumentException {
        
        final String uuid = config.getString(KEY_UUID, null);
        if (uuid == null) {
            throw new IllegalArgumentException("Cannot have null UUID for trader.");
        }
        final UUID uniqueId;
        try {
            uniqueId = UUID.fromString(uuid);
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException("Unable to parse UUID " + uuid + " for trader.", e);
        }
        
        final String name = config.getString(KEY_NAME, null);
        if (name == null) {
            throw new IllegalArgumentException("Cannot have null name for trader.");
        } else if (name.trim().isBlank()) {
            throw new IllegalArgumentException("Cannot have blank name for trader.");
        }
        
        final long logoutTime = config.getLong(KEY_LOGOUT_TIME, -1L);
        if (logoutTime <= 0L) {
            throw new IllegalArgumentException("Cannot have invalid logout time for trader.");
        }
        
        final String reasonName = config.getString(KEY_COMPLETE_REASON, null);
        if (reasonName != null && reasonName.trim().isEmpty()) {
            throw new IllegalArgumentException("Cannot have blank complete reason for trade.");
        }
        final CompleteReason reason;
        if (reasonName == null) {
            reason = null;
        } else {
            try {
                reason = CompleteReason.valueOf(reasonName.trim().toUpperCase());
            } catch (final IllegalArgumentException e) {
                throw new IllegalArgumentException("Complete reason " + reasonName.trim() + " is not a valid complete reason for trade.", e);
            }
        }
        
        final List<?> rawItems = config.getList(KEY_ITEMS, null);
        final Inventory inventory;
        if (rawItems == null) {
            inventory = null;
        } else {
            inventory = Bukkit.getServer().createInventory(null, 27);
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
        
        final Location teleport = config.getLocation(KEY_TELEPORT, null);
        
        this.uniqueId = uniqueId;
        this.name = name;
        
        this.logoutTime = logoutTime;
        this.reason = reason;
        this.inventory = inventory;
        this.teleport = teleport;
    }
    
    @NotNull
    public UUID getUniqueId() {
        return this.uniqueId;
    }
    
    @NotNull
    public String getName() {
        return this.name;
    }
    
    public long getLogoutTime() {
        return this.logoutTime;
    }
    
    @Nullable
    public CompleteReason getReason() {
        return this.reason;
    }
    
    public void setReason(@Nullable final CompleteReason reason) {
        this.reason = reason;
    }
    
    @Nullable
    public Inventory getInventory() {
        return this.inventory;
    }
    
    public void setInventory(@Nullable final Inventory inventory) {
        
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
    
    @Nullable
    public Location getTeleport() {
        return this.teleport;
    }
    
    public void setTeleport(@Nullable final Location teleport) {
        this.teleport = teleport;
    }
    
    @NotNull
    public FileConfiguration getConfig() {
        
        final FileConfiguration config = new YamlConfiguration();
        
        config.set(KEY_UUID, this.getUniqueId().toString());
        config.set(KEY_NAME, this.getName());
        config.set(KEY_LOGOUT_TIME, this.getLogoutTime());
        
        if (this.getReason() != null) {
            config.set(KEY_COMPLETE_REASON, this.getReason().name());
        }
        if (this.getInventory() != null) {
            final List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
            for (final ItemStack item : this.getInventory().getStorageContents()) {
                items.add(item == null ? null : item.serialize());
            }
            config.set(KEY_ITEMS, items);
        }
        config.set(KEY_TELEPORT, this.getTeleport());
        
        return config;
    }
    
    @Override
    public boolean equals(@Nullable final Object object) {
        
        if (object == this) {
            return true;
        }
        
        if (object == null) {
            return false;
        }
        if (!(object instanceof Offline)) {
            return false;
        }
        
        final Offline other = (Offline) object;
        
        if (!this.getUniqueId().equals(other.getUniqueId())) {
            return false;
        }
        if (!this.getName().equalsIgnoreCase(other.getName())) {
            return false;
        }
        return this.getLogoutTime() == other.getLogoutTime();
    }
}
