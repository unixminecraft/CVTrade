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

package org.cubeville.trade.bukkit.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.cubeville.trade.bukkit.CVTrade;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CVTradeCommand implements TabExecutor {

    private final CVTrade tradePlugin;
    
    public CVTradeCommand(@NotNull final CVTrade tradePlugin) {
        this.tradePlugin = tradePlugin;
    }
    
    @Override
    public boolean onCommand(@NotNull final CommandSender commandSender, @NotNull final Command command, @NotNull final String label, @NotNull final String[] rawArgs) {
        
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage("§cOnly Players may execute this command. The console sender may not execute this command.");
            return true;
        }
        
        final Player sender = (Player) commandSender;
        final ArrayList<String> args = new ArrayList<String>(Arrays.asList(rawArgs));
        if (args.isEmpty()) {
            sender.sendMessage("§cPlease specify a sub-command to execute:");
            
            boolean optionGiven = false;
            if (sender.hasPermission("cvtrade.create")) {
                sender.sendMessage(" §f-§r §b/cvtrade create§r §a<name>");
                optionGiven = true;
            }
            if (sender.hasPermission("cvtrade.link")) {
                sender.sendMessage(" §f-§r §b/cvtrade link§r §a<name1> <name2>");
                optionGiven = true;
            }
            if (sender.hasPermission("cvtrade.unlink")) {
                sender.sendMessage(" §f-§r §b/cvtrade unlink§r §a<name>");
                optionGiven = true;
            }
            if (sender.hasPermission("cvtrade.delete")) {
                sender.sendMessage(" §f-§r §b/cvtrade delete§r §a<name>");
                optionGiven = true;
            }
            if (sender.hasPermission("cvtrade.list")) {
                sender.sendMessage(" §f-§r §b/cvtrade list");
                optionGiven = true;
            }
            if (sender.hasPermission("cvtrade.find")) {
                sender.sendMessage(" §f-§r §b/cvtrade find§r §a[radius]");
                optionGiven = true;
            }
            if (sender.hasPermission("cvtrade.info")) {
                sender.sendMessage(" §f-§r §b/cvtrade info§r §a<name>");
                optionGiven = true;
            }
            if (sender.hasPermission("cvtrade.start")) {
                sender.sendMessage(" §f-§r §b/cvtrade start§r §a<name>");
                optionGiven = true;
            }
            if (sender.hasPermission("cvtrade.cancel")) {
                sender.sendMessage(" §f-§r §b/cvtrade cancel");
                optionGiven = true;
            }
            if (sender.hasPermission("cvtrade.ready")) {
                sender.sendMessage(" §f-§r §b/cvtrade ready");
                optionGiven = true;
            }
            if (sender.hasPermission("cvtrade.complete")) {
                sender.sendMessage(" §f-§r §b/cvtrade accept");
                sender.sendMessage(" §f-§r §b/cvtrade reject");
                optionGiven = true;
            }
            
            if (!optionGiven) {
                sender.sendMessage("§cNo sub-commands available.");
            }
            return true;
        }
        
        final String permissionMessage = command.getPermissionMessage() == null ? "§cNo permission." : command.getPermissionMessage();
        final String subCommand = args.remove(0);
        if (subCommand.equalsIgnoreCase("create")) {
            if (!sender.hasPermission("cvtrade.create")) {
                sender.sendMessage(permissionMessage);
                return true;
            }
            
            if (args.isEmpty()) {
                sender.sendMessage("§cPlease specify the name of the TradeChest to create.");
                sender.sendMessage("§cSyntax:§r §b/cvtrade create§r §a<name>");
                return true;
            }
            
            final String name = args.remove(0).toLowerCase();
            if (!args.isEmpty()) {
                sender.sendMessage("§cToo many arguments specified for TradeChest creation.");
                sender.sendMessage("§cSyntax:§r §b/cvtrade create§r §a<name>");
                return true;
            }
            
            if (this.tradePlugin.isTrading(sender.getUniqueId())) {
                sender.sendMessage("§cYou are in an active trade. Please finish or cancel the trade before trying to create a new TradeChest.");
                return true;
            }
            
            if (this.tradePlugin.scheduleCreate(sender.getUniqueId(), name)) {
                sender.sendMessage("§6Please left-click the chest that you would like to use for this TradeChest.");
            } else {
                sender.sendMessage("§cThere is already a TradeChest with the name§r §6" + name + "§r§c.");
                sender.sendMessage("§cPlease use a different or more descriptive name to create the new TradeChest.");
            }
            return true;
        } else if (subCommand.equalsIgnoreCase("link")) {
            if (!sender.hasPermission("cvtrade.link")) {
                sender.sendMessage(permissionMessage);
                return true;
            }
            
            if (args.isEmpty()) {
                sender.sendMessage("§cPlease specify the names of the TradeChests to link.");
                sender.sendMessage("§cSyntax:§r §b/cvtrade link§r §a<name1> <name2>");
                return true;
            }
            final String name1 = args.remove(0).toLowerCase();
            if (args.isEmpty()) {
                sender.sendMessage("§cPlease specify the names of the TradeChests to link.");
                sender.sendMessage("§cSyntax:§r §b/cvtrade link§r §a<name1> <name2>");
                return true;
            }
            final String name2 = args.remove(0).toLowerCase();
            
            if (!args.isEmpty()) {
                sender.sendMessage("§cToo many arguments specified for TradeChest linking.");
                sender.sendMessage("§cSyntax:§r §b/cvtrade link§r §a<name1> <name2>");
                return true;
            }
            
            this.tradePlugin.linkChests(sender, name1, name2);
            return true;
        } else if (subCommand.equalsIgnoreCase("unlink")) {
            if (!sender.hasPermission("cvtrade.unlink")) {
                sender.sendMessage(permissionMessage);
                return true;
            }
            
            if (args.isEmpty()) {
                sender.sendMessage("§cPlease specify the name of the TradeChest you wish to unlink.");
                sender.sendMessage("§cSyntax:§r §b/cvtrade unlink§r §a<name>");
                return true;
            }
            final String name = args.remove(0).toLowerCase();
            if (!args.isEmpty()) {
                sender.sendMessage("§cToo many arguments specified for TradeChest unlinking.");
                sender.sendMessage("§cSyntax:§r §b/cvtrade unlink§r §a<name>");
                return true;
            }
            
            this.tradePlugin.unlinkChest(sender, name);
            return true;
        } else if (subCommand.equalsIgnoreCase("delete")) {
            if (!sender.hasPermission("cvtrade.delete")) {
                sender.sendMessage(permissionMessage);
                return true;
            }
            
            if (args.isEmpty()) {
                sender.sendMessage("§cPlease specify the name of the TradeChest you wish to delete.");
                sender.sendMessage("§cSyntax:§r §b/cvtrade delete§r §a<name>");
                return true;
            }
            final String name = args.remove(0).toLowerCase();
            if (!args.isEmpty()) {
                sender.sendMessage("§cToo many arguments specified for TradeChest deletion.");
                sender.sendMessage("§cSyntax:§r §b/cvtrade delete§r §a<name>");
                return true;
            }
            
            this.tradePlugin.unlinkChest(sender, name);
            this.tradePlugin.deleteChest(sender, name);
            return true;
        } else if (subCommand.equalsIgnoreCase("list")) {
            if (!sender.hasPermission("cvtrade.list")) {
                sender.sendMessage(permissionMessage);
                return true;
            }
            
            boolean small = false;
            if (!args.isEmpty()) {
                if (args.size() > 1) {
                    sender.sendMessage("§cToo many arguments specified for TradeChest listing.");
                    sender.sendMessage("§cSyntax:§r §b/cvtrade list§r §a[-s|--small]");
                    return true;
                }
                final String arg = args.remove(0);
                if (!arg.equals("-s") && (!arg.equals("--small"))) {
                    sender.sendMessage("§cToo many arguments specified for TradeChest listing.");
                    sender.sendMessage("§cSyntax:§r §b/cvtrade list§r §a[-s|--small]");
                    return true;
                }
                
                small = true;
            }
            
            this.tradePlugin.listChests(sender, small);
            return true;
        } else if (subCommand.equalsIgnoreCase("find")) {
            if (!sender.hasPermission("cvtrade.find")) {
                sender.sendMessage(permissionMessage);
                return true;
            }
            
            int radius = 10;
            if (!args.isEmpty()) {
                final String rawRadius = args.remove(0);
                if (!args.isEmpty()) {
                    sender.sendMessage("§cToo many arguments specified for finding a TradeChest.");
                    sender.sendMessage("§cSyntax:§r §b/cvtrade find§r §a[radius]");
                    return true;
                }
                
                try {
                    radius = Integer.parseInt(rawRadius);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§6" + rawRadius + "§r §cis not a valid search radius.");
                    sender.sendMessage("§cPlease specify a positive integer between 1 and 50 (inclusive) for the radius.");
                    return true;
                }
            }
            
            this.tradePlugin.findChest(sender, radius);
            return true;
        } else if (subCommand.equalsIgnoreCase("info")) {
            if (!sender.hasPermission("cvtrade.info")) {
                sender.sendMessage(permissionMessage);
                return true;
            }
            
            if (args.isEmpty()) {
                sender.sendMessage("§cPlease specify the name of the TradeChest you wish to view the details of.");
                sender.sendMessage("§cSyntax:§r §b/cvtrade info§r §a<name>");
                return true;
            }
            final String name = args.remove(0).toLowerCase();
            if (!args.isEmpty()) {
                sender.sendMessage("§cToo many arguments specified for TradeChest information.");
                sender.sendMessage("§cSyntax:§r §b/cvtrade info§r §a<name>");
                return true;
            }
            
            this.tradePlugin.infoChest(sender, name);
            return true;
        } else if (subCommand.equalsIgnoreCase("start")) {
            if (!sender.hasPermission("cvtrade.trade")) {
                sender.sendMessage(permissionMessage);
                return true;
            }
            
            if (args.isEmpty()) {
                sender.sendMessage("§cPlease specify the name of the TradeChest you wish to use for the trade.");
                sender.sendMessage("§cSyntax:§r §b/cvtrade start§r §a<name>");
                return true;
            }
            final String name = args.remove(0).toLowerCase();
            if (!args.isEmpty()) {
                sender.sendMessage("§cToo many argument specified for starting a trade.");
                sender.sendMessage("§cSyntax:§r §b/cvtrade start§r §a<name>");
                return true;
            }
            
            this.tradePlugin.startTrade(sender, name);
            return true;
        } else if (subCommand.equalsIgnoreCase("cancel")) {
            if (!sender.hasPermission("cvtrade.trade")) {
                sender.sendMessage(permissionMessage);
                return true;
            }
            
            this.tradePlugin.cancelTrade(sender, true);
            return true;
        } else if (subCommand.equalsIgnoreCase("ready")) {
            if (!sender.hasPermission("cvtrade.trade")) {
                sender.sendMessage(permissionMessage);
                return true;
            }
            
            this.tradePlugin.readyTrade(sender);
            return true;
        } else if (subCommand.equalsIgnoreCase("accept")) {
            if (!sender.hasPermission("cvtrade.trade")) {
                sender.sendMessage(permissionMessage);
                return true;
            }
            
            this.tradePlugin.acceptTrade(sender);
            return true;
        } else if (subCommand.equalsIgnoreCase("reject")) {
            if (!sender.hasPermission("cvtrade.trade")) {
                sender.sendMessage(permissionMessage);
                return true;
            }
            
            this.tradePlugin.cancelTrade(sender, false);
            return true;
        } else {
            return false;
        }
    }
    
    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull final CommandSender commandSender, @NotNull final Command command, @NotNull final String label, @NotNull final String[] rawArgs) {
        
        final ArrayList<String> completions = new ArrayList<String>();
        if (!(commandSender instanceof Player)) {
            return completions;
        }
        
        final Player sender = (Player) commandSender;
        final ArrayList<String> args = new ArrayList<String>(Arrays.asList(rawArgs));
        
        completions.add("create");
        completions.add("link");
        completions.add("unlink");
        completions.add("delete");
        completions.add("list");
        completions.add("find");
        completions.add("info");
        completions.add("start");
        completions.add("cancel");
        completions.add("ready");
        completions.add("accept");
        completions.add("reject");
        
        if (args.isEmpty()) {
            if (!sender.hasPermission("cvtrade.create")) {
                completions.remove("create");
            }
            if (!sender.hasPermission("cvtrade.link")) {
                completions.remove("link");
            }
            if (!sender.hasPermission("cvtrade.unlink")) {
                completions.remove("unlink");
            }
            if (!sender.hasPermission("cvtrade.delete")) {
                completions.remove("delete");
            }
            if (!sender.hasPermission("cvtrade.list")) {
                completions.remove("list");
            }
            if (!sender.hasPermission("cvtrade.find")) {
                completions.remove("find");
            }
            if (!sender.hasPermission("cvtrade.info")) {
                completions.remove("info");
            }
            if (!sender.hasPermission("cvtrade.trade")) {
                completions.remove("start");
            }
            if (!sender.hasPermission("cvtrade.trade")) {
                completions.remove("cancel");
            }
            if (!sender.hasPermission("cvtrade.trade")) {
                completions.remove("ready");
            }
            if (!sender.hasPermission("cvtrade.trade")) {
                completions.remove("accept");
                completions.remove("reject");
            }
            
            return completions;
        }
        
        final String subCommand = args.remove(0);
        if (args.isEmpty()) {
            completions.removeIf(completion -> !completion.toLowerCase().startsWith(subCommand.toLowerCase()));
            return completions;
        }
        
        completions.clear();
        if (subCommand.equalsIgnoreCase("create")) {
            
            return completions;
            
        } else if (subCommand.equalsIgnoreCase("link")) {
            
            if (!sender.hasPermission("cvtrade.link")) {
                return completions;
            }
            
            completions.addAll(this.tradePlugin.getNotLinked(null));
            if (args.isEmpty()) {
                return completions;
            }
            
            final String firstName = args.remove(0).toLowerCase();
            if (args.isEmpty()) {
                completions.removeIf(completion -> !completion.toLowerCase().startsWith(firstName.toLowerCase()));
                return completions;
            }
            
            completions.clear();
            completions.addAll(this.tradePlugin.getNotLinked(firstName));
            if (args.isEmpty()) {
                return completions;
            }
            
            final String secondName = args.remove(0);
            if (args.isEmpty()) {
                completions.removeIf(completion -> !completion.toLowerCase().startsWith(secondName.toLowerCase()));
                return completions;
            }
            
            completions.clear();
            return completions;
            
        } else if (subCommand.equalsIgnoreCase("unlink")) {
            
            if (!sender.hasPermission("cvtrade.unlink")) {
                return completions;
            }
            
            completions.addAll(this.tradePlugin.getLinked());
            if (args.isEmpty()) {
                return completions;
            }
    
            final String firstName = args.remove(0);
            if (args.isEmpty()) {
                completions.removeIf(completion -> !completion.toLowerCase().startsWith(firstName.toLowerCase()));
                return completions;
            }
    
            completions.clear();
            return completions;
            
        } else if (subCommand.equalsIgnoreCase("delete")) {
            
            if (!sender.hasPermission("cvtrade.delete")) {
                return completions;
            }
            
            completions.addAll(this.tradePlugin.getNotInActiveTrade(false));
            if (args.isEmpty()) {
                return completions;
            }
    
            final String firstName = args.remove(0);
            if (args.isEmpty()) {
                completions.removeIf(completion -> !completion.toLowerCase().startsWith(firstName.toLowerCase()));
                return completions;
            }
    
            completions.clear();
            return completions;
            
        } else if (subCommand.equalsIgnoreCase("list")) {
            
            return completions;
            
        } else if (subCommand.equalsIgnoreCase("find")) {
            
            return completions;
            
        } else if (subCommand.equalsIgnoreCase("info")) {
            
            if (!sender.hasPermission("cvtrade.info")) {
                return completions;
            }
            
            completions.addAll(this.tradePlugin.getAll());
            if (args.isEmpty()) {
                return completions;
            }
    
            final String firstName = args.remove(0);
            if (args.isEmpty()) {
                completions.removeIf(completion -> !completion.toLowerCase().startsWith(firstName.toLowerCase()));
                return completions;
            }
    
            completions.clear();
            return completions;
            
        } else if (subCommand.equalsIgnoreCase("start")) {
            
            if (!sender.hasPermission("cvtrade.trade")) {
                return completions;
            }
            
            completions.addAll(this.tradePlugin.getNotInActiveTrade(true));
            if (args.isEmpty()) {
                return completions;
            }
    
            final String firstName = args.remove(0);
            if (args.isEmpty()) {
                completions.removeIf(completion -> !completion.toLowerCase().startsWith(firstName.toLowerCase()));
                return completions;
            }
    
            completions.clear();
            return completions;
        
        } else if (subCommand.equalsIgnoreCase("cancel")) {
            
            return completions;
            
        } else if (subCommand.equalsIgnoreCase("ready")) {
            
            return completions;
            
        } else if (subCommand.equalsIgnoreCase("accept")) {
            
            return completions;
            
        } else if (subCommand.equalsIgnoreCase("reject")) {
            
            return completions;
            
        } else {
            
            return completions;
        }
    }
}
