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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.Material;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.cubeville.trade.bukkit.command.TradeAdminCommand;
import org.cubeville.trade.bukkit.listener.TradeListener;
import org.cubeville.trade.bukkit.traderoom.BuildStep;
import org.cubeville.trade.bukkit.traderoom.Side;
import org.cubeville.trade.bukkit.traderoom.TradeRoom;
import org.cubeville.trade.bukkit.traderoom.TradeRoomBuilder;
import org.cubeville.trade.bukkit.traderoom.TradeStatus;
import org.cubeville.trade.bukkit.traderoom.Trader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TradePlugin extends JavaPlugin {
    
    private static final int SLOT_REJECT = 36;
    private static final int SLOT_ACCEPT = 44;
    
    private static final long OFFLINE_TIMEOUT = 1000L * 60L * 5L;
    
    private final Logger logger;
    private final BukkitScheduler scheduler;
    private final CommandSender console;
    
    private final File serverStopFile;
    private long serverStart;
    private long serverStop;
    
    private final File tradeRoomFolder;
    private final Map<String, TradeRoom> byName;
    private final Map<TradeRoom, TradeRoom> pairings;
    
    private final Map<UUID, TradeRoomBuilder> builders;
    
    private final File activeTradesFolder;
    private final Map<UUID, ActiveTrade> activeTrades;
    private final File backupInventoryFolder;
    private final Map<UUID, Inventory> tradeInventories;
    
    private final File offlineTraderFolder;
    private final Map<UUID, OfflineTrader> offlineTraders;
    
    public TradePlugin() {
        super();
        
        this.logger = this.getLogger();
        this.scheduler = this.getServer().getScheduler();
        this.console = this.getServer().getConsoleSender();
        
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
        if (!this.tradeRoomFolder.exists()) {
            if (!this.tradeRoomFolder.mkdirs()) {
                throw new RuntimeException("TradeChest folder not created at " + this.tradeRoomFolder.getPath());
            }
        } else if (!this.tradeRoomFolder.isDirectory()) {
            throw new RuntimeException("TradeChest folder is not a folder. Location: " + this.tradeRoomFolder.getPath());
        }
        
        this.byName = new ConcurrentHashMap<String, TradeRoom>();
        this.pairings = new HashMap<TradeRoom, TradeRoom>();
        
        this.builders = new ConcurrentHashMap<UUID, TradeRoomBuilder>();
        
        this.activeTrades = new ConcurrentHashMap<UUID, ActiveTrade>();
        this.activeTradesFolder = new File(dataFolder, Constants.FOLDER_ACTIVE_TRADES);
        if (!this.activeTradesFolder.exists()) {
            if (!this.activeTradesFolder.mkdirs()) {
                throw new RuntimeException("ActiveTrade folder not created at " + this.activeTradesFolder.getPath());
            }
        } else if (!this.activeTradesFolder.isDirectory()) {
            throw new RuntimeException("ActiveTrade folder is not a folder. Location: " + this.activeTradesFolder.getPath());
        }
        
        this.backupInventoryFolder = new File(dataFolder, Constants.FOLDER_BACKUP_INVENTORIES);
        if (!this.backupInventoryFolder.exists()) {
            if (!this.backupInventoryFolder.mkdirs()) {
                throw new RuntimeException("BackupInventory folder not created at " + this.backupInventoryFolder.getPath());
            }
        } else if (!this.backupInventoryFolder.isDirectory()) {
            throw new RuntimeException("BackupInventory folder is not a folder. Location: " + this.backupInventoryFolder.getPath());
        }
        
        this.tradeInventories = new ConcurrentHashMap<UUID, Inventory>();
        
        this.offlineTraders = new ConcurrentHashMap<UUID, OfflineTrader>();
        this.offlineTraderFolder = new File(dataFolder, Constants.FOLDER_OFFLINE_TRADERS);
        if (!this.offlineTraderFolder.exists()) {
            if (!this.offlineTraderFolder.mkdirs()) {
                throw new RuntimeException("OfflineTrader folder not created at " + this.offlineTraderFolder.getPath());
            }
        } else if (!this.offlineTraderFolder.isDirectory()) {
            throw new RuntimeException("OfflineTrader folder is not a folder. Location: " + this.offlineTraderFolder.getPath());
        }
    }
    
    @Override
    public void onEnable() {
        
        // Basic Plugin Startup //
        this.logger.log(Level.INFO, "////////////////////////////////////////////////////////////////////////////////");
        this.logger.log(Level.INFO, "// CVTrade Bukkit plugin for Minecraft Bukkit servers.                        //");
        this.logger.log(Level.INFO, "//                                                                            //");
        this.logger.log(Level.INFO, "// Copyright (C) 2021-2023 Matt Ciolkosz (https://github.com/mciolkosz/)      //");
        this.logger.log(Level.INFO, "// Copyright (C) 2021-2023 Cubeville (https://www.cubeville.org/)             //");
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
        
        this.serverStart = System.currentTimeMillis();
        
        final YamlConfiguration stopConfig = new YamlConfiguration();
        try {
            stopConfig.load(this.serverStopFile);
        } catch (final IOException | InvalidConfigurationException | IllegalArgumentException e) {
            throw new RuntimeException("Unable to load server stop file at " + this.serverStopFile.getPath(), e);
        }
        this.serverStop = stopConfig.getLong(Constants.KEY_SERVER_STOP_TIME, -1L);
        
        if (this.serverStop == -1L) {
            this.serverStop = this.serverStart - TradePlugin.OFFLINE_TIMEOUT;
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
                room = new TradeRoom(config);
            } catch (final IllegalArgumentException e) {
                this.logger.log(Level.WARNING, "Unable to deserialize trade room from file at " + tradeRoomFile.getPath());
                this.logger.log(Level.WARNING, "Skipping trade room.");
                this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                continue;
            }
            
            if (this.byName.containsKey(room.getName().toLowerCase())) {
                this.logger.log(Level.WARNING, "Conflicting name of trade room. Duplicate already registered.");
                this.logger.log(Level.WARNING, "Please remember that names are case-insensitive. Name: " + room.getName());
                this.logger.log(Level.WARNING, "Skipping trade room.");
                continue;
            }
            
            this.byName.put(room.getName().toLowerCase(), room);
        }
        
        // Active Trades
        final File[] activeTradeFiles = this.activeTradesFolder.listFiles();
        if (activeTradeFiles == null) {
            throw new RuntimeException("Cannot list active trade files, null value returned.");
        }
        for (final File activeTradeFile : activeTradeFiles) {
            
            final YamlConfiguration config = new YamlConfiguration();
            try {
                config.load(activeTradeFile);
            } catch (final IOException | InvalidConfigurationException | IllegalArgumentException e) {
                this.logger.log(Level.WARNING, "Unable to load active trade file at " + activeTradeFile.getPath());
                this.logger.log(Level.WARNING, "Skipping active trade.");
                this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                continue;
            }
            
            final ActiveTrade active;
            try {
                active = new ActiveTrade(this, config);
            } catch (final IllegalArgumentException e) {
                this.logger.log(Level.WARNING, "Unable to deserialize active trade from file at " + activeTradeFile.getPath());
                this.logger.log(Level.WARNING, "Skipping active trade.");
                this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                continue;
            }
            
            this.activeTrades.put(active.getUniqueId1(), active);
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
            
            final TradeRoom room = this.byName.get(roomName);
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
            
            final Inventory backupInventory1 = this.getServer().createInventory(null, 27);
            final Inventory backupInventory2 = this.getServer().createInventory(null, 27);
            
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
                } else if (chest1Inventory.getStorageContents()[slot] == null) {
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
                } else if (chest2Inventory.getStorageContents()[slot] == null) {
                    chest2Items[slot] = backupItems2[slot];
                } else if (!backupItems2[slot].equals(chest2Items[slot])) {
                    chest2Items[slot] = backupItems2[slot];
                }
            }
            
            chest1Inventory.setStorageContents(chest1Items);
            chest2Inventory.setStorageContents(chest2Items);
        }
        
        for (final ActiveTrade activeTrade : this.activeTrades.values()) {
            if (activeTrade.getStatus() == ActiveTrade.TradeStatus.DECIDE) {
                this.tradeInventories.put(activeTrade.getUniqueId1(), this.createTradeInventory(activeTrade.getRoom().getChest1().getInventory()));
            }
        }
        
        // Offline Players Initialization //
        final File[] offlineTraderFiles = this.offlineTraderFolder.listFiles();
        if (offlineTraderFiles == null) {
            throw new RuntimeException("Cannot list offline trader files, null value returned.");
        }
        for (final File offlineTraderFile : offlineTraderFiles) {
            
            final YamlConfiguration config = new YamlConfiguration();
            try {
                config.load(offlineTraderFile);
            } catch (final IOException | InvalidConfigurationException | IllegalArgumentException e) {
                this.logger.log(Level.WARNING, "Unable to load offline trader file at " + offlineTraderFile.getPath());
                this.logger.log(Level.WARNING, "Skipping offline trader.");
                this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                continue;
            }
            
            final OfflineTrader trader;
            try {
                trader = new OfflineTrader(this.getServer(), config);
            } catch (final IllegalArgumentException e) {
                this.logger.log(Level.WARNING, "Unable to deserialize offline trader from file at " + offlineTraderFile.getPath());
                this.logger.log(Level.WARNING, "Skipping offline trader.");
                this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                continue;
            }
            
            this.offlineTraders.put(trader.getUniqueId(), trader);
        }
        
        // Commands //
        
        this.registerCommand("tradeadmin", new TradeAdminCommand(this));
        
        // Server Events & Tasks //
        
        this.getServer().getPluginManager().registerEvents(new TradeListener(this), this);
        
        this.scheduler.scheduleSyncRepeatingTask(this, () -> {
            
            for (final OfflineTrader offlineTrader : this.offlineTraders.values()) {
                
                final long now = System.currentTimeMillis();
                if (offlineTrader.getCompleteReason() != null) {
                    continue;
                } else if (now - TradePlugin.OFFLINE_TIMEOUT < this.serverStart) {
                    final long diff = this.serverStop - offlineTrader.getLogoutTime();
                    if (this.serverStart + (TradePlugin.OFFLINE_TIMEOUT - diff) >= now) {
                        continue;
                    }
                } else if (offlineTrader.getLogoutTime() + TradePlugin.OFFLINE_TIMEOUT >= now) {
                    continue;
                }
    
                final UUID offlinePlayerId = offlineTrader.getUniqueId();
                final ActiveTrade offlineTrade = this.activeTrades.get(offlinePlayerId);
                final TradeRoom offlineRoom = offlineTrade.getRoom();
                
                offlineTrader.setCompleteReason(OfflineTrader.CompleteReason.OFFLINE_SELF);
                offlineTrader.setInventory(offlineTrade.getRoom().getChest1().getInventory());
                this.saveOfflineTrader(this.console, offlineTrader);
                
                this.activeTrades.remove(offlinePlayerId);
                this.deleteActiveTrade(this.console, offlineTrade);
                offlineRoom.getChest1().getInventory().clear();
                this.deleteChestInventories(this.console, offlineRoom);
                
                final TradeRoom otherRoom = this.pairings.get(offlineRoom);
                if (otherRoom == null) {
                    continue;
                }
                
                final Iterator<ActiveTrade> tradeIterator = this.activeTrades.values().iterator();
                while (tradeIterator.hasNext()) {
                    
                    final ActiveTrade otherTrade = tradeIterator.next();
                    if (!otherTrade.getRoom().equals(otherRoom)) {
                        continue;
                    }
                    
                    final UUID otherPlayerId = otherTrade.getUniqueId1();
                    final Player otherPlayer = this.getServer().getPlayer(otherPlayerId);
                    
                    if (otherPlayer != null && otherPlayer.isOnline()) {
                        
                        otherPlayer.sendMessage("§cYour trade with§r §6" + offlineTrade.getName1() + "§r §chas been cancelled as they have been offline for too long.");
                        this.itemTransfer(otherPlayer, otherTrade.getRoom().getChest1().getInventory());
                        otherPlayer.sendMessage("§cYour items that were placed in the chest have been returned to you.");
                        
                        this.deleteActiveTrade(otherPlayer, otherTrade);
                        this.deleteChestInventories(otherPlayer, otherRoom);
                        tradeIterator.remove();
                        continue;
                    }
                    
                    OfflineTrader otherOfflineTrader = this.offlineTraders.get(otherPlayerId);
                    if (otherOfflineTrader == null) {
                        
                        final long offlineNow = System.currentTimeMillis();
                        long logoutTime = offlineNow - TradePlugin.OFFLINE_TIMEOUT;
                        if (logoutTime < this.serverStart) {
                            final long diff = offlineNow - this.serverStart;
                            logoutTime = this.serverStop - (TradePlugin.OFFLINE_TIMEOUT - diff);
                        }
                        otherOfflineTrader = new OfflineTrader(otherPlayerId, otherTrade.getName1(), logoutTime);
                    }
                    
                    if (otherOfflineTrader.getCompleteReason() == null) {
                        otherOfflineTrader.setCompleteReason(OfflineTrader.CompleteReason.OFFLINE_OTHER);
                        otherOfflineTrader.setInventory(otherTrade.getRoom().getChest1().getInventory());
                        this.offlineTraders.put(otherPlayerId, otherOfflineTrader);
                        this.saveOfflineTrader(this.console, otherOfflineTrader);
                    }
                    
                    this.deleteActiveTrade(this.console, otherTrade);
                    otherRoom.getChest1().getInventory().clear();
                    this.deleteChestInventories(this.console, otherRoom);
                    tradeIterator.remove();
                }
            }
        }, 10L * 20L, 10L * 20L);
    }
    
    private void registerCommand(@NotNull final String commandName, @NotNull final TabExecutor tabExecutor) throws RuntimeException {
        
        final PluginCommand command = this.getCommand(commandName);
        if (command == null) {
            throw new RuntimeException("Cannot find the command /" + commandName);
        }
        command.setExecutor(tabExecutor);
        command.setTabCompleter(tabExecutor);
    }
    
    @Override
    public void onDisable() {
        
        for (final Player player : this.getServer().getOnlinePlayers()) {
            
            final UUID playerId = player.getUniqueId();
            final ActiveTrade activeTrade = this.activeTrades.get(playerId);
            if (activeTrade == null) {
                continue;
            }
            
            final OfflineTrader offlineTrader = new OfflineTrader(player);
            this.offlineTraders.put(playerId, offlineTrader);
            
            final String playerName = offlineTrader.getName();
            final long logoutTime = offlineTrader.getLogoutTime();
            final OfflineTrader.CompleteReason completeReason = offlineTrader.getCompleteReason();
            final Inventory inventory = offlineTrader.getInventory();
            
            final List<Map<String, Object>> items;
            if (inventory == null) {
                items = null;
            } else {
                items = new ArrayList<Map<String, Object>>();
                for (final ItemStack item : inventory.getStorageContents()) {
                    items.add(item == null ? null : item.serialize());
                }
            }
            
            final File offlineTraderFile = new File(this.offlineTraderFolder, playerId.toString() + ".yml");
            try {
                if (!offlineTraderFile.exists()) {
                    if (!offlineTraderFile.createNewFile()) {
                        this.console.sendMessage("§cThere was an error while updating your returnable items. Please report this to a server administrator.");
                        this.logger.log(Level.WARNING, "ISSUE WHILE SAVING OFFLINETRADER FILE");
                        this.logger.log(Level.WARNING, "Details below:");
                        this.logger.log(Level.WARNING, "OfflineTrader File Location: " + offlineTraderFile.getPath());
                        this.logger.log(Level.WARNING, "OfflineTrader YAML Data:");
                        this.logger.log(Level.WARNING, "uuid: " + playerId.toString());
                        this.logger.log(Level.WARNING, "name:" + playerName);
                        this.logger.log(Level.WARNING, "logout_time:" + logoutTime);
                        this.logger.log(Level.WARNING, "complete_reason: " + (completeReason == null ? "null" : completeReason.name()));
                        this.logger.log(Level.WARNING, "items: " + (items == null ? "null" : items.toString()));
                        this.logger.log(Level.WARNING, "ISSUE:");
                        this.logger.log(Level.WARNING, "OfflineTrader file not created successfully.");
                        return;
                    }
                }
            } catch (SecurityException | IOException e) {
                this.console.sendMessage("§cThere was an error while updating your returnable items. Please report this to a server administrator.");
                this.logger.log(Level.WARNING, "ISSUE WHILE SAVING OFFLINETRADER FILE");
                this.logger.log(Level.WARNING, "Details below:");
                this.logger.log(Level.WARNING, "OfflineTrader File Location: " + offlineTraderFile.getPath());
                this.logger.log(Level.WARNING, "OfflineTrader YAML Data:");
                this.logger.log(Level.WARNING, "uuid: " + playerId.toString());
                this.logger.log(Level.WARNING, "name:" + playerName);
                this.logger.log(Level.WARNING, "logout_time:" + logoutTime);
                this.logger.log(Level.WARNING, "complete_reason: " + (completeReason == null ? "null" : completeReason.name()));
                this.logger.log(Level.WARNING, "items: " + (items == null ? "null" : items.toString()));
                this.logger.log(Level.WARNING, "ISSUE:");
                this.logger.log(Level.WARNING, "Unable to create OfflineTrader file.");
                this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                return;
            }
            
            final YamlConfiguration config = new YamlConfiguration();
            config.set("uuid", playerId.toString());
            config.set("name", playerName);
            config.set("logout_time", logoutTime);
            config.set("complete_reason", completeReason == null ? null : completeReason.name());
            config.set("items", items);
            try {
                config.save(offlineTraderFile);
            } catch (IOException | IllegalArgumentException e) {
                this.console.sendMessage("§cThere was an error while updating your returnable items. Please report this to a server administrator.");
                this.logger.log(Level.WARNING, "ISSUE WHILE SAVING OFFLINETRADER FILE");
                this.logger.log(Level.WARNING, "Details below:");
                this.logger.log(Level.WARNING, "OfflineTrader File Location: " + offlineTraderFile.getPath());
                this.logger.log(Level.WARNING, "OfflineTrader YAML Data:");
                this.logger.log(Level.WARNING, "uuid: " + playerId.toString());
                this.logger.log(Level.WARNING, "name:" + playerName);
                this.logger.log(Level.WARNING, "logout_time:" + logoutTime);
                this.logger.log(Level.WARNING, "complete_reason: " + (completeReason == null ? "null" : completeReason.name()));
                this.logger.log(Level.WARNING, "items: " + (items == null ? "null" : items.toString()));
                this.logger.log(Level.WARNING, "ISSUE:");
                this.logger.log(Level.WARNING, "Unable to save OfflineTrader configuration file.");
                this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
            }
        }
        
        this.serverStop = System.currentTimeMillis();
        final YamlConfiguration stopConfig = new YamlConfiguration();
        stopConfig.set("server_stop_time", this.serverStop);
        try {
            stopConfig.save(this.serverStopFile);
        } catch (IOException | IllegalArgumentException e) {
            this.logger.log(Level.WARNING, "ISSUE WHILE SAVING SERVER STOP TIME.");
            this.logger.log(Level.WARNING, "Details below:");
            this.logger.log(Level.WARNING, "server_stop_time:" + this.serverStop);
            this.logger.log(Level.WARNING, "ISSUE:");
            this.logger.log(Level.WARNING, "Attempted to save the Server Stop file.");
            this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
        }
    }
    
    ///////////////////////////
    // EVENT HANDLER METHODS //
    ///////////////////////////
    
    public boolean blockBreak(@NotNull final Location location) {
        
        for (final TradeRoom room : this.byName.values()) {
            if (room.contains(location)) {
                return true;
            }
        }
        
        return false;
    }
    
    public boolean blockPlace(@NotNull final Chest chest) {
        
        final Location location = chest.getLocation();
        
        for (final TradeRoom room : this.byName.values()) {
            
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
        
        final UUID playerId = player.getUniqueId();
        final ActiveTrade activeTrade = this.activeTrades.get(playerId);
        if (activeTrade == null) {
            return false;
        }
    
        if (activeTrade.getStatus() != ActiveTrade.TradeStatus.DECIDE) {
            return false;
        }
        
        final Inventory tradeInventory = this.tradeInventories.get(playerId);
        if (tradeInventory == null) {
            player.sendMessage("§cThere was an error with your trade. Please report it to a server administrator.");
            player.sendMessage("§cIf you are not trading or the trade inventory did not appear, please report this to a server administrator as well.");
            this.logger.log(Level.WARNING, "ISSUE WHILE DECIDING ON A TRADE");
            this.logger.log(Level.WARNING, "Details below:");
            this.logger.log(Level.WARNING, "Player: " + player.getName());
            this.logger.log(Level.WARNING, "UUID: " + playerId.toString());
            this.logger.log(Level.WARNING, "TradeChest: " + activeTrade.getRoom().getName());
            this.logger.log(Level.WARNING, "Trade Status: " + activeTrade.getStatus().name());
            this.logger.log(Level.WARNING, "ISSUE:");
            this.logger.log(Level.WARNING, "Player " + player.getName() + " is attempting to decide on a trade.");
            this.logger.log(Level.WARNING, "No trade inventory found for player in DECIDE or later phase.");
            return false;
        }
        
        if (!this.checkInventories(inventory, tradeInventory)) {
            return false;
        }
        
        if (slot == TradePlugin.SLOT_REJECT) {
            this.cancelTrade(player, false);
        } else if (slot == TradePlugin.SLOT_ACCEPT) {
            this.acceptTrade(player);
        }
        
        return true;
    }
    
    @NotNull
    public Collection<String> playerCommandSend(@NotNull final Player player) {
        
        final HashSet<String> removals = new HashSet<String>();
        for (final String commandName : this.getDescription().getCommands().keySet()) {
            
            removals.add("cvtrade:" + commandName);
            if (!player.hasPermission(this.getServer().getPluginCommand(commandName).getPermission())) {
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
            
            this.byName.put(room.getName().toLowerCase(), room);
            this.saveTradeRoom(player, room);
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
        for (final TradeRoom check : this.byName.values()) {
            if (check.contains(location)) {
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
                    player.sendMessage("§cIf you wish to trade with this player, please use the room on the other side. Otherwise, wait for the player to complete their trade.");
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
                player.sendMessage("§cIf you wish to trade with this player, please use the room on the other side. Otherwise, wait for the player to complete their trade.");
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
        
        // Player is outside, start trade on side 1
        this.startTrade(player, room, side);
        return false;
    }
    
    private boolean handleInsideRoom(@NotNull final Player player, @NotNull final BlockState state, @NotNull final TradeRoom room, @NotNull final Side side) {
        
        final Location location = state.getLocation();
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
            this.logger.log(Level.WARNING, "Trader 1 UUID: " + (trader1 == null ? "null" : trader1.getUniqueId().toString()));
            this.logger.log(Level.WARNING, "Trader 1 Name: " + (trader1 == null ? "null" : trader1.getName()));
            this.logger.log(Level.WARNING, "Trader 1 Status: " + (trader1 == null ? "null" : trader1.getStatus().name()));
            this.logger.log(Level.WARNING, "Trader 2 UUID: " + (trader2 == null ? "null" : trader2.getUniqueId().toString()));
            this.logger.log(Level.WARNING, "Trader 2 Name: " + (trader2 == null ? "null" : trader2.getName()));
            this.logger.log(Level.WARNING, "Trader 2 Status: " + (trader2 == null ? "null" : trader2.getStatus().name()));
            
            return true;
        }
        
        final UUID uniqueId = player.getUniqueId();
        final TradeStatus status = self.getStatus();
        
        if (room.isChest(location, side)) {
            
            if (other == null) {
                player.sendMessage("§cPlease wait to put your items in the chest until another player begins a trade with you.");
                return true;
            }
            
            if (status == TradeStatus.PREPARE) {
                return false;
            }
            
            if (status == TradeStatus.LOCKED) {
                player.sendMessage("§cYou may not open the chest, as you have locked your trade in.");
                player.sendMessage("§cIf you wish to change your items, please cancel the trade and then re-start it.");
                return true;
            }
            
            if (status == TradeStatus.DECIDE) {
                
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
                    this.logger.log(Level.WARNING, "Trader 1 UUID: " + self.getUniqueId().toString());
                    this.logger.log(Level.WARNING, "Trader 1 Name: " + self.getName());
                    this.logger.log(Level.WARNING, "Trader 1 Status: " + self.getStatus().name());
                    this.logger.log(Level.WARNING, "Trader 2 UUID: " + other.getUniqueId().toString());
                    this.logger.log(Level.WARNING, "Trader 2 Name: " + other.getName());
                    this.logger.log(Level.WARNING, "Trader 2 Status: " + other.getStatus().name());
                    return true;
                }
                
                if (!this.checkInventories(player.getOpenInventory().getTopInventory(), tradeInventory)) {
                    player.closeInventory();
                    player.openInventory(tradeInventory);
                }
                
                return true;
            }
            
            player.sendMessage("§6You cannot open the chest as you have already accepted the trade.");
            player.sendMessage("§6Please wait for the other player to make their decision.");
            return true;
        }
        
        if (room.isButtonOut(location, side)) {
            
            
        }
        
        if (room.isButtonLock(location, side)) {
            
            
        }
        
        if (room.isButtonAccept(location, side)) {
            
            
        }
        
        if (room.isButtonDeny(location, side)) {
            
            
        }
        
        return false;
    }
    
    public void playerJoin(@NotNull final Player player) {
        
        final UUID playerId = player.getUniqueId();
        final OfflineTrader offlineTrader = this.offlineTraders.remove(playerId);
        if (offlineTrader == null) {
            return;
        }
        
        final OfflineTrader.CompleteReason completeReason = offlineTrader.getCompleteReason();
        if (completeReason != null) {
            
            this.scheduler.scheduleSyncDelayedTask(this, () -> {
                
                //TODO: Check to see if this may throw null pointer if player is
                //      offline and is thus null 3 seconds later, when this runs.
                if (!player.isOnline()) {
                    this.offlineTraders.put(playerId, offlineTrader);
                    return;
                }
                
                player.sendMessage(completeReason.getMessage());
                
                final Inventory inventory = offlineTrader.getInventory();
                if (inventory == null) {
                    this.deleteOfflineTrader(player, offlineTrader);
                    return;
                }
                
                this.itemTransfer(player, inventory);
                if (completeReason == OfflineTrader.CompleteReason.ACCEPTED) {
                    player.sendMessage("§aThe items that you received as part of the trade have been placed in your inventory.");
                } else {
                    player.sendMessage("§cThe items that you placed in the chest have been returned to you.");
                }
                
                this.deleteOfflineTrader(player, offlineTrader);
            }, 3L * 20L);
            
            return;
        }
        
        this.scheduler.scheduleSyncDelayedTask(this, () -> {
            
            //TODO: Check to see if this may throw null pointer if player is
            //      offline and is thus null 3 seconds later, when this runs.
            if (!player.isOnline()) {
                this.offlineTraders.put(playerId, offlineTrader);
                return;
            }
            
            this.deleteOfflineTrader(player, offlineTrader);
            
            final ActiveTrade activeTrade = this.activeTrades.get(playerId);
            if (activeTrade == null) {
                
                player.sendMessage("§cThere was an error resuming your trade. Please report this to a server administrator.");
                this.logger.log(Level.WARNING, "ISSUE WHILE AUTOMATICALLY RESTARTING TRADE DURING PLAYER JOIN");
                this.logger.log(Level.WARNING, "Details below:");
                this.logger.log(Level.WARNING, "Player Name: " + player.getName());
                this.logger.log(Level.WARNING, "Player UUID: " + playerId.toString());
                this.logger.log(Level.WARNING, "ISSUE:");
                this.logger.log(Level.WARNING, "Player " + player.getName() + " has rejoined the server during a trade.");
                this.logger.log(Level.WARNING, "Player is OfflineTrader, no ActiveTrade found.");
                
                return;
            }
            
            final TradeRoom room = activeTrade.getRoom();
            final TradeRoom linkedRoom = this.pairings.get(room);
            if (linkedRoom == null) {
                
                player.sendMessage("§cThere was an error resuming your trade. Please report this to a server administrator.");
                this.logger.log(Level.WARNING, "ISSUE WHILE AUTOMATICALLY RESTARTING TRADE DURING PLAYER JOIN");
                this.logger.log(Level.WARNING, "Details below:");
                this.logger.log(Level.WARNING, "Player Name: " + player.getName());
                this.logger.log(Level.WARNING, "Player UUID: " + playerId.toString());
                this.logger.log(Level.WARNING, "TradeChest: " + room.getName());
                this.logger.log(Level.WARNING, "Configured Linked TradeChest: " + room.getLinked());
                this.logger.log(Level.WARNING, "ISSUE:");
                this.logger.log(Level.WARNING, "Player " + player.getName() + " has rejoined the server during a trade.");
                this.logger.log(Level.WARNING, "No linked TradeChest to the one listed above.");
                
                this.itemTransfer(player, room.getChest1().getInventory());
                if (this.tradeInventories.containsKey(playerId)) {
                    if (this.checkInventories(player.getOpenInventory().getTopInventory(), this.tradeInventories.get(playerId))) {
                        player.closeInventory();
                    }
                    this.tradeInventories.remove(playerId);
                }
                
                this.activeTrades.remove(playerId);
                this.deleteActiveTrade(player, activeTrade);
                this.deleteChestInventories(player, room);
                player.sendMessage("§cYour trade has been automatically cancelled. Any items you put in the chest have been automatically returned to you.");
                
                return;
            }
            
            ActiveTrade linkedTrade = null;
            for (final ActiveTrade checkTrade : this.activeTrades.values()) {
                if (checkTrade.getRoom().equals(linkedRoom)) {
                    linkedTrade = checkTrade;
                    break;
                }
            }
            
            if (linkedTrade == null) {
                return;
            }
            
            final UUID otherPlayerId = linkedTrade.getUniqueId1();
            final Player otherPlayer = this.getServer().getPlayer(otherPlayerId);
            if (otherPlayer != null && otherPlayer.isOnline()) {
                
                player.sendMessage("§aYou were trading with§r §6" + otherPlayer.getName() + "§r§a.");
                player.sendMessage("§aYour trade has been restarted where you left off.");
                
                otherPlayer.sendMessage("§6" + player.getName() + "§r §ahas logged back in.");
                otherPlayer.sendMessage("§aYour trade can continue.");
                
                if (activeTrade.getStatus() == ActiveTrade.TradeStatus.DECIDE) {
                    player.openInventory(this.tradeInventories.get(playerId));
                }
                if (linkedTrade.getStatus() == ActiveTrade.TradeStatus.DECIDE) {
                    otherPlayer.openInventory(this.tradeInventories.get(otherPlayerId));
                }
                
                return;
            }
            
            OfflineTrader otherOfflineTrader = this.offlineTraders.get(otherPlayerId);
            if (otherOfflineTrader == null) {
                
                player.sendMessage("§cThere was an error resuming your trade. Please report this to a server administrator.");
                this.logger.log(Level.WARNING, "ISSUE WHILE AUTOMATICALLY RESTARTING TRADE DURING PLAYER JOIN");
                this.logger.log(Level.WARNING, "Details below:");
                this.logger.log(Level.WARNING, "Player Name: " + player.getName());
                this.logger.log(Level.WARNING, "Player UUID: " + playerId.toString());
                this.logger.log(Level.WARNING, "TradeChest: " + room.getName());
                this.logger.log(Level.WARNING, "Linked TradeChest: " + linkedRoom.getName());
                this.logger.log(Level.WARNING, "Other Player Name: " + linkedTrade.getName1());
                this.logger.log(Level.WARNING, "Other Player UUID: " + otherPlayerId.toString());
                this.logger.log(Level.WARNING, "ISSUE:");
                this.logger.log(Level.WARNING, "Player " + player.getName() + " has rejoined the server during a trade.");
                this.logger.log(Level.WARNING, "Other player is not online, has an ActiveTrade, but does not have an OfflineTrader object.");
                
                if (this.tradeInventories.containsKey(playerId)) {
                    if (this.checkInventories(player.getOpenInventory().getTopInventory(), this.tradeInventories.get(playerId))) {
                        player.closeInventory();
                    }
                    this.tradeInventories.remove(playerId);
                }
                
                this.itemTransfer(player, room.getChest1().getInventory());
                this.activeTrades.remove(playerId);
                this.deleteActiveTrade(player, activeTrade);
                this.deleteChestInventories(player, room);
                player.sendMessage("§cYour trade has been automatically cancelled. Any items you put in the chest have been automatically returned to you.");
                
                final long now = System.currentTimeMillis();
                long logoutTime = now - TradePlugin.OFFLINE_TIMEOUT;
                if (logoutTime < this.serverStart) {
                    final long diff = now - this.serverStart;
                    logoutTime = this.serverStop - (TradePlugin.OFFLINE_TIMEOUT - diff);
                }
                
                otherOfflineTrader = new OfflineTrader(otherPlayerId, linkedTrade.getName1(), logoutTime);
                otherOfflineTrader.setCompleteReason(OfflineTrader.CompleteReason.ERROR);
                otherOfflineTrader.setInventory(linkedRoom.getChest1().getInventory());
                this.offlineTraders.put(otherPlayerId, otherOfflineTrader);
                this.saveOfflineTrader(this.console, otherOfflineTrader);
                
                this.activeTrades.remove(otherPlayerId);
                this.deleteActiveTrade(this.console, linkedTrade);
                linkedRoom.getChest1().getInventory().clear();
                this.deleteChestInventories(this.console, linkedRoom);
                
                return;
            }
            
            player.sendMessage("§b" + otherOfflineTrader.getName() + "§r §6is offline.");
            player.sendMessage("§6The trade will resume where it left off when they log back on.");
            player.sendMessage("§6If they do not log in within the next§r §b" + this.formatTime(otherOfflineTrader.getLogoutTime()) + "§r§6, the trade will automatically be cancelled.");
        }, 3L * 20L);
    }
    
    public void playerLeave(@NotNull final Player player) {
        
        final UUID playerId = player.getUniqueId();
        final ActiveTrade activeTrade = this.activeTrades.get(playerId);
        if (activeTrade == null) {
            return;
        }
        
        final OfflineTrader offlineTrader = new OfflineTrader(player);
        this.offlineTraders.put(playerId, offlineTrader);
        
        final TradeRoom room = activeTrade.getRoom();
        
/*        // No linked chest, this is some type of error.
        if (linkedRoom == null) {
            
            this.logger.log(Level.WARNING, "ISSUE WHILE PLAYER LEFT DURING A TRADE");
            this.logger.log(Level.WARNING, "Details below:");
            this.logger.log(Level.WARNING, "Player Name: " + player.getName());
            this.logger.log(Level.WARNING, "Player UUID: " + playerId.toString());
            this.logger.log(Level.WARNING, "TradeChest: " + room.getName());
            this.logger.log(Level.WARNING, "Configured Linked TradeChest: " + room.getLinked());
            this.logger.log(Level.WARNING, "ISSUE:");
            this.logger.log(Level.WARNING, "Player " + player.getName() + " has left during a trade.");
            this.logger.log(Level.WARNING, "No linked TradeChest to the one listed above.");
            
            this.tradeInventories.remove(playerId);
            
            offlineTrader.setCompleteReason(OfflineTrader.CompleteReason.ERROR);
            offlineTrader.setInventory(room.getChest1().getInventory());
            this.saveOfflineTrader(this.console, offlineTrader);
            
            this.activeTrades.remove(playerId);
            this.deleteActiveTrade(this.console, activeTrade);
            room.getChest1().getInventory().clear();
            this.deleteChestInventories(this.console, room);
            return;
        }*/
        
        this.saveOfflineTrader(this.console, offlineTrader);
        
        ActiveTrade linkedTrade = null;
        for (final ActiveTrade checkTrade : this.activeTrades.values()) {
            if (checkTrade.getRoom().equals(linkedRoom)) {
                linkedTrade = checkTrade;
                break;
            }
        }
        
        // No one else has started to trade using the linked chest.
        if (linkedTrade == null) {
            return;
        }
        
        // Someone else has started to trade using the linked chest.
        final UUID otherPlayerId = linkedTrade.getUniqueId1();
        final Player otherPlayer = this.getServer().getPlayer(otherPlayerId);
        if (otherPlayer != null && otherPlayer.isOnline()) {
            otherPlayer.sendMessage("§b" + player.getName() + "§r §6has logged out. They have§r §b" + this.formatTime(offlineTrader.getLogoutTime()) + "§r §6to log back in, or your trade with them will be automatically cancelled.");
            otherPlayer.sendMessage("§6You may also cancel the trade early yourself.");
        }
    }
    
    /////////////////////
    // COMMAND METHODS //
    /////////////////////
    
    public boolean tradeRoomExists(@NotNull final String name) {
        return this.byName.containsKey(name.toLowerCase());
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
            
            if (!builder.setTeleportOut1(player.getLocation())) {
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
    
    public boolean isTrading(@NotNull final UUID playerId) {
        return this.activeTrades.containsKey(playerId);
    }
    
    public void listRooms(@NotNull final Player player, final boolean small) {
        
        player.sendMessage("§8================================");
        player.sendMessage("§6Trade Chests§r §f-§r §b(" + this.byName.size() + ")");
        player.sendMessage("§8--------------------------------");
        
        if (small) {
            final StringBuilder builder = new StringBuilder();
            final Iterator<TradeRoom> iterator = this.byName.values().iterator();
            while (iterator.hasNext()) {
                builder.append("§a").append(iterator.next().getName());
                if (iterator.hasNext()) {
                    builder.append(", §b").append(iterator.next().getName());
                }
                if (iterator.hasNext()) {
                    builder.append(", ");
                }
            }
            
            if (builder.toString().isEmpty()) {
                builder.append("§cNone");
            }
            
            player.sendMessage(builder.toString());
            player.sendMessage("§8================================");
            return;
        }
        
        if (this.byName.values().isEmpty()) {
            player.sendMessage("§cNone");
        } else {
            for (final TradeRoom room : this.byName.values()) {
                player.sendMessage(" §f-§r §a" + room.getName());
            }
        }
        
        player.sendMessage("§8================================");
    }
    
    /*public void infoChest(@NotNull final Player sender, @NotNull final String name) {
        
        final TradeRoom room = this.byName.get(name);
        if (room == null) {
            sender.sendMessage("§cThe TradeChest§r §6" + name + "§r §cdoes not exist. Please verify that you spelled the name correctly.");
            return;
        }
        
        final Location location = room.getChest().getLocation();
        ActiveTrade activeTrade = null;
        for (final ActiveTrade checkTrade : this.activeTrades.values()) {
            if (checkTrade.getRoom().equals(room)) {
                activeTrade = checkTrade;
                break;
            }
        }
        
        final TradeChest linkedChest = this.pairings.get(room);
        ActiveTrade linkedTrade = null;
        if (linkedChest != null) {
            for (final ActiveTrade checkTrade : this.activeTrades.values()) {
                if (checkTrade.getRoom().equals(linkedChest)) {
                    linkedTrade = checkTrade;
                    break;
                }
            }
        }
        
        sender.sendMessage("§8================================");
        
        sender.sendMessage("§fName:§r §6" + room.getName());
        
        sender.sendMessage("§8--------------------------------");
        
        sender.sendMessage("§fWorld:§r §b" + location.getWorld().getName());
        sender.sendMessage("§fX-Coord:§r §b" + location.getBlockX());
        sender.sendMessage("§fY-Coord:§r §b" + location.getBlockY());
        sender.sendMessage("§fZ-Coord:§r §b" + location.getBlockZ());
        
        sender.sendMessage("§8--------------------------------");
        
        sender.sendMessage("§fIs Linked:§r §" + (linkedChest == null ? "cNo" : "aYes"));
        if (linkedChest != null) {
            sender.sendMessage("§fLinked Chest:§r §b" + linkedChest.getName());
        }
        
        sender.sendMessage("§8--------------------------------");
        
        sender.sendMessage("§fIs in Active Trade:§r §" + (activeTrade == null ? "cNo" : "aYes"));
        if (activeTrade != null) {
            sender.sendMessage("§fPlayer Name:§r §b" + activeTrade.getName());
            sender.sendMessage("§fPlayer UUID:§r §b" + activeTrade.getUniqueId().toString());
            sender.sendMessage("§fTrade Status:§r §b" + activeTrade.getStatus().name());
        }
        
        sender.sendMessage("§8--------------------------------");
        
        if (linkedChest != null) {
            sender.sendMessage("§fLinked TradeChest is in Active Trade:§r §" + (linkedTrade == null ? "cNo" : "aYes"));
            if (linkedTrade != null) {
                sender.sendMessage("§fLinked Player Name:§r §b" + linkedTrade.getName());
                sender.sendMessage("§fLinked Player UUID:§r §b" + linkedTrade.getUniqueId().toString());
                sender.sendMessage("§fLinked Trade Status:§r §b" + linkedTrade.getStatus().name());
            }
        }
        
        sender.sendMessage("§8================================");
    }*/
    
    private void startTrade(@NotNull final Player player, @NotNull final TradeRoom room) {
        
        final UUID uniqueId = player.getUniqueId();
        final TradeRoom linkedRoom = this.pairings.get(room);
        if (linkedRoom == null) {
            
            player.sendMessage("§cThe trade room that you selected is not linked to another trade room. Please report this to the server administrators.");
            this.logger.log(Level.WARNING, "ISSUE WHILE STARTING A TRADE");
            this.logger.log(Level.WARNING, "Details below:");
            this.logger.log(Level.WARNING, "Player Name: " + player.getName());
            this.logger.log(Level.WARNING, "Player UUID: " + uniqueId.toString());
            this.logger.log(Level.WARNING, "Trade Room: " + room.getName());
            this.logger.log(Level.WARNING, "ISSUE:");
            this.logger.log(Level.WARNING, "Player " + player.getName() + " is attempting to start a trade.");
            this.logger.log(Level.WARNING, "No linked trade room to the one listed above.");
            return;
        }
        
        for (final ActiveTrade check : this.activeTrades.values()) {
            if (check.getRoom().equals(room)) {
                player.sendMessage("§cThe trade room you selected is already in use in another trade. Please pick a different room to use.");
                return;
            }
        }
        
        ActiveTrade linkedTrade = null;
        for (final ActiveTrade check : this.activeTrades.values()) {
            if (check.getRoom().equals(linkedRoom)) {
                linkedTrade = check;
                break;
            }
        }
        
        if (linkedTrade == null) {
            
            player.teleport(room.getTeleportIn1());
            player.sendMessage("§6Waiting for the other player to begin the trade...");
            player.sendMessage("§aYou may begin placing the items you wish to trade into the chest.");
            
            final ActiveTrade activeTrade = new ActiveTrade(player, room);
            this.activeTrades.put(uniqueId, activeTrade);
            this.saveActiveTrade(player, activeTrade);
            
            return;
        }
        
        final UUID linkedUniqueId = linkedTrade.getUniqueId1();
        final Player linkedPlayer = this.getServer().getPlayer(linkedUniqueId);
        if (linkedPlayer != null && linkedPlayer.isOnline()) {
            
            final ActiveTrade activeTrade = new ActiveTrade(player, room);
            this.activeTrades.put(uniqueId, activeTrade);
            this.saveActiveTrade(player, activeTrade);
            
            player.teleport(room.getTeleportIn1());
            player.sendMessage("§aYou are trading with§r §6" + linkedPlayer.getName() + "§r§a.");
            player.sendMessage("§aYou may begin placing the items you wish to trade into the chest.");
            linkedPlayer.sendMessage("§aYou are now trading with§r §6" + player.getName() + "§r§a.");
            return;
        }
        
        OfflineTrader linkedOfflineTrader = this.offlineTraders.get(linkedUniqueId);
        if (linkedOfflineTrader == null) {
            
            player.sendMessage("§cThere was an error starting your trade. Please report this to a server administrator.");
            this.logger.log(Level.WARNING, "ISSUE WHILE STARTING A TRADE");
            this.logger.log(Level.WARNING, "Details below:");
            this.logger.log(Level.WARNING, "Player Name: " + player.getName());
            this.logger.log(Level.WARNING, "Player UUID: " + uniqueId.toString());
            this.logger.log(Level.WARNING, "TradeChest: " + room.getName());
            this.logger.log(Level.WARNING, "Linked TradeChest: " + linkedRoom.getName());
            this.logger.log(Level.WARNING, "Other Player Name: " + linkedTrade.getName1());
            this.logger.log(Level.WARNING, "Other Player UUID: " + linkedUniqueId.toString());
            this.logger.log(Level.WARNING, "ISSUE:");
            this.logger.log(Level.WARNING, "Player " + player.getName() + " is attempting to start a trade.");
            this.logger.log(Level.WARNING, "Other player is not online, has an ActiveTrade, but does not have an OfflineTrader object.");
            
            this.itemTransfer(player, room.getChest1().getInventory());
            player.sendMessage("§cYour trade has been automatically cancelled. Any items you put in the chest have been automatically returned to you.");
            
            final long now = System.currentTimeMillis();
            long logoutTime = now - TradePlugin.OFFLINE_TIMEOUT;
            if (logoutTime < this.serverStart) {
                final long diff = now - this.serverStart;
                logoutTime = this.serverStop - (TradePlugin.OFFLINE_TIMEOUT - diff);
            }
            
            linkedOfflineTrader = new OfflineTrader(linkedUniqueId, linkedTrade.getName1(), logoutTime);
            linkedOfflineTrader.setCompleteReason(OfflineTrader.CompleteReason.ERROR);
            linkedOfflineTrader.setInventory(linkedRoom.getChest1().getInventory());
            this.offlineTraders.put(linkedUniqueId, linkedOfflineTrader);
            this.saveOfflineTrader(this.console, linkedOfflineTrader);
            
            this.activeTrades.remove(linkedUniqueId);
            this.deleteActiveTrade(this.console, linkedTrade);
            linkedRoom.getChest1().getInventory().clear();
            this.deleteChestInventories(this.console, linkedRoom);
            return;
        }
        
        final ActiveTrade activeTrade = new ActiveTrade(player, room);
        this.activeTrades.put(uniqueId, activeTrade);
        this.saveActiveTrade(player, activeTrade);
        
        player.sendMessage("§6You are trading with§r §b" + linkedOfflineTrader.getName() + "§r§6; Please note that they are offline currently.");
        player.sendMessage("§6If they do not log in within the next§r §b" + this.formatTime(linkedOfflineTrader.getLogoutTime()) + "§r§6, the trade will automatically be cancelled.");
        player.sendMessage("§aYou may begin placing the items you wish to trade into the chest.");
    }
    
    public void cancelTrade(@NotNull final Player player, final boolean cancel) {
        
        final UUID playerId = player.getUniqueId();
        final ActiveTrade activeTrade = this.activeTrades.get(playerId);
        if (activeTrade == null) {
            player.sendMessage("§cYou are not currently in a trade with anyone.");
            return;
        }
        
        final TradeRoom room = activeTrade.getRoom();
        final TradeRoom linkedRoom = this.pairings.get(room);
        if (linkedRoom == null) {
            
            player.sendMessage("§cThere was an error while " + (cancel ? "cancelling" : "rejecting") + " your trade, please report this to a server administrator.");
            this.logger.log(Level.WARNING, "ISSUE WHILE CANCELLING/REJECTING A TRADE");
            this.logger.log(Level.WARNING, "Details below:");
            this.logger.log(Level.WARNING, "Player Name: " + player.getName());
            this.logger.log(Level.WARNING, "Player UUID: " + playerId.toString());
            this.logger.log(Level.WARNING, "TradeChest: " + room.getName());
            this.logger.log(Level.WARNING, "Configured Linked TradeChest: " + room.getLinked());
            this.logger.log(Level.WARNING, "Cancel/Reject: " + (cancel ? "CANCEL" : "REJECT"));
            this.logger.log(Level.WARNING, "ISSUE:");
            this.logger.log(Level.WARNING, "Player " + player.getName() + " is attempting to cancel a trade.");
            this.logger.log(Level.WARNING, "No linked TradeChest to the one listed above.");
            return;
        }
        
        ActiveTrade linkedTrade = null;
        for (final ActiveTrade checkTrade : this.activeTrades.values()) {
            if (checkTrade.getRoom().equals(linkedRoom)) {
                linkedTrade = checkTrade;
                break;
            }
        }
        
        if (linkedTrade == null) {
            
            this.itemTransfer(player, room.getChest1().getInventory());
            if (this.tradeInventories.containsKey(playerId)) {
                if (this.checkInventories(player.getOpenInventory().getTopInventory(), this.tradeInventories.get(playerId))) {
                    player.closeInventory();
                }
                this.tradeInventories.remove(playerId);
            }
            
            this.activeTrades.remove(playerId);
            this.deleteActiveTrade(player, activeTrade);
            this.deleteChestInventories(player, room);
            player.sendMessage("§aYour trade has been " + (cancel ? "cancelled" : "rejected") + ". Any items you put in the chest have been automatically returned to you.");
            return;
        }
        
        final UUID otherPlayerId = linkedTrade.getUniqueId1();
        final Player otherPlayer = this.getServer().getPlayer(otherPlayerId);
        if (otherPlayer != null && otherPlayer.isOnline()) {
            
            this.itemTransfer(player, room.getChest1().getInventory());
            if (this.tradeInventories.containsKey(playerId)) {
                if (this.checkInventories(player.getOpenInventory().getTopInventory(), this.tradeInventories.get(playerId))) {
                    player.closeInventory();
                }
                this.tradeInventories.remove(playerId);
            }
            
            this.activeTrades.remove(playerId);
            this.deleteActiveTrade(player, activeTrade);
            this.deleteChestInventories(player, room);
            player.sendMessage("§aYour trade has been " + (cancel ? "cancelled" : "rejected") + ". Any items you put in the chest have been automatically returned to you.");
    
            this.itemTransfer(otherPlayer, linkedRoom.getChest1().getInventory());
            if (this.tradeInventories.containsKey(otherPlayerId)) {
                if (this.checkInventories(otherPlayer.getOpenInventory().getTopInventory(), this.tradeInventories.get(otherPlayerId))) {
                    otherPlayer.closeInventory();
                }
                this.tradeInventories.remove(otherPlayerId);
            }
            
            this.activeTrades.remove(otherPlayerId);
            this.deleteActiveTrade(otherPlayer, linkedTrade);
            this.deleteChestInventories(otherPlayer, linkedRoom);
            otherPlayer.sendMessage("§cYour trade has been " + (cancel ? "cancelled" : "rejected" ) + " by§r §6" + player.getName() + "§r§c. Any items you put in the chest have been automatically returned to you.");
            return;
        }
        
        OfflineTrader otherOfflineTrader = this.offlineTraders.get(otherPlayerId);
        if (otherOfflineTrader == null) {
            
            player.sendMessage("§cThere was an error while cancelling the trade with the other player. Please report this to a server administrator.");
            this.logger.log(Level.WARNING, "ISSUE WHILE CANCELLING/REJECTING A TRADE");
            this.logger.log(Level.WARNING, "Details below:");
            this.logger.log(Level.WARNING, "Player Name: " + player.getName());
            this.logger.log(Level.WARNING, "Player UUID: " + playerId.toString());
            this.logger.log(Level.WARNING, "TradeChest: " + room.getName());
            this.logger.log(Level.WARNING, "Linked TradeChest: " + linkedRoom.getName());
            this.logger.log(Level.WARNING, "Other Player Name: " + linkedTrade.getName1());
            this.logger.log(Level.WARNING, "Other Player UUID: " + otherPlayerId.toString());
            this.logger.log(Level.WARNING, "Cancel/Reject: " + (cancel ? "CANCEL" : "REJECT"));
            this.logger.log(Level.WARNING, "ISSUE:");
            this.logger.log(Level.WARNING, "Player " + player.getName() + " is attempting to cancel a trade.");
            this.logger.log(Level.WARNING, "Other player is not online, has an ActiveTrade, but does not have an OfflineTrader object.");
            
            final long now = System.currentTimeMillis();
            long logoutTime = now - TradePlugin.OFFLINE_TIMEOUT;
            if (logoutTime < this.serverStart) {
                final long diff = now - this.serverStart;
                logoutTime = this.serverStop - (TradePlugin.OFFLINE_TIMEOUT - diff);
            }
            
            otherOfflineTrader = new OfflineTrader(otherPlayerId, linkedTrade.getName1(), logoutTime);
            otherOfflineTrader.setCompleteReason(OfflineTrader.CompleteReason.ERROR);
        } else {
            otherOfflineTrader.setCompleteReason(cancel ? OfflineTrader.CompleteReason.CANCELLED : OfflineTrader.CompleteReason.REJECTED);
        }
        
        this.itemTransfer(player, room.getChest1().getInventory());
        if (this.tradeInventories.containsKey(playerId)) {
            if (this.checkInventories(player.getOpenInventory().getTopInventory(), this.tradeInventories.get(playerId))) {
                player.closeInventory();
            }
            this.tradeInventories.remove(playerId);
        }
        
        this.activeTrades.remove(playerId);
        this.deleteActiveTrade(player, activeTrade);
        this.deleteChestInventories(player, room);
        player.sendMessage("§cYour trade has been " + (cancel ? "cancelled" : "rejected") + ". Any items you put in the chest have been automatically returned to you.");
        
        otherOfflineTrader.setInventory(linkedRoom.getChest1().getInventory());
        this.offlineTraders.put(otherPlayerId, otherOfflineTrader);
        this.saveOfflineTrader(this.console, otherOfflineTrader);
        
        this.tradeInventories.remove(otherPlayerId);
        this.activeTrades.remove(otherPlayerId);
        this.deleteActiveTrade(this.console, linkedTrade);
        linkedRoom.getChest1().getInventory().clear();
        this.deleteChestInventories(this.console, linkedRoom);
    }
    
    public void readyTrade(@NotNull final Player player) {
        
        final UUID playerId = player.getUniqueId();
        final ActiveTrade activeTrade = this.activeTrades.get(playerId);
        if (activeTrade == null) {
            player.sendMessage("§cYou are not currently in a trade with anyone.");
            return;
        }
        
        if (activeTrade.getStatus().ordinal() >= ActiveTrade.TradeStatus.LOCKED.ordinal()) {
            player.sendMessage("§6You have already marked yourself as ready to trade.");
            player.sendMessage("§6If you need to change your trade items, please cancel and re-start your trade.");
            return;
        }
        
        final TradeRoom room = activeTrade.getRoom();
        final TradeRoom linkedRoom = this.pairings.get(room);
        if (linkedRoom == null) {
            
            player.sendMessage("§cThere was an error while marking your trade as ready. Please report this to a server administrator.");
            this.logger.log(Level.WARNING, "ISSUE WHILE MARKING TRADE AS READY");
            this.logger.log(Level.WARNING, "Details below:");
            this.logger.log(Level.WARNING, "Player Name: " + player.getName());
            this.logger.log(Level.WARNING, "Player UUID: " + playerId.toString());
            this.logger.log(Level.WARNING, "TradeChest: " + room.getName());
            this.logger.log(Level.WARNING, "Configured Linked TradeChest: " + activeTrade.getRoom().getLinked());
            this.logger.log(Level.WARNING, "ISSUE:");
            this.logger.log(Level.WARNING, "Player " + player.getName() + " is attempting to mark themselves as ready for a trade.");
            this.logger.log(Level.WARNING, "No linked TradeChest to the one listed above.");
            return;
        }
        
        final ItemStack[] items = room.getChest1().getInventory().getStorageContents();
        this.saveChestInventories(player, room);
        
        activeTrade.setStatus(ActiveTrade.TradeStatus.LOCKED);
        this.saveActiveTrade(player, activeTrade);
        this.activeTrades.put(playerId, activeTrade);
        player.sendMessage("§aYou have marked yourself as ready to trade.");
        
        ActiveTrade linkedTrade = null;
        for (final ActiveTrade checkTrade : this.activeTrades.values()) {
            if (checkTrade.getRoom().equals(linkedRoom)) {
                linkedTrade = checkTrade;
                break;
            }
        }
        
        if (linkedTrade == null) {
            player.sendMessage("§6Please wait for the other player to begin their trade...");
            return;
        }
        
        final UUID otherPlayerId = linkedTrade.getUniqueId1();
        final Player otherPlayer = this.getServer().getPlayer(otherPlayerId);
        if (otherPlayer == null || !otherPlayer.isOnline()) {
            player.sendMessage("§6Please wait for the other player (§r§b" + linkedTrade.getName1() + "§r§6), they are currently offline.");
            return;
        }
        
        
        if (linkedTrade.getStatus().ordinal() < ActiveTrade.TradeStatus.LOCKED.ordinal()) {
            player.sendMessage("§6Please wait, " + otherPlayer.getName() + " is finishing preparing their trade.");
            otherPlayer.sendMessage("§6" + player.getName() + " is ready to trade.");
            return;
        }
        
        activeTrade.setStatus(ActiveTrade.TradeStatus.DECIDE);
        linkedTrade.setStatus(ActiveTrade.TradeStatus.DECIDE);
        this.saveActiveTrade(player, activeTrade);
        this.saveActiveTrade(otherPlayer, linkedTrade);
        this.activeTrades.put(playerId, activeTrade);
        this.activeTrades.put(otherPlayerId, linkedTrade);
        
        player.sendMessage("§a" + otherPlayer.getName() + " is also ready to trade. Trade starting...");
        otherPlayer.sendMessage("§a" + player.getName() + " is ready to trade now. Trade starting...");
        
        final Inventory senderTradeInventory = this.createTradeInventory(activeTrade.getRoom().getChest1().getInventory());
        final Inventory otherTradeInventory = this.createTradeInventory(linkedTrade.getRoom().getChest1().getInventory());
        
        player.openInventory(otherTradeInventory);
        otherPlayer.openInventory(senderTradeInventory);
        
        this.tradeInventories.put(playerId, otherTradeInventory);
        this.tradeInventories.put(otherPlayerId, senderTradeInventory);
    }
    
    public void viewTrade(@NotNull final Player player) {
        
        final UUID playerId = player.getUniqueId();
        final ActiveTrade activeTrade = this.activeTrades.get(playerId);
        if (activeTrade == null) {
            player.sendMessage("§cYou are not currently in a trade with anyone.");
            return;
        }
        
        switch (activeTrade.getStatus()) {
            case PREPARE:
                player.sendMessage("§cYou cannot view the other player's items; you have not decided what items you want to trade yet.");
                return;
            case LOCKED:
                player.sendMessage("§cYou cannot view the other player's items; the player trading with you has not yet finished deciding what items they want to trade.");
                return;
            case DECIDE:
                break;
            case ACCEPT:
                player.sendMessage("§cYou cannot view the other player's items; you have already accepted the trade.");
                return;
            default:
                player.sendMessage("§cAn error has occurred while viewing the other player's items. Please send this error to a server administrator.");
                this.logger.log(Level.WARNING, "ISSUE WHILE VIEWING TRADE");
                this.logger.log(Level.WARNING, "Details below:");
                this.logger.log(Level.WARNING, "Player Name: " + player.getName());
                this.logger.log(Level.WARNING, "Player UUID: " + playerId.toString());
                this.logger.log(Level.WARNING, "TradeChest: " + activeTrade.getRoom().getName());
                this.logger.log(Level.WARNING, "Trade Status: " + activeTrade.getStatus().name());
                this.logger.log(Level.WARNING, "ISSUE:");
                this.logger.log(Level.WARNING, "Player " + player.getName() + " is attempting to view a trade.");
                this.logger.log(Level.WARNING, "Player matched default case on TradeStatus during trade viewing, meaning they do not have a valid TradeStatus.");
                return;
        }
        
        final Inventory tradeInventory = this.tradeInventories.get(playerId);
        if (tradeInventory == null) {
            player.sendMessage("§cAn error occurred while viewing the other player's items. Please send this error to a server administrator.");
            this.logger.log(Level.WARNING, "ISSUE WHILE VIEWING TRADE");
            this.logger.log(Level.WARNING, "Details below:");
            this.logger.log(Level.WARNING, "Player Name: " + player.getName());
            this.logger.log(Level.WARNING, "Player UUID: " + playerId.toString());
            this.logger.log(Level.WARNING, "TradeChest: " + activeTrade.getRoom().getName());
            this.logger.log(Level.WARNING, "Trade Status: " + activeTrade.getStatus().name());
            this.logger.log(Level.WARNING, "ISSUE:");
            this.logger.log(Level.WARNING, "Player " + player.getName() + " is attempting to view a trade.");
            this.logger.log(Level.WARNING, "Player is in the DECIDE phase, but does not have a trade Inventory available.");
            return;
        }
        
        if (!this.checkInventories(player.getOpenInventory().getTopInventory(), tradeInventory)) {
            player.closeInventory();
            player.openInventory(tradeInventory);
        }
    }
    
    public void acceptTrade(@NotNull final Player player) {
        
        final UUID playerId = player.getUniqueId();
        final ActiveTrade activeTrade = this.activeTrades.get(playerId);
        if (activeTrade == null) {
            player.sendMessage("§cYou are not currently in a trade with anyone.");
            return;
        }
        
        switch (activeTrade.getStatus()) {
            case PREPARE:
                player.sendMessage("§cYou cannot accept a trade; you have not decided what items you want to trade yet.");
                return;
            case LOCKED:
                player.sendMessage("§cThe player trading with you has not yet finished deciding what items they want to trade.");
                return;
            case DECIDE:
                break;
            case ACCEPT:
                player.sendMessage("§cYou have already accepted the trade, please wait for the other person to decide.");
                return;
            default:
                player.sendMessage("§cAn error has occurred while accepting your trade. Please send this error to a server administrator.");
                this.logger.log(Level.WARNING, "ISSUE WHILE ACCEPTING TRADE");
                this.logger.log(Level.WARNING, "Details below:");
                this.logger.log(Level.WARNING, "Player Name: " + player.getName());
                this.logger.log(Level.WARNING, "Player UUID: " + playerId.toString());
                this.logger.log(Level.WARNING, "TradeChest: " + activeTrade.getRoom().getName());
                this.logger.log(Level.WARNING, "Trade Status: " + activeTrade.getStatus().name());
                this.logger.log(Level.WARNING, "ISSUE:");
                this.logger.log(Level.WARNING, "Player " + player.getName() + " is attempting to accept a trade.");
                this.logger.log(Level.WARNING, "Player matched default case on TradeStatus during trade acceptance, meaning they do not have a valid TradeStatus.");
                return;
        }
        
        final TradeRoom room = activeTrade.getRoom();
        final TradeRoom linkedRoom = this.pairings.get(room);
        if (linkedRoom == null) {
            player.sendMessage("§cThere was an error while accepting your trade. Please notify a server administrator.");
            this.logger.log(Level.WARNING, "ISSUE WHILE ACCEPTING TRADE");
            this.logger.log(Level.WARNING, "Details below:");
            this.logger.log(Level.WARNING, "Player Name: " + player.getName());
            this.logger.log(Level.WARNING, "Player UUID: " + playerId.toString());
            this.logger.log(Level.WARNING, "TradeChest: " + room.getName());
            this.logger.log(Level.WARNING, "Configured Linked TradeChest: " + room.getLinked());
            this.logger.log(Level.WARNING, "ISSUE:");
            this.logger.log(Level.WARNING, "Player " + player.getName() + " is attempting to accept a trade.");
            this.logger.log(Level.WARNING, "No linked TradeChest to the one listed above.");
            return;
        }
        
        ActiveTrade linkedTrade = null;
        for (final ActiveTrade checkTrade : this.activeTrades.values()) {
            if (checkTrade.getRoom().equals(linkedRoom)) {
                linkedTrade = checkTrade;
                break;
            }
        }
        
        if (linkedTrade == null) {
            player.sendMessage("§cThere was an error while accepting your trade. Please notify a server administrator.");
            this.logger.log(Level.WARNING, "ISSUE WHILE ACCEPTING TRADE");
            this.logger.log(Level.WARNING, "Details below:");
            this.logger.log(Level.WARNING, "Player Name: " + player.getName());
            this.logger.log(Level.WARNING, "Player UUID: " + playerId.toString());
            this.logger.log(Level.WARNING, "Trade Chest: " + room.getName());
            this.logger.log(Level.WARNING, "Linked Chest: " + linkedRoom.getName());
            this.logger.log(Level.WARNING, "ISSUE:");
            this.logger.log(Level.WARNING, "Player " + player.getName() + " is attempting to accept a trade.");
            this.logger.log(Level.WARNING, "No ActiveTrade for the linked TradeChest, this TradeStatus should not be possible without another ActiveTrade.");
            return;
        }
        
        player.sendMessage("§aYou have accepted the trade.");
        if (linkedTrade.getStatus() != ActiveTrade.TradeStatus.ACCEPT) {
            
            if (this.tradeInventories.containsKey(playerId)) {
                if (this.checkInventories(player.getOpenInventory().getTopInventory(), this.tradeInventories.get(playerId))) {
                    player.closeInventory();
                }
            }
            
            activeTrade.setStatus(ActiveTrade.TradeStatus.ACCEPT);
            this.saveActiveTrade(player, activeTrade);
            this.activeTrades.put(playerId, activeTrade);
            
            player.sendMessage("§6You are waiting on the other player to make a decision. Please continue to wait...");
            return;
        }
        
        final UUID otherPlayerId = linkedTrade.getUniqueId1();
        final Player otherPlayer = this.getServer().getPlayer(otherPlayerId);
        if (otherPlayer != null && otherPlayer.isOnline()) {
            
            otherPlayer.sendMessage("§aThe other person has accepted the trade.");
            player.sendMessage("§aSwapping items...");
            otherPlayer.sendMessage("§aSwapping items...");
            
            this.itemTransfer(player, linkedRoom.getChest1().getInventory());
            if (this.tradeInventories.containsKey(playerId)) {
                if (this.checkInventories(player.getOpenInventory().getTopInventory(), this.tradeInventories.get(playerId))) {
                    player.closeInventory();
                }
                this.tradeInventories.remove(playerId);
            }
            
            this.itemTransfer(otherPlayer, room.getChest1().getInventory());
            if (this.tradeInventories.containsKey(otherPlayerId)) {
                if (this.checkInventories(otherPlayer.getOpenInventory().getTopInventory(), this.tradeInventories.get(otherPlayerId))) {
                    otherPlayer.closeInventory();
                }
                this.tradeInventories.remove(otherPlayerId);
            }
            
            this.activeTrades.remove(playerId);
            this.deleteActiveTrade(player, activeTrade);
            this.deleteChestInventories(player, room);
            
            this.activeTrades.remove(otherPlayerId);
            this.deleteActiveTrade(otherPlayer, linkedTrade);
            this.deleteChestInventories(otherPlayer, linkedRoom);
            
            player.sendMessage("§aTrade complete!");
            otherPlayer.sendMessage("§aTrade complete!");
            return;
        }
        
        OfflineTrader otherOfflineTrader = this.offlineTraders.get(otherPlayerId);
        if (otherOfflineTrader == null) {
            
            player.sendMessage("§cThere was an error accepting your trade. Please report this to a server administrator.");
            this.logger.log(Level.WARNING, "ISSUE WHILE ACCEPTING A TRADE");
            this.logger.log(Level.WARNING, "Details below:");
            this.logger.log(Level.WARNING, "Player Name: " + player.getName());
            this.logger.log(Level.WARNING, "Player UUID: " + playerId.toString());
            this.logger.log(Level.WARNING, "TradeChest: " + room.getName());
            this.logger.log(Level.WARNING, "Linked TradeChest: " + linkedRoom.getName());
            this.logger.log(Level.WARNING, "Other Player Name: " + linkedTrade.getName1());
            this.logger.log(Level.WARNING, "Other Player UUID: " + otherPlayerId.toString());
            this.logger.log(Level.WARNING, "ISSUE:");
            this.logger.log(Level.WARNING, "Player " + player.getName() + " is attempting to accept a trade.");
            this.logger.log(Level.WARNING, "Other player is not online, has an ActiveTrade, but does not have an OfflineTrader object.");
            
            this.itemTransfer(player, room.getChest1().getInventory());
            if (this.tradeInventories.containsKey(playerId)) {
                if (this.checkInventories(player.getOpenInventory().getTopInventory(), this.tradeInventories.get(playerId))) {
                    player.closeInventory();
                }
                this.tradeInventories.remove(playerId);
            }
            
            this.activeTrades.remove(playerId);
            this.deleteActiveTrade(player, activeTrade);
            this.deleteChestInventories(player, room);
            player.sendMessage("§cYour trade has been automatically cancelled. Any items you put in the chest have been automatically returned to you.");
            
            final long now = System.currentTimeMillis();
            long logoutTime = now - TradePlugin.OFFLINE_TIMEOUT;
            if (logoutTime < this.serverStart) {
                final long diff = now - this.serverStart;
                logoutTime = this.serverStop - (TradePlugin.OFFLINE_TIMEOUT - diff);
            }
            
            otherOfflineTrader = new OfflineTrader(otherPlayerId, linkedTrade.getName1(), logoutTime);
            otherOfflineTrader.setCompleteReason(OfflineTrader.CompleteReason.ERROR);
            otherOfflineTrader.setInventory(linkedRoom.getChest1().getInventory());
            this.offlineTraders.put(otherPlayerId, otherOfflineTrader);
            this.saveOfflineTrader(this.console, otherOfflineTrader);
            
            this.tradeInventories.remove(otherPlayerId);
            this.activeTrades.remove(otherPlayerId);
            this.deleteActiveTrade(this.console, linkedTrade);
            linkedRoom.getChest1().getInventory().clear();
            this.deleteChestInventories(this.console, linkedRoom);
            return;
        }
        
        player.sendMessage("§aThe other player has accepted the trade before they went offline.");
        player.sendMessage("§aSwapping items...");
        
        this.itemTransfer(player, linkedRoom.getChest1().getInventory());
        if (this.tradeInventories.containsKey(playerId)) {
            if (this.checkInventories(player.getOpenInventory().getTopInventory(), this.tradeInventories.get(playerId))) {
                player.closeInventory();
            }
            this.tradeInventories.remove(playerId);
        }
        
        player.sendMessage("§aTrade complete!");
        
        otherOfflineTrader.setCompleteReason(OfflineTrader.CompleteReason.ACCEPTED);
        otherOfflineTrader.setInventory(room.getChest1().getInventory());
        this.offlineTraders.put(otherPlayerId, otherOfflineTrader);
        this.saveOfflineTrader(this.console, otherOfflineTrader);
        
        this.activeTrades.remove(playerId);
        this.deleteActiveTrade(player, activeTrade);
        this.deleteChestInventories(player, room);
        
        this.tradeInventories.remove(otherPlayerId);
        this.activeTrades.remove(otherPlayerId);
        this.deleteActiveTrade(this.console, linkedTrade);
        linkedRoom.getChest1().getInventory().clear();
        this.deleteChestInventories(this.console, linkedRoom);
    }
    
    @NotNull
    public List<String> getNotLinked(@Nullable final String name) {
        
        final List<String> notLinked = new ArrayList<String>();
        for (final TradeRoom room : this.byName.values()) {
            if (this.pairings.get(room) == null && (name == null || !name.equalsIgnoreCase(room.getName()))) {
                notLinked.add(room.getName());
            }
        }
        
        return notLinked;
    }
    
    @NotNull
    public List<String> getLinked() {
        
        final List<String> linked = new ArrayList<String>();
        for (final TradeRoom room : this.byName.values()) {
            if (this.pairings.get(room) != null) {
                linked.add(room.getName());
            }
        }
        
        return linked;
    }
    
    @NotNull
    public List<String> getNotInActiveTrade(final boolean linkedRequired) {
        
        final List<TradeRoom> inActiveTrade = new ArrayList<TradeRoom>();
        for (final ActiveTrade activeTrade : this.activeTrades.values()) {
            inActiveTrade.add(activeTrade.getRoom());
        }
    
        final List<String> notInActiveTrade = new ArrayList<String>();
        for (final TradeRoom room : this.byName.values()) {
            if (!inActiveTrade.contains(room)) {
                if (!linkedRequired) {
                    notInActiveTrade.add(room.getName());
                } else if (this.pairings.get(room) != null) {
                    notInActiveTrade.add(room.getName());
                }
            }
        }
        
        return notInActiveTrade;
    }
    
    @NotNull
    public List<String> getAll() {
        return new ArrayList<String>(this.byName.keySet());
    }
    
    ////////////////////
    // HELPER METHODS //
    ////////////////////
    
    private void itemTransfer(@NotNull final Player player, @NotNull final Inventory chestInventory) {
        
        final Inventory playerInventory = player.getInventory();
        final ItemStack[] chestContents = chestInventory.getStorageContents();
        final ItemStack[] playerContents = playerInventory.getStorageContents();
        
        boolean availableSlot = true;
        
        for (int cIndex = 0; cIndex < chestContents.length && availableSlot; cIndex++) {
            boolean itemMoved = false;
            final ItemStack chestItem = chestContents[cIndex];
            if (chestItem == null || chestItem.getType() == Material.AIR) {
                continue;
            }
            
            for (int pIndex = 0; pIndex < playerContents.length && !itemMoved; pIndex++) {
                final ItemStack slot = playerContents[pIndex];
                if (slot == null || slot.getType() == Material.AIR) {
                    playerContents[pIndex] = chestContents[cIndex];
                    chestContents[cIndex] = null;
                    itemMoved = true;
                }
            }
            
            if (!itemMoved) {
                availableSlot = false;
            }
        }
        
        player.getInventory().setStorageContents(playerContents);
        
        boolean itemsRemaining = false;
        if (!availableSlot) {
            for (final ItemStack item : chestContents) {
                if (item != null && item.getType() != Material.AIR) {
                    itemsRemaining = true;
                    break;
                }
            }
        }
        
        if (itemsRemaining) {
            player.sendMessage("§6You did not have enough room in your inventory for everything. Your remaining items have been dropped at your feet.");
            final World world = player.getLocation().getWorld();
            final double x = player.getLocation().getBlockX() + 0.5D;
            final double y = player.getLocation().getBlockY() + 1.0D;
            final double z = player.getLocation().getBlockZ() + 0.5D;
            
            final Location dropLocation = new Location(player.getLocation().getWorld(), x, y, z);
            for (final ItemStack item : chestContents) {
                if (item != null && item.getType() != Material.AIR) {
                    world.dropItemNaturally(dropLocation, item);
                }
            }
        }
        
        for (int slot = 0; slot < chestContents.length; slot++) {
            chestContents[slot] = null;
        }
        chestInventory.setStorageContents(chestContents);
    }
    
    @NotNull
    private String formatTime(final long logoutTime) {
        
        final long now = System.currentTimeMillis();
        final long end = logoutTime + TradePlugin.OFFLINE_TIMEOUT;
        
        final StringBuilder builder = new StringBuilder();
        if (now >= end) {
            builder.append("0 seconds");
            return builder.toString();
        }
        
        long diff = end - now;
        if (diff % 1000L != 0L) {
            diff += 1000L - (diff % 1000L);
        }
        diff /= 1000L;
        
        final long minutes = diff / 60L;
        final long seconds = diff % 60L;
        
        if (minutes > 1L) {
            builder.append(minutes).append(" minutes, ");
        } else if (minutes == 1L) {
            builder.append(minutes).append(" minute, ");
        }
        
        if (!builder.toString().isEmpty() && seconds == 0L) {
            return builder.toString();
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
    
    private boolean linkRooms(@NotNull final CommandSender sender, @NotNull final TradeRoom room1, @NotNull final TradeRoom room2) {
        
        try {
            room2.link(room1);
            room1.link(room2);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(e.getMessage());
            this.logger.log(Level.WARNING, "ISSUE WHILE LINKING TRADEROOMS");
            this.logger.log(Level.WARNING, "Details below:");
            this.logger.log(Level.WARNING, "TradeChest 1:" + room1.getName());
            this.logger.log(Level.WARNING, "TradeChest 2:" + room2.getName());
            this.logger.log(Level.WARNING, "ISSUE:");
            this.logger.log(Level.WARNING, "Player " + sender.getName() + " is attempting to link 2 TradeChests.");
            this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
            return false;
        }
        
        this.pairings.put(room1, room2);
        this.pairings.put(room2, room1);
        
        this.saveTradeRoom(sender, room1);
        this.saveTradeRoom(sender, room2);
        return true;
    }
    
    private void unlinkRoom(@NotNull final CommandSender sender, @NotNull final TradeRoom room) {
        
        final TradeRoom linked = this.pairings.get(room);
        if (linked != null && linked.getLinked() != null && linked.getLinked().equals(room.getName())) {
            
            linked.unlink();
            this.pairings.put(linked, null);
            this.saveTradeRoom(sender, linked);
        }
        
        room.unlink();
        this.pairings.put(room, null);
        this.saveTradeRoom(sender, room);
    }
    
    @NotNull
    private Inventory createTradeInventory(@NotNull final Inventory chestInventory) {
        
        final Inventory tradeInventory = this.getServer().createInventory(null, 45);
        final ItemStack[] items = chestInventory.getStorageContents();
        for (int slot = 0; slot < items.length && slot < 27; slot++) {
            tradeInventory.setItem(slot, items[slot]);
        }
        
        final ItemStack fill = new ItemStack(Material.BLACK_CONCRETE);
        final ItemMeta fillMeta = fill.getItemMeta();
        fillMeta.setDisplayName("");
        fill.setItemMeta(fillMeta);
        for (int slot = 37; slot < 44; slot++) {
            tradeInventory.setItem(slot, fill);
        }
        
        final ItemStack reject = new ItemStack(Material.RED_CONCRETE);
        final ItemMeta rejectMeta = reject.getItemMeta();
        rejectMeta.setDisplayName("REJECT/CANCEL TRADE");
        reject.setItemMeta(rejectMeta);
        tradeInventory.setItem(TradePlugin.SLOT_REJECT, reject);
        
        final ItemStack accept = new ItemStack(Material.LIME_CONCRETE);
        final ItemMeta acceptMeta = accept.getItemMeta();
        acceptMeta.setDisplayName("ACCEPT TRADE");
        accept.setItemMeta(acceptMeta);
        tradeInventory.setItem(TradePlugin.SLOT_ACCEPT, accept);
        
        return tradeInventory;
    }
    
    /////////////////
    // FILE SAVING //
    /////////////////
    
    private void saveTradeRoom(@NotNull final CommandSender sender, @NotNull final TradeRoom room) {
        
        this.scheduler.runTaskAsynchronously(this, () -> {
            
            final File tradeChestFile = new File(this.tradeRoomFolder, room.getName() + ".yml");
            try {
                if (!tradeChestFile.exists()) {
                    if (!tradeChestFile.createNewFile()) {
                        sender.sendMessage("There was an error while updating the TradeChest. Please report this error to a server administrator.");
                        this.logger.log(Level.WARNING, "ISSUE WHILE SAVING TRADECHEST FILE");
                        this.logger.log(Level.WARNING, "Details below:");
                        this.logger.log(Level.WARNING, "TradeChest File Location: " + tradeChestFile.getPath());
                        this.logger.log(Level.WARNING, "TradeChest YAML Data: " + room.getConfig().toString());
                        this.logger.log(Level.WARNING, "ISSUE:");
                        this.logger.log(Level.WARNING, "TradeChest file not created successfully.");
                        return;
                    }
                }
            } catch (SecurityException | IOException e) {
                sender.sendMessage("There was an error while updating the TradeChest. Please report this error to a server administrator.");
                this.logger.log(Level.WARNING, "ISSUE WHILE SAVING TRADECHEST FILE");
                this.logger.log(Level.WARNING, "Details below:");
                this.logger.log(Level.WARNING, "TradeChest File Location: " + tradeChestFile.getPath());
                this.logger.log(Level.WARNING, "TradeChest YAML Data: " + room.getConfig().toString());
                this.logger.log(Level.WARNING, "ISSUE:");
                this.logger.log(Level.WARNING, "Unable to create TradeChest file.");
                this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                return;
            }
            
            final FileConfiguration config = room.getConfig();
            try {
                config.save(tradeChestFile);
            } catch (IOException | IllegalArgumentException e) {
                sender.sendMessage("There was an error while updating the TradeChest. Please report this error to a server administrator.");
                this.logger.log(Level.WARNING, "ISSUE WHILE SAVING TRADECHEST FILE");
                this.logger.log(Level.WARNING, "Details below:");
                this.logger.log(Level.WARNING, "TradeChest File Location: " + tradeChestFile.getPath());
                this.logger.log(Level.WARNING, "TradeChest YAML Data: " + room.getConfig().toString());
                this.logger.log(Level.WARNING, "ISSUE:");
                this.logger.log(Level.WARNING, "Unable to save TradeChest configuration file.");
                this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
            }
        });
    }
    
    private void saveActiveTrade(@NotNull final CommandSender sender, @NotNull final ActiveTrade activeTrade) {
        
        this.scheduler.runTaskAsynchronously(this, () -> {
            
            final File activeTradeFile = new File(this.activeTradesFolder, activeTrade.getUniqueId1().toString() + "-" + activeTrade.getRoom().getName() + ".yml");
            try {
                if (!activeTradeFile.exists()) {
                    if (!activeTradeFile.createNewFile()) {
                        sender.sendMessage("There was an error while updating your trade. Please report this error to a server administrator.");
                        this.logger.log(Level.WARNING, "ISSUE WHILE SAVING ACTIVETRADE FILE");
                        this.logger.log(Level.WARNING, "Details below:");
                        this.logger.log(Level.WARNING, "ActiveTrade File Location: " + activeTradeFile.getPath());
                        this.logger.log(Level.WARNING, "ActiveTrade YAML Data:");
                        this.logger.log(Level.WARNING, "uuid: " + activeTrade.getUniqueId1().toString());
                        this.logger.log(Level.WARNING, "name: " + activeTrade.getName1());
                        this.logger.log(Level.WARNING, "trade_chest:" + activeTrade.getRoom().getName());
                        this.logger.log(Level.WARNING, "trade_status: " + activeTrade.getStatus().name());
                        this.logger.log(Level.WARNING, "ISSUE:");
                        this.logger.log(Level.WARNING, "ActiveTrade file not created successfully.");
                        return;
                    }
                }
            } catch (SecurityException | IOException e) {
                sender.sendMessage("There was an error while updating your trade. Please report this error to a server administrator.");
                this.logger.log(Level.WARNING, "ISSUE WHILE SAVING ACTIVETRADE FILE");
                this.logger.log(Level.WARNING, "Details below:");
                this.logger.log(Level.WARNING, "ActiveTrade File Location: " + activeTradeFile.getPath());
                this.logger.log(Level.WARNING, "ActiveTrade YAML Data:");
                this.logger.log(Level.WARNING, "uuid: " + activeTrade.getUniqueId1().toString());
                this.logger.log(Level.WARNING, "name: " + activeTrade.getName1());
                this.logger.log(Level.WARNING, "trade_chest:" + activeTrade.getRoom().getName());
                this.logger.log(Level.WARNING, "trade_status: " + activeTrade.getStatus().name());
                this.logger.log(Level.WARNING, "ISSUE:");
                this.logger.log(Level.WARNING, "Unable to create ActiveTrade file.");
                this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                return;
            }
            
            final YamlConfiguration config = new YamlConfiguration();
            config.set("uuid", activeTrade.getUniqueId1().toString());
            config.set("name", activeTrade.getName1());
            config.set("trade_chest", activeTrade.getRoom().getName());
            config.set("trade_status", activeTrade.getStatus().name());
            try {
                config.save(activeTradeFile);
            } catch (IOException | IllegalArgumentException e) {
                sender.sendMessage("There was an error while updating your trade. Please report this error to a server administrator.");
                this.logger.log(Level.WARNING, "ISSUE WHILE SAVING ACTIVETRADE FILE");
                this.logger.log(Level.WARNING, "Details below:");
                this.logger.log(Level.WARNING, "ActiveTrade File Location: " + activeTradeFile.getPath());
                this.logger.log(Level.WARNING, "ActiveTrade YAML Data:");
                this.logger.log(Level.WARNING, "uuid: " + activeTrade.getUniqueId1().toString());
                this.logger.log(Level.WARNING, "name: " + activeTrade.getName1());
                this.logger.log(Level.WARNING, "trade_chest:" + activeTrade.getRoom().getName());
                this.logger.log(Level.WARNING, "trade_status: " + activeTrade.getStatus().name());
                this.logger.log(Level.WARNING, "ISSUE:");
                this.logger.log(Level.WARNING, "Unable to save ActiveTrade configuration file.");
                this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
            }
        });
    }
    
    private void saveChestInventories(@NotNull final CommandSender sender, @NotNull final TradeRoom room) {
        
        this.scheduler.runTaskAsynchronously(this, () -> {
            
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
        });
    }
    
    private void saveOfflineTrader(@NotNull final CommandSender sender, @NotNull final OfflineTrader trader) {
        
        this.scheduler.runTaskAsynchronously(this, () -> {
            
            final File offlineTraderFile = new File(this.offlineTraderFolder, trader.getUniqueId().toString() + Constants.FILE_TYPE);
            try {
                if (!offlineTraderFile.exists()) {
                    if (!offlineTraderFile.createNewFile()) {
                        sender.sendMessage("§cThere was an error while updating your returnable items. Please report this to a server administrator.");
                        this.logger.log(Level.WARNING, "ISSUE WHILE SAVING OFFLINETRADER FILE");
                        this.logger.log(Level.WARNING, "Details below:");
                        this.logger.log(Level.WARNING, "OfflineTrader File Location: " + offlineTraderFile.getPath());
                        this.logger.log(Level.WARNING, "OfflineTrader YAML Data:");
                        this.logger.log(Level.WARNING, "uuid: " + trader.getUniqueId().toString());
                        this.logger.log(Level.WARNING, "name:" + trader.getName());
                        this.logger.log(Level.WARNING, "logout_time:" + trader.getLogoutTime());
                        this.logger.log(Level.WARNING, "complete_reason: " + (trader.getCompleteReason() == null ? "null" : trader.getCompleteReason().name()));
                        this.logger.log(Level.WARNING, "items: " + (trader.getInventory() == null ? "null" : trader.getInventory().toString()));
                        this.logger.log(Level.WARNING, "ISSUE:");
                        this.logger.log(Level.WARNING, "OfflineTrader file not created successfully.");
                        return;
                    }
                }
            } catch (SecurityException | IOException e) {
                sender.sendMessage("§cThere was an error while updating your returnable items. Please report this to a server administrator.");
                this.logger.log(Level.WARNING, "ISSUE WHILE SAVING OFFLINETRADER FILE");
                this.logger.log(Level.WARNING, "Details below:");
                this.logger.log(Level.WARNING, "OfflineTrader File Location: " + offlineTraderFile.getPath());
                this.logger.log(Level.WARNING, "OfflineTrader YAML Data:");
                this.logger.log(Level.WARNING, "uuid: " + trader.getUniqueId().toString());
                this.logger.log(Level.WARNING, "name:" + trader.getName());
                this.logger.log(Level.WARNING, "logout_time:" + trader.getLogoutTime());
                this.logger.log(Level.WARNING, "complete_reason: " + (trader.getCompleteReason() == null ? "null" : trader.getCompleteReason().name()));
                this.logger.log(Level.WARNING, "items: " + (trader.getInventory() == null ? "null" : trader.getInventory().toString()));
                this.logger.log(Level.WARNING, "ISSUE:");
                this.logger.log(Level.WARNING, "Unable to create OfflineTrader file.");
                this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                return;
            }
            
            final FileConfiguration config = trader.getConfig();
            try {
                config.save(offlineTraderFile);
            } catch (IOException | IllegalArgumentException e) {
                sender.sendMessage("§cThere was an error while updating your returnable items. Please report this to a server administrator.");
                this.logger.log(Level.WARNING, "ISSUE WHILE SAVING OFFLINETRADER FILE");
                this.logger.log(Level.WARNING, "Details below:");
                this.logger.log(Level.WARNING, "OfflineTrader File Location: " + offlineTraderFile.getPath());
                this.logger.log(Level.WARNING, "OfflineTrader YAML Data:");
                this.logger.log(Level.WARNING, "uuid: " + trader.getUniqueId().toString());
                this.logger.log(Level.WARNING, "name:" + trader.getName());
                this.logger.log(Level.WARNING, "logout_time:" + trader.getLogoutTime());
                this.logger.log(Level.WARNING, "complete_reason: " + (trader.getCompleteReason() == null ? "null" : trader.getCompleteReason().name()));
                this.logger.log(Level.WARNING, "items: " + (trader.getInventory() == null ? "null" : trader.getInventory().toString()));
                this.logger.log(Level.WARNING, "ISSUE:");
                this.logger.log(Level.WARNING, "Unable to save OfflineTrader configuration file.");
                this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
            }
        });
    }
    
    ///////////////////
    // FILE DELETION //
    ///////////////////
    
    private void deleteTradeChest(@NotNull final CommandSender sender, @NotNull final TradeChest tradeChest) {
        
        this.scheduler.runTaskAsynchronously(this, () -> {
            
            final File tradeChestFile = new File(this.tradeRoomFolder, tradeChest.getName() + ".yml");
            try {
                if (tradeChestFile.exists()) {
                    if (!tradeChestFile.delete()) {
                        sender.sendMessage("§cThere was an error while deleting the TradeChest. Please report this error to a server administrator.");
                        this.logger.log(Level.WARNING, "ISSUE WHILE DELETING TRADECHEST FILE");
                        this.logger.log(Level.WARNING, "Details below:");
                        this.logger.log(Level.WARNING, "TradeChest File Location: " + tradeChestFile.getPath());
                        this.logger.log(Level.WARNING, "ISSUE:");
                        this.logger.log(Level.WARNING, "TradeChest file not deleted successfully.");
                    }
                }
            } catch (SecurityException e) {
                sender.sendMessage("§cThere was an error while deleting the TradeChest. Please report this error to a server administrator.");
                this.logger.log(Level.WARNING, "ISSUE WHILE DELETING TRADECHEST FILE");
                this.logger.log(Level.WARNING, "Details below:");
                this.logger.log(Level.WARNING, "TradeChest File Location: " + tradeChestFile.getPath());
                this.logger.log(Level.WARNING, "ISSUE:");
                this.logger.log(Level.WARNING, "Unable to delete TradeChest file.");
                this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
            }
        });
    }
    
    private void deleteActiveTrade(@NotNull final CommandSender sender, @NotNull final ActiveTrade activeTrade) {
        
        this.scheduler.runTaskAsynchronously(this, () -> {
            
            final File activeTradeFile = new File(this.activeTradesFolder, activeTrade.getUniqueId1().toString() + "-" + activeTrade.getRoom().getName() + Constants.FILE_TYPE);
            try {
                if (activeTradeFile.exists()) {
                    if (!activeTradeFile.delete()) {
                        sender.sendMessage("§cThere was an error while completing your trade. Please report this error to a server administrator.");
                        this.logger.log(Level.WARNING, "ISSUE WHILE DELETING ACTIVETRADE FILE");
                        this.logger.log(Level.WARNING, "Details below:");
                        this.logger.log(Level.WARNING, "ActiveTrade File Location: " + activeTradeFile.getPath());
                        this.logger.log(Level.WARNING, "ISSUE:");
                        this.logger.log(Level.WARNING, "ActiveTrade file not deleted successfully.");
                    }
                }
            } catch (SecurityException e) {
                sender.sendMessage("§cThere was an error while completing your trade. Please report this error to a server administrator.");
                this.logger.log(Level.WARNING, "ISSUE WHILE DELETING ACTIVETRADE FILE");
                this.logger.log(Level.WARNING, "Details below:");
                this.logger.log(Level.WARNING, "ActiveTrade File Location: " + activeTradeFile.getPath());
                this.logger.log(Level.WARNING, "ISSUE:");
                this.logger.log(Level.WARNING, "Unable to delete ActiveTrade file.");
                this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
            }
        });
    }
    
    private void deleteChestInventories(@NotNull final CommandSender sender, @NotNull final TradeRoom room) {
        
        this.scheduler.runTaskAsynchronously(this, () -> {
            
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
        });
    }
    
    private void deleteOfflineTrader(@NotNull final CommandSender sender, @NotNull final OfflineTrader offlineTrader) {
        
        this.scheduler.runTaskAsynchronously(this, () -> {
            
            final UUID playerId = offlineTrader.getUniqueId();
            final File offlineTraderFile = new File(this.offlineTraderFolder, playerId.toString() + Constants.FILE_TYPE);
            try {
                if (offlineTraderFile.exists()) {
                    if (!offlineTraderFile.delete()) {
                        sender.sendMessage("§cThere was an error while returning your items. Please report this to a server administrator.");
                        this.logger.log(Level.WARNING, "ISSUE WHILE DELETING OFFLINETRADER FILE");
                        this.logger.log(Level.WARNING, "Details below:");
                        this.logger.log(Level.WARNING, "OfflineTrader File Location: " + offlineTraderFile.getPath());
                        this.logger.log(Level.WARNING, "ISSUE:");
                        this.logger.log(Level.WARNING, "OfflineTrader file not deleted successfully.");
                    }
                }
            } catch (SecurityException e) {
                sender.sendMessage("§cThere was an error while returning your items. Please report this to a server administrator.");
                this.logger.log(Level.WARNING, "ISSUE WHILE DELETING OFFLINETRADER FILE");
                this.logger.log(Level.WARNING, "Details below:");
                this.logger.log(Level.WARNING, "OfflineTrader File Location: " + offlineTraderFile.getPath());
                this.logger.log(Level.WARNING, "ISSUE:");
                this.logger.log(Level.WARNING, "Unable to delete OfflineTrader file.");
                this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
            }
        });
    }
}
