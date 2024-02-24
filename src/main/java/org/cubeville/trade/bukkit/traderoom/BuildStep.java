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

import java.util.Map;
import java.util.TreeMap;
import org.jetbrains.annotations.NotNull;

public enum BuildStep {
    
    NAME,
    REGIONS,
    CHEST_1,
    TELEPORT_IN_1,
    TELEPORT_OUT_1,
    BUTTON_IN_1,
    BUTTON_OUT_1,
    BUTTON_LOCK_1,
    BUTTON_ACCEPT_1,
    BUTTON_DENY_1,
    CHEST_2,
    TELEPORT_IN_2,
    TELEPORT_OUT_2,
    BUTTON_IN_2,
    BUTTON_OUT_2,
    BUTTON_LOCK_2,
    BUTTON_ACCEPT_2,
    BUTTON_DENY_2,
    COMPLETED;
    
    private static final Map<Integer, BuildStep> ORDERED = new TreeMap<Integer, BuildStep>();
    
    static {
        for (final BuildStep step : values()) {
            ORDERED.put(step.ordinal(), step);
        }
    }
    
    @NotNull
    public BuildStep next() {
        
        if (this == COMPLETED) {
            return this;
        }
        return ORDERED.get(this.ordinal() + 1);
    }
}
