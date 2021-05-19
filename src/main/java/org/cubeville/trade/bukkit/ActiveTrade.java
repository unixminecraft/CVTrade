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
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

final class ActiveTrade {
    
    enum TradeStatus {
        PREPARE,
        READY,
        DECIDE,
        ACCEPT;
    }
    
    private final UUID uniqueId;
    private final String name;
    private final TradeChest tradeChest;
    
    private TradeStatus tradeStatus;
    
    ActiveTrade(@NotNull final Player player, @NotNull final TradeChest tradeChest) {
        this(player.getUniqueId(), player.getName(), tradeChest, TradeStatus.PREPARE);
    }
    
    ActiveTrade(@NotNull final UUID uniqueId, @NotNull final String name, @NotNull final TradeChest tradeChest, @NotNull final TradeStatus tradeStatus) {
        this.uniqueId = uniqueId;
        this.name = name;
        this.tradeChest = tradeChest;
        this.tradeStatus = tradeStatus;
    }
    
    @NotNull
    UUID getUniqueId() {
        return this.uniqueId;
    }
    
    @NotNull
    String getName() {
        return this.name;
    }
    
    @NotNull
    TradeChest getTradeChest() {
        return this.tradeChest;
    }
    
    @NotNull
    TradeStatus getTradeStatus() {
        return this.tradeStatus;
    }
    
    void setTradeStatus(@NotNull final TradeStatus tradeStatus) {
        this.tradeStatus = tradeStatus;
    }
}
