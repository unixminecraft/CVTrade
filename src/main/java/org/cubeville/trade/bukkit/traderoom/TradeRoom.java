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
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.cubeville.trade.bukkit.TradePlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TradeRoom {
    
    private static final String KEY_NAME = "name";
    
    private static final String KEY_CHEST_1 = "chest_1";
    private static final String KEY_REGION_1 = "region_1";
    private static final String KEY_TELEPORT_IN_1 = "teleport_in_1";
    private static final String KEY_TELEPORT_OUT_1 = "teleport_out_1";
    private static final String KEY_BUTTON_IN_1 = "button_in_1";
    private static final String KEY_BUTTON_OUT_1 = "button_out_1";
    private static final String KEY_BUTTON_LOCK_1 = "button_lock_1";
    private static final String KEY_BUTTON_ACCEPT_1 = "button_accept_1";
    private static final String KEY_BUTTON_DENY_1 = "button_deny_1";
    
    private static final String KEY_CHEST_2 = "chest_2";
    private static final String KEY_REGION_2 = "region_2";
    private static final String KEY_TELEPORT_IN_2 = "teleport_in_2";
    private static final String KEY_TELEPORT_OUT_2 = "teleport_out_2";
    private static final String KEY_BUTTON_IN_2 = "button_in_2";
    private static final String KEY_BUTTON_OUT_2 = "button_out_2";
    private static final String KEY_BUTTON_LOCK_2 = "button_lock_2";
    private static final String KEY_BUTTON_ACCEPT_2 = "button_accept_2";
    private static final String KEY_BUTTON_DENY_2 = "button_deny_2";
    
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
    
    private Trader trader1;
    private Trader trader2;
    
    private UUID uniqueId1;
    private String name1;
    private TradeStatus status1;
    
    private UUID uniqueId2;
    private String name2;
    private TradeStatus status2;
    
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
    }
    
    public TradeRoom(@NotNull final Configuration config) throws IllegalArgumentException {
        
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
        
        return config;
    }
    
    public boolean contains(@NotNull final Location location) {
        return this.containsChest(location) || this.containsButton(location);
    }
    
    @Nullable
    public Side getSide(@NotNull final Location location) {
        
        final Side side = this.getChestSide(location);
        if (side != null) {
            return side;
        }
        return this.getButtonSide(location);
    }
    
    public boolean containsChest(@NotNull final Location location) {
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
    
    public boolean containsButton(@NotNull final Location location) {
        return this.getButtonSide(location) != null;
    }
    
    @Nullable
    public Side getButtonSide(@NotNull final Location location) {
        
        if (this.buttonIn1.contains(location)) {
            return Side.SIDE_1;
        } else if (this.buttonOut1.contains(location)) {
            return Side.SIDE_1;
        } else if (this.buttonLock1.contains(location)) {
            return Side.SIDE_1;
        } else if (this.buttonAccept1.contains(location)) {
            return Side.SIDE_1;
        } else if (this.buttonDeny1.contains(location)) {
            return Side.SIDE_1;
        } else if (this.buttonIn2.contains(location)) {
            return Side.SIDE_2;
        } else if (this.buttonOut2.contains(location)) {
            return Side.SIDE_2;
        } else if (this.buttonLock2.contains(location)) {
            return Side.SIDE_2;
        } else if (this.buttonAccept2.contains(location)) {
            return Side.SIDE_2;
        } else if (this.buttonDeny2.contains(location)) {
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
    
    @Nullable
    public Trader getTrader1() {
        return this.trader1;
    }
    
    @Nullable
    public Trader getTrader2() {
        return this.trader2;
    }
    
    public void setTrader1(@Nullable final Player player) {
        this.trader1 = player == null ? null : new Trader(player);
    }
    
    public void setTrader2(@Nullable final Player player) {
        this.trader2 = player == null ? null : new Trader(player);
    }
    
    public boolean isActive() {
        return this.getTrader1() != null || this.getTrader2() != null;
    }
    
    public boolean isFull() {
        return this.getTrader1() != null && this.getTrader2() != null;
    }
    
    public boolean isUsing(@NotNull final UUID uniqueId) {
        return this.isTrader1(uniqueId) || this.isTrader2(uniqueId);
    }
    
    public boolean isTrader1(@NotNull final UUID uniqueId) {
        return this.getTrader1() != null && this.getTrader1().getUniqueId().equals(uniqueId);
    }
    
    public boolean isTrader2(@NotNull final UUID uniqueId) {
        return this.getTrader2() != null && this.getTrader2().getUniqueId().equals(uniqueId);
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
