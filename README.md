![https://s30.postimg.org/fu575r281/Sub_Servers_Title.png](https://s30.postimg.org/fu575r281/Sub_Servers_Title.png)<br>
SubServers 2 is a rewrite of SubServers, the Server Management Plugin.<br>
[http://www.spigotmc.org/resources/subservers-2.11264/](http://www.spigotmc.org/resources/subservers-2.11264/)<br>
[http://www.spigotmc.org/resources/subservers-client-bukkit.15506/](http://www.spigotmc.org/resources/subservers-client-bukkit.15506/)

### Documentation
> [http://subservers.ME1312.net/Javadoc/SubServers.Bungee/](http://subservers.ME1312.net/Javadoc/SubServers.Bungee/)<br>
> [http://subservers.ME1312.net/Javadoc/SubServers.Client.Bukkit/](http://subservers.ME1312.net/Javadoc/SubServers.Client.Bukkit/)

## How to Install/Update SubServers.Bungee Pre-Releases
1. Download BungeeCord ([Link](https://www.spigotmc.org/link-forums/bungeecord.28/))
2. Download your favorite commit of SubServers.Bungee ([Click Here](https://github.com/ME1312/SubServers-2/tree/master/Artifacts) for the latest commit)
3. Put them both in a folder together. It should now look like this:<br>
![Example Folder](https://s30.postimg.org/qhcx95jep/Screen_Shot_2016_12_15_at_4_30_15_PM.png)
4. If you are updating, make sure to update the files in `~/SubServers`, they wont reset themselves.
5. You can now launch SubServers via your terminal: `java -jar SubServers.Bungee.jar`
6. All SubServers.Bungee commands can be accessed in console using `/sub help`

## How to Install/Update SubServers.Client Pre-Releases
1. Download your favorite commit of SubServers.Client ([Click Here](https://github.com/ME1312/SubServers-2/tree/master/Artifacts) for the latest commit)
2. Put SubServers.Client into your server's plugins 
3. If you are updating, make sure to update the files in `~/plugins/SubServers`, they wont reset themselves.
4. Start, then stop your server
5. Open config.yml
6. Change `Settings > SubData > Name` to whatever you named this server in BungeeCord/SubServers
7. Make sure SubData Client can connect to your SubData Server (using the ip and password in the config)
8. You can now startup your server
9. All SubServers.Client commands can be accessed in-game by using `/sub help`

## How to Install/Update SubServers.Console Pre-Releases
1. Download your favorite commit of SubServers.Console ([Click Here](https://github.com/ME1312/SubServers-2/tree/master/Artifacts) for the latest commit)
2. Put SubServers.Console into your proxy's plugins
3. Start your proxy
4. You can now access the commands `/popouts` and `/popoutc` for subservers and subcreator instances respectively
