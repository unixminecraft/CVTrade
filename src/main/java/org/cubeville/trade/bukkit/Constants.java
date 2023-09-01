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

public final class Constants {
    
    public static final String FILE_TYPE = ".yml";
    
    public static final String FILE_SERVER_STOP = "server_stop" + FILE_TYPE;
    public static final String KEY_SERVER_STOP_TIME = "server_stop_time";
    
    public static final String FOLDER_TRADE_ROOMS = "trade_rooms";
    
    public static final String FOLDER_BACKUP_INVENTORIES = "backup_inventories";
    public static final String KEY_TRADE_ROOM_NAME = "trade_room_name";
    public static final String KEY_BACKUP_ITEMS_1 = "backup_items_1";
    public static final String KEY_BACKUP_ITEMS_2 = "backup_items_2";
    
    public static final String FOLDER_OFFLINE_TRADERS = "offline_traders";
    
    public static final String FOLDER_OFFLINE_EXTRAS = "offline_extras";
    public static final String KEY_EXTRA_UUID = "extra_uuid";
    public static final String KEY_EXTRA_ITEMS = "extra_items";
    
    private Constants() {
        // Do nothing.
    }
}
