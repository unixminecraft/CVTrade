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

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SerializableAs("tradechest")
public final class TradeChest implements ConfigurationSerializable {
    
    private final String name;
    private final Chest chest;
    
    private String linkedChestName;
    
    public TradeChest(@NotNull final String name, @NotNull final Chest chest) throws IllegalArgumentException {
        this(name, chest, null);
    }
    
    private TradeChest(@NotNull final String name, @NotNull final Chest chest, @Nullable final String linkedChestName) throws IllegalArgumentException {
        if (name.equalsIgnoreCase(linkedChestName)) {
            throw new IllegalArgumentException("Cannot link this TradeChest to itself.");
        }
        if (chest.getInventory().getHolder() instanceof DoubleChest) {
            throw new IllegalArgumentException("Cannot create a TradeChest from a DoubleChest.");
        }
        
        this.name = name;
        this.chest = chest;
        this.linkedChestName = linkedChestName;
    }
    
    @NotNull
    public String getName() {
        return this.name;
    }
    
    @NotNull
    public Chest getChest() {
        return this.chest;
    }
    
    @Nullable
    public String getLinked() {
        return this.linkedChestName;
    }
    
    public void link(@NotNull final TradeChest linkedChest) throws IllegalArgumentException {
        if (this.equals(linkedChest)) {
            throw new IllegalArgumentException("§cYou may not link this TradeChest (§r§6" + this.name + "§r§c) to itself.");
        }
        
        final String checkName = linkedChest.getLinked();
        if (checkName != null && !checkName.equals(this.getName())) {
            throw new IllegalArgumentException("§cYou may not link this TradeChest (§r§6" + this.name + "§r§c) to another TradeChest (§r§6" + linkedChest.getName() + "§r§c) that is linked to a 3rd TradeChest (§r§6" + checkName + "§r§c).");
        }
        
        this.linkedChestName = linkedChest.getName();
    }
    
    public void unlink() {
        this.linkedChestName = null;
    }
    
    @Override
    public boolean equals(final Object object) {
        if (!(object instanceof TradeChest)) {
            return false;
        }
        final TradeChest other = (TradeChest) object;
        return this.name.equalsIgnoreCase(other.name) && this.chest.getLocation().equals(other.chest.getLocation());
    }
    
    @Override
    @NotNull
    public Map<String, Object> serialize() {
        
        final HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("name", this.name);
        data.put("location", this.chest.getLocation());
        data.put("linked", this.linkedChestName);
        return data;
    }
    
    @NotNull
    public static TradeChest deserialize(@NotNull final Map<String, Object> data) throws IllegalArgumentException {
        
        try {
            final String name = (String) data.get("name");
            final Location location = (Location) data.get("location");
            final String linkedChestName = (String) data.get("linked");
    
            if (name == null || location == null) {
                throw new IllegalArgumentException("Cannot create TradeChest, data member is null. Name: " + name + " / Location: " + location);
            }
    
            final BlockState state = Bukkit.getWorld(location.getWorld().getUID()).getBlockAt(location).getState();
            if (!(state instanceof Chest)) {
                throw new IllegalArgumentException("Block at specified location is not a Chest.");
            }
            
            return new TradeChest(name.toLowerCase(), (Chest) state, linkedChestName);
            
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Cannot create TradeChest, invalid data type.", e);
        }
    }
}
