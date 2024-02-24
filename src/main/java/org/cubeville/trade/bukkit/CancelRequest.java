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

package org.cubeville.trade.bukkit;

import org.jetbrains.annotations.NotNull;

final class CancelRequest {
    
    private final String type;
    private final int taskId;
    
    CancelRequest(@NotNull final String type, final int taskId) {
        this.type = type;
        this.taskId = taskId;
    }
    
    @NotNull
    String getType() {
        return this.type;
    }
    
    int getTaskId() {
        return this.taskId;
    }
}
