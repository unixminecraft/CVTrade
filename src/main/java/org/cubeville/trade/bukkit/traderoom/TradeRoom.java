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

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import org.cubeville.trade.bukkit.TradePlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TradeRoom {
    
    private static final String KEY_NAME = "name";
    
    private static final String KEY_CHEST_1 = "chest_1";
    private static final String KEY_ITEMS_1 = "items_1";
    private static final String KEY_REGION_1 = "region_1";
    private static final String KEY_TELEPORT_IN_1 = "teleport_in_1";
    private static final String KEY_TELEPORT_OUT_1 = "teleport_out_1";
    private static final String KEY_BUTTON_IN_1 = "button_in_1";
    private static final String KEY_BUTTON_OUT_1 = "button_out_1";
    private static final String KEY_BUTTON_LOCK_1 = "button_lock_1";
    private static final String KEY_BUTTON_ACCEPT_1 = "button_accept_1";
    private static final String KEY_BUTTON_DENY_1 = "button_deny_1";
    
    private static final String KEY_CHEST_2 = "chest_2";
    private static final String KEY_ITEMS_2 = "items_2";
    private static final String KEY_REGION_2 = "region_2";
    private static final String KEY_TELEPORT_IN_2 = "teleport_in_2";
    private static final String KEY_TELEPORT_OUT_2 = "teleport_out_2";
    private static final String KEY_BUTTON_IN_2 = "button_in_2";
    private static final String KEY_BUTTON_OUT_2 = "button_out_2";
    private static final String KEY_BUTTON_LOCK_2 = "button_lock_2";
    private static final String KEY_BUTTON_ACCEPT_2 = "button_accept_2";
    private static final String KEY_BUTTON_DENY_2 = "button_deny_2";
    
    private static final String KEY_TRADE_STATUS = "trade_status";
    
    private static final String KEY_TRADER_1_UUID = "trader_1_uuid";
    private static final String KEY_TRADER_1_NAME = "trader_1_name";
    private static final String KEY_TRADER_1_LOGOUT_TIME = "trader_1_logout_time";
    
    private static final String KEY_TRADER_2_UUID = "trader_2_uuid";
    private static final String KEY_TRADER_2_NAME = "trader_2_name";
    private static final String KEY_TRADER_2_LOGOUT_TIME = "trader_2_logout_time";
    
    private final String name;
    
    private final Chest chest1;
    private final ProtectedRegion region1;
    private final Location teleportIn1;
    private final Location teleportOut1;
    private final Button buttonIn1;
    private final Button buttonOut1;
    private final Button buttonLock1;
    private final Button buttonAccept1;
    private final Button buttonDeny1;
    
    private final Chest chest2;
    private final ProtectedRegion region2;
    private final Location teleportIn2;
    private final Location teleportOut2;
    private final Button buttonIn2;
    private final Button buttonOut2;
    private final Button buttonLock2;
    private final Button buttonAccept2;
    private final Button buttonDeny2;
    
    private TradeStatus status;
    private Trader trader1;
    private Trader trader2;
    
    @NotNull
    public static TradeRoomBuilder newBuilder(@NotNull final TradePlugin plugin, @NotNull final Player player) {
        return new TradeRoomBuilder(plugin, player);
    }
    
    TradeRoom(
            @NotNull final String name,
            @NotNull final ProtectedRegion region1,
            @NotNull final Chest chest1,
            @NotNull final Location teleportIn1,
            @NotNull final Location teleportOut1,
            @NotNull final Button buttonIn1,
            @NotNull final Button buttonOut1,
            @NotNull final Button buttonLock1,
            @NotNull final Button buttonAccept1,
            @NotNull final Button buttonDeny1,
            @NotNull final ProtectedRegion region2,
            @NotNull final Chest chest2,
            @NotNull final Location teleportIn2,
            @NotNull final Location teleportOut2,
            @NotNull final Button buttonIn2,
            @NotNull final Button buttonOut2,
            @NotNull final Button buttonLock2,
            @NotNull final Button buttonAccept2,
            @NotNull final Button buttonDeny2
    ) {
        
        this.name = name;
        
        this.region1 = region1;
        this.chest1 = chest1;
        this.teleportIn1 = teleportIn1;
        this.teleportOut1 = teleportOut1;
        this.buttonIn1 = buttonIn1;
        this.buttonOut1 = buttonOut1;
        this.buttonLock1 = buttonLock1;
        this.buttonAccept1 = buttonAccept1;
        this.buttonDeny1 = buttonDeny1;
        
        this.region2 = region2;
        this.chest2 = chest2;
        this.teleportIn2 = teleportIn2;
        this.teleportOut2 = teleportOut2;
        this.buttonIn2 = buttonIn2;
        this.buttonOut2 = buttonOut2;
        this.buttonLock2 = buttonLock2;
        this.buttonAccept2 = buttonAccept2;
        this.buttonDeny2 = buttonDeny2;
        
        this.status = null;
        this.trader1 = null;
        this.trader2 = null;
    }
    
    public TradeRoom(@NotNull final Server server, @NotNull final Configuration config) throws IllegalArgumentException {
        
        final String nameRaw = config.getString(KEY_NAME, null);
        final String name = nameRaw == null ? null : nameRaw.trim().toLowerCase();
        if (name == null) {
            throw new IllegalArgumentException("Cannot have null trade room name.");
        } else if (name.isEmpty()) {
            throw new IllegalArgumentException("Cannot have blank trade room name.");
        }
        
        final Location chest1Location = this.getLocation(config, KEY_CHEST_1, "chest 1");
        final BlockState chest1State = chest1Location.getWorld().getBlockAt(chest1Location).getState();
        if (!(chest1State instanceof Chest)) {
            throw new IllegalArgumentException("Cannot have non-chest at chest 1 location.");
        }
        final Chest chest1 = (Chest) chest1State;
        if (chest1.getInventory().getHolder() instanceof DoubleChest) {
            throw new IllegalArgumentException("Cannot have double chest at chest 1 location.");
        }
        
        final Location chest2Location = this.getLocation(config, KEY_CHEST_2, "chest 2");
        if (chest2Location.equals(chest1Location)) {
            throw new IllegalArgumentException("Chest 1 and chest 2 cannot be the same chest.");
        }
        final BlockState chest2State = chest2Location.getWorld().getBlockAt(chest2Location).getState();
        if (!(chest2State instanceof Chest)) {
            throw new IllegalArgumentException("Cannot have non-chest at chest 2 location.");
        }
        final Chest chest2 = (Chest) chest2State;
        if (chest2.getInventory().getHolder() instanceof DoubleChest) {
            throw new IllegalArgumentException("Cannot have double chest at chest 2 location.");
        }
        if (!chest1.getWorld().equals(chest2.getWorld())) {
            throw new IllegalArgumentException("Chests cannot be in different worlds.");
        }
        
        final World world = chest1.getWorld();
        final RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
        if (regionManager == null) {
            throw new IllegalArgumentException("Region manager is null for world " + world.getName());
        }
        
        final String region1Name = config.getString(KEY_REGION_1, null);
        if (region1Name == null) {
            throw new IllegalArgumentException("Cannot have null region 1 name.");
        } else if (region1Name.trim().isEmpty()) {
            throw new IllegalArgumentException("Cannot have blank region 1 name.");
        } else if (region1Name.trim().equalsIgnoreCase(ProtectedRegion.GLOBAL_REGION)) {
            throw new IllegalArgumentException("Cannot use global region for region 1.");
        }
        final ProtectedRegion region1 = regionManager.getRegion(region1Name.trim());
        if (region1 == null) {
            throw new IllegalArgumentException("Region " + region1Name.trim() + " does not exist in world " + world.getName());
        }
        
        final String region2Name = config.getString(KEY_REGION_2, null);
        if (region2Name == null) {
            throw new IllegalArgumentException("Cannot have null region 2 name.");
        } else if (region2Name.trim().isEmpty()) {
            throw new IllegalArgumentException("Cannot have blank region 2 name.");
        } else if (region2Name.trim().equalsIgnoreCase(ProtectedRegion.GLOBAL_REGION)) {
            throw new IllegalArgumentException("Cannot use global region for region 2.");
        } else if (region2Name.trim().equalsIgnoreCase(region1.getId())) {
            throw new IllegalArgumentException("Must use 2 separate regions.");
        }
        final ProtectedRegion region2 = regionManager.getRegion(region2Name.trim());
        if (region2 == null) {
            throw new IllegalArgumentException("Region " + region2Name.trim() + " does not exist in world " + world.getName());
        }
        
        if (region1.contains(region2.getMinimumPoint()) || region1.contains(region2.getMaximumPoint()) || region2.contains(region1.getMinimumPoint()) || region2.contains(region1.getMaximumPoint())) {
            throw new IllegalArgumentException("The regions cannot be overlapping.");
        }
        
        if (!region1.contains(BukkitAdapter.asBlockVector(chest1Location))) {
            throw new IllegalArgumentException("Region " + region1.getId() + " does not contain the chest 1 (" + this.formatLocation(chest1Location) + ").");
        }
        if (!region2.contains(BukkitAdapter.asBlockVector(chest2Location))) {
            throw new IllegalArgumentException("Region " + region2.getId() + " does not contain the chest 2 (" + this.formatLocation(chest2Location) + ").");
        }
        
        final Location teleportIn1 = this.getLocation(config, KEY_TELEPORT_IN_1, "teleport in 1", region1, true, region2, false);
        final Location teleportIn2 = this.getLocation(config, KEY_TELEPORT_IN_2, "teleport in 2", region1, false, region2, true);
        
        final Location teleportOut1 = this.getLocation(config, KEY_TELEPORT_OUT_1, "teleport out 1", region1, false, region2, false);
        final Location teleportOut2 = this.getLocation(config, KEY_TELEPORT_OUT_2, "teleport out 2", region1, false, region2, false);
        
        final Button buttonIn1 = this.getButton(config, KEY_BUTTON_IN_1, "button in 1", region1, false, region2, false);
        final Button buttonIn2 = this.getButton(config, KEY_BUTTON_IN_2, "button_in_2", region1, false, region2, false);
        this.assertDifferent(buttonIn1, "button in 1", buttonIn2, "button in 2");
        
        final Button buttonOut1 = this.getButton(config, KEY_BUTTON_OUT_1, "button out 1", region1, true, region2, false);
        final Button buttonLock1 = this.getButton(config, KEY_BUTTON_LOCK_1, "button lock 1", region1, true, region2, false);
        final Button buttonAccept1 = this.getButton(config, KEY_BUTTON_ACCEPT_1, "button accept 1", region1, true, region2, false);
        final Button buttonDeny1 = this.getButton(config, KEY_BUTTON_DENY_1, "button deny 1", region1, true, region2, false);
        this.assertDifferent(buttonOut1, "button out 1", buttonLock1, "button lock 1");
        this.assertDifferent(buttonOut1, "button out 1", buttonAccept1, "button accept 1");
        this.assertDifferent(buttonOut1, "button out 1", buttonDeny1, "button deny 1");
        this.assertDifferent(buttonLock1, "button lock 1", buttonAccept1, "button accept 1");
        this.assertDifferent(buttonLock1, "button lock 1", buttonDeny1, "button deny 1");
        this.assertDifferent(buttonAccept1, "button accept 1", buttonDeny1, "button deny 1");
        
        final Button buttonOut2 = this.getButton(config, KEY_BUTTON_OUT_2, "button out 2", region1, false, region2, true);
        final Button buttonLock2 = this.getButton(config, KEY_BUTTON_LOCK_2, "button lock 2", region1, false, region2, true);
        final Button buttonAccept2 = this.getButton(config, KEY_BUTTON_ACCEPT_2, "button accept 2", region1, false, region2, true);
        final Button buttonDeny2 = this.getButton(config, KEY_BUTTON_DENY_2, "button deny 2", region1, false, region2, true);
        this.assertDifferent(buttonOut2, "button out 2", buttonLock2, "button lock 2");
        this.assertDifferent(buttonOut2, "button out 2", buttonAccept2, "button accept 2");
        this.assertDifferent(buttonOut2, "button out 2", buttonDeny2, "button deny 2");
        this.assertDifferent(buttonLock2, "button lock 2", buttonAccept2, "button accept 2");
        this.assertDifferent(buttonLock2, "button lock 2", buttonDeny2, "button deny 2");
        this.assertDifferent(buttonAccept2, "button accept 2", buttonDeny2, "button deny 2");
        
        final String rawStatus = config.getString(KEY_TRADE_STATUS, null);
        final TradeStatus status;
        if (rawStatus == null) {
            status = null;
        } else {
            try {
                status = TradeStatus.valueOf(rawStatus.trim().toUpperCase());
            } catch (final IllegalArgumentException e) {
                throw new IllegalArgumentException("Trade status " + rawStatus.trim() + " is not a valid trade status.", e);
            }
        }
        
        final String trader1UUID = config.getString(KEY_TRADER_1_UUID, null);
        final UUID trader1UniqueId;
        if (trader1UUID == null) {
            trader1UniqueId = null;
        } else {
            try {
                trader1UniqueId = UUID.fromString(trader1UUID);
            } catch (final IllegalArgumentException e) {
                throw new IllegalArgumentException("Unable to parse UUID " + trader1UUID + " for trader 1.", e);
            }
        }
        
        final String trader1Name = config.getString(KEY_TRADER_1_NAME, null);
        if (trader1Name != null && trader1Name.trim().isEmpty()) {
            throw new IllegalArgumentException("Cannot have blank name for trader 1.");
        }
        
        final long trader1LogoutTime;
        if (config.isSet(KEY_TRADER_1_LOGOUT_TIME)) {
            trader1LogoutTime = config.getLong(KEY_TRADER_1_LOGOUT_TIME, -1L);
        } else {
            trader1LogoutTime = 0L;
        }
        if (trader1LogoutTime < 0L) {
            throw new IllegalArgumentException("Cannot have invalid logout time for trader 1.");
        }
        
        final Trader trader1;
        if (trader1UniqueId != null && trader1Name != null) {
            trader1 = new Trader(trader1UniqueId, trader1Name.trim(), trader1LogoutTime);
        } else {
            trader1 = null;
        }
        
        final String trader2UUID = config.getString(KEY_TRADER_2_UUID, null);
        final UUID trader2UniqueId;
        if (trader2UUID == null) {
            trader2UniqueId = null;
        } else {
            try {
                trader2UniqueId = UUID.fromString(trader2UUID);
            } catch (final IllegalArgumentException e) {
                throw new IllegalArgumentException("Unable to parse UUID " + trader2UUID + " for trader 2.", e);
            }
        }
        
        final String trader2Name = config.getString(KEY_TRADER_2_NAME, null);
        if (trader2Name != null && trader2Name.trim().isEmpty()) {
            throw new IllegalArgumentException("Cannot have blank name for trader 2.");
        }
        
        final long trader2LogoutTime;
        if (config.isSet(KEY_TRADER_2_LOGOUT_TIME)) {
            trader2LogoutTime = config.getLong(KEY_TRADER_2_LOGOUT_TIME, -1L);
        } else {
            trader2LogoutTime = 0L;
        }
        if (trader2LogoutTime < 0L) {
            throw new IllegalArgumentException("Cannot have invalid logout time for trader 2.");
        }
        
        final Trader trader2;
        if (trader2UniqueId != null && trader2Name != null) {
            trader2 = new Trader(trader2UniqueId, trader2Name.trim(), trader2LogoutTime);
        } else {
            trader2 = null;
        }
        
        final List<?> rawItems1 = config.getList(KEY_ITEMS_1, null);
        if (rawItems1 != null) {
            this.backupInventory(server, rawItems1, chest1);
        }
        final List<?> rawItems2 = config.getList(KEY_ITEMS_2, null);
        if (rawItems2 != null) {
            this.backupInventory(server, rawItems2, chest2);
        }
        
        this.name = name;
        
        this.chest1 = chest1;
        this.region1 = region1;
        this.teleportIn1 = teleportIn1;
        this.teleportOut1 = teleportOut1;
        this.buttonIn1 = buttonIn1;
        this.buttonOut1 = buttonOut1;
        this.buttonLock1 = buttonLock1;
        this.buttonAccept1 = buttonAccept1;
        this.buttonDeny1 = buttonDeny1;
        
        this.chest2 = chest2;
        this.region2 = region2;
        this.teleportIn2 = teleportIn2;
        this.teleportOut2 = teleportOut2;
        this.buttonIn2 = buttonIn2;
        this.buttonOut2 = buttonOut2;
        this.buttonLock2 = buttonLock2;
        this.buttonAccept2 = buttonAccept2;
        this.buttonDeny2 = buttonDeny2;
        
        this.status = status;
        this.trader1 = trader1;
        this.trader2 = trader2;
    }
    
    @NotNull
    private Location getLocation(@NotNull final Configuration config, @NotNull final String key, @NotNull final String type) throws IllegalArgumentException {
        
        final Location location = config.getLocation(key, null);
        if (location == null) {
            throw new IllegalArgumentException("Cannot have null " + type + " location.");
        } else if (location.getWorld() == null) {
            throw new IllegalArgumentException("Cannot have null world for " + type + " location.");
        }
        
        return location;
    }
    
    @NotNull
    private Location getLocation(@NotNull final Configuration config, @NotNull final String key, @NotNull final String type, @NotNull final ProtectedRegion region1, final boolean contains1, @NotNull final ProtectedRegion region2, final boolean contains2) throws IllegalArgumentException {
        
        if (contains1 && contains2) {
            throw new IllegalArgumentException("The " + type + " cannot be contained within both regions.");
        }
        
        final Location location = this.getLocation(config, key, type);
        if (contains1 && !region1.contains(BukkitAdapter.asBlockVector(location))) {
            throw new IllegalArgumentException("Region 1 " + region1.getId() + " does not contain the " + type + " location (" + this.formatLocation(location) + ").");
        } else if (!contains1 && region1.contains(BukkitAdapter.asBlockVector(location))) {
            throw new IllegalArgumentException("Region 1 " + region1.getId() + " contains the " + type + " location (" + this.formatLocation(location) + ").");
        } else if (contains2 && !region2.contains(BukkitAdapter.asBlockVector(location))) {
            throw new IllegalArgumentException("Region 2 " + region2.getId() + " does not contain the " + type + " location (" + this.formatLocation(location) + ").");
        } else if (!contains2 && region2.contains(BukkitAdapter.asBlockVector(location))) {
            throw new IllegalArgumentException("Region 2 " + region2.getId() + " contains the " + type + " location (" + this.formatLocation(location) + ").");
        }
        
        return location;
    }
    
    @NotNull
    private Button getButton(@NotNull final Configuration config, @NotNull final String key, @NotNull final String type, @NotNull final ProtectedRegion region1, final boolean contains1, @NotNull final ProtectedRegion region2, final boolean contains2) throws IllegalArgumentException {
        
        final Location location = this.getLocation(config, key, type, region1, contains1, region2, contains2);
        if (location.getWorld() == null) {
            throw new IllegalArgumentException("Block at " + this.formatLocation(location) + " has a null world for " + type + ".");
        }
        
        final BlockState state = location.getWorld().getBlockAt(location).getState();
        if (!this.isButton(state)) {
            throw new IllegalArgumentException("Block at " + this.formatLocation(location) + " is not a button for " + type + ".");
        }
        
        return new Button(state);
    }
    
    private boolean isButton(@NotNull final BlockState state) {
        
        switch (state.getType()) {
            case STONE_BUTTON:
            case POLISHED_BLACKSTONE_BUTTON:
            case OAK_BUTTON:
            case SPRUCE_BUTTON:
            case BIRCH_BUTTON:
            case JUNGLE_BUTTON:
            case ACACIA_BUTTON:
            case CHERRY_BUTTON:
            case DARK_OAK_BUTTON:
            case MANGROVE_BUTTON:
            case BAMBOO_BUTTON:
            case CRIMSON_BUTTON:
            case WARPED_BUTTON:
                return true;
            default:
                return false;
        }
    }
    
    @NotNull
    private String formatLocation(@NotNull final Location location) {
        
        final StringBuilder builder = new StringBuilder();
        
        builder.append(location.getWorld() == null ? "null" : location.getWorld().getName()).append(", ");
        builder.append(location.getBlockX()).append(", ");
        builder.append(location.getBlockY()).append(", ");
        builder.append(location.getBlockZ());
        
        return builder.toString();
    }
    
    private void assertDifferent(@NotNull final Button existing, @NotNull final String existingType, @NotNull final Button inbound, @NotNull final String inboundType) throws IllegalArgumentException {
        if (existing.equals(inbound)) {
            throw new IllegalArgumentException("The location for the " + inboundType + " is already in use for the " + existingType + ".");
        }
    }
    
    private void backupInventory(@NotNull final Server server, @NotNull final List<?> rawItems, @NotNull final Chest chest) {
        
        final Inventory backupInventory = server.createInventory(null, 27);
        final Inventory chestInventory = chest.getInventory();
        final List<Map<String, Object>> items = (List<Map<String, Object>>) rawItems;
        
        int slot = 0;
        for (final Map<String, Object> item : items) {
            backupInventory.setItem(slot, item == null ? null : ItemStack.deserialize(item));
        }
        
        final ItemStack[] backupItems = backupInventory.getStorageContents();
        final ItemStack[] chestItems = chestInventory.getStorageContents();
        
        for (slot = 0; slot < backupItems.length; slot++) {
            if (backupItems[slot] == null && chestItems[slot] == null) {
                // Do nothing.
            } else if (backupItems[slot] == null) {
                chestItems[slot] = backupItems[slot];
            } else if (chestItems[slot] == null) {
                chestItems[slot] = backupItems[slot];
            } else if (!backupItems[slot].equals(chestItems[slot])) {
                chestItems[slot] = backupItems[slot];
            }
        }
        
        chestInventory.setStorageContents(chestItems);
    }
    
    @NotNull
    public String getName() {
        return this.name;
    }
    
    @NotNull
    public Chest getChest1() {
        return this.chest1;
    }
    
    @NotNull
    public ProtectedRegion getRegion1() {
        return this.region1;
    }
    
    @NotNull
    public Location getTeleportIn1() {
        return this.teleportIn1;
    }
    
    @NotNull
    public Location getTeleportOut1() {
        return this.teleportOut1;
    }
    
    @NotNull
    public Location getButtonIn1() {
        return this.buttonIn1.getLocation();
    }
    
    @NotNull
    public Location getButtonOut1() {
        return this.buttonOut1.getLocation();
    }
    
    @NotNull
    public Location getButtonLock1() {
        return this.buttonLock1.getLocation();
    }
    
    @NotNull
    public Location getButtonAccept1() {
        return this.buttonAccept1.getLocation();
    }
    
    @NotNull
    public Location getButtonDeny1() {
        return this.buttonDeny1.getLocation();
    }
    
    @NotNull
    public Chest getChest2() {
        return this.chest2;
    }
    
    @NotNull
    public ProtectedRegion getRegion2() {
        return this.region2;
    }
    
    @NotNull
    public Location getTeleportIn2() {
        return this.teleportIn2;
    }
    
    @NotNull
    public Location getTeleportOut2() {
        return this.teleportOut2;
    }
    
    @NotNull
    public Location getButtonIn2() {
        return this.buttonIn2.getLocation();
    }
    
    @NotNull
    public Location getButtonOut2() {
        return this.buttonOut2.getLocation();
    }
    
    @NotNull
    public Location getButtonLock2() {
        return this.buttonLock2.getLocation();
    }
    
    @NotNull
    public Location getButtonAccept2() {
        return this.buttonAccept2.getLocation();
    }
    
    @NotNull
    public Location getButtonDeny2() {
        return this.buttonDeny2.getLocation();
    }
    
    @Nullable
    public TradeStatus getStatus() {
        return this.status;
    }
    
    public void setStatus(@Nullable final TradeStatus status) {
        this.status = status;
    }
    
    @Nullable
    public Trader getTrader1() {
        return this.trader1;
    }
    
    public void setTrader1(@Nullable final Trader trader1) {
        this.trader1 = trader1;
    }
    
    @Nullable
    public Trader getTrader2() {
        return this.trader2;
    }
    
    public void setTrader2(@Nullable final Trader trader2) {
        this.trader2 = trader2;
    }
    
    @Nullable
    public Trader getTrader(@NotNull final UUID uniqueId) {
        
        if (this.isTrader1(uniqueId)) {
            return this.getTrader1();
        } else if (this.isTrader2(uniqueId)) {
            return this.getTrader2();
        } else {
            return null;
        }
    }
    
    @NotNull
    public FileConfiguration getConfig() {
        
        final FileConfiguration config = new YamlConfiguration();
        
        config.set(KEY_NAME, this.getName());
        config.set(KEY_CHEST_1, this.getChest1().getLocation());
        config.set(KEY_REGION_1, this.getRegion1().getId());
        config.set(KEY_TELEPORT_IN_1, this.getTeleportIn1());
        config.set(KEY_TELEPORT_OUT_1, this.getTeleportOut1());
        config.set(KEY_BUTTON_IN_1, this.getButtonIn1());
        config.set(KEY_BUTTON_OUT_1, this.getButtonOut1());
        config.set(KEY_BUTTON_LOCK_1, this.getButtonLock1());
        config.set(KEY_BUTTON_ACCEPT_1, this.getButtonAccept1());
        config.set(KEY_BUTTON_DENY_1, this.getButtonDeny1());
        config.set(KEY_CHEST_2, this.getChest2().getLocation());
        config.set(KEY_REGION_2, this.getRegion2().getId());
        config.set(KEY_TELEPORT_IN_2, this.getTeleportIn2());
        config.set(KEY_TELEPORT_OUT_2, this.getTeleportOut2());
        config.set(KEY_BUTTON_IN_2, this.getButtonIn2());
        config.set(KEY_BUTTON_OUT_2, this.getButtonOut2());
        config.set(KEY_BUTTON_LOCK_2, this.getButtonLock2());
        config.set(KEY_BUTTON_ACCEPT_2, this.getButtonAccept2());
        config.set(KEY_BUTTON_DENY_2, this.getButtonDeny2());
        
        if (this.getStatus() != null) {
            config.set(KEY_TRADE_STATUS, this.getStatus().name());
        }
        if (this.getTrader1() != null) {
            config.set(KEY_TRADER_1_UUID, this.getTrader1().getUniqueId().toString());
            config.set(KEY_TRADER_1_NAME, this.getTrader1().getName());
            config.set(KEY_TRADER_1_LOGOUT_TIME, this.getTrader1().getLogoutTime());
        }
        if (this.getTrader2() != null) {
            config.set(KEY_TRADER_2_UUID, this.getTrader2().getUniqueId().toString());
            config.set(KEY_TRADER_2_NAME, this.getTrader2().getName());
            config.set(KEY_TRADER_2_LOGOUT_TIME, this.getTrader2().getLogoutTime());
        }
        
        return config;
    }
    
    public boolean contains(@NotNull final Location location, final boolean exact) {
        return this.containsChest(location) || this.containsButton(location, exact);
    }
    
    @Nullable
    public Side getSide(@NotNull final Location location) {
        
        final Side side = this.getChestSide(location);
        if (side != null) {
            return side;
        }
        return this.getButtonSide(location, true);
    }
    
    private boolean containsChest(@NotNull final Location location) {
        return this.getChestSide(location) != null;
    }
    
    @Nullable
    public Side getChestSide(@NotNull final Location location) {
        
        if (this.getChest1().getLocation().equals(location)) {
            return Side.SIDE_1;
        } else if (this.getChest2().getLocation().equals(location)) {
            return Side.SIDE_2;
        } else {
            return null;
        }
    }
    
    private boolean containsButton(@NotNull final Location location, final boolean exact) {
        return this.getButtonSide(location, exact) != null;
    }
    
    @Nullable
    private Side getButtonSide(@NotNull final Location location, final boolean exact) {
        
        if (this.buttonIn1.contains(location, exact)) {
            return Side.SIDE_1;
        } else if (this.buttonOut1.contains(location, exact)) {
            return Side.SIDE_1;
        } else if (this.buttonLock1.contains(location, exact)) {
            return Side.SIDE_1;
        } else if (this.buttonAccept1.contains(location, exact)) {
            return Side.SIDE_1;
        } else if (this.buttonDeny1.contains(location, exact)) {
            return Side.SIDE_1;
        } else if (this.buttonIn2.contains(location, exact)) {
            return Side.SIDE_2;
        } else if (this.buttonOut2.contains(location, exact)) {
            return Side.SIDE_2;
        } else if (this.buttonLock2.contains(location, exact)) {
            return Side.SIDE_2;
        } else if (this.buttonAccept2.contains(location, exact)) {
            return Side.SIDE_2;
        } else if (this.buttonDeny2.contains(location, exact)) {
            return Side.SIDE_2;
        } else {
            return null;
        }
    }
    
    public boolean isChest(@NotNull final Location location, @NotNull final Side side) {
        return side == Side.SIDE_1 && this.getChest1().getLocation().equals(location) || side == Side.SIDE_2 && this.getChest2().getLocation().equals(location);
    }
    
    public boolean isButtonOut(@NotNull final Location location, @NotNull final Side side) {
        return side == Side.SIDE_1 && this.getButtonOut1().equals(location) || side == Side.SIDE_2 && this.getButtonOut2().equals(location);
    }
    
    public boolean isButtonLock(@NotNull final Location location, @NotNull final Side side) {
        return side == Side.SIDE_1 && this.getButtonLock1().equals(location) || side == Side.SIDE_2 && this.getButtonLock2().equals(location);
    }
    
    public boolean isButtonAccept(@NotNull final Location location, @NotNull final Side side) {
        return side == Side.SIDE_1 && this.getButtonAccept1().equals(location) || side == Side.SIDE_2 && this.getButtonAccept2().equals(location);
    }
    
    public boolean isButtonDeny(@NotNull final Location location, @NotNull final Side side) {
        return side == Side.SIDE_1 && this.getButtonDeny1().equals(location) || side == Side.SIDE_2 && this.getButtonDeny2().equals(location);
    }
    
    public boolean isActive() {
        return this.getTrader1() != null || this.getTrader2() != null;
    }
    
    public boolean isFull() {
        return this.getTrader1() != null && this.getTrader2() != null;
    }
    
    public boolean isUsing(@NotNull final UUID uniqueId) {
        return this.getTrader(uniqueId) != null;
    }
    
    public boolean isTrader1(@NotNull final UUID uniqueId) {
        return this.getTrader1() != null && this.getTrader1().getUniqueId().equals(uniqueId);
    }
    
    public boolean isTrader2(@NotNull final UUID uniqueId) {
        return this.getTrader2() != null && this.getTrader2().getUniqueId().equals(uniqueId);
    }
    
    public boolean hasNotLocked(@NotNull final UUID uniqueId) {
        
        if (this.isTrader1(uniqueId)) {
            return this.getStatus() == TradeStatus.PREPARE || this.getStatus() == TradeStatus.LOCKED_2;
        } else if (this.isTrader2(uniqueId)) {
            return this.getStatus() == TradeStatus.PREPARE || this.getStatus() == TradeStatus.LOCKED_1;
        } else {
            return false;
        }
    }
    
    public boolean hasLocked(@NotNull final UUID uniqueId) {
        return this.isTrader1(uniqueId) && this.getStatus() == TradeStatus.LOCKED_1 || this.isTrader2(uniqueId) && this.getStatus() == TradeStatus.LOCKED_2;
    }
    
    public boolean hasNotAccepted(@NotNull final UUID uniqueId) {
        
        if (this.isTrader1(uniqueId)) {
            return this.getStatus() == TradeStatus.DECIDE || this.getStatus() == TradeStatus.ACCEPT_2;
        } else if (this.isTrader2(uniqueId)) {
            return this.getStatus() == TradeStatus.DECIDE || this.getStatus() == TradeStatus.ACCEPT_1;
        } else {
            return false;
        }
    }
    
    public boolean hasAccepted(@NotNull final UUID uniqueId) {
        return this.isTrader1(uniqueId) && this.getStatus() == TradeStatus.ACCEPT_1 || this.isTrader2(uniqueId) && this.getStatus() == TradeStatus.ACCEPT_2;
    }
    
    public boolean hasCompleted() {
        return this.getStatus() == TradeStatus.COMPLETE;
    }
    
    @Nullable
    public Inventory createTradeInventory(@NotNull final Server server, @NotNull final UUID uniqueId) {
        
        if (this.isTrader1(uniqueId)) {
            return this.createTradeInventory(server, this.chest1);
        } else if (this.isTrader2(uniqueId)) {
            return this.createTradeInventory(server, this.chest2);
        } else {
            return null;
        }
    }
    
    @NotNull
    public Inventory createTradeInventory(@NotNull final Server server, @NotNull final Chest chest) {
        
        final Inventory inventory = server.createInventory(null, 45);
        final ItemStack[] items = chest.getInventory().getStorageContents();
        for (int slot = 0; slot < items.length && slot < 27; slot++) {
            inventory.setItem(slot, items[slot]);
        }
        
        final ItemStack fill = new ItemStack(Material.BLACK_CONCRETE);
        final ItemMeta fillMeta = fill.getItemMeta();
        fillMeta.setDisplayName("");
        fill.setItemMeta(fillMeta);
        for (int slot = 37; slot < 44; slot++) {
            inventory.setItem(slot, fill);
        }
        
        final ItemStack reject = new ItemStack(Material.RED_CONCRETE);
        final ItemMeta rejectMeta = reject.getItemMeta();
        rejectMeta.setDisplayName("REJECT/CANCEL TRADE");
        reject.setItemMeta(rejectMeta);
        inventory.setItem(TradePlugin.SLOT_REJECT, reject);
        
        final ItemStack accept = new ItemStack(Material.LIME_CONCRETE);
        final ItemMeta acceptMeta = accept.getItemMeta();
        acceptMeta.setDisplayName("ACCEPT TRADE");
        accept.setItemMeta(acceptMeta);
        inventory.setItem(TradePlugin.SLOT_ACCEPT, accept);
        
        return inventory;
    }
    
    public void swapItems(@NotNull final Player self, @NotNull final Player other) {
        
        final Chest chestSelf;
        final Chest chestOther;
        
        if (this.isTrader1(self.getUniqueId())) {
            chestSelf = this.chest1;
            chestOther = this.chest2;
        } else {
            chestSelf = this.chest2;
            chestOther = this.chest1;
        }
        
        final Inventory extraSelf = this.transferItems(self.getServer(), self.getInventory(), chestOther.getInventory());
        final Inventory extraOther = this.transferItems(other.getServer(), other.getInventory(), chestSelf.getInventory());
        
        self.sendMessage("§aTrade complete!");
        other.sendMessage("§aTrade complete!");
        
        if (extraSelf != null) {
            
            self.sendMessage("§6There were items sent to you that could not be put in your inventory. Be sure to pick them up before leaving the trade room.");
            final Location location = self.getLocation().add(new Vector(0.0D, 1.0D, 0.0D));
            final World world = self.getWorld();
            
            for (final ItemStack item : extraSelf.getStorageContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    world.dropItemNaturally(location, item);
                }
            }
        }
        if (extraOther != null) {
            
            other.sendMessage("§6There were items sent to you that could not be put in your inventory. Be sure to pick them up before leaving the trade room.");
            final Location location = other.getLocation().add(new Vector(0.0D, 1.0D, 0.0D));
            final World world = other.getWorld();
            
            for (final ItemStack item : extraOther.getStorageContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    world.dropItemNaturally(location, item);
                }
            }
        }
    }
    
    @Nullable
    public Inventory swapItems(@NotNull final Player self, @NotNull final Offline other) {
        
        final Chest chestSelf;
        final Chest chestOther;
        
        if (this.isTrader1(self.getUniqueId())) {
            chestSelf = this.chest1;
            chestOther = this.chest2;
        } else {
            chestSelf = this.chest2;
            chestOther = this.chest1;
        }
        
        final Inventory otherInventory;
        if (other.getInventory() == null) {
            otherInventory = self.getServer().createInventory(null, 27);
        } else {
            otherInventory = other.getInventory();
        }
        
        final Inventory extraSelf = this.transferItems(self.getServer(), self.getInventory(), chestOther.getInventory());
        final Inventory extraOther = this.transferItems(self.getServer(), otherInventory, chestSelf.getInventory());
        
        self.sendMessage("§aTrade complete!");
        other.setInventory(otherInventory);
        
        if (extraSelf != null) {
            
            self.sendMessage("§6There were items sent to you that could not be put in your inventory. Be sure to pick them up before leaving the trade room.");
            final Location location = self.getLocation().add(new Vector(0.0D, 1.0D, 0.0D));
            final World world = self.getWorld();
            
            for (final ItemStack item : extraSelf.getStorageContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    world.dropItemNaturally(location, item);
                }
            }
        }
        
        return extraOther;
    }
    
    public void returnItems(@NotNull final Player self, @NotNull final Player other) {
        this.returnItems(self);
        this.returnItems(other);
    }
    
    @Nullable
    public Inventory returnItems(@NotNull final Player self, @Nullable final Offline other) {
        
        this.returnItems(self);
        
        if (other == null) {
            return null;
        }
        return this.returnItems(self.getServer(), other);
    }
    
    public void returnItems(@NotNull final Player player) {
        
        final Chest chest = this.isTrader1(player.getUniqueId()) ? this.chest1 : this.chest2;
        final Inventory extra = this.transferItems(player.getServer(), player.getInventory(), chest.getInventory());
        
        player.sendMessage("§aReturn complete!");
        
        if (extra != null) {
            
            player.sendMessage("§6There were items returned to you that could not be put in your inventory. Be sure to pick them up before leaving the trade room.");
            final Location location = player.getLocation().add(new Vector(0.0D, 1.0D, 0.0D));
            final World world = player.getWorld();
            
            for (final ItemStack item : extra.getStorageContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    world.dropItemNaturally(location, item);
                }
            }
        }
    }
    
    @Nullable
    public Inventory returnItems(@NotNull final Server server, @NotNull final Offline offline) {
        
        final Chest chest = this.isTrader1(offline.getUniqueId()) ? this.chest1 : this.chest2;
        final Inventory inventory = offline.getInventory() == null ? server.createInventory(null, 27) : offline.getInventory();
        final Inventory extra = this.transferItems(server, inventory, chest.getInventory());
        offline.setInventory(inventory);
        
        return extra;
    }
    
    @Nullable
    private Inventory transferItems(@NotNull final Server server, @NotNull final Inventory to, @NotNull final Inventory from) {
        
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
        
        if (!dropRequired) {
            from.setStorageContents(fromItems);
            return null;
        }
        
        final ItemStack[] extraItems = new ItemStack[27];
        for (int slot = 0; slot < 27; slot++) {
            final ItemStack item = fromItems[slot];
            if (item != null && item.getType() != Material.AIR) {
                extraItems[slot] = item;
                fromItems[slot] = null;
            }
        }
        
        final Inventory extra = server.createInventory(null, 27);
        extra.setStorageContents(extraItems);
        from.setStorageContents(fromItems);
        
        return extra;
    }
    
    @Override
    public boolean equals(@Nullable final Object object) {
        
        if (object == this) {
            return true;
        }
        
        if (object == null) {
            return false;
        }
        if (!(object instanceof TradeRoom)) {
            return false;
        }
        
        final TradeRoom other = (TradeRoom) object;
        
        if (!this.getName().equals(other.getName())) {
            return false;
        }
        if (!this.getChest1().getLocation().equals(other.getChest1().getLocation())) {
            return false;
        }
        if (!this.getChest2().getLocation().equals(other.getChest2().getLocation())) {
            return false;
        }
        if (!this.buttonIn1.equals(other.buttonIn1)) {
            return false;
        }
        if (!this.buttonIn2.equals(other.buttonIn2)) {
            return false;
        }
        if (!this.buttonOut1.equals(other.buttonOut1)) {
            return false;
        }
        if (!this.buttonOut2.equals(other.buttonOut2)) {
            return false;
        }
        if (!this.buttonLock1.equals(other.buttonLock1)) {
            return false;
        }
        if (!this.buttonLock2.equals(other.buttonLock2)) {
            return false;
        }
        if (!this.buttonAccept1.equals(other.buttonAccept1)) {
            return false;
        }
        if (!this.buttonAccept2.equals(other.buttonAccept2)) {
            return false;
        }
        if (!this.buttonDeny1.equals(other.buttonDeny1)) {
            return false;
        }
        return this.buttonDeny2.equals(other.buttonDeny2);
    }
}
