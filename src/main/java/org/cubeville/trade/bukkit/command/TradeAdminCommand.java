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

package org.cubeville.trade.bukkit.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.cubeville.trade.bukkit.TradePlugin;
import org.jetbrains.annotations.NotNull;

public final class TradeAdminCommand implements TabExecutor {
    
    private final TradePlugin plugin;
    
    public TradeAdminCommand(@NotNull final TradePlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label, @NotNull final String[] rawArgs) {
        
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly Players may execute this command. The console sender may not execute this command.");
            return true;
        }
        
        final Player player = (Player) sender;
        final List<String> args = new ArrayList<String>(Arrays.asList(rawArgs));
        
        if (args.isEmpty()) {
            player.sendMessage("§bAvailable commands:");
            player.sendMessage(" §f-§r §b/tradeadmin startbuilder");
            player.sendMessage(" §f-§r §b/tradeadmin stopbuilder");
            player.sendMessage(" §f-§r §b/tradeadmin resetbuilder");
            player.sendMessage(" §f-§r §b/tradeadmin setname§r §a<trade room name>");
            player.sendMessage(" §f-§r §b/tradeadmin setregions§r §a<region 1> <region 2>");
            player.sendMessage(" §f-§r §b/tradeadmin setteleport");
            return true;
        }
        
        final String arg = args.remove(0);
        if (arg.equalsIgnoreCase("startbuilder")) {
            
            if (!args.isEmpty()) {
                player.sendMessage("§cSyntax:§r §b/tradeadmin startbuilder");
                return true;
            }
            
            this.plugin.startBuilder(player);
            return true;
        }
        if (arg.equalsIgnoreCase("stopbuilder")) {
            
            if (!args.isEmpty()) {
                player.sendMessage("§cSyntax:§r §b/tradeadmin stopbuilder");
                return true;
            }
            
            this.plugin.stopBuilder(player);
            return true;
        }
        if (arg.equalsIgnoreCase("resetbuilder")) {
            
            if (!args.isEmpty()) {
                player.sendMessage("§cSyntax:§r §b/tradeadmin resetbuilder");
                return true;
            }
            
            this.plugin.resetBuilder(player);
            return true;
        }
        if (arg.equalsIgnoreCase("setname")) {
            
            if (args.isEmpty()) {
                player.sendMessage("§cSyntax:§r §b/tradeadmin setname§r §a<trade room name>");
                return true;
            }
            
            final String name = args.remove(0);
            if (!args.isEmpty()) {
                player.sendMessage("§cSyntax:§r §b/tradeadmin setname§r §a<trade room name>");
                return true;
            }
            
            this.plugin.setName(player, name);
            return true;
        }
        if (arg.equalsIgnoreCase("setregions")) {
            
            if (args.isEmpty()) {
                player.sendMessage("§cSyntax:§r §b/tradeadmin setregions§r §a<region 1> <region 2>");
                return true;
            }
            
            final String name1 = args.remove(0);
            if (args.isEmpty()) {
                player.sendMessage("§cSyntax:§r §b/tradeadmin setregions§r §a<region 1> <region 2>");
                return true;
            }
            
            final String name2 = args.remove(0);
            if (!args.isEmpty()) {
                player.sendMessage("§cSyntax:§r §b/tradeadmin setregions§r §a<region 1> <region 2>");
                return true;
            }
            
            this.plugin.setRegions(player, name1, name2);
        }
        if (arg.equalsIgnoreCase("setteleport")) {
            
            if (!args.isEmpty()) {
                player.sendMessage("§cSyntax:§r §b/tradeadmin setteleport");
                return true;
            }
            
            this.plugin.setTeleport(player);
            return true;
        }
        
        player.sendMessage("§bAvailable commands:");
        player.sendMessage(" §f-§r §b/tradeadmin startbuilder");
        player.sendMessage(" §f-§r §b/tradeadmin stopbuilder");
        player.sendMessage(" §f-§r §b/tradeadmin resetbuilder");
        player.sendMessage(" §f-§r §b/tradeadmin setname§r §a<trade room name>");
        player.sendMessage(" §f-§r §b/tradeadmin setregions§r §a<region 1> <region 2>");
        player.sendMessage(" §f-§r §b/tradeadmin setteleport");
        return true;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public List<String> onTabComplete(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label, @NotNull final String[] rawArgs) {
        
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }
        
        final List<String> args = new ArrayList<String>(Arrays.asList(rawArgs));
        final List<String> completions = new ArrayList<String>();
        
        completions.add("startbuilder");
        completions.add("stopbuilder");
        completions.add("resetbuilder");
        completions.add("setname");
        completions.add("setregions");
        completions.add("setteleport");
        
        if (args.isEmpty()) {
            return completions;
        }
        
        final String arg = args.remove(0);
        if (args.isEmpty()) {
            completions.removeIf(completion -> !completion.toLowerCase().startsWith(arg.toLowerCase()));
            return completions;
        }
        
        return Collections.emptyList();
    }
}
