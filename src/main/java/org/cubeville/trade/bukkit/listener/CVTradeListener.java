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

package org.cubeville.trade.bukkit.listener;

import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.cubeville.trade.bukkit.CVTrade;
import org.jetbrains.annotations.NotNull;

public final class CVTradeListener implements Listener {
    
    private final CVTrade tradePlugin;
    
    public CVTradeListener(@NotNull final CVTrade tradePlugin) {
        this.tradePlugin = tradePlugin;
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
        
        if (this.tradePlugin.inventoryClick(player, event.getInventory(), event.getSlot())) {
            event.setCancelled(true);
        }
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
    
                this.tradePlugin.performCreate(event.getPlayer(), event.getClickedBlock());
                break;
            case RIGHT_CLICK_BLOCK:
                if (event.getClickedBlock() == null) {
                    return;
                }
                
                if (this.tradePlugin.rightClickedBlock(event.getPlayer(), event.getClickedBlock())) {
                    event.setCancelled(true);
                }
                break;
            default:
                break;
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        this.tradePlugin.playerJoin(event.getPlayer());
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerKick(final PlayerKickEvent event) {
        this.tradePlugin.playerLeave(event.getPlayer());
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        this.tradePlugin.playerLeave(event.getPlayer());
    }
}
