////////////////////////////////////////////////////////////////////////////////
// This file is part of CVTrade.                                              //
//                                                                            //
// CVTrade Bukkit plugin for Minecraft Bukkit servers.                        //
//                                                                            //
// Copyright (C) 2021 Matt Ciolkosz (https://github.com/mciolkosz/)           //
// Copyright (C) 2021 Cubeville (https://www.cubeville.org/)                  //
//                                                                            //
// This program is free software: you can redistribute it and/or modify       //
// it under the terms of the GNU General Public License as published by       //
// the Free Software Foundation, either version 3 of the License, or          //
// (at your option) any later version.                                        //
//                                                                            //
// This program is distributed in the hope that it will be useful,            //
// but WITHOUT ANY WARRANTY; without even the implied warranty of             //
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              //
// GNU General Public License for more details.                               //
//                                                                            //
// You should have received a copy of the GNU General Public License          //
// along with this program.  If not, see <http://www.gnu.org/licenses/>.      //
////////////////////////////////////////////////////////////////////////////////

package org.cubeville.trade.bukkit;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class OfflineTrader {
    
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
}
