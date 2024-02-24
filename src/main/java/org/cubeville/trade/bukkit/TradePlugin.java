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

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;
import org.cubeville.trade.bukkit.command.TradeAdminCommand;
import org.cubeville.trade.bukkit.listener.TradeListener;
import org.cubeville.trade.bukkit.traderoom.BuildStep;
import org.cubeville.trade.bukkit.traderoom.CompleteReason;
import org.cubeville.trade.bukkit.traderoom.Offline;
import org.cubeville.trade.bukkit.traderoom.Side;
import org.cubeville.trade.bukkit.traderoom.TradeRoom;
import org.cubeville.trade.bukkit.traderoom.TradeRoomBuilder;
import org.cubeville.trade.bukkit.traderoom.TradeStatus;
import org.cubeville.trade.bukkit.traderoom.Trader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TradePlugin extends JavaPlugin {
    
    public static final int SLOT_REJECT = 36;
    public static final int SLOT_ACCEPT = 44;
    
    private static final long OFFLINE_TIMEOUT = 1000L * 60L * 2L;
    
    private final Logger logger;
    private final Server server;
    private final BukkitScheduler scheduler;
    private final CommandSender console;
    
    private final File serverStopFile;
    
    private final File tradeRoomFolder;
    private final Map<UUID, TradeRoomBuilder> builders;
    private final Map<String, TradeRoom> tradeRooms;
    private final Map<UUID, CancelRequest> cancelRequests;
    
    private final Set<UUID> recentJoins;
    private final Set<UUID> ignoredJoins;
    
    private final File backupInventoryFolder;
    private final Map<UUID, Inventory> tradeInventories;
    
    private final File offlineFolder;
    private final Map<UUID, Offline> offlines;
    
    private final File extraFolder;
    private final Map<UUID, List<ItemStack>> extras;
    
    public TradePlugin() {
        super();
        
        this.logger = this.getLogger();
        this.server = this.getServer();
        this.scheduler = this.server.getScheduler();
        this.console = this.server.getConsoleSender();
        
        final File dataFolder = this.getDataFolder();
        try {
            if (!dataFolder.exists()) {
                if (!dataFolder.mkdirs()) {
                    throw new RuntimeException("Plugin data folder not created at " + dataFolder.getPath());
                }
            } else if (!dataFolder.isDirectory()) {
                throw new RuntimeException("Plugin data folder is not a directory. Location: " + dataFolder.getPath());
            }
        } catch (SecurityException e) {
            throw new RuntimeException("Unable to validate Plugin data folder at " + dataFolder.getPath(), e);
        }
        
        this.serverStopFile = new File(dataFolder, Constants.FILE_SERVER_STOP);
        try {
            if (!this.serverStopFile.exists()) {
                if (!this.serverStopFile.createNewFile()) {
                    throw new RuntimeException("Server Stop file not created at " + this.serverStopFile.getPath());
                }
            } else if (!this.serverStopFile.isFile()) {
                throw new RuntimeException("Server Stop file is not a file. Location: " + this.serverStopFile.getPath());
            }
        } catch (SecurityException | IOException e) {
            throw new RuntimeException("Unable to validate Server Stop file at " + this.serverStopFile.getPath(), e);
        }
        
        this.tradeRoomFolder = new File(dataFolder, Constants.FOLDER_TRADE_ROOMS);
        this.builders = new ConcurrentHashMap<UUID, TradeRoomBuilder>();
        this.tradeRooms = new ConcurrentHashMap<String, TradeRoom>();
        this.cancelRequests = new ConcurrentHashMap<UUID, CancelRequest>();
        
        this.recentJoins = new HashSet<UUID>();
        this.ignoredJoins = new HashSet<UUID>();
        
        if (!this.tradeRoomFolder.exists()) {
            if (!this.tradeRoomFolder.mkdirs()) {
                throw new RuntimeException("TradeChest folder not created at " + this.tradeRoomFolder.getPath());
            }
        } else if (!this.tradeRoomFolder.isDirectory()) {
            throw new RuntimeException("TradeChest folder is not a folder. Location: " + this.tradeRoomFolder.getPath());
        }
        
        this.backupInventoryFolder = new File(dataFolder, Constants.FOLDER_BACKUP_INVENTORIES);
        this.tradeInventories = new ConcurrentHashMap<UUID, Inventory>();
        if (!this.backupInventoryFolder.exists()) {
            if (!this.backupInventoryFolder.mkdirs()) {
                throw new RuntimeException("BackupInventory folder not created at " + this.backupInventoryFolder.getPath());
            }
        } else if (!this.backupInventoryFolder.isDirectory()) {
            throw new RuntimeException("BackupInventory folder is not a folder. Location: " + this.backupInventoryFolder.getPath());
        }
        
        this.offlineFolder = new File(dataFolder, Constants.FOLDER_OFFLINE_TRADERS);
        this.offlines = new ConcurrentHashMap<UUID, Offline>();
        if (!this.offlineFolder.exists()) {
            if (!this.offlineFolder.mkdirs()) {
                throw new RuntimeException("OfflineTrader folder not created at " + this.offlineFolder.getPath());
            }
        } else if (!this.offlineFolder.isDirectory()) {
            throw new RuntimeException("OfflineTrader folder is not a folder. Location: " + this.offlineFolder.getPath());
        }
        
        this.extraFolder = new File(dataFolder, Constants.FOLDER_OFFLINE_EXTRAS);
        this.extras = new ConcurrentHashMap<UUID, List<ItemStack>>();
        if (!this.extraFolder.exists()) {
            if (!this.extraFolder.mkdirs()) {
                throw new RuntimeException("ExtraInventory folder not created at " + this.extraFolder.getPath());
            }
        } else if (!this.extraFolder.isDirectory()) {
            throw new RuntimeException("ExtraInventory folder is not a folder. Location: " + this.extraFolder.getPath());
        }
        
    }
    
    @Override
    public void onEnable() {
        
        // Basic Plugin Startup //
        this.logger.log(Level.INFO, "////////////////////////////////////////////////////////////////////////////////");
        this.logger.log(Level.INFO, "// CVTrade Bukkit plugin for Minecraft Bukkit servers.                        //");
        this.logger.log(Level.INFO, "//                                                                            //");
        this.logger.log(Level.INFO, "// Copyright (C) 2021-2024 Matt Ciolkosz (https://github.com/mciolkosz/)      //");
        this.logger.log(Level.INFO, "// Copyright (C) 2021-2024 Cubeville (https://www.cubeville.org/)             //");
        this.logger.log(Level.INFO, "//                                                                            //");
        this.logger.log(Level.INFO, "// This program is free software: you can redistribute it and/or modify       //");
        this.logger.log(Level.INFO, "// it under the terms of the GNU General Public License as published by       //");
        this.logger.log(Level.INFO, "// the Free Software Foundation, either version 3 of the License, or          //");
        this.logger.log(Level.INFO, "// (at your option) any later version.                                        //");
        this.logger.log(Level.INFO, "//                                                                            //");
        this.logger.log(Level.INFO, "// This program is distributed in the hope that it will be useful,            //");
        this.logger.log(Level.INFO, "// but WITHOUT ANY WARRANTY; without even the implied warranty of             //");
        this.logger.log(Level.INFO, "// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              //");
        this.logger.log(Level.INFO, "// GNU General Public License for more details.                               //");
        this.logger.log(Level.INFO, "//                                                                            //");
        this.logger.log(Level.INFO, "// You should have received a copy of the GNU General Public License          //");
        this.logger.log(Level.INFO, "// along with this program.  If not, see <http://www.gnu.org/licenses/>.      //");
        this.logger.log(Level.INFO, "////////////////////////////////////////////////////////////////////////////////");
        
        // Server statistics //
        
        final long now = System.currentTimeMillis();
        
        final YamlConfiguration stopConfig = new YamlConfiguration();
        try {
            stopConfig.load(this.serverStopFile);
        } catch (final IOException | InvalidConfigurationException | IllegalArgumentException e) {
            throw new RuntimeException("Unable to load server stop file at " + this.serverStopFile.getPath(), e);
        }
        
        final long serverStop = stopConfig.getLong(Constants.KEY_SERVER_STOP_TIME, -1L);
        final long offlineTime;
        if (serverStop == -1L) {
            offlineTime = now - OFFLINE_TIMEOUT;
        } else {
            offlineTime = now - serverStop;
        }
        
        // TradeRoom Initialization //
        // Load in the TradeRooms
        
        final File[] tradeRoomFiles = this.tradeRoomFolder.listFiles();
        if (tradeRoomFiles == null) {
            throw new RuntimeException("Cannot list trade room files, null value returned.");
        }
        
        for (final File tradeRoomFile : tradeRoomFiles) {
            
            final YamlConfiguration config = new YamlConfiguration();
            try {
                config.load(tradeRoomFile);
            } catch (final IOException | InvalidConfigurationException | IllegalArgumentException e) {
                this.logger.log(Level.WARNING, "Unable to load trade room file at " + tradeRoomFile.getPath());
                this.logger.log(Level.WARNING, "Skipping trade room.");
                this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                continue;
            }
            
            final TradeRoom room;
            try {
                room = new TradeRoom(this.server, offlineTime, config);
            } catch (final IllegalArgumentException e) {
                this.logger.log(Level.WARNING, "Unable to deserialize trade room from file at " + tradeRoomFile.getPath());
                this.logger.log(Level.WARNING, "Skipping trade room.");
                this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                continue;
            }
            
            if (this.tradeRooms.containsKey(room.getName().toLowerCase())) {
                this.logger.log(Level.WARNING, "Conflicting name of trade room. Duplicate already registered.");
                this.logger.log(Level.WARNING, "Please remember that names are case-insensitive. Name: " + room.getName());
                this.logger.log(Level.WARNING, "Skipping trade room.");
                continue;
            }
            
            this.tradeRooms.put(room.getName().toLowerCase(), room);
        }
        
        // Backup Inventories
        final File[] backupInventoryFiles = this.backupInventoryFolder.listFiles();
        if (backupInventoryFiles == null) {
            throw new RuntimeException("Cannot list backup inventory files, null value returned.");
        }
        for (final File backupInventoryFile : backupInventoryFiles) {
            
            final YamlConfiguration config = new YamlConfiguration();
            try {
                config.load(backupInventoryFile);
            } catch (final IOException | InvalidConfigurationException | IllegalArgumentException e) {
                this.logger.log(Level.WARNING, "Unable to load backup inventory file at " + backupInventoryFile.getPath());
                this.logger.log(Level.WARNING, "Skipping backup inventory.");
                this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                continue;
            }
            
            final String roomName = config.getString(Constants.KEY_TRADE_ROOM_NAME, null);
            if (roomName == null) {
                this.logger.log(Level.WARNING, "Trade room name is null in backup inventory file at " + backupInventoryFile.getPath());
                this.logger.log(Level.WARNING, "Skipping backup inventory.");
                continue;
            }
            
            final TradeRoom room = this.tradeRooms.get(roomName);
            if (room == null) {
                this.logger.log(Level.WARNING, "Trade room name is invalid in backup inventory file at " + backupInventoryFile.getPath());
                this.logger.log(Level.WARNING, "No trade room for name " + roomName);
                this.logger.log(Level.WARNING, "Skipping backup inventory.");
                continue;
            }
            
            final List<?> rawItems1 = config.getList(Constants.KEY_BACKUP_ITEMS_1, null);
            if (rawItems1 == null) {
                this.logger.log(Level.WARNING, "Backup items for chest 1 are null in backup inventory file at " + backupInventoryFile.getPath());
                this.logger.log(Level.WARNING, "Skipping backup inventory.");
                continue;
            }
            
            final List<?> rawItems2 = config.getList(Constants.KEY_BACKUP_ITEMS_2, null);
            if (rawItems2 == null) {
                this.logger.log(Level.WARNING, "Backup items for chest 2 are null in backup inventory file at " + backupInventoryFile.getPath());
                this.logger.log(Level.WARNING, "Skipping backup inventory.");
                continue;
            }
            
            final Inventory backupInventory1 = this.server.createInventory(null, 27);
            final Inventory backupInventory2 = this.server.createInventory(null, 27);
            
            final List<Map<String, Object>> items1 = (List<Map<String, Object>>) rawItems1;
            final List<Map<String, Object>> items2 = (List<Map<String, Object>>) rawItems2;
            
            int slot = 0;
            for (final Map<String, Object> item : items1) {
                backupInventory1.setItem(slot, item == null ? null : ItemStack.deserialize(item));
                slot++;
            }
            slot = 0;
            for (final Map<String, Object> item : items2) {
                backupInventory2.setItem(slot, item == null ? null : ItemStack.deserialize(item));
                slot++;
            }
            
            final ItemStack[] backupItems1 = backupInventory1.getStorageContents();
            final ItemStack[] backupItems2 = backupInventory2.getStorageContents();
            
            final Inventory chest1Inventory = room.getChest1().getInventory();
            final Inventory chest2Inventory = room.getChest2().getInventory();
            
            final ItemStack[] chest1Items = chest1Inventory.getStorageContents();
            final ItemStack[] chest2Items = chest2Inventory.getStorageContents();
            
            for (slot = 0; slot < backupItems1.length; slot++) {
                if (backupItems1[slot] == null && chest1Items[slot] == null) {
                    // Do nothing.
                } else if (backupItems1[slot] == null) {
                    chest1Items[slot] = backupItems1[slot];
                } else if (chest1Items[slot] == null) {
                    chest1Items[slot] = backupItems1[slot];
                } else if (!backupItems1[slot].equals(chest1Items[slot])) {
                    chest1Items[slot] = backupItems1[slot];
                }
            }
            for (slot = 0; slot < backupItems2.length; slot++) {
                if (backupItems2[slot] == null && chest2Items[slot] == null) {
                    // Do nothing.
                } else if (backupItems2[slot] == null) {
                    chest2Items[slot] = backupItems2[slot];
                } else if (chest2Items[slot] == null) {
                    chest2Items[slot] = backupItems2[slot];
                } else if (!backupItems2[slot].equals(chest2Items[slot])) {
                    chest2Items[slot] = backupItems2[slot];
                }
            }
            
            chest1Inventory.setStorageContents(chest1Items);
            chest2Inventory.setStorageContents(chest2Items);
        }
        
        for (final TradeRoom room : this.tradeRooms.values()) {
            
            if (room.getStatus() == null) {
                continue;
            }
            if (room.getStatus().ordinal() < TradeStatus.DECIDE.ordinal()) {
                continue;
            }
            if (!room.hasCompleted()) {
                continue;
            }
            
            final Trader trader1 = room.getTrader1();
            if (trader1 != null) {
                this.tradeInventories.put(trader1.getUniqueId(), room.createTradeInventory(this.server, room.getChest1()));
            }
            
            final Trader trader2 = room.getTrader2();
            if (trader2 != null) {
                this.tradeInventories.put(trader2.getUniqueId(), room.createTradeInventory(this.server, room.getChest2()));
            }
        }
        
        // Offline Players Initialization //
        final File[] offlineFiles = this.offlineFolder.listFiles();
        if (offlineFiles == null) {
            throw new RuntimeException("Cannot list offline trader files, null value returned.");
        }
        for (final File offlineFile : offlineFiles) {
            
            final YamlConfiguration config = new YamlConfiguration();
            try {
                config.load(offlineFile);
            } catch (final IOException | InvalidConfigurationException | IllegalArgumentException e) {
                this.logger.log(Level.WARNING, "Unable to load offline trader file at " + offlineFile.getPath());
                this.logger.log(Level.WARNING, "Skipping offline trader.");
                this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                continue;
            }
            
            final Offline offline;
            try {
                offline = new Offline(offlineTime, config);
            } catch (final IllegalArgumentException e) {
                this.logger.log(Level.WARNING, "Unable to deserialize offline trader from file at " + offlineFile.getPath());
                this.logger.log(Level.WARNING, "Skipping offline trader.");
                this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                continue;
            }
            
            this.offlines.put(offline.getUniqueId(), offline);
        }
        
        // Extra Offline Player Inventory //
        final File[] extraFiles = this.extraFolder.listFiles();
        if (extraFiles == null) {
            throw new RuntimeException("Cannot list extra inventory files, null value returned.");
        }
        for (final File extraFile : extraFiles) {
            
            final YamlConfiguration config = new YamlConfiguration();
            try {
                config.load(extraFile);
            } catch (final IOException | InvalidConfigurationException | IllegalArgumentException e) {
                this.logger.log(Level.WARNING, "Unable to load extra inventory file at " + extraFile.getPath());
                this.logger.log(Level.WARNING, "Skipping extra inventory.");
                this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                continue;
            }
            
            final String uuid = config.getString(Constants.KEY_EXTRA_UUID, null);
            if (uuid == null) {
                throw new IllegalArgumentException("Cannot have null UUID for extra inventory.");
            }
            final UUID uniqueId;
            try {
                uniqueId = UUID.fromString(uuid);
            } catch (final IllegalArgumentException e) {
                throw new IllegalArgumentException("Unable to parse UUID " + uuid + " for extra inventory.", e);
            }
            
            final List<?> rawItems = config.getList(Constants.KEY_EXTRA_ITEMS, null);
            if (rawItems == null) {
                throw new IllegalArgumentException("Cannot have null item list for extra inventory.");
            }
            final List<ItemStack> stacks = new ArrayList<ItemStack>();
            final List<Map<String, Object>> items = (List<Map<String, Object>>) rawItems;
            for (final Map<String, Object> serialized : items) {
                stacks.add(ItemStack.deserialize(serialized));
            }
            
            if (this.extras.containsKey(uniqueId)) {
                this.extras.get(uniqueId).addAll(stacks);
            } else {
                this.extras.put(uniqueId, stacks);
            }
        }
        
        // Commands //
        
        this.registerCommand("tradeadmin", new TradeAdminCommand(this));
        
        // Server Events & Tasks //
        
        this.server.getPluginManager().registerEvents(new TradeListener(this), this);
        
        this.scheduler.runTaskTimer(this, () -> {
            
            for (final TradeRoom room : this.tradeRooms.values()) {
                
                if (!room.isActive()) {
                    continue;
                }
                
                final Trader trader1 = room.getTrader1();
                final Trader trader2 = room.getTrader2();
                
                if (trader1 != null) {
                    if (trader1.isOffline()) {
                        this.processOfflineTrader(room, trader1, Side.SIDE_1);
                    } else {
                        this.processOnlineTrader(room, trader1, Side.SIDE_1);
                    }
                }
                if (trader2 != null) {
                    if (trader2.isOffline()) {
                        this.processOfflineTrader(room, trader2, Side.SIDE_2);
                    } else {
                        this.processOnlineTrader(room, trader2, Side.SIDE_2);
                    }
                }
            }
        }, 200L, 200L);
    }
    
    private void registerCommand(@NotNull final String commandName, @NotNull final TabExecutor tabExecutor) throws RuntimeException {
        
        final PluginCommand command = this.getCommand(commandName);
        if (command == null) {
            throw new RuntimeException("Cannot find the command /" + commandName);
        }
        command.setExecutor(tabExecutor);
        command.setTabCompleter(tabExecutor);
    }
    
    private void processOnlineTrader(@NotNull final TradeRoom room, @NotNull final Trader trader, @NotNull final Side side) {
        
        final Player player = this.server.getPlayer(trader.getUniqueId());
        if (player == null || !player.isOnline()) {
            
            if (!trader.isOffline()) {
                trader.setOffline(true);
                this.saveRoom(this.console, room);
            }
            
            this.processOfflineTrader(room, trader, side);
            return;
        }
        
        final ProtectedRegion region = side == Side.SIDE_1 ? room.getRegion1() : room.getRegion2();
        if (region.contains(BukkitAdapter.asBlockVector(player.getLocation()))) {
            return;
        }
        
        final Location teleport = side == Side.SIDE_1 ? room.getTeleportIn1() : room.getTeleportIn2();
        player.teleport(teleport);
        player.sendMessage("§6Not sure how you got out, but please finish or cancel your trade before you leave.");
    }
    
    private void processOfflineTrader(@NotNull final TradeRoom room, @NotNull final Trader trader, @NotNull final Side side) {
            
        final long now = System.currentTimeMillis();
        final long expire = trader.getLogoutTime() + OFFLINE_TIMEOUT;
        
        if (expire >= now) {
            return;
        }
        
        final Offline offline = new Offline(trader);
        offline.setReason(CompleteReason.OFFLINE_SELF);
        offline.setTeleport(side == Side.SIDE_1 ? room.getTeleportOut1() : room.getTeleportOut2());
        final Inventory extra = room.returnItems(this.server, offline);
        
        this.offlines.put(offline.getUniqueId(), offline);
        this.saveOffline(offline, extra);
        
        room.setStatus(null);
        
        final Trader other = side == Side.SIDE_1 ? room.getTrader2() : room.getTrader1();
        if (other != null) {
            
            final Player otherPlayer = this.server.getPlayer(other.getUniqueId());
            if (otherPlayer == null || !otherPlayer.isOnline()) {
                
                if (!other.isOffline()) {
                    other.setOffline(true);
                }
                
                final Offline otherOffline = new Offline(other);
                otherOffline.setReason(CompleteReason.OFFLINE_OTHER);
                otherOffline.setTeleport(side == Side.SIDE_1 ? room.getTeleportOut2() : room.getTeleportOut1());
                final Inventory extraOther = room.returnItems(this.server, otherOffline);
                
                this.offlines.put(otherOffline.getUniqueId(), otherOffline);
                this.saveOffline(otherOffline, extraOther);
                
                if (side == Side.SIDE_1) {
                    room.setTrader2(null);
                } else {
                    room.setTrader1(null);
                }
                
            } else {
                
                if (other.isOffline()) {
                    other.setOffline(false);
                }
                
                room.setStatus(TradeStatus.COMPLETE);
                
                otherPlayer.sendMessage("§cYour trade with " + offline.getName() + " has been cancelled because they were offline for too long.");
                otherPlayer.sendMessage("§cPlease exit the trade room once all of your items have been returned. Be sure to check for any dropped and floating items.");
                otherPlayer.sendMessage("§6Returning items...");
                room.returnItems(otherPlayer);
                
            }
            
            this.deleteChestInventories(this.console, room);
        }
        
        if (side == Side.SIDE_1) {
            room.setTrader1(null);
        } else {
            room.setTrader2(null);
        }
        
        this.saveRoom(this.console, room);
    }
    
    @Override
    public void onDisable() {
        
        final Set<UUID> ignore = new HashSet<UUID>();
        for (final Player player : this.server.getOnlinePlayers()) {
            
            final UUID uniqueId = player.getUniqueId();
            if (ignore.contains(uniqueId)) {
                continue;
            }
            
            TradeRoom room = null;
            Trader self = null;
            Side side = null;
            for (final TradeRoom check : this.tradeRooms.values()) {
                
                final Trader check1 = check.getTrader1();
                final Trader check2 = check.getTrader2();
                
                if (check1 != null && uniqueId.equals(check1.getUniqueId())) {
                    room = check;
                    self = check1;
                    side = Side.SIDE_1;
                    break;
                } else if (check2 != null && uniqueId.equals(check2.getUniqueId())) {
                    room = check;
                    self = check2;
                    side = Side.SIDE_2;
                    break;
                }
            }
            
            if (self == null) {
                continue;
            }
            
            self.setOffline(true);
            
            final Trader other = side == Side.SIDE_1 ? room.getTrader2() : room.getTrader1();
            if (other == null) {
                this.saveRoom(this.console, room, true);
                continue;
            }
            
            if (!other.isOffline()) {
                other.setOffline(true);
            }
            this.saveRoom(this.console, room, true);
            ignore.add(other.getUniqueId());
        }
        
        final long now = System.currentTimeMillis();
        final YamlConfiguration stopConfig = new YamlConfiguration();
        stopConfig.set(Constants.KEY_SERVER_STOP_TIME, now);
        try {
            stopConfig.save(this.serverStopFile);
        } catch (IOException | IllegalArgumentException e) {
            this.logger.log(Level.WARNING, "ISSUE WHILE SAVING SERVER STOP TIME.");
            this.logger.log(Level.WARNING, "Details below:");
            this.logger.log(Level.WARNING, "server_stop_time:" + now);
            this.logger.log(Level.WARNING, "ISSUE:");
            this.logger.log(Level.WARNING, "Attempted to save the Server Stop file.");
            this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
        }
    }
    
    ///////////////////////////
    // EVENT HANDLER METHODS //
    ///////////////////////////
    
    public boolean blockBreak(@NotNull final Location location) {
        
        for (final TradeRoom room : this.tradeRooms.values()) {
            if (room.contains(location, false)) {
                return true;
            }
        }
        
        return false;
    }
    
    public boolean blockPlace(@NotNull final Chest chest) {
        
        final Location location = chest.getLocation();
        
        for (final TradeRoom room : this.tradeRooms.values()) {
            
            final Location chest1Loc = room.getChest1().getLocation();
            if (chest1Loc.equals(location.add(1.0D, 0.0D, 0.0D))) {
                return true;
            } else if (chest1Loc.equals(location.add(-2.0D, 0.0D, 0.0D))) {
                return true;
            } else if (chest1Loc.equals(location.add(1.0D, 0.0D, 1.0D))) {
                return true;
            } else if (chest1Loc.equals(location.add(0.0D, 0.0D, -2.0D))) {
                return true;
            }
            
            final Location chest2Loc = room.getChest2().getLocation();
            if (chest2Loc.equals(location.add(1.0D, 0.0D, 0.0D))) {
                return true;
            } else if (chest2Loc.equals(location.add(-2.0D, 0.0D, 0.0D))) {
                return true;
            } else if (chest2Loc.equals(location.add(1.0D, 0.0D, 1.0D))) {
                return true;
            } else if (chest2Loc.equals(location.add(0.0D, 0.0D, -2.0D))) {
                return true;
            }
        }
        
        return false;
    }
    
    public boolean inventoryClick(@NotNull final Player player, @NotNull final Inventory inventory, final int slot) {
        
        final UUID uniqueId = player.getUniqueId();
        
        TradeRoom room = null;
        for (final TradeRoom check : this.tradeRooms.values()) {
            if (check.isUsing(uniqueId)) {
                room = check;
                break;
            }
        }
        
        if (room == null) {
            return false;
        }
        
        final Trader self = new Trader(player);
        final Trader other;
        final Side otherSide;
        
        if (room.isTrader1(uniqueId)) {
            other = room.getTrader2();
            otherSide = Side.SIDE_2;
        } else {
            other = room.getTrader1();
            otherSide = Side.SIDE_1;
        }
        
        if (other == null) {
            return false;
        }
        
        final TradeStatus status = room.getStatus();
        if (status == null || status.ordinal() < TradeStatus.DECIDE.ordinal()) {
            return false;
        }
        
        if (room.hasAccepted(uniqueId)) {
            if (slot == SLOT_REJECT) {
                player.sendMessage("§cYou have already accepted the trade. You may not reject it.");
            } else if (slot == SLOT_ACCEPT) {
                player.sendMessage("§aYou have already accepted the trade.");
            }
            return true;
        }
        
        final Inventory tradeInventory = this.tradeInventories.get(uniqueId);
        if (tradeInventory == null) {
            player.sendMessage("§cThere was an error with your trade. Please report it to the system administrators.");
            player.sendMessage("§cIf you are not trading or the trade inventory did not appear, please report this to the system administrators as well.");
            this.logger.log(Level.WARNING, "No trade inventory found for player in DECIDE or later phase.");
            this.logger.log(Level.WARNING, "Trade Room: " + room.getName());
            this.logger.log(Level.WARNING, "Player UUID: " + uniqueId.toString());
            this.logger.log(Level.WARNING, "Player Name: " + player.getName());
            this.logger.log(Level.WARNING, "Trade Status: " + status.name());
            return false;
        }
        
        if (!this.checkInventories(inventory, tradeInventory)) {
            return false;
        }
        
        if (slot == SLOT_REJECT) {
            
            if (room.hasNotAccepted(uniqueId)) {
                
                if (this.cancelRequests.containsKey(uniqueId)) {
                    this.scheduler.cancelTask(this.cancelRequests.remove(uniqueId).getTaskId());
                }
                
                this.rejectTrade(player, room, otherSide, self, other);
                return true;
            }
            
            this.scheduler.cancelTask(this.cancelRequests.remove(uniqueId).getTaskId());
            this.cancelTrade(player, room, otherSide, self, other);
            player.sendMessage("§aYou have cancelled the trade.");
            player.sendMessage("§6Please remember to pick up your items that may have fallen on the ground before you leave the trade room.");
            return true;
            
        } else if (slot == SLOT_ACCEPT) {
            
            if (this.cancelRequests.containsKey(uniqueId)) {
                player.sendMessage("§6" + this.cancelRequests.remove(uniqueId).getType() + " request cancelled.");
            }
            player.sendMessage("§aYou have accepted the trade.");
            
            if (room.hasNotAccepted(other.getUniqueId())) {
                
                if (other.isOffline()) {
                    player.sendMessage("§bPlease wait for " + other.getName() + " to log back in and finish deciding on the trade.");
                } else {
                    player.sendMessage("§bPlease wait while " + other.getName() + " finishes deciding on the trade.");
                }
                
                room.setStatus(room.isTrader1(uniqueId) ? TradeStatus.ACCEPT_1 : TradeStatus.ACCEPT_2);
                this.saveRoom(player, room);
                return true;
            }
            
            this.acceptTrade(player, room, otherSide, self, other);
            return true;
        }
        
        return true;
    }
    
    @NotNull
    public Collection<String> playerCommandSend(@NotNull final Player player) {
        
        final HashSet<String> removals = new HashSet<String>();
        for (final String commandName : this.getDescription().getCommands().keySet()) {
            
            removals.add("cvtrade:" + commandName);
            if (!player.hasPermission(this.server.getPluginCommand(commandName).getPermission())) {
                removals.add(commandName);
            }
        }
    
        return removals;
    }
    
    public void buildRoom(@NotNull final Player player, @NotNull final BlockState state) {
        
        if (state.getType() == Material.AIR) {
            return;
        }
        
        final TradeRoomBuilder builder = this.builders.get(player.getUniqueId());
        if (builder == null) {
            return;
        }
        
        final BuildStep step = builder.getStep();
        if (step == BuildStep.CHEST_1) {
            
            if (!builder.setChest1(state.getLocation())) {
                player.sendMessage("§cPlease try this step again.");
                return;
            }
            
            player.sendMessage("§aTrade chest 1 set.§r");
            player.sendMessage("§aPlease set the trade room 1st entry teleport location with the command§r §b/tradeadmin setteleport§r§a.");
            return;
        }
        
        if (step == BuildStep.BUTTON_IN_1) {
            
            if (!builder.setButtonIn1(state.getLocation())) {
                player.sendMessage("§cPlease try this step again.");
                return;
            }
            
            player.sendMessage("§aEntry teleport button 1 set.§r");
            player.sendMessage("§aPlease left click on the button to use as the 1st exit button.");
            player.sendMessage("§6MAKE SURE YOU ARE NOT IN CREATIVE MODE (Survival is recommended).");
            return;
        }
        
        if (step == BuildStep.BUTTON_OUT_1) {
            
            if (!builder.setButtonOut1(state.getLocation())) {
                player.sendMessage("§cPlease try this step again.");
                return;
            }
            
            player.sendMessage("§aExit teleport button 1 set.§r");
            player.sendMessage("§aPlease left click on the button to use as the 1st trade lock button.");
            player.sendMessage("§6MAKE SURE YOU ARE NOT IN CREATIVE MODE (Survival is recommended).");
            return;
        }
        
        if (step == BuildStep.BUTTON_LOCK_1) {
            
            if (!builder.setButtonLock1(state.getLocation())) {
                player.sendMessage("§cPlease try this step again.");
                return;
            }
            
            player.sendMessage("§aTrade lock button 1 set.§r");
            player.sendMessage("§aPlease left click on the button to use as the 1st trade accept button.");
            player.sendMessage("§6MAKE SURE YOU ARE NOT IN CREATIVE MODE (Survival is recommended).");
            return;
        }
        
        if (step == BuildStep.BUTTON_ACCEPT_1) {
            
            if (!builder.setButtonAccept1(state.getLocation())) {
                player.sendMessage("§cPlease try this step again.");
                return;
            }
            
            player.sendMessage("§aTrade accept button 1 set.§r");
            player.sendMessage("§aPlease left click on the button to use as the 1st trade deny button.");
            player.sendMessage("§6MAKE SURE YOU ARE NOT IN CREATIVE MODE (Survival is recommended).");
            return;
        }
        
        if (step == BuildStep.BUTTON_DENY_1) {
            
            if (!builder.setButtonDeny1(state.getLocation())) {
                player.sendMessage("§cPlease try this step again.");
                return;
            }
            
            player.sendMessage("§aTrade deny button 1 set.§r");
            player.sendMessage("§aPlease left click on the chest to use as the 2nd trade chest.");
            player.sendMessage("§6MAKE SURE YOU ARE NOT IN CREATIVE MODE (Survival is recommended).");
            return;
        }
        
        if (step == BuildStep.CHEST_2) {
            
            if (!builder.setChest2(state.getLocation())) {
                player.sendMessage("§cPlease try this step again.");
                return;
            }
            
            player.sendMessage("§aTrade chest 2 set.§r");
            player.sendMessage("§aPlease set the trade room 2nd entry teleport location with the command§r §b/tradeadmin setteleport§r§a.");
            return;
        }
        
        if (step == BuildStep.BUTTON_IN_2) {
            
            if (!builder.setButtonIn2(state.getLocation())) {
                player.sendMessage("§cPlease try this step again.");
                return;
            }
            
            player.sendMessage("§aEntry teleport button 2 set.§r");
            player.sendMessage("§aPlease left click on the button to use as the 2nd exit button.");
            player.sendMessage("§6MAKE SURE YOU ARE NOT IN CREATIVE MODE (Survival is recommended).");
            return;
        }
        
        if (step == BuildStep.BUTTON_OUT_2) {
            
            if (!builder.setButtonOut2(state.getLocation())) {
                player.sendMessage("§cPlease try this step again.");
                return;
            }
            
            player.sendMessage("§aExit teleport button 2 set.§r");
            player.sendMessage("§aPlease left click on the button to use as the 2nd trade lock button.");
            player.sendMessage("§6MAKE SURE YOU ARE NOT IN CREATIVE MODE (Survival is recommended).");
            return;
        }
        
        if (step == BuildStep.BUTTON_LOCK_2) {
            
            if (!builder.setButtonLock2(state.getLocation())) {
                player.sendMessage("§cPlease try this step again.");
                return;
            }
            
            player.sendMessage("§aTrade lock button 2 set.§r");
            player.sendMessage("§aPlease left click on the button to use as the 2nd trade accept button.");
            player.sendMessage("§6MAKE SURE YOU ARE NOT IN CREATIVE MODE (Survival is recommended).");
            return;
        }
        
        if (step == BuildStep.BUTTON_ACCEPT_2) {
            
            if (!builder.setButtonAccept2(state.getLocation())) {
                player.sendMessage("§cPlease try this step again.");
                return;
            }
            
            player.sendMessage("§aTrade accept button 2 set.§r");
            player.sendMessage("§aPlease left click on the button to use as the 2nd trade deny button.");
            player.sendMessage("§6MAKE SURE YOU ARE NOT IN CREATIVE MODE (Survival is recommended).");
            return;
        }
        
        if (step == BuildStep.BUTTON_DENY_2) {
            
            if (!builder.setButtonDeny2(state.getLocation())) {
                player.sendMessage("§cPlease try this step again.");
                return;
            }
            
            player.sendMessage("§aTrade deny button 2 set.§r");
            player.sendMessage("§aPlease wait while the trade room is built.");
            
            final TradeRoom room;
            try {
                room = builder.build();
            } catch (final IllegalStateException e) {
                player.sendMessage("§cAn error occurred while attempting to build the trade room.");
                return;
            }
            
            this.tradeRooms.put(room.getName().toLowerCase(), room);
            this.saveRoom(player, room);
            this.builders.remove(player.getUniqueId());
            player.sendMessage("§aTrade room§r §6" + room.getName() + "§r §acreated successfully.");
            
            return;
        }
        
        player.sendMessage("§cYou are not on the correct build step.");
        player.sendMessage("§cYour current step is§r §6" + step.name().toLowerCase() + "§r§c.");
    }
    
    public boolean rightClickedBlock(@NotNull final Player player, @NotNull final BlockState state) {
        
        final UUID uniqueId = player.getUniqueId();
        final TradeRoomBuilder builder = this.builders.get(uniqueId);
        if (builder != null) {
            player.sendMessage("§cYou are currently building a trade room. You are on step§r §6" + builder.getStep().name().toLowerCase() + "§r§c.");
            player.sendMessage("§cPlease finish building the trade room first.");
            return true;
        }
        
        final Location location = state.getLocation();
        TradeRoom room = null;
        for (final TradeRoom check : this.tradeRooms.values()) {
            if (check.contains(location, true)) {
                room = check;
                break;
            }
        }
        
        if (room == null) {
            return false;
        }
        
        final Side side = room.getSide(location);
        if (side == null) {
            return false;
        }
        
        // No one's in here
        if (!room.isActive()) {
            
            if (side == Side.SIDE_1) {
                
                // Player is inside side 1, eject
                if (!location.equals(room.getButtonIn1())) {
                    player.sendMessage("§cYou cannot use these trade room functions because you are not using this trade room.");
                    player.sendMessage("§cTo start a trade, please use the button to enter this trade room.");
                    player.teleport(room.getTeleportOut1());
                    return true;
                }
                
                // Player is outside, start trade on side 1
                this.startTrade(player, room, side);
                return false;
            }
            
            // Player is inside side 2, eject
            if (!location.equals(room.getButtonIn2())) {
                player.sendMessage("§cYou cannot use these trade room functions because you are not using this trade room.");
                player.sendMessage("§cTo start a trade, please use the button to enter this trade room.");
                player.teleport(room.getTeleportOut2());
                return true;
            }
            
            // Player is outside, start trade on side 2
            this.startTrade(player, room, side);
            return false;
        }
        
        // 2 players in the room
        if (room.isFull()) {
            
            // They are 2 other players
            if (!room.isUsing(uniqueId)) {
                player.sendMessage("§cYou cannot use this trade room as it is being used by other players.");
                player.sendMessage("§cPlease wait for the current players to finish, and then you can use this trade room.");
                
                // Player is in side 1, eject
                if (side == Side.SIDE_1 && !location.equals(room.getButtonIn1())) {
                    player.sendMessage("§cYou were not supposed to be in there. Out you go!");
                    player.teleport(room.getTeleportOut1());
                    return true;
                }
                
                // Player is in side 2, eject
                if (side == Side.SIDE_2 && !location.equals(room.getButtonIn2())) {
                    player.sendMessage("§cYou were not supposed to be in there. Out you go!");
                    player.teleport(room.getTeleportOut2());
                    return true;
                }
                return false;
            }
            
            // Player is one of the traders
            // Player is outside, supposed to be in side 1, send in
            if (side == Side.SIDE_1 && location.equals(room.getButtonIn1())) {
                player.sendMessage("§6Unsure how you got outside. Back in you go!");
                player.teleport(room.isTrader1(uniqueId) ? room.getTeleportIn1() : room.getTeleportIn2());
                return false;
            }
            
            // Player is outside, supposed to be in side 2, send in
            if (side == Side.SIDE_2 && location.equals(room.getButtonIn2())) {
                player.sendMessage("§6Unsure how you got outside. Back in you go!");
                player.teleport(room.isTrader2(uniqueId) ? room.getTeleportIn2() : room.getTeleportIn1());
                return false;
            }
            
            // Player is in side 2, supposed to be in side 1, swap
            if (side == Side.SIDE_2 && room.isTrader1(uniqueId)) {
                player.sendMessage("§6Unsure how you got over there. Back to your side you go!");
                player.teleport(room.getTeleportIn1());
                return true;
            }
            
            // Player is in side 1, supposed to be in side 2, swap
            if (side == Side.SIDE_1 && room.isTrader2(uniqueId)) {
                player.sendMessage("§6Unsure how you got over there. Back to your side you go!");
                player.teleport(room.getTeleportIn2());
                return true;
            }
            
            // Player is in their assigned room, handle actions inside the room
            return this.handleInsideRoom(player, state, room, side);
        }
        
        // 1 player in the room
        // Trader is in side 1
        final Trader trader1 = room.getTrader1();
        if (trader1 != null) {
            final UUID uniqueId1 = trader1.getUniqueId();
            
            // Player is somewhere on side 1
            if (side == Side.SIDE_1) {
                
                // Player is outside, supposed to be in side 1, send in
                if (uniqueId.equals(uniqueId1) && location.equals(room.getButtonIn1())) {
                    player.sendMessage("§6Unsure how you got outside. Back in you go!");
                    player.teleport(room.getTeleportIn1());
                    return false;
                }
                
                // Player is in side 1, is not the current trader, eject
                if (!uniqueId.equals(uniqueId1) && !location.equals(room.getButtonIn1())) {
                    player.sendMessage("§cYou were not supposed to be in there. Out you go!");
                    player.teleport(room.getTeleportOut1());
                    return true;
                }
                
                // Player is outside, is not the current trader, deny
                if (!uniqueId.equals(uniqueId1) && location.equals(room.getButtonIn1())) {
                    player.sendMessage("§cSomeone else is already using that trade room.");
                    if (room.hasCompleted()) {
                        player.sendMessage("§cThey are completing their previous trade. Please wait until they leave to start your trade.");
                    } else {
                        player.sendMessage("§cIf you wish to trade with this player, please use the room on the other side. Otherwise, wait for the player to complete their trade.");
                    }
                    return true;
                }
                
                // Player is in their assigned room, handle actions inside the room
                return this.handleInsideRoom(player, state, room, side);
            }
            
            // Player is somewhere on side 2
            // Player is outside, supposed to be in side 1, send in
            if (uniqueId.equals(uniqueId1) && location.equals(room.getButtonIn2())) {
                player.sendMessage("§6Unsure how you got outside. Back in you go!");
                player.teleport(room.getTeleportIn1());
                return false;
            }
            
            // Player is in side 2, is not the current trader, eject
            if (!uniqueId.equals(uniqueId1) && !location.equals(room.getButtonIn2())) {
                player.sendMessage("§cYou were not supposed to be in there. Out you go!");
                player.teleport(room.getTeleportOut2());
                return true;
            }
            
            // Player is in side 2, supposed to be in side 1, swap
            if (uniqueId.equals(uniqueId1) && !location.equals(room.getButtonIn2())) {
                player.sendMessage("§6Unsure how you got over there. Back to your side you go!");
                player.teleport(room.getTeleportIn1());
                return true;
            }
            
            // Player is outside, trade room is finishing previous trade, deny
            if (room.hasCompleted()) {
                player.sendMessage("§cThe previous trade is finishing up. Please wait until the player has left to start your trade.");
                return true;
            }
            
            // Player is outside, start trade on side 2
            this.startTrade(player, room, side);
            return false;
        }
        
        // Trader is in side 2
        final Trader trader2 = room.getTrader2();
        if (trader2 == null) {
            
            // This should never happen, but handle it anyway.
            player.sendMessage("§cAn unknown error occurred. Please report it to the system administrators.");
            
            this.logger.log(Level.WARNING, "Trade room is active, but no traders can be retrieved.");
            this.logger.log(Level.WARNING, "Trade Room: " + room.getName());
            this.logger.log(Level.WARNING, "Player UUID: " + uniqueId.toString());
            this.logger.log(Level.WARNING, "Player Name: " + player.getName());
            this.logger.log(Level.WARNING, "Activating World: " + (location.getWorld() == null ? "null" : location.getWorld().getName()));
            this.logger.log(Level.WARNING, "Activating X: " + location.getBlockX());
            this.logger.log(Level.WARNING, "Activating Y: " + location.getBlockY());
            this.logger.log(Level.WARNING, "Activating Z: " + location.getBlockZ());
            this.logger.log(Level.WARNING, "Trader 1 UUID: null");
            this.logger.log(Level.WARNING, "Trader 1 Name: null");
            this.logger.log(Level.WARNING, "Trader 1 Status: null");
            this.logger.log(Level.WARNING, "Trader 2 UUID: null");
            this.logger.log(Level.WARNING, "Trader 2 Name: null");
            this.logger.log(Level.WARNING, "Trader 2 Status: null");
            
            return true;
        }
        
        final UUID uniqueId2 = trader2.getUniqueId();
        
        // Player is somewhere on side 2
        if (side == Side.SIDE_2) {
            
            // Player is outside, supposed to be in side 2, send in
            if (uniqueId.equals(uniqueId2) && location.equals(room.getButtonIn2())) {
                player.sendMessage("§6Unsure how you got outside. Back in you go!");
                player.teleport(room.getTeleportIn2());
                return false;
            }
            
            // Player is in side 2, is not the current trader, eject
            if (!uniqueId.equals(uniqueId2) && !location.equals(room.getButtonIn2())) {
                player.sendMessage("§cYou were not supposed to be in there. Out you go!");
                player.teleport(room.getTeleportOut2());
                return true;
            }
            
            // Player is outside, is not the current trader, deny
            if (!uniqueId.equals(uniqueId2) && location.equals(room.getButtonIn2())) {
                player.sendMessage("§cSomeone else is already using that trade room.");
                if (room.hasCompleted()) {
                    player.sendMessage("§cThey are completing their previous trade. Please wait until they leave to start your trade.");
                } else {
                    player.sendMessage("§cIf you wish to trade with this player, please use the room on the other side. Otherwise, wait for the player to complete their trade.");
                }
                return true;
            }
            
            // Player is in their assigned room, handle actions inside the room
            return this.handleInsideRoom(player, state, room, side);
        }
        
        // Player is somewhere on side 1
        // Player is outside, supposed to be in side 2, send in
        if (uniqueId.equals(uniqueId2) && location.equals(room.getButtonIn1())) {
            player.sendMessage("§6Unsure how you got outside. Back in you go!");
            player.teleport(room.getTeleportIn2());
            return false;
        }
        
        // Player is in side 1, is not the current trader, eject
        if (!uniqueId.equals(uniqueId2) && !location.equals(room.getButtonIn1())) {
            player.sendMessage("§cYou were not supposed to be in there. Out you go!");
            player.teleport(room.getTeleportOut1());
            return true;
        }
        
        // Player is in side 1, supposed to be in side 2, swap
        if (uniqueId.equals(uniqueId2) && !location.equals(room.getButtonIn1())) {
            player.sendMessage("§6Unsure how you got over there. Back to your side you go!");
            player.teleport(room.getTeleportIn2());
            return true;
        }
        
        // Player is outside, trade room is finishing previous trade, deny
        if (room.hasCompleted()) {
            player.sendMessage("§cThe previous trade is finishing up. Please wait until the player has left to start your trade.");
            return true;
        }
        
        // Player is outside, start trade on side 1
        this.startTrade(player, room, side);
        return false;
    }
    
    private boolean handleInsideRoom(@NotNull final Player player, @NotNull final BlockState state, @NotNull final TradeRoom room, @NotNull final Side side) {
        
        final Location location = state.getLocation();
        final TradeStatus status = room.getStatus();
        final Trader self;
        final Trader other;
        
        if (side == Side.SIDE_1) {
            self = room.getTrader1();
            other = room.getTrader2();
        } else {
            self = room.getTrader2();
            other = room.getTrader1();
        }
        
        if (self == null) {
            
            // This should never happen, but handle it anyway.
            player.sendMessage("§cAn unknown error occurred. Please report it to the system administrators.");
            
            final Trader trader1 = room.getTrader1();
            final Trader trader2 = room.getTrader2();
            
            this.logger.log(Level.WARNING, "Trade room is active, trader as self cannot be retrieved.");
            this.logger.log(Level.WARNING, "Trade Room: " + room.getName());
            this.logger.log(Level.WARNING, "Player UUID: " + player.getUniqueId().toString());
            this.logger.log(Level.WARNING, "Player Name: " + player.getName());
            this.logger.log(Level.WARNING, "Activating World: " + (location.getWorld() == null ? "null" : location.getWorld().getName()));
            this.logger.log(Level.WARNING, "Activating X: " + location.getBlockX());
            this.logger.log(Level.WARNING, "Activating Y: " + location.getBlockY());
            this.logger.log(Level.WARNING, "Activating Z: " + location.getBlockZ());
            this.logger.log(Level.WARNING, "Trade Status: " + (room.getStatus() == null ? "null" : room.getStatus().name()));
            this.logger.log(Level.WARNING, "Trader 1 UUID: " + (trader1 == null ? "null" : trader1.getUniqueId().toString()));
            this.logger.log(Level.WARNING, "Trader 1 Name: " + (trader1 == null ? "null" : trader1.getName()));
            this.logger.log(Level.WARNING, "Trader 2 UUID: " + (trader2 == null ? "null" : trader2.getUniqueId().toString()));
            this.logger.log(Level.WARNING, "Trader 2 Name: " + (trader2 == null ? "null" : trader2.getName()));
            
            return true;
        }
        
        if (status == null) {
            
            // This should never happen, but handle it anyway.
            player.sendMessage("§cAn unknown error occurred. Please report it to the system administrators.");
            
            final Trader trader1 = room.getTrader1();
            final Trader trader2 = room.getTrader2();
            
            this.logger.log(Level.WARNING, "Trade room is active, trade status is null.");
            this.logger.log(Level.WARNING, "Trade Room: " + room.getName());
            this.logger.log(Level.WARNING, "Player UUID: " + player.getUniqueId().toString());
            this.logger.log(Level.WARNING, "Player Name: " + player.getName());
            this.logger.log(Level.WARNING, "Activating World: " + (location.getWorld() == null ? "null" : location.getWorld().getName()));
            this.logger.log(Level.WARNING, "Activating X: " + location.getBlockX());
            this.logger.log(Level.WARNING, "Activating Y: " + location.getBlockY());
            this.logger.log(Level.WARNING, "Activating Z: " + location.getBlockZ());
            this.logger.log(Level.WARNING, "Trade Status: null");
            this.logger.log(Level.WARNING, "Trader 1 UUID: " + (trader1 == null ? "null" : trader1.getUniqueId().toString()));
            this.logger.log(Level.WARNING, "Trader 1 Name: " + (trader1 == null ? "null" : trader1.getName()));
            this.logger.log(Level.WARNING, "Trader 2 UUID: " + (trader2 == null ? "null" : trader2.getUniqueId().toString()));
            this.logger.log(Level.WARNING, "Trader 2 Name: " + (trader2 == null ? "null" : trader2.getName()));
            
            return true;
        }
        
        final UUID uniqueId = player.getUniqueId();
        
        if (room.isChest(location, side)) {
            
            if (room.hasCompleted()) {
                
                if (this.cancelRequests.containsKey(uniqueId)) {
                    this.scheduler.cancelTask(this.cancelRequests.remove(uniqueId).getTaskId());
                }
                
                player.sendMessage("§6You cannot open the chest as you have already completed the trade.");
                player.sendMessage("§6Please make sure you pick up any items you own, and please exit the room.");
                return true;
            }
            
            if (this.cancelRequests.containsKey(uniqueId)) {
                player.sendMessage("§6" + this.cancelRequests.remove(uniqueId).getType() + " request cancelled.");
            }
            
            if (other == null) {
                player.sendMessage("§cPlease wait to put your items in the chest until another player begins a trade with you.");
                return true;
            }
            
            if (room.hasNotLocked(uniqueId)) {
                return false;
            }
            
            if (room.hasLocked(uniqueId)) {
                player.sendMessage("§cYou may not open the chest, as you have locked your trade in.");
                player.sendMessage("§cIf you wish to change your items, please cancel the trade and then re-start it.");
                return true;
            }
            
            if (room.hasNotAccepted(uniqueId)) {
                
                final Inventory tradeInventory = this.tradeInventories.get(uniqueId);
                if (tradeInventory == null) {
                    player.sendMessage("§cAn unknown error occurred. Please report it to the system administrators.");
                    this.logger.log(Level.WARNING, "Trade is in DECIDE phase, unable to open trade inventory.");
                    this.logger.log(Level.WARNING, "Trade Room: " + room.getName());
                    this.logger.log(Level.WARNING, "Player UUID: " + player.getUniqueId().toString());
                    this.logger.log(Level.WARNING, "Player Name: " + player.getName());
                    this.logger.log(Level.WARNING, "Activating World: " + (location.getWorld() == null ? "null" : location.getWorld().getName()));
                    this.logger.log(Level.WARNING, "Activating X: " + location.getBlockX());
                    this.logger.log(Level.WARNING, "Activating Y: " + location.getBlockY());
                    this.logger.log(Level.WARNING, "Activating Z: " + location.getBlockZ());
                    this.logger.log(Level.WARNING, "Trade Status: " + status.name());
                    this.logger.log(Level.WARNING, "Trader 1 UUID: " + self.getUniqueId().toString());
                    this.logger.log(Level.WARNING, "Trader 1 Name: " + self.getName());
                    this.logger.log(Level.WARNING, "Trader 2 UUID: " + other.getUniqueId().toString());
                    this.logger.log(Level.WARNING, "Trader 2 Name: " + other.getName());
                    return true;
                }
                
                if (!this.checkInventories(player.getOpenInventory().getTopInventory(), tradeInventory)) {
                    player.closeInventory();
                    player.openInventory(tradeInventory);
                }
                
                return true;
            }
            
            player.sendMessage("§6You cannot open the chest as you have already accepted the trade.");
            if (other.isOffline()) {
                player.sendMessage("§6Please wait for " + other.getName() + " to log back in and make their decision.");
            } else {
                player.sendMessage("§6Please wait for " + other.getName() + " to make their decision.");
            }
            
            return true;
        }
        
        if (room.isButtonOut(location, side)) {
            
            if (room.hasCompleted()) {
                
                if (this.cancelRequests.containsKey(uniqueId)) {
                    this.scheduler.cancelTask(this.cancelRequests.remove(uniqueId).getTaskId());
                }
                
                player.sendMessage("§aThank you for using the trade room.");
                if (side == Side.SIDE_1) {
                    player.teleport(room.getTeleportOut1());
                    room.setTrader1(null);
                } else {
                    player.teleport(room.getTeleportOut2());
                    room.setTrader2(null);
                }
                
                if (other == null) {
                    room.setStatus(null);
                }
                this.saveRoom(player, room);
                return false;
            }
            
            if (!this.cancelRequests.containsKey(uniqueId)) {
                
                this.cancelRequests.put(uniqueId, new CancelRequest("Exit", this.scheduler.runTaskLater(this, () -> {
                    if (this.cancelRequests.containsKey(uniqueId)) {
                        player.sendMessage("§c" + this.cancelRequests.remove(uniqueId).getType() + " confirmation expired.");
                    }
                }, 600L).getTaskId()));
                
                player.sendMessage("§cAre you sure you want to exit the trade room?");
                player.sendMessage("§6Press the exit button again within 30 seconds to exit the trade room.");
                player.sendMessage("§bIf you do not wish to exit the trade room, please use the lock or accept buttons, or the chest, or wait 30 seconds.");
                return false;
            }
            
            final Player otherPlayer = other == null ? null : this.server.getPlayer(other.getUniqueId());
            if (side == Side.SIDE_1) {
                player.teleport(room.getTeleportOut1());
                if (otherPlayer != null && otherPlayer.isOnline()) {
                    otherPlayer.teleport(room.getTeleportOut2());
                }
            } else {
                player.teleport(room.getTeleportOut2());
                if (otherPlayer != null && otherPlayer.isOnline()) {
                    otherPlayer.teleport(room.getTeleportOut1());
                }
            }
            
            this.scheduler.cancelTask(this.cancelRequests.remove(uniqueId).getTaskId());
            this.cancelTrade(player, room, side.getOther(), self, other);
            
            room.setStatus(null);
            room.setTrader1(null);
            room.setTrader2(null);
            this.saveRoom(player, room);
            
            player.sendMessage("§aYou have exited the trade room.");
            if (otherPlayer != null && otherPlayer.isOnline()) {
                otherPlayer.sendMessage("§aYou have exited the trade room.");
            }
            
            return false;
        }
        
        if (room.isButtonLock(location, side)) {
            
            if (room.hasCompleted()) {
                
                if (this.cancelRequests.containsKey(uniqueId)) {
                    this.scheduler.cancelTask(this.cancelRequests.remove(uniqueId).getTaskId());
                }
                
                player.sendMessage("§cYou have already completed your trade.");
                player.sendMessage("§6Please make sure you pick up any items you own, and please exit the room.");
                return true;
            }
            
            if (this.cancelRequests.containsKey(uniqueId)) {
                player.sendMessage("§6" + this.cancelRequests.remove(uniqueId).getType() + " request cancelled.");
            }
            
            if (other == null) {
                player.sendMessage("§cYou cannot use this button until another player begins a trade with you.");
                return true;
            }
            
            if (room.hasNotLocked(uniqueId)) {
                
                this.saveChestInventories(player, room);
                player.sendMessage("§aYou have locked in your items for the trade.");
                
                if (room.hasNotLocked(other.getUniqueId())) {
                    
                    if (other.isOffline()) {
                        player.sendMessage("§bPlease wait for " + other.getName() + " to log back in and finish selecting what they wish to trade.");
                    } else {
                        player.sendMessage("§bPlease wait while " + other.getName() + " finishes selecting what they wish to trade.");
                    }
                    
                    room.setStatus(room.isTrader1(uniqueId) ? TradeStatus.LOCKED_1 : TradeStatus.LOCKED_2);
                    this.saveRoom(player, room);
                    return false;
                }
                
                room.setStatus(TradeStatus.DECIDE);
                this.saveRoom(player, room);
                this.displayNewTrade(player, location, room, status, self, other);
                return false;
            }
            
            if (room.hasLocked(uniqueId)) {
                
                player.sendMessage("§cYou have already locked in your trade.");
                if (other.isOffline()) {
                    player.sendMessage("§bPlease wait for " + other.getName() + " to log back in and finish selecting what they wish to trade.");
                } else {
                    player.sendMessage("§bPlease wait while " + other.getName() + " finishes selecting what they wish to trade.");
                }
                return true;
            }
            
            if (!room.hasAccepted(uniqueId)) {
                player.sendMessage("§cYou have already locked in your trade, you must choose whether to accept or reject the trade.");
                return true;
            }
            
            player.sendMessage("§cYou have already accepted the trade.");
            if (other.isOffline()) {
                player.sendMessage("§6Please wait for " + other.getName() + " to log back in and make their decision.");
            } else {
                player.sendMessage("§6Please wait for " + other.getName() + " to make their decision.");
            }
            return true;
        }
        
        if (room.isButtonAccept(location, side)) {
            
            if (room.hasCompleted()) {
                
                if (this.cancelRequests.containsKey(uniqueId)) {
                    this.scheduler.cancelTask(this.cancelRequests.remove(uniqueId).getTaskId());
                }
                
                player.sendMessage("§6You cannot open the chest as you have already completed the trade.");
                player.sendMessage("§6Please make sure you pick up any items you own, and please exit the room.");
                return true;
            }
            
            if (this.cancelRequests.containsKey(uniqueId)) {
                player.sendMessage("§6" + this.cancelRequests.remove(uniqueId).getType() + " request cancelled.");
            }
            
            if (other == null) {
                player.sendMessage("§cYou cannot use this button until another player begins a trade with you.");
                return true;
            }
            
            if (room.hasNotLocked(uniqueId)) {
                player.sendMessage("§cYou must select your items to trade, and then press the lock button first.");
                return true;
            }
            
            if (room.hasLocked(uniqueId)) {
                player.sendMessage("§cYou must wait for " + other.getName() + " to select the items that they wish to trade.");
                return true;
            }
            
            if (room.hasNotAccepted(uniqueId)) {
                
                player.sendMessage("§aYou have accepted the trade.");
                
                if (room.hasNotAccepted(other.getUniqueId())) {
                    
                    if (other.isOffline()) {
                        player.sendMessage("§bPlease wait for " + other.getName() + " to log back in and finish deciding on the trade.");
                    } else {
                        player.sendMessage("§bPlease wait while " + other.getName() + " finishes deciding on the trade.");
                    }
                    
                    room.setStatus(room.isTrader1(uniqueId) ? TradeStatus.ACCEPT_1 : TradeStatus.ACCEPT_2);
                    this.saveRoom(player, room);
                    return false;
                }
                
                this.acceptTrade(player, room, side.getOther(), self, other);
                return false;
            }
            
            player.sendMessage("§cYou have already accepted the trade.");
            if (other.isOffline()) {
                player.sendMessage("§6Please wait for " + other.getName() + " to log back in and make their decision.");
            } else {
                player.sendMessage("§6Please wait for " + other.getName() + " to make their decision.");
            }
            return true;
        }
        
        if (room.isButtonDeny(location, side)) {
            
            if (room.hasCompleted()) {
                
                if (this.cancelRequests.containsKey(uniqueId)) {
                    this.scheduler.cancelTask(this.cancelRequests.remove(uniqueId).getTaskId());
                }
                
                player.sendMessage("§aThank you for using the trade room.");
                if (side == Side.SIDE_1) {
                    player.teleport(room.getTeleportOut1());
                    room.setTrader1(null);
                } else {
                    player.teleport(room.getTeleportOut2());
                    room.setTrader2(null);
                }
                
                if (other == null) {
                    room.setStatus(null);
                }
                this.saveRoom(player, room);
                return false;
            }
            
            if (other == null || room.hasNotLocked(uniqueId) || room.hasLocked(uniqueId)) {
                
                if (!this.cancelRequests.containsKey(uniqueId)) {
                    
                    this.cancelRequests.put(uniqueId, new CancelRequest("Cancel", this.scheduler.runTaskLater(this, () -> {
                        if (this.cancelRequests.containsKey(uniqueId)) {
                            player.sendMessage("§c" + this.cancelRequests.remove(uniqueId).getType() + " confirmation expired.");
                        }
                    }, 600L).getTaskId()));
                    
                    player.sendMessage("§cAre you sure you want to cancel the trade?");
                    player.sendMessage("§6Press the exit button again within 30 seconds to cancel the trade.");
                    player.sendMessage("§bIf you do not wish to cancel the trade, please use the lock or accept buttons, or the chest, or wait 30 seconds.");
                    return false;
                }
                
                this.scheduler.cancelTask(this.cancelRequests.remove(uniqueId).getTaskId());
                this.cancelTrade(player, room, side.getOther(), self, other);
                player.sendMessage("§aYou have cancelled the trade.");
                player.sendMessage("§6Please remember to pick up your items that may have fallen on the ground before you leave the trade room.");
                return false;
            }
            
            if (room.hasNotAccepted(uniqueId)) {
                
                if (this.cancelRequests.containsKey(uniqueId)) {
                    this.scheduler.cancelTask(this.cancelRequests.remove(uniqueId).getTaskId());
                }
                
                this.rejectTrade(player, room, side.getOther(), self, other);
                return false;
            }
            
            if (!this.cancelRequests.containsKey(uniqueId)) {
                
                this.cancelRequests.put(uniqueId, new CancelRequest("Cancel", this.scheduler.runTaskLater(this, () -> {
                    if (this.cancelRequests.containsKey(uniqueId)) {
                        player.sendMessage("§c" + this.cancelRequests.remove(uniqueId).getType() + " confirmation expired.");
                    }
                }, 600L).getTaskId()));
                
                player.sendMessage("§cAre you sure you want to cancel the trade?");
                player.sendMessage("§6Press the exit button again within 30 seconds to cancel the trade.");
                player.sendMessage("§bIf you do not wish to cancel the trade, please use the lock or accept buttons, or the chest, or wait 30 seconds.");
                return false;
            }
            
            this.scheduler.cancelTask(this.cancelRequests.remove(uniqueId).getTaskId());
            this.cancelTrade(player, room, side.getOther(), self, other);
            player.sendMessage("§aYou have cancelled the trade.");
            player.sendMessage("§6Please remember to pick up your items that may have fallen on the ground before you leave the trade room.");
            return false;
        }
        
        return false;
    }
    
    public void playerJoin(@NotNull final UUID uniqueId) {
        
        this.recentJoins.add(uniqueId);
        final Offline offline = this.offlines.get(uniqueId);
        
        if (offline != null) {
            
            final CompleteReason reason = offline.getReason() == null ? CompleteReason.ERROR : offline.getReason();
            this.scheduler.runTaskLater(this, () -> {
                
                this.recentJoins.remove(uniqueId);
                final Player player = this.server.getPlayer(uniqueId);
                if (player == null || !player.isOnline()) {
                    return;
                }
                
                this.offlines.remove(uniqueId);
                player.sendMessage(reason.getMessage());
                
                final Location teleport = offline.getTeleport();
                if (teleport != null) {
                    player.teleport(teleport);
                    player.sendMessage("§aYou have been moved outside of the trade room.");
                }
                
                final Inventory inventory = offline.getInventory();
                if (inventory != null) {
                    this.transferItems(player, inventory, reason);
                }
                
                this.deleteOffline(offline);
                
            }, 60L);
            
            return;
        }
        
        this.scheduler.runTaskLater(this, () -> {
            
            this.recentJoins.remove(uniqueId);
            final Player player = this.server.getPlayer(uniqueId);
            if (player == null || !player.isOnline()) {
                return;
            }
            
            if (this.ignoredJoins.remove(uniqueId)) {
                return;
            }
            
            TradeRoom room = null;
            Trader self = null;
            Side side = null;
            for (final TradeRoom check : this.tradeRooms.values()) {
                
                final Trader check1 = check.getTrader1();
                final Trader check2 = check.getTrader2();
                
                if (check1 != null && uniqueId.equals(check1.getUniqueId())) {
                    room = check;
                    self = check1;
                    side = Side.SIDE_1;
                    break;
                } else if (check2 != null && uniqueId.equals(check2.getUniqueId())) {
                    room = check;
                    self = check2;
                    side = Side.SIDE_2;
                    break;
                }
            }
            
            if (self == null) {
                return;
            }
            
            self.setOffline(false);
            
            final Trader other = side == Side.SIDE_1 ? room.getTrader2() : room.getTrader1();
            if (other == null) {
                player.sendMessage("§aYou have re-joined your trade. There is no one currently trading with you.");
                this.saveRoom(player, room);
                return;
            }
            
            final Player otherPlayer = this.server.getPlayer(other.getUniqueId());
            if (otherPlayer == null || !otherPlayer.isOnline()) {
                
                if (!other.isOffline()) {
                    other.setOffline(true);
                }
                
                player.sendMessage("§aYou have re-joined your trade.");
                player.sendMessage("§6" + other.getName() + " is offline, the trade will restart when the log back in.");
                player.sendMessage("§6If they do not log in within the next§r §b" + this.formatTime(other.getLogoutTime()) + "§r§6, the trade will automatically be cancelled.");
                
                this.saveRoom(player, room);
                return;
            }
            
            if (other.isOffline()) {
                other.setOffline(false);
            }
            
            player.sendMessage("§aYou have re-joined your trade. " + other.getName() + " is online, you can continue where you left off.");
            otherPlayer.sendMessage("§a" + self.getName() + " has logged in and re-joined your trade, you can continue where you left off.");
            
            this.saveRoom(player, room);
            
        }, 60L);
    }
    
    public void playerLeave(@NotNull final UUID uniqueId) {
        
        TradeRoom room = null;
        Trader self = null;
        Side side = null;
        for (final TradeRoom check : this.tradeRooms.values()) {
            
            final Trader check1 = check.getTrader1();
            final Trader check2 = check.getTrader2();
            
            if (check1 != null && uniqueId.equals(check1.getUniqueId())) {
                room = check;
                self = check1;
                side = Side.SIDE_1;
                break;
            } else if (check2 != null && uniqueId.equals(check2.getUniqueId())) {
                room = check;
                self = check2;
                side = Side.SIDE_2;
                break;
            }
        }
        
        if (self == null) {
            return;
        }
        
        self.setOffline(true);
        
        final Trader other = side == Side.SIDE_1 ? room.getTrader2() : room.getTrader1();
        if (other == null) {
            this.saveRoom(this.console, room);
            return;
        }
        
        final Player otherPlayer = this.server.getPlayer(other.getUniqueId());
        if (otherPlayer == null || !otherPlayer.isOnline()) {
            
            if (!other.isOffline()) {
                other.setOffline(true);
            }
            
            this.saveRoom(this.console, room);
            return;
        }
        
        if (other.isOffline()) {
            other.setOffline(false);
        }
        
        otherPlayer.sendMessage("§a" + self.getName() + " has logged out during your trade. The trade will automatically resume when the log back in.");
        otherPlayer.sendMessage("§6If they do not log in within the next§r §b" + this.formatTime(self.getLogoutTime()) + "§r§6, the trade will automatically be cancelled.");
        
        this.saveRoom(this.console, room);
    }
    
    /////////////////////
    // COMMAND METHODS //
    /////////////////////
    
    public boolean tradeRoomExists(@NotNull final String name) {
        return this.tradeRooms.containsKey(name.toLowerCase());
    }
    
    public void startBuilder(@NotNull final Player player) {
        
        final TradeRoomBuilder builder = this.builders.get(player.getUniqueId());
        if (builder != null) {
            player.sendMessage("§cYou have already started a trade room builder.");
            player.sendMessage("§cYou are currently waiting on step§r §6" + builder.getStep().name() + "§r§c.");
            return;
        }
        
        this.builders.put(player.getUniqueId(), TradeRoom.newBuilder(this, player));
        player.sendMessage("§aYou have started to build a new trade room.");
        player.sendMessage("§aPlease enter the name of the trade room with the command§r §b/tradeadmin setname§r §6<name>§r§a.");
    }
    
    public boolean stopBuilder(@NotNull final Player player) {
        
        if (!this.builders.containsKey(player.getUniqueId())) {
            player.sendMessage("§cYou have not started a trade room builder.");
            player.sendMessage("§cYou can start a new trade room builder with the command§r §b/tradeadmin startbuilder§r§c.");
            return false;
        }
        
        this.builders.remove(player.getUniqueId());
        player.sendMessage("§aYou have stopped your active trade room builder.");
        return true;
    }
    
    public void resetBuilder(@NotNull final Player player) {
        if (this.stopBuilder(player)) {
            this.startBuilder(player);
        }
    }
    
    public void setName(@NotNull final Player player, @NotNull final String name) {
        
        final TradeRoomBuilder builder = this.builders.get(player.getUniqueId());
        if (builder == null) {
            player.sendMessage("§cYou do not have an active trade room builder.");
            player.sendMessage("§cPlease start a trade room builder with the command§r §b/tradeadmin startbuilder§r§a.");
            return;
        }
        
        final BuildStep step = builder.getStep();
        if (step != BuildStep.NAME) {
            player.sendMessage("§cYou are not on the correct step to set the trade room's name.");
            player.sendMessage("§cYou are currently on the following step:§r §6" + step.name().toLowerCase());
            return;
        }
        
        if (!builder.setName(name)) {
            player.sendMessage("§cPlease try this step again.");
            return;
        }
        
        player.sendMessage("§aTrade room name set.§r");
        player.sendMessage("§aPlease enter the trade room region names with the command§r §b/tradeadmin setregions§r §6<region 1> <region 2>§r§a.");
    }
    
    public void setRegions(@NotNull final Player player, @NotNull final String name1, @NotNull final String name2) {
        
        final TradeRoomBuilder builder = this.builders.get(player.getUniqueId());
        if (builder == null) {
            player.sendMessage("§cYou do not have an active trade room builder.");
            player.sendMessage("§cPlease start a trade room builder with the command§r §b/trade adminstartbuilder§r§a.");
            return;
        }
        
        final BuildStep step = builder.getStep();
        if (step != BuildStep.REGIONS) {
            player.sendMessage("§cYou are not on the correct step to set the trade room's regions.");
            player.sendMessage("§cYou are currently on the following step:§r §6" + step.name().toLowerCase());
            return;
        }
        
        if (!builder.setRegions(name1, name2)) {
            player.sendMessage("§cPlease try this step again.");
            return;
        }
        
        player.sendMessage("§aTrade room regions set.§r");
        player.sendMessage("§aPlease left click on the chest to use as the 1st trade chest.");
        player.sendMessage("§6MAKE SURE YOU ARE NOT IN CREATIVE MODE (Survival is recommended).");
    }
    
    public void setTeleport(@NotNull final Player player) {
        
        final TradeRoomBuilder builder = this.builders.get(player.getUniqueId());
        if (builder == null) {
            player.sendMessage("§cYou do not have an active trade room builder.");
            player.sendMessage("§cPlease start a trade room builder with the command§r §b/tradeadmin startbuilder§r§a.");
            return;
        }
        
        final BuildStep step = builder.getStep();
        if (step == BuildStep.TELEPORT_IN_1) {
            
            if (!builder.setTeleportIn1(player.getLocation())) {
                player.sendMessage("§cPlease try this step again.");
                return;
            }
            
            player.sendMessage("§aEntry teleport location 1 set.§r");
            player.sendMessage("§aPlease set the trade room 1st exit teleport location with the command§r §b/tradeadmin setteleport§r§a.");
            return;
        }
        
        if (step == BuildStep.TELEPORT_OUT_1) {
            
            if (!builder.setTeleportOut1(player.getLocation())) {
                player.sendMessage("§cPlease try this step again.");
                return;
            }
            
            player.sendMessage("§aExit teleport location 1 set.§r");
            player.sendMessage("§aPlease left click on the button to use as the 1st entry button.");
            player.sendMessage("§6MAKE SURE YOU ARE NOT IN CREATIVE MODE (Survival is recommended).");
            return;
        }
        
        if (step == BuildStep.TELEPORT_IN_2) {
            
            if (!builder.setTeleportIn2(player.getLocation())) {
                player.sendMessage("§cPlease try this step again.");
                return;
            }
            
            player.sendMessage("§aEntry teleport location 2 set.§r");
            player.sendMessage("§aPlease set the trade room 2nd exit teleport location with the command§r §b/tradeadmin setteleport§r§a.");
            return;
        }
        
        if (step == BuildStep.TELEPORT_OUT_2) {
            
            if (!builder.setTeleportOut2(player.getLocation())) {
                player.sendMessage("§cPlease try this step again.");
                return;
            }
            
            player.sendMessage("§aExit teleport location 2 set.§r");
            player.sendMessage("§aPlease left click on the button to use as the 2nd entry button.");
            player.sendMessage("§6MAKE SURE YOU ARE NOT IN CREATIVE MODE (Survival is recommended).");
            return;
        }
        
        player.sendMessage("§cYou are not on the correct step to set the trade room's teleport locations.");
        player.sendMessage("§cYou are currently on the following step:§r §6" + step.name().toLowerCase());
    }
    
    private void startTrade(@NotNull final Player player, @NotNull final TradeRoom room, @NotNull final Side side) {
        
        if (this.recentJoins.contains(player.getUniqueId())) {
            this.ignoredJoins.add(player.getUniqueId());
        }
        
        final Trader self = new Trader(player);
        final Trader other;
        
        if (side == Side.SIDE_1) {
            player.teleport(room.getTeleportIn1());
            room.setTrader1(self);
            other = room.getTrader2();
        } else {
            player.teleport(room.getTeleportIn2());
            room.setTrader2(self);
            other = room.getTrader1();
        }
        
        final Player otherPlayer = other == null ? null : this.server.getPlayer(other.getUniqueId());
        
        if (other == null) {
            player.sendMessage("§aYou have started a trade.");
            player.sendMessage("§6When another player joins your trade, you can begin adding your items to the chest.");
            room.setStatus(TradeStatus.WAIT);
        } else if (otherPlayer == null || !otherPlayer.isOnline()) {
            
            if (!other.isOffline()) {
                other.setOffline(true);
            }
            room.setStatus(TradeStatus.PREPARE);
            
            player.sendMessage("§aYou have started a trade with " + other.getName() + ".");
            player.sendMessage("§6They are currently offline currently. You can begin putting items in your chest when they log back in.");
            player.sendMessage("§6If they do not log in with in the next " + this.formatTime(other.getLogoutTime()) + ", the trade will be automatically cancelled.");
            
        } else {
            
            if (other.isOffline()) {
                other.setOffline(false);
            }
            room.setStatus(TradeStatus.PREPARE);
            
            player.sendMessage("§aYou have started a trade with " + other.getName() + ".");
            player.sendMessage("§aYou may begin adding items to the chest.");
            otherPlayer.sendMessage("§a" + self.getName() + " has started trading with you.");
            otherPlayer.sendMessage("§aYou may now begin adding items to the chest.");
            
        }
        
        this.saveRoom(player, room);
    }
    
    public void acceptTrade(@NotNull final Player player, @NotNull final TradeRoom room, @NotNull final Side otherSide, @NotNull final Trader self, @NotNull final Trader other) {
        
        final Player otherPlayer = this.server.getPlayer(other.getUniqueId());
        if (otherPlayer != null && otherPlayer.isOnline()) {
            
            if (other.isOffline()) {
                other.setOffline(false);
            }
            
            otherPlayer.sendMessage("§a" + self.getName() + " has accepted the trade.");
            player.sendMessage("§aSwapping items...");
            otherPlayer.sendMessage("§aSwapping items...");
            
            room.swapItems(player, otherPlayer);
            room.setStatus(TradeStatus.COMPLETE);
            this.saveRoom(player, room);
            this.deleteChestInventories(player, room);
            
            if (this.tradeInventories.containsKey(self.getUniqueId())) {
                if (this.checkInventories(player.getOpenInventory().getTopInventory(), this.tradeInventories.remove(self.getUniqueId()))) {
                    player.closeInventory();
                }
            }
            if (this.tradeInventories.containsKey(other.getUniqueId())) {
                if (this.checkInventories(otherPlayer.getOpenInventory().getTopInventory(), this.tradeInventories.remove(other.getUniqueId()))) {
                    otherPlayer.closeInventory();
                }
            }
            
            return;
        }
        
        player.sendMessage("§a" + other.getName() + " has accepted the trade before they went offline.");
        player.sendMessage("§aSwapping items...");
        
        final Offline offline = new Offline(other);
        offline.setReason(CompleteReason.ACCEPTED);
        
        final Inventory extraOther = room.swapItems(player, offline);
        room.setStatus(TradeStatus.COMPLETE);
        
        if (otherSide == Side.SIDE_1) {
            offline.setTeleport(room.getTeleportOut1());
            room.setTrader1(null);
        } else {
            offline.setTeleport(room.getTeleportOut2());
            room.setTrader2(null);
        }
        
        this.saveRoom(player, room);
        this.deleteChestInventories(player, room);
        
        this.offlines.put(offline.getUniqueId(), offline);
        this.saveOffline(offline, extraOther);
        
        if (this.tradeInventories.containsKey(self.getUniqueId())) {
            if (this.checkInventories(player.getOpenInventory().getTopInventory(), this.tradeInventories.remove(self.getUniqueId()))) {
                player.closeInventory();
            }
        }
        this.tradeInventories.remove(offline.getUniqueId());
    }
    
    public void rejectTrade(@NotNull final Player player, @NotNull final TradeRoom room, @NotNull final Side otherSide, @NotNull final Trader self, @NotNull final Trader other) {
        
        final Player otherPlayer = this.server.getPlayer(other.getUniqueId());
        if (otherPlayer != null && otherPlayer.isOnline()) {
            
            if (other.isOffline()) {
                other.setOffline(false);
            }
            
            otherPlayer.sendMessage("§c" + self.getName() + " has rejected the trade.");
            player.sendMessage("§6Returning items...");
            otherPlayer.sendMessage("§6Returning items...");
            
            room.returnItems(player, otherPlayer);
            room.setStatus(TradeStatus.COMPLETE);
            this.saveRoom(player, room);
            this.deleteChestInventories(player, room);
            
            if (this.tradeInventories.containsKey(self.getUniqueId())) {
                if (this.checkInventories(player.getOpenInventory().getTopInventory(), this.tradeInventories.remove(self.getUniqueId()))) {
                    player.closeInventory();
                }
            }
            if (this.tradeInventories.containsKey(other.getUniqueId())) {
                if (this.checkInventories(otherPlayer.getOpenInventory().getTopInventory(), this.tradeInventories.remove(other.getUniqueId()))) {
                    otherPlayer.closeInventory();
                }
            }
            
            return;
        }
        
        player.sendMessage("§6" + other.getName() + " will be notified that the trade was rejected when the next log in.");
        player.sendMessage("§6Returning items...");
        
        final Offline offline = new Offline(other);
        offline.setReason(CompleteReason.REJECTED);
        
        final Inventory extra = room.returnItems(player, offline);
        room.setStatus(TradeStatus.COMPLETE);
        
        if (otherSide == Side.SIDE_1) {
            offline.setTeleport(room.getTeleportOut1());
            room.setTrader1(null);
        } else {
            offline.setTeleport(room.getTeleportOut2());
            room.setTrader2(null);
        }
        
        this.saveRoom(player, room);
        this.deleteChestInventories(player, room);
        
        this.offlines.put(offline.getUniqueId(), offline);
        this.saveOffline(offline, extra);
        
        if (this.tradeInventories.containsKey(self.getUniqueId())) {
            if (this.checkInventories(player.getOpenInventory().getTopInventory(), this.tradeInventories.remove(self.getUniqueId()))) {
                player.closeInventory();
            }
        }
        this.tradeInventories.remove(offline.getUniqueId());
    }
    
    public void cancelTrade(@NotNull final Player player, @NotNull final TradeRoom room, @NotNull final Side otherSide, @NotNull final Trader self, @Nullable final Trader other) {
        
        final Player otherPlayer = other == null ? null : this.server.getPlayer(other.getUniqueId());
        if (otherPlayer != null && otherPlayer.isOnline()) {
            
            if (other.isOffline()) {
                other.setOffline(false);
            }
            
            otherPlayer.sendMessage("§c" + self.getName() + " has cancelled the trade.");
            player.sendMessage("§6Returning items...");
            otherPlayer.sendMessage("§6Returning items...");
            
            room.returnItems(player, otherPlayer);
            room.setStatus(TradeStatus.COMPLETE);
            this.saveRoom(player, room);
            this.deleteChestInventories(player, room);
            
            if (this.tradeInventories.containsKey(self.getUniqueId())) {
                if (this.checkInventories(player.getOpenInventory().getTopInventory(), this.tradeInventories.remove(self.getUniqueId()))) {
                    player.closeInventory();
                }
            }
            if (this.tradeInventories.containsKey(other.getUniqueId())) {
                if (this.checkInventories(otherPlayer.getOpenInventory().getTopInventory(), this.tradeInventories.remove(other.getUniqueId()))) {
                    otherPlayer.closeInventory();
                }
            }
            
            return;
        }
        
        if (other != null) {
            player.sendMessage("§6" + other.getName() + " will be notified that the trade was cancelled when the next log in.");
        }
        player.sendMessage("§6Returning items...");
        
        final Offline offline = other == null ? null : new Offline(other);
        if (offline != null) {
            offline.setReason(CompleteReason.CANCELLED);
        }
        
        final Inventory extraOther = room.returnItems(player, offline);
        room.setStatus(TradeStatus.COMPLETE);
        
        if (offline != null) {
            if (otherSide == Side.SIDE_1) {
                offline.setTeleport(room.getTeleportOut1());
                room.setTrader1(null);
            } else {
                offline.setTeleport(room.getTeleportOut2());
                room.setTrader2(null);
            }
        }
        
        this.saveRoom(player, room);
        this.deleteChestInventories(player, room);
        
        if (offline != null) {
            this.offlines.put(offline.getUniqueId(), offline);
        }
        this.saveOffline(offline, extraOther);
        
        if (this.tradeInventories.containsKey(self.getUniqueId())) {
            if (this.checkInventories(player.getOpenInventory().getTopInventory(), this.tradeInventories.remove(self.getUniqueId()))) {
                player.closeInventory();
            }
        }
        if (offline != null) {
            this.tradeInventories.remove(offline.getUniqueId());
        }
    }
    
    ////////////////////
    // HELPER METHODS //
    ////////////////////
    
    private void displayNewTrade(@NotNull final Player player, @NotNull final Location location, @NotNull final TradeRoom room, @NotNull final TradeStatus status, @NotNull final Trader self, @NotNull final Trader other) {
        
        final Player otherPlayer = this.server.getPlayer(other.getUniqueId());
        if (otherPlayer == null || !otherPlayer.isOnline()) {
            player.sendMessage("§6Please wait for " + other.getName() + " to log back in. When they do, the trade screen will open automatically.");
            return;
        }
        
        player.sendMessage("§aYou may now decide on the trade.");
        otherPlayer.sendMessage("§a" + self.getName() + " has finished selecting what they want to trade. You may now decide on the trade.");
        
        final Inventory selfInventory = room.createTradeInventory(this.server, self.getUniqueId());
        if (selfInventory == null) {
            player.sendMessage("§cAn unknown error occurred while attempting to display your trade inventory. Please report it to the system administrators.");
            otherPlayer.sendMessage("§cAn unknown error occurred while attempting to display " + self.getName() + "'s trade inventory. Please report it to the system administrators.");
            this.logger.log(Level.WARNING, "Trade inventory not created for trader 1.");
            this.logger.log(Level.WARNING, "Trade Room: " + room.getName());
            this.logger.log(Level.WARNING, "Player UUID: " + player.getUniqueId().toString());
            this.logger.log(Level.WARNING, "Player Name: " + player.getName());
            this.logger.log(Level.WARNING, "Activating World: " + (location.getWorld() == null ? "null" : location.getWorld().getName()));
            this.logger.log(Level.WARNING, "Activating X: " + location.getBlockX());
            this.logger.log(Level.WARNING, "Activating Y: " + location.getBlockY());
            this.logger.log(Level.WARNING, "Activating Z: " + location.getBlockZ());
            this.logger.log(Level.WARNING, "Trade Status: " + status.name());
            this.logger.log(Level.WARNING, "Trader 1 UUID: " + self.getUniqueId().toString());
            this.logger.log(Level.WARNING, "Trader 1 Name: " + self.getName());
            this.logger.log(Level.WARNING, "Trader 2 UUID: " + other.getUniqueId().toString());
            this.logger.log(Level.WARNING, "Trader 2 Name: " + other.getName());
            return;
        }
        
        final Inventory otherInventory = room.createTradeInventory(this.server, other.getUniqueId());
        if (otherInventory == null) {
            player.sendMessage("§cAn unknown error occurred while attempting to display " + other.getName() + "'s trade inventory. Please report it to the system administrators.");
            otherPlayer.sendMessage("§cAn unknown error occurred while attempting to display your trade inventory. Please report it to the system administrators.");
            this.logger.log(Level.WARNING, "Trade inventory not created for trader 2.");
            this.logger.log(Level.WARNING, "Trade Room: " + room.getName());
            this.logger.log(Level.WARNING, "Player UUID: " + player.getUniqueId().toString());
            this.logger.log(Level.WARNING, "Player Name: " + player.getName());
            this.logger.log(Level.WARNING, "Activating World: " + (location.getWorld() == null ? "null" : location.getWorld().getName()));
            this.logger.log(Level.WARNING, "Activating X: " + location.getBlockX());
            this.logger.log(Level.WARNING, "Activating Y: " + location.getBlockY());
            this.logger.log(Level.WARNING, "Activating Z: " + location.getBlockZ());
            this.logger.log(Level.WARNING, "Trade Status: " + status.name());
            this.logger.log(Level.WARNING, "Trader 1 UUID: " + self.getUniqueId().toString());
            this.logger.log(Level.WARNING, "Trader 1 Name: " + self.getName());
            this.logger.log(Level.WARNING, "Trader 2 UUID: " + other.getUniqueId().toString());
            this.logger.log(Level.WARNING, "Trader 2 Name: " + other.getName());
            return;
        }
        
        player.openInventory(otherInventory);
        otherPlayer.openInventory(selfInventory);
        
        this.tradeInventories.put(self.getUniqueId(), otherInventory);
        this.tradeInventories.put(other.getUniqueId(), selfInventory);
    }
    
    private void transferItems(@NotNull final Player player, @NotNull final Inventory from, @NotNull final CompleteReason reason) {
        
        final Inventory to = player.getInventory();
        final ItemStack[] toItems = to.getStorageContents();
        final ItemStack[] fromItems = from.getStorageContents();
        
        boolean slotOpen = true;
        for (int f = 0; f < fromItems.length && slotOpen; f++) {
            
            boolean moved = false;
            ItemStack fromItem = fromItems[f];
            if (fromItem == null || fromItem.getType() == Material.AIR) {
                continue;
            }
            
            for (int t = 0; t < toItems.length; t++) {
                
                final ItemStack slot = toItems[t];
                if (slot == null || slot.getType() == Material.AIR) {
                    continue;
                }
                
                final int available = slot.getMaxStackSize() - slot.getAmount();
                if (available == 0) {
                    continue;
                }
                
                if (slot.getType() != fromItem.getType()) {
                    continue;
                }
                
                if (!slot.getItemMeta().equals(fromItem.getItemMeta())) {
                    continue;
                }
                
                final int moveable = Math.min(fromItem.getAmount(), available);
                final int remaining = fromItem.getAmount() - moveable;
                toItems[t] = new ItemStack(slot.getType(), slot.getAmount() + moveable);
                
                if (remaining == 0) {
                    fromItems[f] = null;
                    moved = true;
                    break;
                }
                
                fromItem = new ItemStack(fromItem.getType(), remaining);
                fromItems[f] = fromItem;
            }
            
            if (moved) {
                continue;
            }
            
            for (int t = 0; t < toItems.length && !moved; t++) {
                
                final ItemStack slot = toItems[t];
                if (slot != null && slot.getType() != Material.AIR) {
                    continue;
                }
                
                toItems[t] = fromItem;
                fromItems[f] = null;
                moved = true;
            }
            
            if (!moved) {
                slotOpen = false;
            }
        }
        
        to.setStorageContents(toItems);
        
        boolean dropRequired = false;
        if (!slotOpen) {
            for (final ItemStack item : fromItems) {
                if (item != null && item.getType() != Material.AIR) {
                    dropRequired = true;
                    break;
                }
            }
        }
        
        if (reason == CompleteReason.ACCEPTED) {
            player.sendMessage("§aThe items that you received as part of the trade have been placed in your inventory.");
        } else {
            player.sendMessage("§cThe items that you placed in the chest have been returned to you.");
        }
        
        if (!dropRequired) {
            from.setStorageContents(fromItems);
            return;
        }
        
        player.sendMessage("§6Some of the items could not be put in your inventory. Be sure to pick them up.");
        final Location location = player.getLocation().add(new Vector(0.0D, 1.0D, 0.0D));
        final World world = player.getWorld();
        
        for (int slot = 0; slot < fromItems.length; slot++) {
            final ItemStack item = fromItems[slot];
            if (item != null && item.getType() != Material.AIR) {
                world.dropItemNaturally(location, item);
                fromItems[slot] = null;
            }
        }
        
        from.setStorageContents(fromItems);
    }
    
    @NotNull
    private String formatTime(final long logoutTime) {
        
        final long now = System.currentTimeMillis();
        final long expire = logoutTime + OFFLINE_TIMEOUT;
        
        final StringBuilder builder = new StringBuilder();
        if (now >= expire) {
            builder.append("0 seconds");
            return builder.toString();
        }
        
        long diff = expire - now;
        if (diff % 1000L != 0L) {
            diff += 1000L - (diff % 1000L);
        }
        diff /= 1000L;
        
        final long minutes = diff / 60L;
        final long seconds = diff % 60L;
        
        if (minutes > 1L) {
            builder.append(minutes).append(" minutes");
        } else if (minutes == 1L) {
            builder.append(minutes).append(" minute");
        }
        
        if (!builder.toString().isEmpty() && seconds == 0L) {
            return builder.toString();
        }
        
        if (!builder.isEmpty()) {
            builder.append(", ");
        }
        
        builder.append(seconds).append(" second");
        if (seconds != 1L) {
            builder.append("s");
        }
        
        return builder.toString();
    }
    
    private boolean checkInventories(@NotNull final Inventory inventory, @NotNull final Inventory tradeInventory) {
        
        final ItemStack[] inventoryItems = inventory.getStorageContents();
        final ItemStack[] tradeInventoryItems = tradeInventory.getStorageContents();
        
        if (inventoryItems.length != tradeInventoryItems.length) {
            return false;
        }
        
        for (int checkSlot = 0; checkSlot < inventoryItems.length; checkSlot++) {
            if (inventoryItems[checkSlot] == null) {
                if (tradeInventoryItems[checkSlot] != null) {
                    return false;
                }
            } else if (tradeInventoryItems[checkSlot] == null) {
                return false;
            } else if (!inventoryItems[checkSlot].equals(tradeInventoryItems[checkSlot])) {
                return false;
            }
        }
        
        return true;
    }
    
    /////////////////
    // FILE SAVING //
    /////////////////
    
    private void saveRoom(@NotNull final CommandSender sender, @NotNull final TradeRoom room) {
        this.saveRoom(sender, room, false);
    }
    
    private void saveRoom(@NotNull final CommandSender sender, @NotNull final TradeRoom room, final boolean shutdown) {
        
        final Runnable runnable = () -> {
            synchronized (this.tradeRoomFolder) {
                
                final File tradeRoomFile = new File(this.tradeRoomFolder, room.getName() + Constants.FILE_TYPE);
                final FileConfiguration config = room.getConfig();
                
                try {
                    if (!tradeRoomFile.exists()) {
                        if (!tradeRoomFile.createNewFile()) {
                            sender.sendMessage("There was an error while updating the TradeChest. Please report this error to a server administrator.");
                            this.logger.log(Level.WARNING, "ISSUE WHILE SAVING TRADECHEST FILE");
                            this.logger.log(Level.WARNING, "Details below:");
                            this.logger.log(Level.WARNING, "TradeChest File Location: " + tradeRoomFile.getPath());
                            this.logger.log(Level.WARNING, "TradeChest YAML Data: " + config.toString());
                            this.logger.log(Level.WARNING, "ISSUE:");
                            this.logger.log(Level.WARNING, "TradeChest file not created successfully.");
                            return;
                        }
                    }
                } catch (SecurityException | IOException e) {
                    sender.sendMessage("There was an error while updating the TradeChest. Please report this error to a server administrator.");
                    this.logger.log(Level.WARNING, "ISSUE WHILE SAVING TRADECHEST FILE");
                    this.logger.log(Level.WARNING, "Details below:");
                    this.logger.log(Level.WARNING, "TradeChest File Location: " + tradeRoomFile.getPath());
                    this.logger.log(Level.WARNING, "TradeChest YAML Data: " + config.toString());
                    this.logger.log(Level.WARNING, "ISSUE:");
                    this.logger.log(Level.WARNING, "Unable to create TradeChest file.");
                    this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                    return;
                }
                
                
                try {
                    config.save(tradeRoomFile);
                } catch (IOException | IllegalArgumentException e) {
                    sender.sendMessage("There was an error while updating the TradeChest. Please report this error to a server administrator.");
                    this.logger.log(Level.WARNING, "ISSUE WHILE SAVING TRADECHEST FILE");
                    this.logger.log(Level.WARNING, "Details below:");
                    this.logger.log(Level.WARNING, "TradeChest File Location: " + tradeRoomFile.getPath());
                    this.logger.log(Level.WARNING, "TradeChest YAML Data: " + config.toString());
                    this.logger.log(Level.WARNING, "ISSUE:");
                    this.logger.log(Level.WARNING, "Unable to save TradeChest configuration file.");
                    this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                }
            }
        };
        
        if (shutdown) {
            runnable.run();
        } else {
            this.scheduler.runTaskAsynchronously(this, runnable);
        }
    }
    
    private void saveChestInventories(@NotNull final CommandSender sender, @NotNull final TradeRoom room) {
        
        this.scheduler.runTaskAsynchronously(this, () -> {
            synchronized (this.backupInventoryFolder) {
                
                final List<Map<String, Object>> items1 = new ArrayList<Map<String, Object>>();
                for (final ItemStack item : room.getChest1().getInventory().getStorageContents()) {
                    items1.add(item == null ? null : item.serialize());
                }
                final List<Map<String, Object>> items2 = new ArrayList<Map<String, Object>>();
                for (final ItemStack item : room.getChest2().getInventory().getStorageContents()) {
                    items2.add(item == null ? null : item.serialize());
                }
                
                final File backupInventoryFile = new File(this.backupInventoryFolder, room.getName() + Constants.FILE_TYPE);
                try {
                    if (!backupInventoryFile.exists()) {
                        if (!backupInventoryFile.createNewFile()) {
                            sender.sendMessage("§cThere was an error while updating your trade. Please report this error to a server administrator.");
                            this.logger.log(Level.WARNING, "ISSUE WHILE SAVING BACKUP INVENTORY FILE");
                            this.logger.log(Level.WARNING, "Details below:");
                            this.logger.log(Level.WARNING, "Backup Inventory File Location: " + backupInventoryFile.getPath());
                            this.logger.log(Level.WARNING, "Backup Inventory YAML Data:");
                            this.logger.log(Level.WARNING, "Trade Room Name:" + room.getName());
                            this.logger.log(Level.WARNING, "Chest 1 Items: " + items1.toString());
                            this.logger.log(Level.WARNING, "Chest 2 Items: " + items2.toString());
                            this.logger.log(Level.WARNING, "ISSUE:");
                            this.logger.log(Level.WARNING, "Backup Inventory file not created successfully.");
                            return;
                        }
                    }
                } catch (SecurityException | IOException e) {
                    sender.sendMessage("§cThere was an error while updating your trade. Please report this error to a server administrator.");
                    this.logger.log(Level.WARNING, "ISSUE WHILE SAVING BACKUP INVENTORY FILE");
                    this.logger.log(Level.WARNING, "Details below:");
                    this.logger.log(Level.WARNING, "Backup Inventory File Location: " + backupInventoryFile.getPath());
                    this.logger.log(Level.WARNING, "Backup Inventory YAML Data:");
                    this.logger.log(Level.WARNING, "Trade Room Name:" + room.getName());
                    this.logger.log(Level.WARNING, "Chest 1 Items: " + items1.toString());
                    this.logger.log(Level.WARNING, "Chest 2 Items: " + items2.toString());
                    this.logger.log(Level.WARNING, "ISSUE:");
                    this.logger.log(Level.WARNING, "Unable to create Backup Inventory file.");
                    this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                    return;
                }
                
                final YamlConfiguration config = new YamlConfiguration();
                config.set(Constants.KEY_TRADE_ROOM_NAME, room.getName());
                config.set(Constants.KEY_BACKUP_ITEMS_1, items1);
                config.set(Constants.KEY_BACKUP_ITEMS_2, items2);
                
                try {
                    config.save(backupInventoryFile);
                } catch (IOException | IllegalArgumentException e) {
                    sender.sendMessage("§cThere was an error while updating your trade. Please report this error to a server administrator.");
                    this.logger.log(Level.WARNING, "ISSUE WHILE SAVING BACKUP INVENTORY FILE");
                    this.logger.log(Level.WARNING, "Details below:");
                    this.logger.log(Level.WARNING, "Backup Inventory File Location: " + backupInventoryFile.getPath());
                    this.logger.log(Level.WARNING, "Backup Inventory YAML Data:");
                    this.logger.log(Level.WARNING, "Trade Room Name:" + room.getName());
                    this.logger.log(Level.WARNING, "Chest 1 Items: " + items1.toString());
                    this.logger.log(Level.WARNING, "Chest 2 Items: " + items2.toString());
                    this.logger.log(Level.WARNING, "ISSUE:");
                    this.logger.log(Level.WARNING, "Unable to save Backup Inventory configuration file.");
                    this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                }
            }
        });
    }
    
    private void saveOffline(@Nullable final Offline offline, @Nullable final Inventory extra) {
        
        if (offline == null) {
            return;
        }
        
        if (extra != null) {
            
            final List<ItemStack> items = new ArrayList<ItemStack>();
            for (final ItemStack item : extra.getStorageContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    items.add(item);
                }
            }
            
            if (this.extras.containsKey(offline.getUniqueId())) {
                this.extras.get(offline.getUniqueId()).addAll(items);
            } else {
                this.extras.put(offline.getUniqueId(), items);
            }
        }
        
        this.scheduler.runTaskAsynchronously(this, () -> {
            synchronized (this.offlineFolder) {
                
                final File offlineFile = new File(this.offlineFolder, offline.getUniqueId().toString() + Constants.FILE_TYPE);
                try {
                    if (!offlineFile.exists()) {
                        if (!offlineFile.createNewFile()) {
                            this.logger.log(Level.WARNING, "ISSUE WHILE SAVING OFFLINETRADER FILE");
                            this.logger.log(Level.WARNING, "Details below:");
                            this.logger.log(Level.WARNING, "OfflineTrader File Location: " + offlineFile.getPath());
                            this.logger.log(Level.WARNING, "OfflineTrader YAML Data:");
                            this.logger.log(Level.WARNING, "uuid: " + offline.getUniqueId().toString());
                            this.logger.log(Level.WARNING, "name:" + offline.getName());
                            this.logger.log(Level.WARNING, "logout_time:" + offline.getLogoutTime());
                            this.logger.log(Level.WARNING, "complete_reason: " + (offline.getReason() == null ? "null" : offline.getReason().name()));
                            this.logger.log(Level.WARNING, "items: " + (offline.getInventory() == null ? "null" : offline.getInventory().toString()));
                            this.logger.log(Level.WARNING, "ISSUE:");
                            this.logger.log(Level.WARNING, "OfflineTrader file not created successfully.");
                            return;
                        }
                    }
                } catch (SecurityException | IOException e) {
                    this.logger.log(Level.WARNING, "ISSUE WHILE SAVING OFFLINETRADER FILE");
                    this.logger.log(Level.WARNING, "Details below:");
                    this.logger.log(Level.WARNING, "OfflineTrader File Location: " + offlineFile.getPath());
                    this.logger.log(Level.WARNING, "OfflineTrader YAML Data:");
                    this.logger.log(Level.WARNING, "uuid: " + offline.getUniqueId().toString());
                    this.logger.log(Level.WARNING, "name:" + offline.getName());
                    this.logger.log(Level.WARNING, "logout_time:" + offline.getLogoutTime());
                    this.logger.log(Level.WARNING, "complete_reason: " + (offline.getReason() == null ? "null" : offline.getReason().name()));
                    this.logger.log(Level.WARNING, "items: " + (offline.getInventory() == null ? "null" : offline.getInventory().toString()));
                    this.logger.log(Level.WARNING, "ISSUE:");
                    this.logger.log(Level.WARNING, "Unable to create OfflineTrader file.");
                    this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                    return;
                }
                
                final FileConfiguration config = offline.getConfig();
                try {
                    config.save(offlineFile);
                } catch (IOException | IllegalArgumentException e) {
                    this.logger.log(Level.WARNING, "ISSUE WHILE SAVING OFFLINETRADER FILE");
                    this.logger.log(Level.WARNING, "Details below:");
                    this.logger.log(Level.WARNING, "OfflineTrader File Location: " + offlineFile.getPath());
                    this.logger.log(Level.WARNING, "OfflineTrader YAML Data:");
                    this.logger.log(Level.WARNING, "uuid: " + offline.getUniqueId().toString());
                    this.logger.log(Level.WARNING, "name:" + offline.getName());
                    this.logger.log(Level.WARNING, "logout_time:" + offline.getLogoutTime());
                    this.logger.log(Level.WARNING, "complete_reason: " + (offline.getReason() == null ? "null" : offline.getReason().name()));
                    this.logger.log(Level.WARNING, "items: " + (offline.getInventory() == null ? "null" : offline.getInventory().toString()));
                    this.logger.log(Level.WARNING, "ISSUE:");
                    this.logger.log(Level.WARNING, "Unable to save OfflineTrader configuration file.");
                    this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                }
                
                if (extra == null) {
                    return;
                }
                
                final File extraFile = new File(this.extraFolder, offline.getUniqueId().toString() + Constants.FILE_TYPE);
                try {
                    if (!extraFile.exists()) {
                        if (!extraFile.createNewFile()) {
                            this.logger.log(Level.WARNING, "ISSUE WHILE SAVING EXTRAINVENTORY FILE");
                            this.logger.log(Level.WARNING, "Details below:");
                            this.logger.log(Level.WARNING, "ExtraInventory File Location: " + extraFile.getPath());
                            this.logger.log(Level.WARNING, "ExtraInventory YAML Data:");
                            this.logger.log(Level.WARNING, "uuid: " + offline.getUniqueId().toString());
                            this.logger.log(Level.WARNING, "items: " + extra.toString());
                            this.logger.log(Level.WARNING, "ISSUE:");
                            this.logger.log(Level.WARNING, "ExtraInventory file not created successfully.");
                            return;
                        }
                    }
                } catch (SecurityException | IOException e) {
                    this.logger.log(Level.WARNING, "ISSUE WHILE SAVING EXTRAINVENTORY FILE");
                    this.logger.log(Level.WARNING, "Details below:");
                    this.logger.log(Level.WARNING, "ExtraInventory File Location: " + extraFile.getPath());
                    this.logger.log(Level.WARNING, "ExtraInventory YAML Data:");
                    this.logger.log(Level.WARNING, "uuid: " + offline.getUniqueId().toString());
                    this.logger.log(Level.WARNING, "items: " + extra.toString());
                    this.logger.log(Level.WARNING, "ISSUE:");
                    this.logger.log(Level.WARNING, "Unable to create ExtraInventory file.");
                    this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                    return;
                }
                
                final FileConfiguration extraConfig = new YamlConfiguration();
                try {
                    extraConfig.load(extraFile);
                } catch (final IOException | InvalidConfigurationException | IllegalArgumentException e) {
                    this.logger.log(Level.WARNING, "ISSUE WHILE LOADING EXTRAINVENTORY FILE");
                    this.logger.log(Level.WARNING, "Details below:");
                    this.logger.log(Level.WARNING, "ExtraInventory File Location: " + extraFile.getPath());
                    this.logger.log(Level.WARNING, "ExtraInventory YAML Data:");
                    this.logger.log(Level.WARNING, "uuid: " + offline.getUniqueId().toString());
                    this.logger.log(Level.WARNING, "items: " + extra.toString());
                    this.logger.log(Level.WARNING, "ISSUE:");
                    this.logger.log(Level.WARNING, "Unable to load ExtraInventory file.");
                    this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                    return;
                }
                
                if (extraConfig.isSet(Constants.KEY_EXTRA_UUID)) {
                    if (!offline.getUniqueId().toString().equalsIgnoreCase(extraConfig.getString(Constants.KEY_EXTRA_UUID, null))) {
                        this.logger.log(Level.WARNING, "ISSUE WHILE VALIDATING EXTRAINVENTORY FILE");
                        this.logger.log(Level.WARNING, "Details below:");
                        this.logger.log(Level.WARNING, "ExtraInventory File Location: " + extraFile.getPath());
                        this.logger.log(Level.WARNING, "ExtraInventory YAML Data:");
                        this.logger.log(Level.WARNING, "uuid: " + offline.getUniqueId().toString());
                        this.logger.log(Level.WARNING, "items: " + extra.toString());
                        this.logger.log(Level.WARNING, "ISSUE:");
                        this.logger.log(Level.WARNING, "UUID does not match existing.");
                        return;
                    }
                } else {
                    extraConfig.set(Constants.KEY_EXTRA_UUID, offline.getUniqueId().toString());
                }
                
                List<Map<String, Object>> items = (List<Map<String, Object>>) extraConfig.getList(Constants.KEY_EXTRA_ITEMS, null);
                if (items == null) {
                    items = new ArrayList<Map<String, Object>>();
                }
                
                for (final ItemStack item : extra.getStorageContents()) {
                    if (item != null && item.getType() != Material.AIR) {
                        items.add(item.serialize());
                    }
                }
                
                extraConfig.set(Constants.KEY_EXTRA_ITEMS, items);
                
                try {
                    extraConfig.save(extraFile);
                } catch (IOException | IllegalArgumentException e) {
                    this.logger.log(Level.WARNING, "ISSUE WHILE SAVING EXTRAINVENTORY FILE");
                    this.logger.log(Level.WARNING, "Details below:");
                    this.logger.log(Level.WARNING, "ExtraInventory File Location: " + extraFile.getPath());
                    this.logger.log(Level.WARNING, "ExtraInventory YAML Data:");
                    this.logger.log(Level.WARNING, "uuid: " + offline.getUniqueId().toString());
                    this.logger.log(Level.WARNING, "items: " + extra.toString());
                    this.logger.log(Level.WARNING, "ISSUE:");
                    this.logger.log(Level.WARNING, "Unable to save ExtraInventory configuration file.");
                    this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                }
            }
        });
    }
    
    ///////////////////
    // FILE DELETION //
    ///////////////////
    
    private void deleteChestInventories(@NotNull final CommandSender sender, @NotNull final TradeRoom room) {
        
        this.scheduler.runTaskAsynchronously(this, () -> {
            synchronized (this.backupInventoryFolder) {
                
                final File backupInventoryFile = new File(this.backupInventoryFolder, room.getName() + Constants.FILE_TYPE);
                try {
                    if (backupInventoryFile.exists()) {
                        if (!backupInventoryFile.delete()) {
                            sender.sendMessage("§cThere was an error while completing your trade. Please report this error to a server administrator.");
                            this.logger.log(Level.WARNING, "ISSUE WHILE DELETING BACKUP INVENTORY FILE");
                            this.logger.log(Level.WARNING, "Details below:");
                            this.logger.log(Level.WARNING, "Backup Inventory File Location: " + backupInventoryFile.getPath());
                            this.logger.log(Level.WARNING, "ISSUE:");
                            this.logger.log(Level.WARNING, "Backup Inventory file not deleted successfully.");
                        }
                    }
                } catch (SecurityException e) {
                    sender.sendMessage("§cThere was an error while completing your trade. Please report this error to a server administrator.");
                    this.logger.log(Level.WARNING, "ISSUE WHILE DELETING BACKUP INVENTORY FILE");
                    this.logger.log(Level.WARNING, "Details below:");
                    this.logger.log(Level.WARNING, "Backup Inventory File Location: " + backupInventoryFile.getPath());
                    this.logger.log(Level.WARNING, "ISSUE:");
                    this.logger.log(Level.WARNING, "Unable to delete Backup Inventory file.");
                    this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                }
            }
        });
    }
    
    private void deleteOffline(@NotNull final Offline offline) {
        
        this.scheduler.runTaskAsynchronously(this, () -> {
            synchronized (this.offlineFolder) {
                
                final File offlineFile = new File(this.offlineFolder, offline.getUniqueId().toString() + Constants.FILE_TYPE);
                try {
                    if (offlineFile.exists()) {
                        if (!offlineFile.delete()) {
                            this.logger.log(Level.WARNING, "ISSUE WHILE DELETING OFFLINETRADER FILE");
                            this.logger.log(Level.WARNING, "Details below:");
                            this.logger.log(Level.WARNING, "OfflineTrader File Location: " + offlineFile.getPath());
                            this.logger.log(Level.WARNING, "ISSUE:");
                            this.logger.log(Level.WARNING, "OfflineTrader file not deleted successfully.");
                        }
                    }
                } catch (SecurityException e) {
                    this.logger.log(Level.WARNING, "ISSUE WHILE DELETING OFFLINETRADER FILE");
                    this.logger.log(Level.WARNING, "Details below:");
                    this.logger.log(Level.WARNING, "OfflineTrader File Location: " + offlineFile.getPath());
                    this.logger.log(Level.WARNING, "ISSUE:");
                    this.logger.log(Level.WARNING, "Unable to delete OfflineTrader file.");
                    this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                }
            }
        });
    }
}
