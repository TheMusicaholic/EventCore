# EventCore

[![download](https://img.shields.io/github/downloads/DavidArchive/EventCore/total?style=for-the-badge)](https://github.com/DavidArchive/EventCore/releases/latest)
![license](https://img.shields.io/github/license/DavidArchive/EventCore?style=for-the-badge)
![stars](https://img.shields.io/github/stars/DavidArchive/EventCore?style=for-the-badge)
![forks](https://img.shields.io/github/forks/DavidArchive/EventCore?style=for-the-badge)

---

## Installation
1. Download jar from [here](https://github.com/DavidArchive/EventCore/releases/latest)
2. Put the jar in your plugins folder
3. Restart your server (not reload)

EventCore runs on [Paper](https://papermc.io/) and [Canvas](https://canvasmc.io/), from Minecraft 1.21.11 to 26.3: the
same jar works on all of them (servers for 26.1 and newer need Java 25). It needs WorldEdit (used by `/event drop`), on
Canvas a version with Folia support.

## Update
1. Download the new jar from [here](https://github.com/DavidArchive/EventCore/releases/latest)
2. Replace the old jar with the new one
3. Delete the old `config.yml` and restart your server

## Event world
The event takes place in one world, `Settings.EventWorld` in the `config.yml`, so it can run on the same server as
other worlds. EventCore only works in that world: the rest of the server isn't touched.

- Players join the event with `/event`, which teleports them to the spawn set with `/event setSpawn`, and leave it with
  `/event leave`, which sends them back to where they were. `/event kickall` sends everyone out of the event to the
  server spawn: the one set with `/setspawn` if EssentialsX is installed, otherwise the main world's spawn.
- Their own items, ender chest, game mode, health, hunger, experience and effects are put away when they enter the event
  world, in any way, and given back when they leave it, in any way. Nothing can be brought into the event or taken out
  of it. The saved state is kept with the player's data, so it survives restarts and crashes.
- Players who log out in the event world are back in the event when they log in again.
- In the event world, players can only use the event's commands (`/event`, `/event leave`, `/spawn`), so other plugins'
  commands (like `/home`, `/tpa` or `/ec`) can't get them around the event. Other commands can be allowed with
  `Settings.BlockCommands.AllowedCommands` in the `config.yml`. Staff (`event.command` or `event.commands.bypass`) can
  use all commands.
- `/event whitelist on` closes the event: only whitelisted players (`/event whitelist add <player>`) and staff can join
  it then. Players who are already in it stay.
- In the configured commands, `%world%` is replaced with the event world (like `minecraft:event`). Use
  `/execute in %world% run ...` to limit a command to the event world: a plain `/kill @e` kills the entities of every
  world on the server.

---

### Maven

```xml
<repository>
  <id>allaystudios</id>
  <url>https://repo.allay-studios.com/public</url>
</repository>

<dependency>
  <groupId>me.david</groupId>
  <artifactId>EventCore</artifactId>
  <version>VERSION</version>
</dependency>
```

### Gradle Kotlin

```kotlin
maven {
  name = "allaystudios"
  url = uri("https://repo.allay-studios.com/public")
}

implementation("me.david:EventCore:VERSION")
```

Example API Usage:

```java
EventCoreAPI api = EventCoreAPI.get();

GameManager gameManager = api.getGameManager();
gameManager.start();
gameManager.stop("Winner");

KitManager kitManager = api.getKitManager();
kitManager.enable("cool kit");
kitManager.give(Bukkit.getPlayer("JavaMio"));

MapManager mapManager = api.getMapManager();
mapManager.drop();
```

---

<details>
    <summary><h3 style="display: inline;">Commands</h3></summary>

| Command                        | Action                                                  |
|--------------------------------|:--------------------------------------------------------|
| `/event`                       | Join the event (teleports to the spawn)                 |
| `/event leave`                 | Leave the event (teleports back, own items back)        |
| `/event help`                  | Show all commands                                       |
| `/event start`                 | Start the event                                         |
| `/event stop <winner>`         | Stop the event                                          |
| `/event drop`                  | Drop with the commands defined in the config.yml        |
| `/event autoBorder <on / off>` | Toggle AutoBorder                                       |
| `/event setSpawn`              | Set the spawn location (in the event world)             |
| `/event whitelist <on / off>`  | Toggle the event whitelist                              |
| `/event whitelist list`        | Show the whitelisted players                            |
| `/event whitelist add <name>`  | Add a player to the event whitelist                     |
| `/event whitelist remove <name>` | Remove a player from the event whitelist              |
| `/event kickspec`              | Send all spectators out of the event                    |
| `/event kickall`               | Send all players out of the event to the server spawn (exclude `event.spec`) |
| `/event clearall`              | Clear the inventories of all players in the event       |
| `/kit <player>`                | Give a player the saved kit                             |
| `/kit *`                       | Give all players the saved kit                          |
| `/kit give <player/*> <name>`  | Give a player (or everyone with `*`) a specific kit     |
| `/kit enable <name>`           | Enable a kit                                            |
| `/kit save <name>`             | Saves your current inventory as kit                     |
| `/kit delete <name>`           | Delete a kit                                            |
| `/revive <player>`             | Revive a player                                         |
| `/revive *`                    | Revive all players who are not in gamemode 0            |
| `/announce <message>`          | Announce a message to the players in the event          |
| `/spawn`                       | Teleport to the spawn (in the event world, elsewhere another plugin's `/spawn` is used) |

</details>

---

<details>
    <summary><h3 style="display: inline;">Permissions</h3></summary>

| Permissions           |                                                                                        |
|-----------------------|:---------------------------------------------------------------------------------------|
| `event.bypass`        | Disables protect while not started (break blocks, place blocks, interact, hit players) |
| `event.command`       | Use the /event admin commands (and join the event while it's whitelisted)              |
| `event.spawn`         | Use /spawn                                                                             |
| `event.join`          | Use /event to join the event (everyone by default)                                     |
| `event.whitelist.bypass` | Join the event while it's whitelisted                                               |
| `event.commands.bypass` | Use all commands in the event world, not only the event's                            |

</details>

---

<details>
    <summary><h3 style="display: inline;">Placeholders</h3></summary>

| Placeholder          | Description                                       | Example |
|:---------------------|:--------------------------------------------------|:--------|
| `%eventcore_total%`  | Total players in the event world                  | 12      |
| `%eventcore_alive%`  | Players alive in the event (players in gamemode 0) | 4      |
| `%eventcore_kills%`  | Kills of the player                               | 6       |
| `%eventcore_deaths%` | Deaths of the player                              | 3       |
| `%eventcore_kd%`     | K/D of the player                                 | 2.00    |
| `%eventcore_totems%` | Totem count of the player                         | 8       |
| `%eventcore_border%` | Current border size of the world the player is on | 30      |
| `%eventcore_ping%`   | Ping of the player                                | 18ms    |
| `%eventcore_tps%`    | Server TPS (via [Spark](https://spark.lucko.me/)) | 20.00   |
