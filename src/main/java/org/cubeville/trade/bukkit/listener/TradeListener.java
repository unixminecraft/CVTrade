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

package org.cubeville.trade.bukkit.listener;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.cubeville.trade.bukkit.TradePlugin;
import org.jetbrains.annotations.NotNull;

public final class TradeListener implements Listener {
    
    private final TradePlugin plugin;
    
    public TradeListener(@NotNull final TradePlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(final BlockBreakEvent event) {
        
        if (event.isCancelled()) {
            return;
        }
        
        if (this.plugin.blockBreak(event.getBlock().getState().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cYou may not break that item: it is part of a trade room.");
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(final BlockPlaceEvent event) {
        
        if (event.isCancelled()) {
            return;
        }
        
        final Block block = event.getBlock();
        if (block.getType() == Material.AIR) {
            return;
        }
        
        final BlockState state = block.getState();
        if (!(state instanceof Chest)) {
            return;
        }
        
        if (this.plugin.blockPlace((Chest) state)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cYou may not place that chest: the chest that would connect to it is a trade chest, and they are not allowed to be double chests.");
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(final InventoryClickEvent event) {
        
        if (event.isCancelled()) {
            return;
        }
        
        final HumanEntity human = event.getWhoClicked();
        if (!(human instanceof Player)) {
            return;
        }
        
        final Player player = (Player) human;
        if (event.getSlotType() != InventoryType.SlotType.CONTAINER) {
            return;
        }
        
        if (event.getRawSlot() != event.getSlot()) {
            return;
        }
        
        if (this.plugin.inventoryClick(player, event.getInventory(), event.getSlot())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onPlayerCommandSend(final PlayerCommandSendEvent event) {
        event.getCommands().removeAll(this.plugin.playerCommandSend(event.getPlayer()));
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        
        switch (event.getAction()) {
            case LEFT_CLICK_BLOCK:
                if (event.useInteractedBlock() == Event.Result.DENY) {
                    return;
                }
                if (event.getClickedBlock() == null) {
                    return;
                }
                
                this.plugin.buildRoom(event.getPlayer(), event.getClickedBlock().getState());
                break;
            case RIGHT_CLICK_BLOCK:
                if (event.getClickedBlock() == null) {
                    return;
                }
                
                if (this.plugin.rightClickedBlock(event.getPlayer(), event.getClickedBlock().getState())) {
                    event.setCancelled(true);
                }
                break;
            default:
                break;
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        this.plugin.playerJoin(event.getPlayer());
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerKick(final PlayerKickEvent event) {
        this.plugin.playerLeave(event.getPlayer());
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        this.plugin.playerLeave(event.getPlayer());
    }
}
