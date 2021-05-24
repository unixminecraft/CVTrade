# CVTrade

CVTrade is a Bukkit plugin to facilitate item trading on Minecraft servers in a way that is more secure than traditional trades.

## Dependencies

The only dependency is a Minecraft server that implements a supported version of the [Bukkit API](https://hub.spigotmc.org/javadocs/bukkit/index.html). Supported versions can be found in the [pom.xml](pom.xml) file.

## Installation

Simply place the .jar file into the `plugins/` folder on your Minecraft Bukkit server.

## Usage / Features

This plugin is intended to add some security when 2 players wish to trade items on a Minecraft server. It is not intended to be a fully-secure solution, but adds to the protection abilities that other plugins can provide, such as [WorldGuard](https://enginehub.org/worldguard/).

#### Usage

The general step-by-step for a player trade is as follows:<br/>
1) Two players will start a trade with `/cvtrade start <TradeChest name>`, each using 1 of a pair of TradeChests. They do not have to start at the same time.
2) The players will select which items they want to trade, and place them in their respective TradeChests.
3) When each player is ready, they will mark themselves as such with `/cvtrade ready`. A player can mark themselves as "ready" before the other player has started the trade.
4) When both players are "ready", an inventory will open showing the contents of the other TradeChest, and a bar at the bottom with options to reject/cancel the trade on the left, or accept the trade on the right.
  A) If a player closes the inventory before they make a choice, they can re-open the inventory with `/cvtrade view`. 
  B) The player may also use `/cvtrade reject` (or `/cvtrade cancel`) to reject the trade, or `/cvtrade accept` to accept the trade.
5) If either player rejects the trade, the items in each player's respective TradeChest will be automatically returned to the player's inventory (or dropped at their feet, if their inventory is full).
6) If both players accept the trade, the contents from the opposing TradeChest (the inventory that they viewed) will be placed in their inventory.

- Either player may cancel the trade at any time with `/cvtrade cancel`, which will return the items in the player's TradeChest (much like rejecting the trade).

#### Features

The main feature of this plugin is the ability for Player A to confirm that Player B will actually give A the items that B says they will before giving up their own items.<br/>
There are other features included in the plugin, including:<br/>
- Only allowing access to a TradeChest if the player has started a trade.
- Only allowing access when the player is in the "Prepare" status (deciding which items they wish to trade).
- When a player has marked themselves as "Ready", the TradeChest's inventory is saved in case of a server crash, or other unexpected event.
- If a player logs out at any point in the trade, they will have 5 minutes to log back in, or the trade will be automatically cancelled.
  - This includes if the player that logs out has accepted the trade and is waiting for the opposing player to make a decision.
  - If the trade is cancelled (or accepted) while a player is offline, the appropriate items will be placed in their inventory shortly after logging back in.
- If a player is offline (assuming the trade has been accepted or cancelled), and the server has crashed/rebooted, the items to be returned are saved, and should be returned shortly after the player next logs back into the server.

- Please note that TradeChest names are not case-sensitive, and will always be converted to lowercase.

## Commands

All the commands for the plugin, along with their respective permission nodes with a simple description.

#### Base Command

- `/cvtrade` - `cvtrade.cvtrade`: The base command for all CVTrade commands. If no arguments are specified, this will list all subcommands the player has permission to.

#### Regular Player Commands

- `/cvtrade start <TradeChest name>` - `cvtrade.trade`: Used to start a trade, specifying the TradeChest they wish to use. Names are not case-sensitive. They cannot access any TradeChest before using this command, and can only access the TradeChest specified.
- `/cvtrade ready` - `cvtrade.trade`: Used after a player has placed all the items they want to trade into the TradeChest. They will not be able to access the TradeChest after using this command.
- `/cvtrade view` - `cvtrade.trade`: Used to re-open the trade inventory that is presented to the player. This can only be used after both players have used the `/cvtrade ready` command.
- `/cvtrade accept` - `cvtrade.trade`: Used to accept the items that the opposing player has offered. Equivalent to clicking on the "Accept" option in the trade inventory that is presented to the player. This command can only be used after both players have used the `/cvtrade ready` command.
- `/cvtrade reject` - `cvtrade.trade`: Used to reject the items that the opposing player has offered. Equivalent to clicking on the "Reject" option in the trade inventory that is presented to the player. THis command can be used at any point during the trade.
- `/cvtrade cancel` - `cvtrade.trade`: Used to cancel the trade, at any point during the trade.

#### Administrator Commands

- `/cvtrade create <TradeChest Name>` - `cvtrade.create`: Creates a new TradeChest. Names are not case-sensitive. All TradeChest names must be unique.
- `/cvtrade delete <TradeChest Name>` - `cvtrade.delete`: Deletes a TradeChest, severing the link to its paired TradeChest, if one exists. If either the TradeChest or the linked TradeChest are being used in a trade, the command will not progress and will give an error message.
- `/cvtrade link <TradeChest 1 Name> <TradeChest 2 Name>` - `cvtrade.link`: Links 2 unlinked TradeChests. If either TradeChests are already linked to some other TradeChest, this command will give an error message.
- `/cvtrade unlink <TradeChest Name>` - `cvtrade.unlink`: Unlinks a TradeChest from its paired TradeChest, if any is linked. If either this TradeChest or the linked TradeChest are being used in a trade, the command will not progress and will give an error message.
- `/cvtrade list [-s|--small]` - `cvtrade.list`: Lists all TradeChests on the server.
- `/cvtrade find [radius (1-50)]` - `cvtrade.find`: Finds all TradeChests (if any) within the specified radius (blocks) of the player. If no radius is specified, 10 will be used.
- `/cvtrade info <TradeChest Name>` - `cvtrade.info`: Lists the detailed information about the TradeChest:
  - Location of the TradeChest
  - If the TradeChest is linked to another TradeChest or not
  - Name of the linked TradeChest (if applicable)
  - If the TradeChest is being used in a trade or not
  - The Name and UUID of the player actively trading (if applicable)
  - The status of the trade that the player is in (if applicable)
  - The Name and UUID of the player actively trading with the linked TradeChest (if applicable)
  - The status of the trade that the opposing player is in (if applicable)


## Support / Issues

Issues can be reported [here](https://github.com/unixminecraft/CVTrade/issues/).

## Licensing

This project is licensed under the GNU General Public License, version 3 (commonly referred to as GPLv3). A copy of the license can be obtained from [this repository](LICENSE) for from the [Free Software Foundation's site](http://www.gnu.org/licenses/gpl-3.0.en.html).