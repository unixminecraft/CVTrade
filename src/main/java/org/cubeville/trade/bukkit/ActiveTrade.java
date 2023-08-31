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

import java.util.UUID;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.cubeville.trade.bukkit.traderoom.Side;
import org.cubeville.trade.bukkit.traderoom.TradeRoom;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class ActiveTrade {
    
    private static final String KEY_UUID_1 = "uuid_1";
    private static final String KEY_UUID_2 = "uuid_2";
    private static final String KEY_NAME_1 = "name_1";
    private static final String KEY_NAME_2 = "name_2";
    private static final String KEY_ROOM = "room";
    private static final String KEY_STATUS = "status";
    
    enum TradeStatus {
        PREPARE,
        LOCKED_1,
        LOCKED_2,
        DECIDE,
        ACCEPT_1,
        ACCEPT_2;
    }
    
    private UUID uniqueId1;
    private UUID uniqueId2;
    private String name1;
    private String name2;
    private final TradeRoom room;
    
    private TradeStatus status;
    
    ActiveTrade(@NotNull final Player player, @NotNull final TradeRoom room, @NotNull final Side side) {
        
        if (side == Side.SIDE_1) {
            this.uniqueId1 = player.getUniqueId();
            this.name1 = player.getName();
        } else {
            this.uniqueId2 = player.getUniqueId();
            this.name2 = player.getName();
        }
        this.room = room;
        this.status = TradeStatus.PREPARE;
    }
    
    ActiveTrade(@NotNull final TradePlugin plugin, @NotNull final Configuration config) throws IllegalArgumentException {
        
        final String uuid1 = config.getString(KEY_UUID_1, null);
        final UUID uniqueId1;
        if (uuid1 == null) {
            uniqueId1 = null;
        } else {
            try {
                uniqueId1 = UUID.fromString(uuid1);
            } catch (final IllegalArgumentException e) {
                throw new IllegalArgumentException("Cannot parse UUID 1 " + uuid1 + " for active trade.", e);
            }
        }
        
        final String uuid2 = config.getString(KEY_UUID_2, null);
        final UUID uniqueId2;
        if (uuid2 == null) {
            uniqueId2 = null;
        } else {
            try {
                uniqueId2 = UUID.fromString(uuid2);
            } catch (final IllegalArgumentException e) {
                throw new IllegalArgumentException("Cannot parse UUID 2 " + uuid2 + " for active trade.", e);
            }
        }
        
        if (uniqueId1 == null && uniqueId2 == null) {
            throw new IllegalArgumentException("Both UUIDs cannot be null for active trade.");
        }
        
        final String name1 = config.getString(KEY_NAME_1, null);
        if (name1 != null && name1.trim().isBlank()) {
            throw new IllegalArgumentException("Cannot have blank name 1 for active trade.");
        }
        
        final String name2 = config.getString(KEY_NAME_2, null);
        if (name2 != null && name2.trim().isBlank()) {
            throw new IllegalArgumentException("Cannot have blank name 2 for active trade.");
        }
        
        if (name1 == null && name2 == null) {
            throw new IllegalArgumentException("Both names cannot be blank for active trade.");
        }
        
        final String roomName = config.getString(KEY_ROOM, null);
        if (roomName == null) {
            throw new IllegalArgumentException("Cannot have null trade room for active trade.");
        } else if (roomName.trim().isEmpty()) {
            throw new IllegalArgumentException("Cannot have empty trade room name for active trade.");
        }
        final TradeRoom room = plugin.getTradeRoom(roomName.trim());
        if (room == null) {
            throw new IllegalArgumentException("Trade room " + roomName.trim() + " does not exist for active trade.");
        }
        
        final String statusName = config.getString(KEY_STATUS, null);
        if (statusName == null) {
            throw new IllegalArgumentException("Cannot have null trade status for active trade.");
        } else if (statusName.trim().isEmpty()) {
            throw new IllegalArgumentException("Cannot have empty trade status name for active trade.");
        }
        final TradeStatus status;
        try {
            status = TradeStatus.valueOf(statusName.trim().toUpperCase());
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException("Trade status " + statusName.trim() + " is not a valid trade status for active trade.", e);
        }
        
        this.uniqueId1 = uniqueId1;
        this.uniqueId2 = uniqueId2;
        this.name1 = name1 == null ? null : name1.trim();
        this.name2 = name2 == null ? null : name2.trim();
        this.room = room;
        this.status = status;
    }
    
    @Nullable
    UUID getUniqueId1() {
        return this.uniqueId1;
    }
    
    @Nullable
    UUID getUniqueId2() {
        return this.uniqueId2;
    }
    
    @Nullable
    String getName1() {
        return this.name1;
    }
    
    @Nullable
    String getName2() {
        return this.name2;
    }
    
    @NotNull
    TradeRoom getRoom() {
        return this.room;
    }
    
    @NotNull
    TradeStatus getStatus() {
        return this.status;
    }
    
    void setStatus(@NotNull final TradeStatus status) {
        this.status = status;
    }
    
    boolean isTrading(@NotNull final UUID uniqueId) {
        return uniqueId.equals(this.getUniqueId1()) || uniqueId.equals(this.getUniqueId2());
    }
}
