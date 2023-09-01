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

import org.jetbrains.annotations.NotNull;

public enum CompleteReason {
    
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
    public String getMessage() {
        return this.message;
    }
}
