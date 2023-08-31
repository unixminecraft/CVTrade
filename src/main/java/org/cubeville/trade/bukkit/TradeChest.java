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

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a single chest that can be used for trading.
 */
@SerializableAs("tradechest")
public final class TradeChest implements ConfigurationSerializable {
    
    private static final String KEY_NAME = "name";
    private static final String KEY_LOCATION = "location";
    private static final String KEY_LINKED = "linked";
    
    private final String name;
    private final Chest chest;
    private String linked;
    
    /**
     * Sets up a {@link TradeChest} that is not linked to any other
     * {@link TradeChest}.
     * 
     * @param name The name of the {@link TradeChest}.
     * @param chest The single {@link Chest} to use as the {@link TradeChest}.
     * @throws IllegalArgumentException If the {@link TradeChest} is a
     *                                  {@link DoubleChest}.
     */
    public TradeChest(@NotNull final String name, @NotNull final Chest chest) throws IllegalArgumentException {
        this(name, chest, null);
    }
    
    /**
     * Sets up a {@link TradeChest} that is linked to the {@link TradeChest}
     * with the given name.
     * 
     * @param name The name of the {@link TradeChest}.
     * @param chest The single {@link Chest} to use as the {@link TradeChest}.
     * @param linked The name of the linked {@link TradeChest}, or {@code null}
     *               if no {@link TradeChest} should be linked.
     * @throws IllegalArgumentException If the {@link TradeChest} is a
     *                                  {@link DoubleChest}.
     */
    private TradeChest(@NotNull final String name, @NotNull final Chest chest, @Nullable final String linked) throws IllegalArgumentException {
        
        if (name.equalsIgnoreCase(linked)) {
            throw new IllegalArgumentException("Cannot link this TradeChest to itself.");
        }
        if (chest.getInventory().getHolder() instanceof DoubleChest) {
            throw new IllegalArgumentException("Cannot create a TradeChest from a DoubleChest.");
        }
        
        this.name = name;
        this.chest = chest;
        this.linked = linked;
    }
    
    /**
     * Gets the name of this {@link TradeChest}.
     * 
     * @return The name of this {@link TradeChest}.
     */
    @NotNull
    public String getName() {
        return this.name;
    }
    
    /**
     * Gets the {@link Chest} used for this {@link TradeChest}.
     * 
     * @return The {@link Chest} used for this {@link TradeChest}.
     */
    @NotNull
    public Chest getChest() {
        return this.chest;
    }
    
    /**
     * Gets the name of the linked {@link TradeChest}, or {@code null} if no
     * {@link TradeChest} is linked.
     * 
     * @return The name of the linked {@link TradeChest}, or {@code null}.
     */
    @Nullable
    public String getLinked() {
        return this.linked;
    }
    
    /**
     * Links the given {@link TradeChest} to this {@link TradeChest}.
     * 
     * @param other The {@link TradeChest} to link to this one.
     * @throws IllegalArgumentException If the given {@link TradeChest} is equal
     *                                  to this {@link TradeChest}, or if it is
     *                                  already linked to a different, 3rd
     *                                  {@link TradeChest}.
     */
    public void link(@NotNull final TradeChest other) throws IllegalArgumentException {
        
        if (this.equals(other)) {
            throw new IllegalArgumentException("§cYou may not link this TradeChest (§r§6" + this.name + "§r§c) to itself.");
        }
        
        final String checkName = other.getLinked();
        if (checkName != null && !checkName.equals(this.getName())) {
            throw new IllegalArgumentException("§cYou may not link this TradeChest (§r§6" + this.name + "§r§c) to another TradeChest (§r§6" + other.getName() + "§r§c) that is linked to a 3rd TradeChest (§r§6" + checkName + "§r§c).");
        }
        
        this.linked = other.getName();
    }
    
    /**
     * Unlinks this {@link TradeChest} from its linked {@link TradeChest}.
     * <p>
     * This will not unlink the linked {@link TradeChest} from this
     * {@link TradeChest}. That action must be performed manually.
     */
    public void unlink() {
        this.linked = null;
    }
    
    /**
     * Checks if the given {@link Object} is equal to this {@link TradeChest}.
     * 
     * @param object The {@link Object} to compare to.
     * @return {@code true} if the given {@link Object} is equal to this
     *         {@link TradeChest}, {@code false} otherwise.
     */
    @Override
    public boolean equals(@Nullable final Object object) {
        
        if (object == this) {
            return true;
        }
        
        if (object == null) {
            return false;
        }
        if (!(object instanceof TradeChest)) {
            return false;
        }
        
        final TradeChest other = (TradeChest) object;
        return this.name.equalsIgnoreCase(other.name) && this.chest.getLocation().equals(other.chest.getLocation());
    }
    
    /**
     * Serializes this {@link TradeChest} into a {@link Map}.
     * 
     * @return The serialized {@link Map} representing this {@link TradeChest}.
     */
    @Override
    @NotNull
    public Map<String, Object> serialize() {
        
        final Map<String, Object> data = new HashMap<String, Object>();
        
        data.put(KEY_NAME, this.name);
        data.put(KEY_LOCATION, this.chest.getLocation());
        data.put(KEY_LINKED, this.linked);
        
        return data;
    }
    
    /**
     * Deserializes the given {@link Map} into a {@link TradeChest}, throwing an
     * {@link IllegalArgumentException} if the data cannot be parsed properly.
     * 
     * @param data The {@link Map} of data to deserialize.
     * @return A {@link TradeChest} constructed from the given data.
     * @throws IllegalArgumentException If the given {@link Map} of data cannot
     *                                  properly create a {@link TradeChest}.
     */
    @NotNull
    public static TradeChest deserialize(@NotNull final Map<String, Object> data) throws IllegalArgumentException {
        
        try {
            final String name = (String) data.get("name");
            final Location location = (Location) data.get("location");
            final String linkedChestName = (String) data.get("linked");
            
            if (name == null || location == null) {
                throw new IllegalArgumentException("Cannot create TradeChest, data member is null. Name: " + name + " / Location: " + location);
            }
            
            final World world = location.getWorld();
            if (world == null) {
                throw new IllegalArgumentException("Location does not contain a valid World.");
            }
            
            final BlockState state = world.getBlockAt(location).getState();
            if (!(state instanceof Chest)) {
                throw new IllegalArgumentException("Block at specified location is not a Chest.");
            }
            
            return new TradeChest(name.toLowerCase(), (Chest) state, linkedChestName);
            
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Cannot create TradeChest, invalid data type.", e);
        }
    }
}
