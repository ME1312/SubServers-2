# SubServers 2 [BETA]
SubServers 2 is a rewrite of SubServers, the Server Management Plugin.

## Notable Improvements (Over SubServers v1)
* Hosts (and Host Driver API)
* SubData Direct (and API)
* Names arn't case sensitive
* The Proxy hosts the Servers (instead of the Servers hosting the Proxy)
* Just about everything your players will see either looks like BungeeCord or is Customizable

## How to Install/Update SubServers.Bungee
1. Download BungeeCord ([Link](https://www.spigotmc.org/link-forums/bungeecord.28/))
2. Download your favorite commit of SubServers.Bungee ([Click Here](https://github.com/ME1312/SubServers-2/tree/master/Artifacts) for the latest commit)
3. Put them both in a folder together. It should now look like this:
![Example Folder](https://s30.postimg.org/qhcx95jep/Screen_Shot_2016_12_15_at_4_30_15_PM.png)
4. If you are updating, make sure to update the files in `~/SubServers`, they wont reset themselves.
5. You can now launch SubServers via your terminal: `java -jar SubServers.Bungee.jar`
6. All SubServers.Bungee commands can be accessed in console using `sub help`

## How to Install/Update SubServers.Client
1. Download your favorite commit of SubServers.Client ([Click Here](https://github.com/ME1312/SubServers-2/tree/master/Artifacts) for the latest commit)
2. Put SubServers.Client into your server's plugins 
3. If you are updating, make sure to update the files in `~/plugins/SubServers`, they wont reset themselves.
4. Start, then stop your server
5. Open config.yml
6. Change `Settings > SubData > Name` to whatever you named this server in BungeeCord/SubServers
7. Make sure SubData Client can connect to your SubData Server (using the ip and password in the config)
8. You can now startup your server
9. All SubServers.Client commands can be accessed in-game by using `sub help`
