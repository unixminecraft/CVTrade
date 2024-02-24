/* 
 * This file is part of CVTrade.
 * 
 * CVTrade Bukkit plugin for Minecraft Bukkit servers.
 * 
 * Copyright (C) 2021-2024 Matt Ciolkosz (https://github.com/mciolkosz/)
 * Copyright (C) 2021-2024 Cubeville (https://www.cubeville.org/)
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

import java.util.UUID;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Trader {
    
    private final UUID uniqueId;
    private final String name;
    
    private long logoutTime;
    
    public Trader(@NotNull final Player player) {
        this(player.getUniqueId(), player.getName(), 0L);
    }
    
    public Trader(@NotNull final UUID uniqueId, @NotNull final String name, final long logoutTime) {
        this.uniqueId = uniqueId;
        this.name = name;
        this.logoutTime = logoutTime;
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
    
    public void setLogoutTime(final long logoutTime) {
        this.logoutTime = Math.max(logoutTime, 0L);
    }
    
    public boolean isOffline() {
        return this.logoutTime != 0L;
    }
    
    public void setOffline(final boolean offline) {
        this.setLogoutTime(offline ? System.currentTimeMillis() : 0L);
    }
    
    @Override
    public boolean equals(@Nullable final Object object) {
        
        if (object == this) {
            return true;
        }
        
        if (object == null) {
            return false;
        }
        if (!(object instanceof Trader)) {
            return false;
        }
        
        final Trader other = (Trader) object;
        
        if (!this.getUniqueId().equals(other.getUniqueId())) {
            return false;
        }
        if (!this.getName().equalsIgnoreCase(other.getName())) {
            return false;
        }
        return this.getLogoutTime() == other.getLogoutTime();
    }
}
