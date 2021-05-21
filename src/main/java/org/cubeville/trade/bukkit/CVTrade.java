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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.cubeville.trade.bukkit.command.CVTradeCommand;
import org.cubeville.trade.bukkit.listener.CVTradeListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CVTrade extends JavaPlugin {
    
    private static final int SLOT_REJECT = 36;
    private static final int SLOT_ACCEPT = 44;
    
    private static final long OFFLINE_TIMEOUT = 1000L * 60L * 5L;
    
    private Logger logger;
    private BukkitScheduler scheduler;
    private CommandSender console;
    
    private File tradeChestFolder;
    private ConcurrentHashMap<String, TradeChest> byName;
    private ConcurrentHashMap<Location, TradeChest> byLocation;
    private HashMap<TradeChest, TradeChest> pairings;
    
    private ConcurrentHashMap<UUID, String> scheduledCreations;
    
    private File activeTradesFolder;
    private ConcurrentHashMap<UUID, ActiveTrade> activeTrades;
    private File backupInventoryFolder;
    private ConcurrentHashMap<UUID, Inventory> tradeInventories;
    
    private File offlineInventoryFolder;
    private ConcurrentHashMap<UUID, OfflineTrader> offlineTraders;
    
    @Override
    public void onEnable() {
        
        // Basic Plugin Startup //
        
        this.logger = this.getLogger();
        this.scheduler = this.getServer().getScheduler();
        this.console = this.getServer().getConsoleSender();
    
        this.logger.log(Level.INFO, "////////////////////////////////////////////////////////////////////////////////");
        this.logger.log(Level.INFO, "// CVTrade Bukkit plugin for Minecraft Bukkit servers.                        //");
        this.logger.log(Level.INFO, "//                                                                            //");
        this.logger.log(Level.INFO, "// Copyright (C) 2021 Matt Ciolkosz (https://github.com/mciolkosz/)           //");
        this.logger.log(Level.INFO, "// Copyright (C) 2021 Cubeville (https://www.cubeville.org/)                  //");
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
        
        // TradeChest Initialization //
        
        // Load in the TradeChests.
        this.byName = new ConcurrentHashMap<String, TradeChest>();
        this.byLocation = new ConcurrentHashMap<Location, TradeChest>();
        this.pairings = new HashMap<TradeChest, TradeChest>();
        
        ConfigurationSerialization.registerClass(TradeChest.class);
        this.tradeChestFolder = new File(this.getDataFolder(), "Trade_Chests");
        if (!this.tradeChestFolder.exists()) {
            if (!this.tradeChestFolder.mkdirs()) {
                throw new RuntimeException("TradeChest folder not created at " + this.tradeChestFolder.getPath());
            }
        } else if (!this.tradeChestFolder.isDirectory()) {
            throw new RuntimeException("TradeChest folder is not a folder. Location: " + this.tradeChestFolder.getPath());
        }
        
        for (final File tradeChestFile : this.tradeChestFolder.listFiles()) {
            final YamlConfiguration config = YamlConfiguration.loadConfiguration(tradeChestFile);
            final TradeChest tradeChest;
            try {
                tradeChest = config.getSerializable("tradechest", TradeChest.class);
            } catch (IllegalArgumentException e) {
                this.logger.log(Level.WARNING, "Unable to deserialize trade chest.");
                this.logger.log(Level.WARNING, "File: " + tradeChestFile.getName(), e);
                this.logger.log(Level.WARNING, "Skipping trade chest.");
                continue;
            }
            
            if (tradeChest == null) {
                this.logger.log(Level.WARNING, "Trade chest for file " + tradeChestFile.getName() + " is null, skipping.");
                continue;
            }
            if (this.byName.containsKey(tradeChest.getName())) {
                this.logger.log(Level.WARNING, "Conflicting name of trade chest already registered.");
                this.logger.log(Level.WARNING, "Names: " + tradeChest.getName());
                this.logger.log(Level.WARNING, "Please remember that names are case-insensitive. Skipping.");
                continue;
            }
            this.byName.put(tradeChest.getName(), tradeChest);
            this.byLocation.put(tradeChest.getChest().getLocation(), tradeChest);
        }
        
        // Link the TradeChests where possible
        final HashSet<TradeChest> ignore = new HashSet<TradeChest>();
        for (final TradeChest tradeChest : this.byName.values()) {
            
            if (ignore.contains(tradeChest)) {
                continue;
            }
            
            final String linkedName = tradeChest.getLinked();
            TradeChest linked = null;
            if (linkedName != null) {
                linked = this.byName.get(linkedName);
            }
            
            if (linked == null) {
                this.unlinkChest(this.console, tradeChest);
                continue;
            }
            
            final String checkName = linked.getLinked();
            if (checkName == null) {
                if (this.linkChests(this.console, tradeChest, linked)) {
                    ignore.add(linked);
                }
                continue;
            }
            
            if (checkName.equals(tradeChest.getName())) {
                if (this.linkChests(this.console, tradeChest, linked)) {
                    ignore.add(linked);
                }
                continue;
            }
            
            final TradeChest check = this.byName.get(checkName);
            if (check == null) {
                this.unlinkChest(this.console, linked);
                if (this.linkChests(this.console, tradeChest, linked)) {
                    ignore.add(linked);
                }
                continue;
            }
            
            final String checkLinkedName = check.getLinked();
            if (checkLinkedName == null) {
                this.unlinkChest(this.console, linked);
                if (this.linkChests(this.console, tradeChest, linked)) {
                    ignore.add(linked);
                }
                continue;
            }
            
            if (!checkLinkedName.equals(linked.getName())) {
                this.unlinkChest(this.console, linked);
                if (this.linkChests(this.console, tradeChest, linked)) {
                    ignore.add(linked);
                }
                continue;
            }
            
            this.unlinkChest(this.console, tradeChest);
        }
        
        // Scheduled Creations (right click to create) //
        
        this.scheduledCreations = new ConcurrentHashMap<UUID, String>();
        
        // Active Trades Initialization    //
        // Backup Inventory Initialization //
        
        // Active Trades
        this.activeTrades = new ConcurrentHashMap<UUID, ActiveTrade>();
        this.activeTradesFolder = new File(this.getDataFolder(), "Active_Trades");
        if (!this.activeTradesFolder.exists()) {
            if (!this.activeTradesFolder.mkdirs()) {
                throw new RuntimeException("ActiveTrade folder not created at " + this.activeTradesFolder.getPath());
            }
        } else if (!this.activeTradesFolder.isDirectory()) {
            throw new RuntimeException("ActiveTrade folder is not a folder. Location: " + this.activeTradesFolder.getPath());
        }
        
        for (final File activeTradeFile : this.activeTradesFolder.listFiles()) {
            
            final YamlConfiguration config = YamlConfiguration.loadConfiguration(activeTradeFile);
            final UUID playerId = UUID.fromString(config.getString("uuid"));
            final String playerName = config.getString("name");
            final TradeChest tradeChest = this.byName.get(config.getString("trade_chest"));
            final ActiveTrade.TradeStatus tradeStatus = ActiveTrade.TradeStatus.valueOf(config.getString("trade_status"));
            
            if (tradeChest == null) {
                this.logger.log(Level.WARNING, "Cannot re-create ActiveTrade.");
                this.logger.log(Level.WARNING, "TradeChest cannot be found: " + config.getString("trade_chest"));
                continue;
            }
            
            this.activeTrades.put(playerId, new ActiveTrade(playerId, playerName, tradeChest, tradeStatus));
        }
        
        // Backup Inventories
        this.backupInventoryFolder = new File(this.getDataFolder(), "Backup_Inventories");
        if (!this.backupInventoryFolder.exists()) {
            if (!this.backupInventoryFolder.mkdirs()) {
                throw new RuntimeException("BackupInventory folder not created at " + this.backupInventoryFolder.getPath());
            }
        } else if (!this.backupInventoryFolder.isDirectory()) {
            throw new RuntimeException("BackupInventory folder is not a folder. Location: " + this.backupInventoryFolder.getPath());
        }
        
        for (final File backupInventoryFile : this.backupInventoryFolder.listFiles()) {
            
            final YamlConfiguration config = YamlConfiguration.loadConfiguration(backupInventoryFile);
            final String tradeChestName = config.getString("trade_chest_name");
            
            final TradeChest tradeChest = this.byName.get(tradeChestName);
            if (tradeChest == null) {
                continue;
            }
            
            final Inventory backupInventory = this.getServer().createInventory(null, 27);
            final List<Map<String, Object>> items = (List<Map<String, Object>>) config.getList("items");
            
            int slot = 0;
            for (final Map<String, Object> item : items) {
                if (item == null) {
                    backupInventory.setItem(slot, null);
                } else {
                    backupInventory.setItem(slot, ItemStack.deserialize(item));
                }
                slot++;
            }
            
            slot = 0;
            final Inventory chestInventory = tradeChest.getChest().getInventory();
            for (final ItemStack backupItem : backupInventory.getStorageContents()) {
                if (backupItem == null && chestInventory.getStorageContents()[slot] == null) {
                    slot++;
                } else if (backupItem == null) {
                    chestInventory.setItem(slot, backupItem);
                    slot++;
                } else if (chestInventory.getStorageContents()[slot] == null) {
                    chestInventory.setItem(slot, backupItem);
                    slot++;
                } else if (!backupItem.equals(chestInventory.getStorageContents()[slot])) {
                    chestInventory.setItem(slot, backupItem);
                    slot++;
                }
                
            }
        }
        
        this.tradeInventories = new ConcurrentHashMap<UUID, Inventory>();
        for (final ActiveTrade activeTrade : this.activeTrades.values()) {
            if (activeTrade.getTradeStatus() == ActiveTrade.TradeStatus.DECIDE) {
                this.tradeInventories.put(activeTrade.getUniqueId(), this.createTradeInventory(activeTrade.getTradeChest().getChest().getInventory()));
            }
        }
        
        // Offline Players Initialization //
        
        this.offlineTraders = new ConcurrentHashMap<UUID, OfflineTrader>();
        this.offlineInventoryFolder = new File(this.getDataFolder(), "Offline_Inventories");
        if (!this.offlineInventoryFolder.exists()) {
            if (!this.offlineInventoryFolder.mkdirs()) {
                throw new RuntimeException("OfflineInventory folder not created at " + this.offlineInventoryFolder.getPath());
            }
        } else if (!this.offlineInventoryFolder.isDirectory()) {
            throw new RuntimeException("OfflineInventory folder is not a folder. Location: " + this.offlineInventoryFolder.getPath());
        }
        
        for (final File offlineInventoryFile : this.offlineInventoryFolder.listFiles()) {
            final YamlConfiguration config = YamlConfiguration.loadConfiguration(offlineInventoryFile);
            final UUID playerId = UUID.fromString(config.getString("uuid"));
            final String playerName = config.getString("name");
            final long logoutTime = config.getLong("logout_time");
            final OfflineTrader.CompleteReason completeReason = config.getString("complete_reason") == null ? null : OfflineTrader.CompleteReason.valueOf(config.getString("reason"));
            final List<Map<String, Object>> items = config.getList("items") == null ? null : (List<Map<String, Object>>) config.getList("items");
            
            Inventory inventory = null;
            if (items != null) {
                inventory = this.getServer().createInventory(null, 27);
                for (int index = 0; index < items.size() && index < 27; index++) {
                    final Map<String, Object> item = items.get(index);
                    ItemStack itemStack = null;
                    if (item != null) {
                        itemStack = ItemStack.deserialize(item);
                    }
                    inventory.setItem(index, itemStack);
                }
            }
            
            final OfflineTrader offlineTrader = new OfflineTrader(playerId, playerName, logoutTime);
            offlineTrader.setCompleteReason(completeReason);
            offlineTrader.setInventory(inventory);
            this.offlineTraders.put(playerId, offlineTrader);
        }
        
        // Commands //
        
        final CVTradeCommand tradeTabExecutor = new CVTradeCommand(this);
        final PluginCommand tradeCommand = this.getCommand("cvtrade");
        tradeCommand.setExecutor(tradeTabExecutor);
        tradeCommand.setTabCompleter(tradeTabExecutor);
        
        // Server Events & Tasks //
        
        this.getServer().getPluginManager().registerEvents(new CVTradeListener(this), this);
        
        this.scheduler.scheduleSyncRepeatingTask(this, () -> {
            
            for (final OfflineTrader offlineTrader : this.offlineTraders.values()) {
                
                if (offlineTrader.getLogoutTime() + CVTrade.OFFLINE_TIMEOUT >= System.currentTimeMillis() || offlineTrader.getCompleteReason() != null) {
                    continue;
                }
    
                final UUID offlinePlayerId = offlineTrader.getUniqueId();
                final ActiveTrade offlineTrade = this.activeTrades.get(offlinePlayerId);
                final TradeChest offlineChest = offlineTrade.getTradeChest();
                
                offlineTrader.setCompleteReason(OfflineTrader.CompleteReason.OFFLINE_SELF);
                offlineTrader.setInventory(offlineTrade.getTradeChest().getChest().getInventory());
                this.saveOfflineTrader(this.console, offlineTrader);
                
                this.activeTrades.remove(offlinePlayerId);
                this.deleteActiveTrade(this.console, offlineTrade);
                offlineChest.getChest().getInventory().clear();
                this.deleteChestInventory(this.console, offlineChest);
                
                final TradeChest otherChest = this.pairings.get(offlineChest);
                if (otherChest == null) {
                    continue;
                }
    
                final Iterator<ActiveTrade> tradeIterator = this.activeTrades.values().iterator();
                while (tradeIterator.hasNext()) {
                    
                    final ActiveTrade otherTrade = tradeIterator.next();
                    if (!otherTrade.getTradeChest().equals(otherChest)) {
                        continue;
                    }
                    
                    final UUID otherPlayerId = otherTrade.getUniqueId();
                    final Player otherPlayer = this.getServer().getPlayer(otherPlayerId);
                    
                    if (otherPlayer != null && otherPlayer.isOnline()) {
                        
                        otherPlayer.sendMessage("§cYour trade with§r §6" + offlineTrade.getName() + "§r §chas been cancelled as they have been offline for too long.");
                        this.itemTransfer(otherPlayer, otherTrade.getTradeChest().getChest().getInventory());
                        otherPlayer.sendMessage("§cYour items that were placed in the chest have been returned to you.");
                        
                        this.deleteActiveTrade(otherPlayer, otherTrade);
                        this.deleteChestInventory(otherPlayer, otherChest);
                        tradeIterator.remove();
                        continue;
                    }
                    
                    OfflineTrader otherOfflineTrader = this.offlineTraders.get(otherPlayerId);
                    if (otherOfflineTrader == null) {
                        otherOfflineTrader = new OfflineTrader(otherPlayerId, otherTrade.getName(), System.currentTimeMillis() - CVTrade.OFFLINE_TIMEOUT);
                    }
                    
                    if (otherOfflineTrader.getCompleteReason() == null) {
                        otherOfflineTrader.setCompleteReason(OfflineTrader.CompleteReason.OFFLINE_OTHER);
                        otherOfflineTrader.setInventory(otherTrade.getTradeChest().getChest().getInventory());
                        this.offlineTraders.put(otherPlayerId, otherOfflineTrader);
                        this.saveOfflineTrader(this.console, otherOfflineTrader);
                    }
                    
                    this.deleteActiveTrade(this.console, otherTrade);
                    otherChest.getChest().getInventory().clear();
                    this.deleteTradeChest(this.console, otherChest);
                    tradeIterator.remove();
                }
            }
        }, 10L * 20L, 10L * 20L);
    }
    
    ///////////////////////////
    // EVENT HANDLER METHODS //
    ///////////////////////////
    
    public boolean inventoryClick(@NotNull final Player player, @NotNull final Inventory inventory, final int slot) {
        
        final UUID playerId = player.getUniqueId();
        final ActiveTrade activeTrade = this.activeTrades.get(playerId);
        if (activeTrade == null) {
            return false;
        }
    
        if (activeTrade.getTradeStatus() != ActiveTrade.TradeStatus.DECIDE) {
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
            this.logger.log(Level.WARNING, "TradeChest: " + activeTrade.getTradeChest().getName());
            this.logger.log(Level.WARNING, "Trade Status: " + activeTrade.getTradeStatus().name());
            this.logger.log(Level.WARNING, "ISSUE:");
            this.logger.log(Level.WARNING, "Player " + player.getName() + " is attempting to decide on a trade.");
            this.logger.log(Level.WARNING, "No trade inventory found for player in DECIDE or later phase.");
            return false;
        }
        
        if (!this.checkInventories(inventory, tradeInventory)) {
            return false;
        }
        
        if (slot == CVTrade.SLOT_REJECT) {
            this.cancelTrade(player, false);
        } else if (slot == CVTrade.SLOT_ACCEPT) {
            this.acceptTrade(player);
        }
        
        return true;
    }
    
    public void performCreate(@NotNull final Player player, @NotNull final Block block) {
        
        if (block.getType() == Material.AIR) {
            return;
        }
        
        if (!this.scheduledCreations.containsKey(player.getUniqueId())) {
            return;
        }
        
        final String name = this.scheduledCreations.remove(player.getUniqueId());
        if (name == null) {
            player.sendMessage("§cThere was an error with your TradeChest creation.");
            player.sendMessage("§cNo name was saved when you entered the command to create the TradeChest.");
            player.sendMessage("§cPlease try the command again. If the issue persists, please contact a server administrator.");
            return;
        }
        
        final TradeChest checkNameChest = this.byName.get(name);
        if (checkNameChest != null) {
            player.sendMessage("§cSomeone has registered a TradeChest with that name between the time that you entered the command and clicked on the Chest.");
            player.sendMessage("§cPlease check with your teammates in case you are both working on the same thing without realizing it.");
            return;
        }
        
        if (!(block.getState() instanceof Chest)) {
            player.sendMessage("§cThat is not a Chest. Please re-run the create command, and left-click on a Chest.");
            return;
        }
        
        final Chest chest = (Chest) block.getState();
        if (chest.getInventory().getHolder() instanceof DoubleChest) {
            player.sendMessage("§cYou may not use a DoubleChest to create a TradeChest.");
            return;
        }
        
        final TradeChest checkLocationChest = this.byLocation.get(chest.getLocation());
        if (checkLocationChest != null) {
            player.sendMessage("§cThis Chest is already registered§r §6(" + checkLocationChest.getName() + ")§r§c.");
            return;
        }
        
        final TradeChest tradeChest = new TradeChest(name, chest);
        this.byName.put(name, tradeChest);
        this.byLocation.put(chest.getLocation(), tradeChest);
        this.saveTradeChest(player, tradeChest);
        
        player.sendMessage("§aTradeChest§r §6" + name + "§r §acreated successfully.");
        
        boolean linked = false;
        final Iterator<Map.Entry<TradeChest, TradeChest>> iterator = this.pairings.entrySet().iterator();
        while (iterator.hasNext()) {
            
            final Map.Entry<TradeChest, TradeChest> entry = iterator.next();
            if (entry.getValue() != null) {
                continue;
            }
            
            final TradeChest unpairedChest = entry.getKey();
            final String linkedName = unpairedChest.getLinked();
            if (linkedName == null) {
                continue;
            }
            if (!linkedName.equals(name)) {
                continue;
            }
            if (linked) {
                this.unlinkChest(player, unpairedChest);
                continue;
            }
            
            if (!this.linkChests(player, tradeChest, unpairedChest)) {
                continue;
            }
            player.sendMessage("&bThis TradeChest has been automatically linked with another TradeChest§r §6(" + unpairedChest.getName() + ")§r§b.");
            linked = true;
            iterator.remove();
        }
    }
    
    public boolean rightClickedBlock(@NotNull final Player player, @NotNull final Block block) {
        
        if (!(block.getState() instanceof Chest)) {
            return false;
        }
        
        final TradeChest tradeChest = this.byLocation.get(block.getLocation());
        if (tradeChest == null) {
            return false;
        }
        
        ActiveTrade activeTrade = null;
        for (final ActiveTrade checkTrade : this.activeTrades.values()) {
            if (checkTrade.getTradeChest().equals(tradeChest)) {
                activeTrade = checkTrade;
                break;
            }
        }
        
        if (activeTrade == null) {
            return false;
        }
        
        if (!activeTrade.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage("§cThis chest is a TradeChest, and you are not actively using this chest for a trade.");
            player.sendMessage("§cPlease start a trade using this chest. If you did start a trade using this chest, please report this to a server administrator.");
            return true;
        }
        
        if (activeTrade.getTradeStatus().ordinal() >= ActiveTrade.TradeStatus.READY.ordinal()) {
            player.sendMessage("§cYou cannot edit the contents of this chest after saying you are ready to trade.");
            player.sendMessage("§6If you need to change the items you want to trade, please cancel the trade and restart it.");
            return true;
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
    
            final TradeChest tradeChest = activeTrade.getTradeChest();
            final TradeChest linkedChest = this.pairings.get(tradeChest);
            if (linkedChest == null) {
                
                player.sendMessage("§cThere was an error resuming your trade. Please report this to a server administrator.");
                this.logger.log(Level.WARNING, "ISSUE WHILE AUTOMATICALLY RESTARTING TRADE DURING PLAYER JOIN");
                this.logger.log(Level.WARNING, "Details below:");
                this.logger.log(Level.WARNING, "Player Name: " + player.getName());
                this.logger.log(Level.WARNING, "Player UUID: " + playerId.toString());
                this.logger.log(Level.WARNING, "TradeChest: " + tradeChest.getName());
                this.logger.log(Level.WARNING, "Configured Linked TradeChest: " + tradeChest.getLinked());
                this.logger.log(Level.WARNING, "ISSUE:");
                this.logger.log(Level.WARNING, "Player " + player.getName() + " has rejoined the server during a trade.");
                this.logger.log(Level.WARNING, "No linked TradeChest to the one listed above.");
    
                this.itemTransfer(player, tradeChest.getChest().getInventory());
                if (this.tradeInventories.containsKey(playerId)) {
                    if (this.checkInventories(player.getOpenInventory().getTopInventory(), this.tradeInventories.get(playerId))) {
                        player.closeInventory();
                    }
                    this.tradeInventories.remove(playerId);
                }
                
                this.activeTrades.remove(playerId);
                this.deleteActiveTrade(player, activeTrade);
                this.deleteChestInventory(player, tradeChest);
                player.sendMessage("§cYour trade has been automatically cancelled. Any items you put in the chest have been automatically returned to you.");
                
                return;
            }
            
            ActiveTrade linkedTrade = null;
            for (final ActiveTrade checkTrade : this.activeTrades.values()) {
                if (checkTrade.getTradeChest().equals(linkedChest)) {
                    linkedTrade = checkTrade;
                    break;
                }
            }
            
            if (linkedTrade == null) {
                return;
            }
            
            final UUID otherPlayerId = linkedTrade.getUniqueId();
            final Player otherPlayer = this.getServer().getPlayer(otherPlayerId);
            if (otherPlayer != null && otherPlayer.isOnline()) {
                
                player.sendMessage("§aYou were trading with§r §6" + otherPlayer.getName() + "§r§a.");
                player.sendMessage("§aYour trade has been restarted where you left off.");
                
                otherPlayer.sendMessage("§6" + player.getName() + "§r §ahas logged back in.");
                otherPlayer.sendMessage("§aYour trade can continue.");
                
                if (activeTrade.getTradeStatus() == ActiveTrade.TradeStatus.DECIDE) {
                    player.openInventory(this.tradeInventories.get(playerId));
                }
                if (linkedTrade.getTradeStatus() == ActiveTrade.TradeStatus.DECIDE) {
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
                this.logger.log(Level.WARNING, "TradeChest: " + tradeChest.getName());
                this.logger.log(Level.WARNING, "Linked TradeChest: " + linkedChest.getName());
                this.logger.log(Level.WARNING, "Other Player Name: " + linkedTrade.getName());
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
                
                this.itemTransfer(player, tradeChest.getChest().getInventory());
                this.activeTrades.remove(playerId);
                this.deleteActiveTrade(player, activeTrade);
                this.deleteChestInventory(player, tradeChest);
                player.sendMessage("§cYour trade has been automatically cancelled. Any items you put in the chest have been automatically returned to you.");
                
                otherOfflineTrader = new OfflineTrader(otherPlayerId, linkedTrade.getName(), System.currentTimeMillis() - CVTrade.OFFLINE_TIMEOUT);
                otherOfflineTrader.setCompleteReason(OfflineTrader.CompleteReason.ERROR);
                otherOfflineTrader.setInventory(linkedChest.getChest().getInventory());
                this.offlineTraders.put(otherPlayerId, otherOfflineTrader);
                this.saveOfflineTrader(this.console, otherOfflineTrader);
                
                this.activeTrades.remove(otherPlayerId);
                this.deleteActiveTrade(this.console, linkedTrade);
                linkedChest.getChest().getInventory().clear();
                this.deleteChestInventory(this.console, linkedChest);
                
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
        
        final TradeChest tradeChest = activeTrade.getTradeChest();
        final TradeChest linkedChest = this.pairings.get(tradeChest);
        
        // No linked chest, this is some type of error.
        if (linkedChest == null) {
            
            this.logger.log(Level.WARNING, "ISSUE WHILE PLAYER LEFT DURING A TRADE");
            this.logger.log(Level.WARNING, "Details below:");
            this.logger.log(Level.WARNING, "Player Name: " + player.getName());
            this.logger.log(Level.WARNING, "Player UUID: " + playerId.toString());
            this.logger.log(Level.WARNING, "TradeChest: " + tradeChest.getName());
            this.logger.log(Level.WARNING, "Configured Linked TradeChest: " + tradeChest.getLinked());
            this.logger.log(Level.WARNING, "ISSUE:");
            this.logger.log(Level.WARNING, "Player " + player.getName() + " has left during a trade.");
            this.logger.log(Level.WARNING, "No linked TradeChest to the one listed above.");
            
            if (this.tradeInventories.containsKey(playerId)) {
                this.tradeInventories.remove(playerId);
            }
            
            offlineTrader.setCompleteReason(OfflineTrader.CompleteReason.ERROR);
            offlineTrader.setInventory(tradeChest.getChest().getInventory());
            this.saveOfflineTrader(this.console, offlineTrader);
    
            this.activeTrades.remove(playerId);
            this.deleteActiveTrade(this.console, activeTrade);
            tradeChest.getChest().getInventory().clear();
            this.deleteChestInventory(this.console, tradeChest);
            return;
        }
    
        this.saveOfflineTrader(this.console, offlineTrader);
        
        ActiveTrade linkedTrade = null;
        for (final ActiveTrade checkTrade : this.activeTrades.values()) {
            if (checkTrade.getTradeChest().equals(linkedChest)) {
                linkedTrade = checkTrade;
                break;
            }
        }
        
        // No one else has started to trade using the linked chest.
        if (linkedTrade == null) {
            return;
        }
        
        // Someone else has started to trade using the linked chest.
        final UUID otherPlayerId = linkedTrade.getUniqueId();
        final Player otherPlayer = this.getServer().getPlayer(otherPlayerId);
        if (otherPlayer != null && otherPlayer.isOnline()) {
            otherPlayer.sendMessage("§b" + player.getName() + "§r §6has logged out. They have 5 minutes to log back in, or your trade with them will be automatically cancelled.");
            otherPlayer.sendMessage("§6You may also cancel the trade early yourself.");
        }
    }
    
    /////////////////////
    // COMMAND METHODS //
    /////////////////////
    
    public boolean isTrading(@NotNull final UUID playerId) {
        return this.activeTrades.containsKey(playerId);
    }
    
    public boolean scheduleCreate(@NotNull final UUID playerId, @NotNull final String name) {
        
        if (this.byName.get(name) != null) {
            return false;
        }
        
        final String oldName = this.scheduledCreations.put(playerId, name);
        if (oldName != null) {
            final Player player = this.getServer().getPlayer(playerId);
            if (player != null) {
                player.sendMessage("§cWARNING§r: §6Your old request to create a TradeChest named§r §b" + oldName + "§r §6has been cancelled.");
            }
            this.logger.log(Level.INFO, "Un-scheduled previous TradeChest '" + oldName + "' creation for UUID '" + playerId.toString() + "'.");
        }
        
        return true;
    }
    
    public void linkChests(@NotNull final Player player, @NotNull final String name1, @NotNull final String name2) {
        
        final TradeChest chest1 = this.byName.get(name1);
        if (chest1 == null) {
            player.sendMessage("§cThe TradeChest§r §6" + name1 + "§r §cdoes not exist. Please verify that you spelled the name correctly.");
            return;
        }
        
        final TradeChest chest2 = this.byName.get(name2);
        if (chest2 == null) {
            player.sendMessage("§cThe TradeChest§r §6" + name2 + "§r §cdoes not exist. Please verify that you spelled the name correctly.");
            return;
        }
        
        final TradeChest check1 = this.pairings.get(chest1);
        final TradeChest check2 = this.pairings.get(chest2);
        
        // No links set up yet. Link the TradeChests.
        if (check1 == null && check2 == null) {
            
            if (this.linkChests(player, chest1, chest2)) {
                player.sendMessage("§aSuccessfully linked the TradeChests.");
            }
            
        // No link on Chest1 yet.
        } else if (check1 == null) {
            
            // Chest2 is linked to Chest1. Finish the link.
            if (check2.equals(chest1)) {
                if (this.linkChests(player, chest1, chest2)) {
                    player.sendMessage("§6Finished linking the TradeChests (was partially linked before).");
                }
                
            // Chest2 is linked to some "Chest3".
            } else {
                final TradeChest check3 = this.pairings.get(check2);
                
                // "Chest3" has no link. Link Chest1 and Chest2.
                if (check3 == null) {
                    if (this.linkChests(player, chest1, chest2)) {
                        player.sendMessage("§6Corrected an incorrectly-linked TradeChest setup. The link has been completed.");
                    }
                    
                // "Chest3" is linked to Chest2. Unlink Chest1.
                } else if (check3.equals(chest2)) {
                    this.unlinkChest(player, chest1);
                    player.sendMessage("§cTradeChest§r §6" + name2 + "§r §cis already linked to another TradeChest.");
                    
                // "Chest3" is linked to some "Chest4". Re-link Chest1 and Chest2.
                } else {
                    if (this.linkChests(player, chest1, chest2)) {
                        player.sendMessage("§6Corrected an incorrectly-linked TradeChest setup. The link has been completed.");
                    }
                }
            }
            
        // No link on Chest2 yet.
        } else if (check2 == null) {
            
            // Chest1 is linked to Chest2. Finish the link.
            if (check1.equals(chest2)) {
                if (this.linkChests(player, chest1, chest2)) {
                    player.sendMessage("§6Finished linking the TradeChests (was partially linked before).");
                }
                
            // Chest1 is linked to some "Chest3".
            } else {
                final TradeChest check3 = this.pairings.get(check1);
                
                // "Chest3" has no link. Link Chest1 and Chest2.
                if (check3 == null) {
                    if (this.linkChests(player, chest1, chest2)) {
                        player.sendMessage("§6Corrected an incorrectly-linked TradeChest setup. The link has been completed.");
                    }
                    
                // "Chest3" is linked to Chest1. Unlink Chest2.
                } else if (check3.equals(chest1)) {
                    this.unlinkChest(player, chest2);
                    player.sendMessage("§cTradeChest§r §6" + name1 + "§r §cis already linked to another TradeChest.");
    
                    // "Chest3" is linked to some "Chest4". Re-link Chest1 and Chest2.
                } else {
                    if (this.linkChests(player, chest1, chest2)) {
                        player.sendMessage("§6Corrected an incorrectly-linked TradeChest setup. The link has been completed.");
                    }
                }
            }
            
        // Chest1 and Chest2 are already linked.
        } else if (check1.equals(chest2) && check2.equals(chest1)) {
            
            player.sendMessage("§6The TradeChests are already linked.");
            
        // Chest1 is linked to Chest2, Chest2 is linked to some "Chest3".
        } else if (check1.equals(chest2)) {
            final TradeChest check3 = this.pairings.get(check2);
    
            // "Chest3" has no link. Link Chest1 and Chest2.
            if (check3 == null) {
                if (this.linkChests(player, chest1, chest2)) {
                    player.sendMessage("§6Corrected an incorrectly-linked TradeChest setup. The link has been completed.");
                }
        
                // "Chest3" is linked to Chest2. Unlink Chest1.
            } else if (check3.equals(chest2)) {
                this.unlinkChest(player, chest1);
                player.sendMessage("§cTradeChest§r §6" + name2 + "§r §cis already linked to another TradeChest.");
        
                // "Chest3" is linked to some "Chest4". Re-link Chest1 and Chest2.
            } else {
                if (this.linkChests(player, chest1, chest2)) {
                    player.sendMessage("§6Corrected an incorrectly-linked TradeChest setup. The link has been completed.");
                }
            }
            
        // Chest2 is linked to Chest1, Chest1, is linked to some "Chest3".
        } else if (check2.equals(chest1)) {
            final TradeChest check3 = this.pairings.get(check1);
    
            // "Chest3" has no link. Link Chest1 and Chest2.
            if (check3 == null) {
                if (this.linkChests(player, chest1, chest2)) {
                    player.sendMessage("§6Corrected an incorrectly-linked TradeChest setup. The link has been completed.");
                }
        
            // "Chest3" is linked to Chest1. Unlink Chest2.
            } else if (check3.equals(chest1)) {
                this.unlinkChest(player, chest2);
                player.sendMessage("§cTradeChest§r §6" + name1 + "§r §cis already linked to another TradeChest.");
        
            // "Chest3" is linked to some "Chest4". Re-link Chest1 and Chest2.
            } else {
                if (this.linkChests(player, chest1, chest2)) {
                    player.sendMessage("§6Corrected an incorrectly-linked TradeChest setup. The link has been completed.");
                }
            }
            
        // Neither TradeChest is linked to the other.
        } else {
            boolean canLink = false;
            
            final TradeChest check3 = this.pairings.get(check1);
            if (check3 == null) {
                if (this.linkChests(player, chest1, check1)) {
                    player.sendMessage("§cTradeChest§r §6" + name1 + "§r §cwas partially linked to another TradeChest, correcting that link.");
                    player.sendMessage("§cNo link will be made between§r §6" + name1 + "§r §cand§r §6" + name2 + "§r §c.");
                }
            } else if (check3.equals(chest1)) {
                player.sendMessage("§cTradeChest§r §6" + name1 + "§r §cis linked to another TradeChest, correcting that link.");
                player.sendMessage("§cNo link will be made between§r §6" + name1 + "§r §cand§r §6" + name2 + "§r §c.");
            } else {
                this.unlinkChest(player, chest1);
                canLink = true;
            }
            
            final TradeChest check4 = this.pairings.get(check2);
            if (check4 == null) {
                if (this.linkChests(player, chest2, check2)) {
                    player.sendMessage("§cTradeChest§r §6" + name2 + "§r §cwas partially linked to another TradeChest, correcting that link.");
                    player.sendMessage("§cNo link will be made between§r §6" + name1 + "§r §cand§r §6" + name2 + "§r §c.");
                }
            } else if (check4.equals(chest2)) {
                player.sendMessage("§cTradeChest§r §6" + name2 + "§r §cis linked to another TradeChest, correcting that link.");
                player.sendMessage("§cNo link will be made between§r §6" + name1 + "§r §cand§r §6" + name2 + "§r §c.");
            } else if (canLink) {
                if (this.linkChests(player, chest1, chest2)) {
                    player.sendMessage("§6There were bad links between the 2 TradeChests and others. They have been cleaned up and the 2 TradeChests linked.");
                }
            } else {
                this.unlinkChest(player, chest2);
                player.sendMessage("§cUnable to link the TradeChests due to other existing links to other TradeChests.");
            }
        }
    }
    
    public void unlinkChest(@NotNull final Player player, @NotNull final String name) {
    
        final TradeChest tradeChest = this.byName.get(name);
        if (tradeChest == null) {
            player.sendMessage("§cThe TradeChest§r §6" + name + "§r §cdoes not exist. Please verify that you spelled the name correctly.");
            return;
        }
        
        final TradeChest linkedChest = this.pairings.get(tradeChest);
        if (linkedChest == null) {
            player.sendMessage("§6This TradeChest is not linked to any other TradeChest. No action required.");
            return;
        }
        
        for (final ActiveTrade activeTrade : this.activeTrades.values()) {
            if (activeTrade.getTradeChest().equals(tradeChest)) {
                player.sendMessage("§cThis TradeChest is being used in a trade at the moment. You may not unlink it.");
                return;
            }
            if (activeTrade.getTradeChest().equals(linkedChest)) {
                player.sendMessage("§cThe TradeChest that is linked to this TradeChest is being used in a trade at the moment. You may not unlink it.");
                return;
            }
        }
        
        final TradeChest checkChest = this.pairings.get(linkedChest);
        if (checkChest == null) {
            player.sendMessage("§6The link was not fully set up. No unlinking required on the linked TradeChest.");
        } else if (!checkChest.equals(tradeChest)) {
            player.sendMessage("§6Bad link from this TradeChest to its linked TradeChest. No unlinking required on the linked TradeChest.");
        } else {
            this.unlinkChest(player, linkedChest);
            player.sendMessage("§aLinked TradeChest unlinked from this TradeChest successfully.");
        }
        
        this.unlinkChest(player, tradeChest);
        player.sendMessage("§aThis TradeChest unlinked from its linked TradeChest successfully.");
        
        final HashSet<TradeChest> unlinked = new HashSet<TradeChest>();
        final Iterator<Map.Entry<TradeChest, TradeChest>> iterator = this.pairings.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<TradeChest, TradeChest> entry = iterator.next();
            if (entry.getValue().equals(tradeChest)) {
                unlinked.add(entry.getKey());
                iterator.remove();
            }
        }
        
        for (final TradeChest unlinkedChest : unlinked) {
            this.unlinkChest(player, unlinkedChest);
        }
        
        if (!unlinked.isEmpty()) {
            player.sendMessage("§aOther TradeChests had their links to this TradeChest removed.");
        }
    }
    
    public void deleteChest(@NotNull final Player player, @NotNull final String name) {
        
        final TradeChest tradeChest = this.byName.remove(name);
        if (tradeChest == null) {
            player.sendMessage("§cThe TradeChest§r §6" + name + "§r §cdoes not exist. Please verify that you spelled the name correctly.");
            return;
        }
        
        for (final ActiveTrade activeTrade : this.activeTrades.values()) {
            if (activeTrade.getTradeChest().equals(tradeChest)) {
                player.sendMessage("§cThat TradeChest is being used in a trade at the moment. You may not delete it.");
                return;
            }
        }
        
        final TradeChest linkedChest = this.pairings.get(tradeChest);
        if (linkedChest != null) {
            for (final ActiveTrade activeTrade : this.activeTrades.values()) {
                if (activeTrade.getTradeChest().equals(linkedChest)) {
                    player.sendMessage("§cThe TradeChest linked to that TradeChest is being used in a trade at the moment. You may not delete it.");
                    return;
                }
            }
        }
        
        this.byLocation.remove(tradeChest.getChest().getLocation());
        this.pairings.remove(tradeChest);
        this.deleteTradeChest(player, tradeChest);
    }
    
    public void listChests(@NotNull final Player player, final boolean small) {
    
        player.sendMessage("§8================================");
        player.sendMessage("§6Trade Chests§r §f-§r §b(" + this.byName.size() + ")");
        player.sendMessage("§8--------------------------------");
    
        if (small) {
            final StringBuilder builder = new StringBuilder();
            final Iterator<TradeChest> iterator = this.byName.values().iterator();
            while (iterator.hasNext()) {
                builder.append("§a").append(iterator.next().getName());
                if (iterator.hasNext()) {
                    builder.append(", §b").append(iterator.next().getName());
                }
                if (iterator.hasNext()) {
                    builder.append(", ");
                }
            }
            
            player.sendMessage(builder.toString());
            player.sendMessage("§8================================");
            return;
        }
        
        for (final TradeChest tradeChest : this.byName.values()) {
            player.sendMessage(" §f-§r §a" + tradeChest.getName());
        }
    
        player.sendMessage("§8================================");
    }
    
    public void findChest(@NotNull final Player player, final int radius) {
        
        if (radius < 1 || radius > 50) {
            player.sendMessage("§6" + radius + "§r §cis not a valid search radius.");
            player.sendMessage("§cPlease specify a positive integer between 1 and 50 (inclusive) for the radius.");
            return;
        }
        
        final Location location = player.getLocation();
        final UUID worldId = location.getWorld().getUID();
        final ConcurrentHashMap<Location, TradeChest> sameWorld = new ConcurrentHashMap<Location, TradeChest>();
        for (final Map.Entry<Location, TradeChest> entry : this.byLocation.entrySet()) {
            if (entry.getKey().getWorld().getUID().equals(worldId)) {
                sameWorld.put(entry.getKey(), entry.getValue());
            }
        }
        
        if (sameWorld.isEmpty()) {
            player.sendMessage("§6There are no TradeChests in the same World as you.");
            return;
        }
        
        final ArrayList<String> nearby = new ArrayList<String>();
        for (final Map.Entry<Location, TradeChest> entry : sameWorld.entrySet()) {
            if (location.distance(entry.getKey()) <= (double) radius) {
                nearby.add(entry.getValue().getName());
            }
        }
        
        if (nearby.isEmpty()) {
            player.sendMessage("§6There are no TradeChests in the same World as you.");
            return;
        }
    
        player.sendMessage("§8================================");
        player.sendMessage("§6Trade Chests within a radius of " + radius + "§r§f:");
        player.sendMessage("§8--------------------------------");
        
        for (final String name : nearby) {
            player.sendMessage(" §f-§r §a" + name);
        }
    
        player.sendMessage("§8================================");
    }
    
    public void infoChest(@NotNull final Player sender, @NotNull final String name) {
    
        final TradeChest tradeChest = this.byName.get(name);
        if (tradeChest == null) {
            sender.sendMessage("§cThe TradeChest§r §6" + name + "§r §cdoes not exist. Please verify that you spelled the name correctly.");
            return;
        }
        
        final Location location = tradeChest.getChest().getLocation();
        final TradeChest linkedChest = this.pairings.get(tradeChest);
        
        sender.sendMessage("§8================================");
        sender.sendMessage("§fName:§r §6" + tradeChest.getName());
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
    
        sender.sendMessage("§8================================");
    }
    
    public void startTrade(@NotNull final Player player, @NotNull final String name) {
        
        final UUID playerId = player.getUniqueId();
        if (this.scheduledCreations.containsKey(playerId)) {
            player.sendMessage("§cYou are trying to create a TradeChest right now. Please finish the creation first.");
            return;
        }
        
        if (this.activeTrades.containsKey(playerId)) {
            player.sendMessage("§cYou have already started a trade. You cannot start another until this one is complete.");
            return;
        }
        
        final TradeChest tradeChest = this.byName.get(name);
        if (tradeChest == null) {
            player.sendMessage("§cThe TradeChest§r §6" + name + "§r §cdoes not exist. Please verify that you spelled the name correctly.");
            return;
        }
        
        final TradeChest linkedChest = this.pairings.get(tradeChest);
        if (linkedChest == null) {
            
            player.sendMessage("§cThe TradeChest that you selected is not linked to another TradeChest. Please report this to the server administrators.");
            this.logger.log(Level.WARNING, "ISSUE WHILE STARTING A TRADE");
            this.logger.log(Level.WARNING, "Details below:");
            this.logger.log(Level.WARNING, "Player Name: " + player.getName());
            this.logger.log(Level.WARNING, "Player UUID: " + playerId.toString());
            this.logger.log(Level.WARNING, "TradeChest: " + tradeChest.getName());
            this.logger.log(Level.WARNING, "Configured Linked TradeChest: " + tradeChest.getLinked());
            this.logger.log(Level.WARNING, "ISSUE:");
            this.logger.log(Level.WARNING, "Player " + player.getName() + " is attempting to start a trade.");
            this.logger.log(Level.WARNING, "No linked TradeChest to the one listed above.");
            return;
        }
        
        for (final ActiveTrade checkTrade : this.activeTrades.values()) {
            if (checkTrade.getTradeChest().equals(tradeChest)) {
                player.sendMessage("§cThe TradeChest you selected is already in use in another trade. Please pick a different chest to use.");
                return;
            }
        }
        
        ActiveTrade linkedTrade = null;
        for (final ActiveTrade checkTrade : this.activeTrades.values()) {
            if (checkTrade.getTradeChest().equals(linkedChest)) {
                linkedTrade = checkTrade;
                break;
            }
        }
        
        if (linkedTrade == null) {
            
            player.sendMessage("§6Waiting for the other player to begin the trade...");
            player.sendMessage("§aYou may begin placing the items you wish to trade into the chest.");
    
            final ActiveTrade activeTrade = new ActiveTrade(player, tradeChest);
            this.activeTrades.put(playerId, activeTrade);
            this.saveActiveTrade(player, activeTrade);
            
            return;
        }
    
        final UUID otherPlayerId = linkedTrade.getUniqueId();
        final Player otherPlayer = this.getServer().getPlayer(otherPlayerId);
        if (otherPlayer != null && otherPlayer.isOnline()) {
    
            final ActiveTrade activeTrade = new ActiveTrade(player, tradeChest);
            this.activeTrades.put(playerId, activeTrade);
            this.saveActiveTrade(player, activeTrade);
            
            player.sendMessage("§aYou are trading with§r §6" + otherPlayer.getName() + "§r§a.");
            player.sendMessage("§aYou may begin placing the items you wish to trade into the chest.");
            otherPlayer.sendMessage("§aYou are now trading with§r §6" + player.getName() + "§r§a.");
            return;
        }
        
        OfflineTrader otherOfflineTrader = this.offlineTraders.get(otherPlayerId);
        if (otherOfflineTrader == null) {
    
            player.sendMessage("§cThere was an error starting your trade. Please report this to a server administrator.");
            this.logger.log(Level.WARNING, "ISSUE WHILE STARTING A TRADE");
            this.logger.log(Level.WARNING, "Details below:");
            this.logger.log(Level.WARNING, "Player Name: " + player.getName());
            this.logger.log(Level.WARNING, "Player UUID: " + playerId.toString());
            this.logger.log(Level.WARNING, "TradeChest: " + tradeChest.getName());
            this.logger.log(Level.WARNING, "Linked TradeChest: " + linkedChest.getName());
            this.logger.log(Level.WARNING, "Other Player Name: " + linkedTrade.getName());
            this.logger.log(Level.WARNING, "Other Player UUID: " + otherPlayerId.toString());
            this.logger.log(Level.WARNING, "ISSUE:");
            this.logger.log(Level.WARNING, "Player " + player.getName() + " is attempting to start a trade.");
            this.logger.log(Level.WARNING, "Other player is not online, has an ActiveTrade, but does not have an OfflineTrader object.");
    
            this.itemTransfer(player, tradeChest.getChest().getInventory());
            player.sendMessage("§cYour trade has been automatically cancelled. Any items you put in the chest have been automatically returned to you.");
    
            otherOfflineTrader = new OfflineTrader(otherPlayerId, linkedTrade.getName(), System.currentTimeMillis() - CVTrade.OFFLINE_TIMEOUT);
            otherOfflineTrader.setCompleteReason(OfflineTrader.CompleteReason.ERROR);
            otherOfflineTrader.setInventory(linkedChest.getChest().getInventory());
            this.offlineTraders.put(otherPlayerId, otherOfflineTrader);
            this.saveOfflineTrader(this.console, otherOfflineTrader);
            
            this.activeTrades.remove(otherPlayerId);
            this.deleteActiveTrade(this.console, linkedTrade);
            linkedChest.getChest().getInventory().clear();
            this.deleteChestInventory(this.console, linkedChest);
            return;
        }
    
        final ActiveTrade activeTrade = new ActiveTrade(player, tradeChest);
        this.activeTrades.put(playerId, activeTrade);
        this.saveActiveTrade(player, activeTrade);
    
        player.sendMessage("§6You are trading with§r §b" + otherPlayer.getName() + "§r§6; Please note that they are offline currently.");
        player.sendMessage("§6If they do not log in within the next§r §b" + this.formatTime(otherOfflineTrader.getLogoutTime()) + "§r§6, the trade will automatically be cancelled.");
        player.sendMessage("§aYou may begin placing the items you wish to trade into the chest.");
    }
    
    public void cancelTrade(@NotNull final Player player, final boolean cancel) {
        
        final UUID playerId = player.getUniqueId();
        final ActiveTrade activeTrade = this.activeTrades.get(playerId);
        if (activeTrade == null) {
            player.sendMessage("§cYou are not currently in a trade with anyone.");
            return;
        }
        
        final TradeChest tradeChest = activeTrade.getTradeChest();
        final TradeChest linkedChest = this.pairings.get(tradeChest);
        if (linkedChest == null) {
            
            player.sendMessage("§cThere was an error while " + (cancel ? "cancelling" : "rejecting") + " your trade, please report this to a server administrator.");
            this.logger.log(Level.WARNING, "ISSUE WHILE CANCELLING/REJECTING A TRADE");
            this.logger.log(Level.WARNING, "Details below:");
            this.logger.log(Level.WARNING, "Player Name: " + player.getName());
            this.logger.log(Level.WARNING, "Player UUID: " + playerId.toString());
            this.logger.log(Level.WARNING, "TradeChest: " + tradeChest.getName());
            this.logger.log(Level.WARNING, "Configured Linked TradeChest: " + tradeChest.getLinked());
            this.logger.log(Level.WARNING, "Cancel/Reject: " + (cancel ? "CANCEL" : "REJECT"));
            this.logger.log(Level.WARNING, "ISSUE:");
            this.logger.log(Level.WARNING, "Player " + player.getName() + " is attempting to cancel a trade.");
            this.logger.log(Level.WARNING, "No linked TradeChest to the one listed above.");
            return;
        }
        
        ActiveTrade linkedTrade = null;
        for (final ActiveTrade checkTrade : this.activeTrades.values()) {
            if (checkTrade.getTradeChest().equals(linkedChest)) {
                linkedTrade = checkTrade;
                break;
            }
        }
        
        if (linkedTrade == null) {
    
            this.itemTransfer(player, tradeChest.getChest().getInventory());
            if (this.tradeInventories.containsKey(playerId)) {
                if (this.checkInventories(player.getOpenInventory().getTopInventory(), this.tradeInventories.get(playerId))) {
                    player.closeInventory();
                }
                this.tradeInventories.remove(playerId);
            }
            
            this.activeTrades.remove(playerId);
            this.deleteActiveTrade(player, activeTrade);
            this.deleteChestInventory(player, tradeChest);
            player.sendMessage("§aYour trade has been " + (cancel ? "cancelled" : "rejected") + ". Any items you put in the chest have been automatically returned to you.");
            return;
        }
        
        final UUID otherPlayerId = linkedTrade.getUniqueId();
        final Player otherPlayer = this.getServer().getPlayer(otherPlayerId);
        if (otherPlayer != null && otherPlayer.isOnline()) {
    
            this.itemTransfer(player, tradeChest.getChest().getInventory());
            if (this.tradeInventories.containsKey(playerId)) {
                if (this.checkInventories(player.getOpenInventory().getTopInventory(), this.tradeInventories.get(playerId))) {
                    player.closeInventory();
                }
                this.tradeInventories.remove(playerId);
            }
            
            this.activeTrades.remove(playerId);
            this.deleteActiveTrade(player, activeTrade);
            this.deleteChestInventory(player, tradeChest);
            player.sendMessage("§aYour trade has been " + (cancel ? "cancelled" : "rejected") + ". Any items you put in the chest have been automatically returned to you.");
    
            this.itemTransfer(otherPlayer, linkedChest.getChest().getInventory());
            if (this.tradeInventories.containsKey(otherPlayerId)) {
                if (this.checkInventories(otherPlayer.getOpenInventory().getTopInventory(), this.tradeInventories.get(otherPlayerId))) {
                    otherPlayer.closeInventory();
                }
                this.tradeInventories.remove(otherPlayerId);
            }
            
            this.activeTrades.remove(otherPlayerId);
            this.deleteActiveTrade(otherPlayer, linkedTrade);
            this.deleteChestInventory(otherPlayer, linkedChest);
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
            this.logger.log(Level.WARNING, "TradeChest: " + tradeChest.getName());
            this.logger.log(Level.WARNING, "Linked TradeChest: " + linkedChest.getName());
            this.logger.log(Level.WARNING, "Other Player Name: " + linkedTrade.getName());
            this.logger.log(Level.WARNING, "Other Player UUID: " + otherPlayerId.toString());
            this.logger.log(Level.WARNING, "Cancel/Reject: " + (cancel ? "CANCEL" : "REJECT"));
            this.logger.log(Level.WARNING, "ISSUE:");
            this.logger.log(Level.WARNING, "Player " + player.getName() + " is attempting to cancel a trade.");
            this.logger.log(Level.WARNING, "Other player is not online, has an ActiveTrade, but does not have an OfflineTrader object.");
    
            otherOfflineTrader = new OfflineTrader(otherPlayerId, linkedTrade.getName(), System.currentTimeMillis() - CVTrade.OFFLINE_TIMEOUT);
            otherOfflineTrader.setCompleteReason(OfflineTrader.CompleteReason.ERROR);
        } else {
            otherOfflineTrader.setCompleteReason(cancel ? OfflineTrader.CompleteReason.CANCELLED : OfflineTrader.CompleteReason.REJECTED);
        }
    
        this.itemTransfer(player, tradeChest.getChest().getInventory());
        if (this.tradeInventories.containsKey(playerId)) {
            if (this.checkInventories(player.getOpenInventory().getTopInventory(), this.tradeInventories.get(playerId))) {
                player.closeInventory();
            }
            this.tradeInventories.remove(playerId);
        }
        
        this.activeTrades.remove(playerId);
        this.deleteActiveTrade(player, activeTrade);
        this.deleteChestInventory(player, tradeChest);
        player.sendMessage("§cYour trade has been " + (cancel ? "cancelled" : "rejected") + ". Any items you put in the chest have been automatically returned to you.");
        
        otherOfflineTrader.setInventory(linkedChest.getChest().getInventory());
        this.offlineTraders.put(otherPlayerId, otherOfflineTrader);
        this.saveOfflineTrader(this.console, otherOfflineTrader);
        
        this.tradeInventories.remove(otherPlayerId);
        this.activeTrades.remove(otherPlayerId);
        this.deleteActiveTrade(this.console, linkedTrade);
        linkedChest.getChest().getInventory().clear();
        this.deleteChestInventory(this.console, linkedChest);
    }
    
    public void readyTrade(@NotNull final Player player) {
        
        final UUID playerId = player.getUniqueId();
        final ActiveTrade activeTrade = this.activeTrades.get(playerId);
        if (activeTrade == null) {
            player.sendMessage("§cYou are not currently in a trade with anyone.");
            return;
        }
        
        if (activeTrade.getTradeStatus().ordinal() >= ActiveTrade.TradeStatus.READY.ordinal()) {
            player.sendMessage("§6You have already marked yourself as ready to trade.");
            player.sendMessage("§6If you need to change your trade items, please cancel and re-start your trade.");
            return;
        }
        
        final TradeChest tradeChest = activeTrade.getTradeChest();
        final TradeChest linkedChest = this.pairings.get(tradeChest);
        if (linkedChest == null) {
            
            player.sendMessage("§cThere was an error while marking your trade as ready. Please report this to a server administrator.");
            this.logger.log(Level.WARNING, "ISSUE WHILE MARKING TRADE AS READY");
            this.logger.log(Level.WARNING, "Details below:");
            this.logger.log(Level.WARNING, "Player Name: " + player.getName());
            this.logger.log(Level.WARNING, "Player UUID: " + playerId.toString());
            this.logger.log(Level.WARNING, "TradeChest: " + tradeChest.getName());
            this.logger.log(Level.WARNING, "Configured Linked TradeChest: " + activeTrade.getTradeChest().getLinked());
            this.logger.log(Level.WARNING, "ISSUE:");
            this.logger.log(Level.WARNING, "Player " + player.getName() + " is attempting to mark themselves as ready for a trade.");
            this.logger.log(Level.WARNING, "No linked TradeChest to the one listed above.");
            return;
        }
        
        final ItemStack[] items = tradeChest.getChest().getInventory().getStorageContents();
        this.saveChestInventory(player, tradeChest);
        
        activeTrade.setTradeStatus(ActiveTrade.TradeStatus.READY);
        this.saveActiveTrade(player, activeTrade);
        this.activeTrades.put(playerId, activeTrade);
        player.sendMessage("§aYou have marked yourself as ready to trade.");
        
        ActiveTrade linkedTrade = null;
        for (final ActiveTrade checkTrade : this.activeTrades.values()) {
            if (checkTrade.getTradeChest().equals(linkedChest)) {
                linkedTrade = checkTrade;
                break;
            }
        }
        
        if (linkedTrade == null) {
            player.sendMessage("§6Please wait for the other player to begin their trade...");
            return;
        }
        
        final UUID otherPlayerId = linkedTrade.getUniqueId();
        final Player otherPlayer = this.getServer().getPlayer(otherPlayerId);
        if (otherPlayer == null || !otherPlayer.isOnline()) {
            player.sendMessage("§6Please wait for the other player (§r§b" + linkedTrade.getName() + "§r§6), they are currently offline.");
            return;
        }
        
        
        if (linkedTrade.getTradeStatus().ordinal() < ActiveTrade.TradeStatus.READY.ordinal()) {
            player.sendMessage("§6Please wait, " + otherPlayer.getName() + " is finishing preparing their trade.");
            otherPlayer.sendMessage("§6" + player.getName() + " is ready to trade.");
            return;
        }
        
        activeTrade.setTradeStatus(ActiveTrade.TradeStatus.DECIDE);
        linkedTrade.setTradeStatus(ActiveTrade.TradeStatus.DECIDE);
        this.saveActiveTrade(player, activeTrade);
        this.saveActiveTrade(otherPlayer, linkedTrade);
        this.activeTrades.put(playerId, activeTrade);
        this.activeTrades.put(otherPlayerId, linkedTrade);
        
        player.sendMessage("§a" + otherPlayer.getName() + " is also ready to trade. Trade starting...");
        otherPlayer.sendMessage("§a" + player.getName() + " is ready to trade now. Trade starting...");
        
        final Inventory senderTradeInventory = this.createTradeInventory(activeTrade.getTradeChest().getChest().getInventory());
        final Inventory otherTradeInventory = this.createTradeInventory(linkedTrade.getTradeChest().getChest().getInventory());
        
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
    
        switch (activeTrade.getTradeStatus()) {
            case PREPARE:
                player.sendMessage("§cYou cannot view the other player's items; you have not decided what items you want to trade yet.");
                return;
            case READY:
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
                this.logger.log(Level.WARNING, "TradeChest: " + activeTrade.getTradeChest().getName());
                this.logger.log(Level.WARNING, "Trade Status: " + activeTrade.getTradeStatus().name());
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
            this.logger.log(Level.WARNING, "TradeChest: " + activeTrade.getTradeChest().getName());
            this.logger.log(Level.WARNING, "Trade Status: " + activeTrade.getTradeStatus().name());
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
        
        switch (activeTrade.getTradeStatus()) {
            case PREPARE:
                player.sendMessage("§cYou cannot accept a trade; you have not decided what items you want to trade yet.");
                return;
            case READY:
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
                this.logger.log(Level.WARNING, "TradeChest: " + activeTrade.getTradeChest().getName());
                this.logger.log(Level.WARNING, "Trade Status: " + activeTrade.getTradeStatus().name());
                this.logger.log(Level.WARNING, "ISSUE:");
                this.logger.log(Level.WARNING, "Player " + player.getName() + " is attempting to accept a trade.");
                this.logger.log(Level.WARNING, "Player matched default case on TradeStatus during trade acceptance, meaning they do not have a valid TradeStatus.");
                return;
        }
        
        activeTrade.setTradeStatus(ActiveTrade.TradeStatus.ACCEPT);
        this.saveActiveTrade(player, activeTrade);
        this.activeTrades.put(playerId, activeTrade);
        
        final TradeChest tradeChest = activeTrade.getTradeChest();
        final TradeChest linkedChest = this.pairings.get(tradeChest);
        if (linkedChest == null) {
            player.sendMessage("§cThere was an error while accepting your trade. Please notify a server administrator.");
            this.logger.log(Level.WARNING, "ISSUE WHILE ACCEPTING TRADE");
            this.logger.log(Level.WARNING, "Details below:");
            this.logger.log(Level.WARNING, "Player Name: " + player.getName());
            this.logger.log(Level.WARNING, "Player UUID: " + playerId.toString());
            this.logger.log(Level.WARNING, "TradeChest: " + tradeChest.getName());
            this.logger.log(Level.WARNING, "Configured Linked TradeChest: " + tradeChest.getLinked());
            this.logger.log(Level.WARNING, "ISSUE:");
            this.logger.log(Level.WARNING, "Player " + player.getName() + " is attempting to accept a trade.");
            this.logger.log(Level.WARNING, "No linked TradeChest to the one listed above.");
            return;
        }
        
        ActiveTrade linkedTrade = null;
        for (final ActiveTrade checkTrade : this.activeTrades.values()) {
            if (checkTrade.getTradeChest().equals(linkedChest)) {
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
            this.logger.log(Level.WARNING, "Trade Chest: " + tradeChest.getName());
            this.logger.log(Level.WARNING, "Linked Chest: " + linkedChest.getName());
            this.logger.log(Level.WARNING, "ISSUE:");
            this.logger.log(Level.WARNING, "Player " + player.getName() + " is attempting to accept a trade.");
            this.logger.log(Level.WARNING, "No ActiveTrade for the linked TradeChest, this TradeStatus should not be possible without another ActiveTrade.");
            return;
        }
        
        player.sendMessage("§aYou have accepted the trade.");
        if (linkedTrade.getTradeStatus() != ActiveTrade.TradeStatus.ACCEPT) {
            player.sendMessage("§6You are waiting on the other player to make a decision. Please continue to wait...");
            return;
        }
        
        final UUID otherPlayerId = linkedTrade.getUniqueId();
        final Player otherPlayer = this.getServer().getPlayer(otherPlayerId);
        if (otherPlayer != null && otherPlayer.isOnline()) {
    
            otherPlayer.sendMessage("§aThe other person has accepted the trade.");
            player.sendMessage("§aSwapping items...");
            otherPlayer.sendMessage("§aSwapping items...");
    
            this.itemTransfer(player, linkedChest.getChest().getInventory());
            if (this.tradeInventories.containsKey(playerId)) {
                if (this.checkInventories(player.getOpenInventory().getTopInventory(), this.tradeInventories.get(playerId))) {
                    player.closeInventory();
                }
                this.tradeInventories.remove(playerId);
            }
            
            this.itemTransfer(otherPlayer, tradeChest.getChest().getInventory());
            if (this.tradeInventories.containsKey(otherPlayerId)) {
                if (this.checkInventories(otherPlayer.getOpenInventory().getTopInventory(), this.tradeInventories.get(otherPlayerId))) {
                    otherPlayer.closeInventory();
                }
                this.tradeInventories.remove(otherPlayerId);
            }
    
            this.activeTrades.remove(playerId);
            this.deleteActiveTrade(player, activeTrade);
            this.deleteChestInventory(player, tradeChest);
            
            this.activeTrades.remove(otherPlayerId);
            this.deleteActiveTrade(otherPlayer, linkedTrade);
            this.deleteChestInventory(otherPlayer, linkedChest);
    
            player.sendMessage("§aTrade complete!");
            otherPlayer.sendMessage("§aTrade complete!");
        }
        
        OfflineTrader otherOfflineTrader = this.offlineTraders.get(otherPlayerId);
        if (otherOfflineTrader == null) {
    
            player.sendMessage("§cThere was an error accepting your trade. Please report this to a server administrator.");
            this.logger.log(Level.WARNING, "ISSUE WHILE STARTING A TRADE");
            this.logger.log(Level.WARNING, "Details below:");
            this.logger.log(Level.WARNING, "Player Name: " + player.getName());
            this.logger.log(Level.WARNING, "Player UUID: " + playerId.toString());
            this.logger.log(Level.WARNING, "TradeChest: " + tradeChest.getName());
            this.logger.log(Level.WARNING, "Linked TradeChest: " + linkedChest.getName());
            this.logger.log(Level.WARNING, "Other Player Name: " + linkedTrade.getName());
            this.logger.log(Level.WARNING, "Other Player UUID: " + otherPlayerId.toString());
            this.logger.log(Level.WARNING, "ISSUE:");
            this.logger.log(Level.WARNING, "Player " + player.getName() + " is attempting to accept a trade.");
            this.logger.log(Level.WARNING, "Other player is not online, has an ActiveTrade, but does not have an OfflineTrader object.");
    
            this.itemTransfer(player, tradeChest.getChest().getInventory());
            if (this.tradeInventories.containsKey(playerId)) {
                if (this.checkInventories(player.getOpenInventory().getTopInventory(), this.tradeInventories.get(playerId))) {
                    player.closeInventory();
                }
                this.tradeInventories.remove(playerId);
            }
            
            this.activeTrades.remove(playerId);
            this.deleteActiveTrade(player, activeTrade);
            this.deleteChestInventory(player, tradeChest);
            player.sendMessage("§cYour trade has been automatically cancelled. Any items you put in the chest have been automatically returned to you.");
    
            otherOfflineTrader = new OfflineTrader(otherPlayerId, linkedTrade.getName(), System.currentTimeMillis() - CVTrade.OFFLINE_TIMEOUT);
            otherOfflineTrader.setCompleteReason(OfflineTrader.CompleteReason.ERROR);
            otherOfflineTrader.setInventory(linkedChest.getChest().getInventory());
            this.offlineTraders.put(otherPlayerId, otherOfflineTrader);
            this.saveOfflineTrader(this.console, otherOfflineTrader);
            
            this.tradeInventories.remove(otherPlayerId);
            this.activeTrades.remove(otherPlayerId);
            this.deleteActiveTrade(this.console, linkedTrade);
            linkedChest.getChest().getInventory().clear();
            this.deleteChestInventory(this.console, linkedChest);
            return;
        }
        
        player.sendMessage("§aThe other player has accepted the trade before they went offline.");
        player.sendMessage("§aSwapping items...");
    
        this.itemTransfer(player, linkedChest.getChest().getInventory());
        if (this.tradeInventories.containsKey(playerId)) {
            if (this.checkInventories(player.getOpenInventory().getTopInventory(), this.tradeInventories.get(playerId))) {
                player.closeInventory();
            }
            this.tradeInventories.remove(playerId);
        }
    
        player.sendMessage("§aTrade complete!");
        
        otherOfflineTrader.setCompleteReason(OfflineTrader.CompleteReason.ACCEPTED);
        otherOfflineTrader.setInventory(tradeChest.getChest().getInventory());
        this.offlineTraders.put(otherPlayerId, otherOfflineTrader);
        this.saveOfflineTrader(this.console, otherOfflineTrader);
    
        this.activeTrades.remove(playerId);
        this.deleteActiveTrade(player, activeTrade);
        this.deleteChestInventory(player, tradeChest);
        
        this.tradeInventories.remove(otherPlayerId);
        this.activeTrades.remove(otherPlayerId);
        this.deleteActiveTrade(this.console, linkedTrade);
        linkedChest.getChest().getInventory().clear();
        this.deleteChestInventory(this.console, linkedChest);
    }
    
    @NotNull
    public ArrayList<String> getNotLinked(@Nullable final String name) {
        
        final ArrayList<String> notLinked = new ArrayList<String>();
        for (final TradeChest tradeChest : this.byName.values()) {
            if (this.pairings.get(tradeChest) == null && (name == null || !name.equalsIgnoreCase(tradeChest.getName()))) {
                notLinked.add(tradeChest.getName());
            }
        }
        
        return notLinked;
    }
    
    @NotNull
    public ArrayList<String> getLinked() {
        
        final ArrayList<String> linked = new ArrayList<String>();
        for (final TradeChest tradeChest : this.byName.values()) {
            if (this.pairings.get(tradeChest) != null) {
                linked.add(tradeChest.getName());
            }
        }
        
        return linked;
    }
    
    @NotNull
    public ArrayList<String> getNotInActiveTrade(final boolean linkedRequired) {
        
        final ArrayList<TradeChest> inActiveTrade = new ArrayList<TradeChest>();
        for (final ActiveTrade activeTrade : this.activeTrades.values()) {
            inActiveTrade.add(activeTrade.getTradeChest());
        }
    
        final ArrayList<String> notInActiveTrade = new ArrayList<String>();
        for (final TradeChest tradeChest : this.byName.values()) {
            if (!inActiveTrade.contains(tradeChest)) {
                if (!linkedRequired) {
                    notInActiveTrade.add(tradeChest.getName());
                } else if (this.pairings.get(tradeChest) != null) {
                    notInActiveTrade.add(tradeChest.getName());
                }
            }
        }
        
        return notInActiveTrade;
    }
    
    @NotNull
    public ArrayList<String> getAll() {
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
        final long end = logoutTime + CVTrade.OFFLINE_TIMEOUT;
        
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
            if (!inventoryItems[checkSlot].equals(tradeInventoryItems[checkSlot])) {
                return false;
            }
        }
        
        return true;
    }
    
    private boolean linkChests(@NotNull final CommandSender sender, @NotNull final TradeChest chest1, @NotNull final TradeChest chest2) {
        
        try {
            chest2.link(chest1);
            chest1.link(chest2);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(e.getMessage());
            this.logger.log(Level.WARNING, "ISSUE WHILE LINKING TRADECHESTS");
            this.logger.log(Level.WARNING, "Details below:");
            this.logger.log(Level.WARNING, "TradeChest 1:" + chest1.getName());
            this.logger.log(Level.WARNING, "TradeChest 2:" + chest2.getName());
            this.logger.log(Level.WARNING, "ISSUE:");
            this.logger.log(Level.WARNING, "Player " + sender.getName() + " is attempting to link 2 TradeChests.");
            this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
            return false;
        }
        
        this.pairings.put(chest1, chest2);
        this.pairings.put(chest2, chest1);
        
        this.saveTradeChest(sender, chest1);
        this.saveTradeChest(sender, chest2);
        return true;
    }
    
    private void unlinkChest(@NotNull final CommandSender sender, @NotNull final TradeChest tradeChest) {
        
        final TradeChest linked = this.pairings.get(tradeChest);
        if (linked != null && linked.getLinked() != null && linked.getLinked().equals(tradeChest.getName())) {
            
            linked.unlink();
            this.pairings.put(linked, null);
            this.saveTradeChest(sender, linked);
        }
        
        tradeChest.unlink();
        this.pairings.put(tradeChest, null);
        this.saveTradeChest(sender, tradeChest);
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
        tradeInventory.setItem(CVTrade.SLOT_REJECT, reject);
        
        final ItemStack accept = new ItemStack(Material.LIME_CONCRETE);
        final ItemMeta acceptMeta = accept.getItemMeta();
        acceptMeta.setDisplayName("ACCEPT TRADE");
        accept.setItemMeta(acceptMeta);
        tradeInventory.setItem(CVTrade.SLOT_ACCEPT, accept);
        
        return tradeInventory;
    }
    
    /////////////////
    // FILE SAVING //
    /////////////////
    
    private void saveTradeChest(@NotNull final CommandSender sender, @NotNull final TradeChest tradeChest) {
        
        this.scheduler.runTaskAsynchronously(this, () -> {
            
            final File tradeChestFile = new File(this.tradeChestFolder, tradeChest.getName() + ".yml");
            try {
                if (!tradeChestFile.exists()) {
                    if (!tradeChestFile.createNewFile()) {
                        sender.sendMessage("There was an error while updating the TradeChest. Please report this error to a server administrator.");
                        this.logger.log(Level.WARNING, "ISSUE WHILE SAVING TRADECHEST FILE");
                        this.logger.log(Level.WARNING, "Details below:");
                        this.logger.log(Level.WARNING, "TradeChest File Location: " + tradeChestFile.getPath());
                        this.logger.log(Level.WARNING, "TradeChest YAML Data: " + tradeChest.serialize().toString());
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
                this.logger.log(Level.WARNING, "TradeChest YAML Data: " + tradeChest.serialize().toString());
                this.logger.log(Level.WARNING, "ISSUE:");
                this.logger.log(Level.WARNING, "Unable to create TradeChest file.");
                this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                return;
            }
            
            final YamlConfiguration config = new YamlConfiguration();
            config.set("tradechest", tradeChest);
            try {
                config.save(tradeChestFile);
            } catch (IOException | IllegalArgumentException e) {
                sender.sendMessage("There was an error while updating the TradeChest. Please report this error to a server administrator.");
                this.logger.log(Level.WARNING, "ISSUE WHILE SAVING TRADECHEST FILE");
                this.logger.log(Level.WARNING, "Details below:");
                this.logger.log(Level.WARNING, "TradeChest File Location: " + tradeChestFile.getPath());
                this.logger.log(Level.WARNING, "TradeChest YAML Data: " + tradeChest.serialize().toString());
                this.logger.log(Level.WARNING, "ISSUE:");
                this.logger.log(Level.WARNING, "Unable to save TradeChest configuration file.");
                this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
            }
        });
    }
    
    private void saveActiveTrade(@NotNull final CommandSender sender, @NotNull final ActiveTrade activeTrade) {
        
        this.scheduler.runTaskAsynchronously(this, () -> {
            
            final File activeTradeFile = new File(this.activeTradesFolder, activeTrade.getUniqueId().toString() + "-" + activeTrade.getTradeChest().getName() + ".yml");
            try {
                if (!activeTradeFile.exists()) {
                    if (!activeTradeFile.createNewFile()) {
                        sender.sendMessage("There was an error while updating your trade. Please report this error to a server administrator.");
                        this.logger.log(Level.WARNING, "ISSUE WHILE SAVING ACTIVETRADE FILE");
                        this.logger.log(Level.WARNING, "Details below:");
                        this.logger.log(Level.WARNING, "ActiveTrade File Location: " + activeTradeFile.getPath());
                        this.logger.log(Level.WARNING, "ActiveTrade YAML Data:");
                        this.logger.log(Level.WARNING, "uuid: " + activeTrade.getUniqueId().toString());
                        this.logger.log(Level.WARNING, "name: " + activeTrade.getName());
                        this.logger.log(Level.WARNING, "trade_chest:" + activeTrade.getTradeChest().getName());
                        this.logger.log(Level.WARNING, "trade_status: " + activeTrade.getTradeStatus().name());
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
                this.logger.log(Level.WARNING, "uuid: " + activeTrade.getUniqueId().toString());
                this.logger.log(Level.WARNING, "name: " + activeTrade.getName());
                this.logger.log(Level.WARNING, "trade_chest:" + activeTrade.getTradeChest().getName());
                this.logger.log(Level.WARNING, "trade_status: " + activeTrade.getTradeStatus().name());
                this.logger.log(Level.WARNING, "ISSUE:");
                this.logger.log(Level.WARNING, "Unable to create ActiveTrade file.");
                this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                return;
            }
            
            final YamlConfiguration config = new YamlConfiguration();
            config.set("uuid", activeTrade.getUniqueId().toString());
            config.set("name", activeTrade.getName());
            config.set("trade_chest", activeTrade.getTradeChest().getName());
            config.set("trade_status", activeTrade.getTradeStatus().name());
            try {
                config.save(activeTradeFile);
            } catch (IOException | IllegalArgumentException e) {
                sender.sendMessage("There was an error while updating your trade. Please report this error to a server administrator.");
                this.logger.log(Level.WARNING, "ISSUE WHILE SAVING ACTIVETRADE FILE");
                this.logger.log(Level.WARNING, "Details below:");
                this.logger.log(Level.WARNING, "ActiveTrade File Location: " + activeTradeFile.getPath());
                this.logger.log(Level.WARNING, "ActiveTrade YAML Data:");
                this.logger.log(Level.WARNING, "uuid: " + activeTrade.getUniqueId().toString());
                this.logger.log(Level.WARNING, "name: " + activeTrade.getName());
                this.logger.log(Level.WARNING, "trade_chest:" + activeTrade.getTradeChest().getName());
                this.logger.log(Level.WARNING, "trade_status: " + activeTrade.getTradeStatus().name());
                this.logger.log(Level.WARNING, "ISSUE:");
                this.logger.log(Level.WARNING, "Unable to save ActiveTrade configuration file.");
                this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
            }
        });
    }
    
    private void saveChestInventory(@NotNull final CommandSender sender, @NotNull final TradeChest tradeChest) {
        
        this.scheduler.runTaskAsynchronously(this, () -> {
    
            final List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
            for (final ItemStack item : tradeChest.getChest().getInventory().getStorageContents()) {
                items.add(item == null ? null : item.serialize());
            }
            
            final File backupInventoryFile = new File(this.backupInventoryFolder, "BackupInventory-" + tradeChest.getName() + ".yml");
            try {
                if (!backupInventoryFile.exists()) {
                    if (!backupInventoryFile.createNewFile()) {
                        sender.sendMessage("§cThere was an error while updating your trade. Please report this error to a server administrator.");
                        this.logger.log(Level.WARNING, "ISSUE WHILE SAVING BACKUP INVENTORY FILE");
                        this.logger.log(Level.WARNING, "Details below:");
                        this.logger.log(Level.WARNING, "Backup Inventory File Location: " + backupInventoryFile.getPath());
                        this.logger.log(Level.WARNING, "Backup Inventory YAML Data:");
                        this.logger.log(Level.WARNING, "trade_chest_name:" + tradeChest.getName());
                        this.logger.log(Level.WARNING, "items: " + items.toString());
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
                this.logger.log(Level.WARNING, "trade_chest_name:" + tradeChest.getName());
                this.logger.log(Level.WARNING, "items: " + items.toString());
                this.logger.log(Level.WARNING, "ISSUE:");
                this.logger.log(Level.WARNING, "Unable to create Backup Inventory file.");
                this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
                return;
            }
            
            final YamlConfiguration config = new YamlConfiguration();
            config.set("trade_chest_name", tradeChest.getName());
            config.set("items", items);
            try {
                config.save(backupInventoryFile);
            } catch (IOException | IllegalArgumentException e) {
                sender.sendMessage("§cThere was an error while updating your trade. Please report this error to a server administrator.");
                this.logger.log(Level.WARNING, "ISSUE WHILE SAVING BACKUP INVENTORY FILE");
                this.logger.log(Level.WARNING, "Details below:");
                this.logger.log(Level.WARNING, "Backup Inventory File Location: " + backupInventoryFile.getPath());
                this.logger.log(Level.WARNING, "Backup Inventory YAML Data:");
                this.logger.log(Level.WARNING, "trade_chest_name:" + tradeChest.getName());
                this.logger.log(Level.WARNING, "items: " + items.toString());
                this.logger.log(Level.WARNING, "ISSUE:");
                this.logger.log(Level.WARNING, "Unable to save Backup Inventory configuration file.");
                this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
            }
        });
    }
    
    private void saveOfflineTrader(@NotNull final CommandSender sender, @NotNull final OfflineTrader offlineTrader) {
        
        this.scheduler.runTaskAsynchronously(this, () -> {
            
            final UUID playerId = offlineTrader.getUniqueId();
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
            
            final File offlineInventoryFile = new File(this.offlineInventoryFolder, playerId.toString() + ".yml");
            try {
                if (!offlineInventoryFile.exists()) {
                    if (!offlineInventoryFile.createNewFile()) {
                        sender.sendMessage("§cThere was an error while updating your returnable items. Please report this to a server administrator.");
                        this.logger.log(Level.WARNING, "ISSUE WHILE SAVING OFFLINETRADER FILE");
                        this.logger.log(Level.WARNING, "Details below:");
                        this.logger.log(Level.WARNING, "OfflineTrader File Location: " + offlineInventoryFile.getPath());
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
                sender.sendMessage("§cThere was an error while updating your returnable items. Please report this to a server administrator.");
                this.logger.log(Level.WARNING, "ISSUE WHILE SAVING OFFLINETRADER FILE");
                this.logger.log(Level.WARNING, "Details below:");
                this.logger.log(Level.WARNING, "OfflineTrader File Location: " + offlineInventoryFile.getPath());
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
                config.save(offlineInventoryFile);
            } catch (IOException | IllegalArgumentException e) {
                sender.sendMessage("§cThere was an error while updating your returnable items. Please report this to a server administrator.");
                this.logger.log(Level.WARNING, "ISSUE WHILE SAVING OFFLINETRADER FILE");
                this.logger.log(Level.WARNING, "Details below:");
                this.logger.log(Level.WARNING, "OfflineTrader File Location: " + offlineInventoryFile.getPath());
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
        });
    }
    
    ///////////////////
    // FILE DELETION //
    ///////////////////
    
    private void deleteTradeChest(@NotNull final CommandSender sender, @NotNull final TradeChest tradeChest) {
        
        this.scheduler.runTaskAsynchronously(this, () -> {
    
            final File tradeChestFile = new File(this.tradeChestFolder, tradeChest.getName() + ".yml");
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
    
            final File activeTradeFile = new File(this.activeTradesFolder, activeTrade.getUniqueId().toString() + "-" + activeTrade.getTradeChest().getName() + ".yml");
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
    
    private void deleteChestInventory(@NotNull final CommandSender sender, @NotNull final TradeChest tradeChest) {
        
        this.scheduler.runTaskAsynchronously(this, () -> {
    
            final File backupInventoryFile = new File(this.backupInventoryFolder, "BackupInventory-" + tradeChest.getName() + ".yml");
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
            final File offlineInventoryFile = new File(this.offlineInventoryFolder, playerId.toString() + ".yml");
            try {
                if (offlineInventoryFile.exists()) {
                    if (!offlineInventoryFile.delete()) {
                        sender.sendMessage("§cThere was an error while returning your items. Please report this to a server administrator.");
                        this.logger.log(Level.WARNING, "ISSUE WHILE DELETING OFFLINETRADER FILE");
                        this.logger.log(Level.WARNING, "Details below:");
                        this.logger.log(Level.WARNING, "OfflineTrader File Location: " + offlineInventoryFile.getPath());
                        this.logger.log(Level.WARNING, "ISSUE:");
                        this.logger.log(Level.WARNING, "OfflineTrader file not deleted successfully.");
                    }
                }
            } catch (SecurityException e) {
                sender.sendMessage("§cThere was an error while returning your items. Please report this to a server administrator.");
                this.logger.log(Level.WARNING, "ISSUE WHILE DELETING OFFLINETRADER FILE");
                this.logger.log(Level.WARNING, "Details below:");
                this.logger.log(Level.WARNING, "OfflineTrader File Location: " + offlineInventoryFile.getPath());
                this.logger.log(Level.WARNING, "ISSUE:");
                this.logger.log(Level.WARNING, "Unable to delete OfflineTrader file.");
                this.logger.log(Level.WARNING, e.getClass().getSimpleName() + " thrown.", e);
            }
        });
    }
}
