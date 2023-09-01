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

import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Directional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class Button {
    
    private final Location location;
    private final Location attachedBlock;
    
    Button(@NotNull final BlockState state) {
        this.location = state.getLocation();
        this.attachedBlock = state.getBlock().getRelative(((Directional) state.getBlockData()).getFacing().getOppositeFace()).getLocation();
    }
    
    @NotNull
    Location getLocation() {
        return this.location;
    }
    
    boolean contains(@NotNull final Location location, final boolean exact) {
        if (exact) {
            return this.location.equals(location);
        } else {
            return this.location.equals(location) || this.attachedBlock.equals(location);
        }
    }
    
    @Override
    public boolean equals(@Nullable final Object object) {
        
        if (object == this) {
            return true;
        }
        
        if (object == null) {
            return false;
        }
        if (!(object instanceof Button)) {
            return false;
        }
        
        final Button other = (Button) object;
        return this.getLocation().equals(other.getLocation()) && this.attachedBlock.equals(other.attachedBlock);
    }
}
