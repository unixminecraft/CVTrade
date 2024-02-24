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

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.cubeville.trade.bukkit.TradePlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TradeRoomBuilder {
    
    private final TradePlugin plugin;
    private final Player player;
    private final World world;
    
    private String name;
    
    private ProtectedRegion region1;
    private ProtectedRegion region2;
    
    private Chest chest1;
    private Location teleportIn1;
    private Location teleportOut1;
    private Button buttonIn1;
    private Button buttonOut1;
    private Button buttonLock1;
    private Button buttonAccept1;
    private Button buttonDeny1;
    
    private Chest chest2;
    private Location teleportIn2;
    private Location teleportOut2;
    private Button buttonIn2;
    private Button buttonOut2;
    private Button buttonLock2;
    private Button buttonAccept2;
    private Button buttonDeny2;
    
    private BuildStep step;
    
    TradeRoomBuilder(@NotNull final TradePlugin plugin, @NotNull final Player player) {
        
        this.plugin = plugin;
        this.player = player;
        this.world = this.player.getWorld();
        
        this.name = null;
        
        this.region1 = null;
        this.region2 = null;
        
        this.chest1 = null;
        this.teleportIn1 = null;
        this.teleportOut1 = null;
        this.buttonIn1 = null;
        this.buttonOut1 = null;
        this.buttonLock1 = null;
        this.buttonAccept1 = null;
        this.buttonDeny1 = null;
        
        this.chest2 = null;
        this.teleportIn2 = null;
        this.teleportOut2 = null;
        this.buttonIn2 = null;
        this.buttonOut2 = null;
        this.buttonLock2 = null;
        this.buttonAccept2 = null;
        this.buttonDeny2 = null;
        
        this.step = BuildStep.NAME;
    }
    
    public boolean setName(@NotNull final String name) {
        
        final String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            this.player.sendMessage("§cThe trade room name cannot be blank.");
            return false;
        }
        if (this.plugin.tradeRoomExists(trimmed)) {
            this.player.sendMessage("§cThe trade room name§r §6" + trimmed + "§r §cis already in use.");
            return false;
        }
        
        this.name = trimmed.toLowerCase();
        
        this.nextStep();
        return true;
    }
    
    public boolean setRegions(@NotNull final String name1, @NotNull final String name2) {
        
        if (name1.equalsIgnoreCase(name2)) {
            this.player.sendMessage("§cYou must use 2 separate regions.");
            return false;
        }
        
        if (name1.equalsIgnoreCase(ProtectedRegion.GLOBAL_REGION) || name2.equalsIgnoreCase(ProtectedRegion.GLOBAL_REGION)) {
            this.player.sendMessage("§cA trade room cannot use the global region.");
            return false;
        }
        
        final RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(this.world));
        if (regionManager == null) {
            this.player.sendMessage("§cA trade room cannot be set up in the world§r §6" + this.world.getName() + "§r§c: No region managers exist.");
            return false;
        }
        
        final ProtectedRegion region1 = regionManager.getRegion(name1);
        if (region1 == null) {
            this.player.sendMessage("§cThe region§r §6" + name1 + "§r §cdoes not exist. Please create it before setting up the trade room region.");
            return false;
        }
        
        final ProtectedRegion region2 = regionManager.getRegion(name2);
        if (region2 == null) {
            this.player.sendMessage("§cThe region§r §6" + name2 + "§r §cdoes not exist. Please create it before setting up the trade room region.");
            return false;
        }
        
        if (region1.contains(region2.getMinimumPoint()) || region1.contains(region2.getMaximumPoint()) || region2.contains(region1.getMinimumPoint()) || region2.contains(region1.getMaximumPoint())) {
            this.player.sendMessage("§cThe regions may not overlap. Please adjust the regions, or use different ones.");
            return false;
        }
        
        this.region1 = region1;
        this.region2 = region2;
        
        this.nextStep();
        return true;
    }
    
    public boolean setChest1(@NotNull final Location location) {
        
        final World world = this.checkLocation(location, "trade chest 1", true, false);
        if (world == null) {
            return false;
        }
        
        final BlockState state = world.getBlockAt(location).getState();
        if (!(state instanceof Chest)) {
            this.player.sendMessage("§cPlease select a single chest as the 1st trade chest, you may not use a§r §6" + state.getType().name().toLowerCase() + "§r§c.");
            return false;
        }
        
        final Chest chest = (Chest) state;
        if (chest.getInventory().getHolder() instanceof DoubleChest) {
            this.player.sendMessage("§cYou may not use a double chest as the 1st trade chest, only single chests are allowed.");
            return false;
        }
        
        this.chest1 = chest;
        
        this.nextStep();
        return true;
    }
    
    public boolean setTeleportIn1(@NotNull final Location location) {
        
        if (this.checkLocation(location, "entry teleport 1", true, false) == null) {
            return false;
        }
        
        this.teleportIn1 = location;
        
        this.nextStep();
        return true;
    }
    
    public boolean setTeleportOut1(@NotNull final Location location) {
        
        if (this.checkLocation(location, "exit teleport 1", false, false) == null) {
            return false;
        }
        
        this.teleportOut1 = location;
        
        this.nextStep();
        return true;
    }
    
    public boolean setButtonIn1(@NotNull final Location location) {
        
        final BlockState state = this.checkButton(location, "entry button 1", false, false);
        if (state == null) {
            return false;
        }
        
        this.buttonIn1 = new Button(state);
        
        this.nextStep();
        return true;
    }
    
    public boolean setButtonOut1(@NotNull final Location location) {
        
        final BlockState state = this.checkButton(location, "exit button 1", true, false);
        if (state == null) {
            return false;
        }
        
        this.buttonOut1 = new Button(state);
        
        this.nextStep();
        return true;
    }
    
    public boolean setButtonLock1(@NotNull final Location location) {
        
        final BlockState state = this.checkButton(location, "trade lock button 1", true, false);
        if (state == null) {
            return false;
        }
        
        final Button button = new Button(state);
        if (this.buttonOut1.equals(button)) {
            this.player.sendMessage("§cYou cannot use the same button for the 1st trade room's exit and lock buttons.");
            return false;
        }
        
        this.buttonLock1 = button;
        
        this.nextStep();
        return true;
    }
    
    public boolean setButtonAccept1(@NotNull final Location location) {
        
        final BlockState state = this.checkButton(location, "trade accept button 1", true, false);
        if (state == null) {
            return false;
        }
        
        final Button button = new Button(state);
        if (this.buttonOut1.equals(button)) {
            this.player.sendMessage("§cYou cannot use the same button for the 1st trade room's exit and accept buttons.");
            return false;
        }
        if (this.buttonLock1.equals(button)) {
            this.player.sendMessage("§cYou cannot use the same button for the 1st trade room's lock and accept buttons.");
            return false;
        }
        
        this.buttonAccept1 = button;
        
        this.nextStep();
        return true;
    }
    
    public boolean setButtonDeny1(@NotNull final Location location) {
        
        final BlockState state = this.checkButton(location, "trade deny button 1", true, false);
        if (state == null) {
            return false;
        }
        
        final Button button = new Button(state);
        if (this.buttonOut1.equals(button)) {
            this.player.sendMessage("§cYou cannot use the same button for the 1st trade room's exit and deny buttons.");
            return false;
        }
        if (this.buttonLock1.equals(button)) {
            this.player.sendMessage("§cYou cannot use the same button for the 1st trade room's lock and deny buttons.");
            return false;
        }
        if (this.buttonAccept1.equals(button)) {
            this.player.sendMessage("§cYou cannot use the same button for the 1st trade room's accept and deny buttons.");
            return false;
        }
        
        this.buttonDeny1 = button;
        
        this.nextStep();
        return true;
    }
    
    public boolean setChest2(@NotNull final Location location) {
        
        final World world = this.checkLocation(location, "trade chest 2", false, true);
        if (world == null) {
            return false;
        }
        
        final BlockState state = world.getBlockAt(location).getState();
        if (!(state instanceof Chest)) {
            this.player.sendMessage("§cPlease select a single chest as the 2nd trade chest, you may not use a§r §6" + state.getType().name().toLowerCase() + "§r§c.");
            return false;
        }
        
        final Chest chest = (Chest) state;
        if (chest.getInventory().getHolder() instanceof DoubleChest) {
            this.player.sendMessage("§cYou may not use a double chest as the 2nd trade chest, only single chests are allowed.");
            return false;
        }
        
        this.chest2 = chest;
        
        this.nextStep();
        return true;
    }
    
    public boolean setTeleportIn2(@NotNull final Location location) {
        
        if (this.checkLocation(location, "entry teleport 2", false, true) == null) {
            return false;
        }
        
        this.teleportIn2 = location;
        
        this.nextStep();
        return true;
    }
    
    public boolean setTeleportOut2(@NotNull final Location location) {
        
        if (this.checkLocation(location, "exit teleport 2", false, false) == null) {
            return false;
        }
        
        this.teleportOut2 = location;
        
        this.nextStep();
        return true;
    }
    
    public boolean setButtonIn2(@NotNull final Location location) {
        
        final BlockState state = this.checkButton(location, "entry button 2", false, false);
        if (state == null) {
            return false;
        }
        
        final Button button = new Button(state);
        if (this.buttonIn1.equals(button)) {
            this.player.sendMessage("§cYou cannot use the same button for both entry buttons.");
            return false;
        }
        
        this.buttonIn2 = button;
        
        this.nextStep();
        return true;
    }
    
    public boolean setButtonOut2(@NotNull final Location location) {
        
        final BlockState state = this.checkButton(location, "exit button 2", false, true);
        if (state == null) {
            return false;
        }
        
        this.buttonOut2 = new Button(state);
        
        this.nextStep();
        return true;
    }
    
    public boolean setButtonLock2(@NotNull final Location location) {
        
        final BlockState state = this.checkButton(location, "trade lock button 2", false, true);
        if (state == null) {
            return false;
        }
        
        final Button button = new Button(state);
        if (this.buttonOut2.equals(button)) {
            this.player.sendMessage("§cYou cannot use the same button for the 2nd trade room's exit and lock buttons.");
            return false;
        }
        
        this.buttonLock2 = button;
        
        this.nextStep();
        return true;
    }
    
    public boolean setButtonAccept2(@NotNull final Location location) {
        
        final BlockState state = this.checkButton(location, "trade accept button 2", false, true);
        if (state == null) {
            return false;
        }
        
        final Button button = new Button(state);
        if (this.buttonOut2.equals(button)) {
            this.player.sendMessage("§cYou cannot use the same button for the 2nd trade room's exit and accept buttons.");
            return false;
        }
        if (this.buttonLock2.equals(button)) {
            this.player.sendMessage("§cYou cannot use the same button for the 2nd trade room's lock and accept buttons.");
            return false;
        }
        
        this.buttonAccept2 = button;
        
        this.nextStep();
        return true;
    }
    
    public boolean setButtonDeny2(@NotNull final Location location) {
        
        final BlockState state = this.checkButton(location, "trade deny button 2", false, true);
        if (state == null) {
            return false;
        }
        
        final Button button = new Button(state);
        if (this.buttonOut2.equals(button)) {
            this.player.sendMessage("§cYou cannot use the same button for the 2nd trade room's exit and deny buttons.");
            return false;
        }
        if (this.buttonLock2.equals(button)) {
            this.player.sendMessage("§cYou cannot use the same button for the 2nd trade room's lock and deny buttons.");
            return false;
        }
        if (this.buttonAccept2.equals(button)) {
            this.player.sendMessage("§cYou cannot use the same button for the 2nd trade room's accept and deny buttons.");
            return false;
        }
        
        this.buttonDeny2 = button;
        
        this.nextStep();
        return true;
    }
    
    @NotNull
    public BuildStep getStep() {
        return this.step;
    }
    
    private void nextStep() {
        this.step = this.step.next();
    }
    
    /**
     * Builds the data, returning the new {@link TradeRoom}.
     * 
     * @return The newly-built {@link TradeRoom}.
     * @throws IllegalStateException If some part of the data stored in this
     *                               {@link TradeRoomBuilder} is invalid.
     */
    @NotNull
    public TradeRoom build() throws IllegalStateException {
        
        final List<String> fixes = new ArrayList<String>();
        if (this.name == null) {
            fixes.add("Room Name");
        }
        if (this.region1 == null) {
            fixes.add("Region 1");
        }
        if (this.region2 == null) {
            fixes.add("Region 2");
        }
        if (this.chest1 == null) {
            fixes.add("Trade Chest 1");
        }
        if (this.teleportIn1 == null) {
            fixes.add("Teleport Entry Location 1");
        }
        if (this.teleportOut1 == null) {
            fixes.add("Teleport Out Location 1");
        }
        if (this.buttonIn1 == null) {
            fixes.add("Entry Button 1");
        }
        if (this.buttonOut1 == null) {
            fixes.add("Exit Button 1");
        }
        if (this.buttonLock1 == null) {
            fixes.add("Lock Trade Button 1");
        }
        if (this.buttonAccept1 == null) {
            fixes.add("Accept Trade Button 1");
        }
        if (this.buttonDeny1 == null) {
            fixes.add("Deny Trade Button 1");
        }
        if (this.chest2 == null) {
            fixes.add("Trade Chest 2");
        }
        if (this.teleportIn2 == null) {
            fixes.add("Teleport Entry Location 2");
        }
        if (this.teleportOut2 == null) {
            fixes.add("Teleport Out Location 2");
        }
        if (this.buttonIn2 == null) {
            fixes.add("Entry Button 2");
        }
        if (this.buttonOut2 == null) {
            fixes.add("Exit Button 2");
        }
        if (this.buttonLock2 == null) {
            fixes.add("Lock Trade Button 2");
        }
        if (this.buttonAccept2 == null) {
            fixes.add("Accept Trade Button 2");
        }
        if (this.buttonDeny2 == null) {
            fixes.add("Deny Trade Button 2");
        }
        
        if (!fixes.isEmpty()) {
            
            this.player.sendMessage("§cYou cannot yet build the trade room. Please set the following items before building:");
            for (final String fix : fixes) {
                this.player.sendMessage(" §f-§r §b" + fix);
            }
            
            throw new IllegalStateException();
        }
        
        if (this.step != BuildStep.COMPLETED) {
            this.player.sendMessage("§cYou cannot yet build the trade room. An issue occurred, you are still on step§r §6" + this.step.name().toLowerCase() + "§r§c.");
            throw new IllegalStateException();
        }
        
        return new TradeRoom(
                this.name,
                this.region1,
                this.chest1,
                this.teleportIn1,
                this.teleportOut1,
                this.buttonIn1,
                this.buttonOut1,
                this.buttonLock1,
                this.buttonAccept1,
                this.buttonDeny1,
                this.region2,
                this.chest2,
                this.teleportIn2,
                this.teleportOut2,
                this.buttonIn2,
                this.buttonOut2,
                this.buttonLock2,
                this.buttonAccept2,
                this.buttonDeny2
        );
    }
    
    @Nullable
    private World checkLocation(@NotNull final Location location, @NotNull final String type, final boolean contains1, final boolean contains2) {
        
        final World world = location.getWorld();
        if (world == null) {
            this.player.sendMessage("§cThe " + type + " location cannot have a null world.");
            return null;
        }
        if (contains1 && (!world.getUID().equals(this.world.getUID()) || !this.region1.contains(BukkitAdapter.asBlockVector(location)))) {
            this.player.sendMessage("§cThe " + type + " must be within trade room region 1.");
            return null;
        }
        if (!contains1 && this.region1.contains(BukkitAdapter.asBlockVector(location))) {
            this.player.sendMessage("§cThe " + type + " must not be within the trade room region 1.");
            return null;
        }
        if (contains2 && (!world.getUID().equals(this.world.getUID()) || !this.region2.contains(BukkitAdapter.asBlockVector(location)))) {
            this.player.sendMessage("§cThe " + type + " must be within trade room region 2.");
            return null;
        }
        if (!contains2 && this.region2.contains(BukkitAdapter.asBlockVector(location))) {
            this.player.sendMessage("§cThe " + type + " must not be within the trade room region 2.");
            return null;
        }
        
        return world;
    }
    
    @Nullable
    private BlockState checkButton(@NotNull final Location location, @NotNull final String type, final boolean contains1, final boolean contains2) {
        
        final World world = this.checkLocation(location, type, contains1, contains2);
        if (world == null) {
            return null;
        }
        
        final BlockState state = world.getBlockAt(location).getState();
        if (!this.isButton(state)) {
            this.player.sendMessage("§cPlease use a button for the " + type + ". You cannot use a " + state.getType().name().toLowerCase() + ".");
            return null;
        }
        
        return state;
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
}
