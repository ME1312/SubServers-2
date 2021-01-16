package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.SubProxy;

import net.md_5.bungee.api.config.ListenerInfo;

import java.util.LinkedList;
import java.util.UUID;

/**
 * Download Proxy Info Packet
 */
public class PacketDownloadPlatformInfo implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private SubProxy plugin;
    private UUID tracker;

    /**
     * New PacketDownloadPlatformInfo (In)
     *
     * @param plugin SubPlugin
     */
    public PacketDownloadPlatformInfo(SubProxy plugin) {
        this.plugin = plugin;
    }

    /**
     * New PacketDownloadPlatformInfo (Out)
     *
     * @param plugin SubPlugin
     * @param tracker Receiver ID
     */
    public PacketDownloadPlatformInfo(SubProxy plugin, UUID tracker) {
        this.plugin = plugin;
        this.tracker = tracker;
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        if (tracker != null) data.set(0x0000, tracker);
        ObjectMap<String> info = new ObjectMap<String>();


        ObjectMap<String> subservers = new ObjectMap<String>();
        subservers.set("version", plugin.api.getWrapperVersion().toString());
        if (plugin.api.getWrapperBuild() != null) subservers.set("build", plugin.api.getWrapperBuild().toString());
        subservers.set("last-reload", plugin.resetDate);
        subservers.set("proxies", plugin.api.getProxies().size());
        subservers.set("hosts", plugin.api.getHosts().size());
        subservers.set("subservers", plugin.api.getSubServers().size());
        info.set("subservers", subservers);


        ObjectMap<String> bungee = new ObjectMap<String>();
        bungee.set("version", plugin.api.getProxyVersion().toString());
        bungee.set("disabled-cmds", plugin.getConfig().getDisabledCommands());
        bungee.set("player-limit", plugin.getConfig().getPlayerLimit());
        bungee.set("servers", plugin.api.getServers().size());
        LinkedList<ObjectMap<String>> listeners = new LinkedList<ObjectMap<String>>();
        for (ListenerInfo next : plugin.getConfig().getListeners()) {
            ObjectMap<String> listener = new ObjectMap<String>();
            listener.set("forced-hosts", next.getForcedHosts());
            listener.set("motd", next.getMotd());
            listener.set("priorities", next.getServerPriority());
            listener.set("player-limit", next.getMaxPlayers());
            listeners.add(listener);
        }
        bungee.set("listeners", listeners);
        info.set("bungee", bungee);


        ObjectMap<String> minecraft = new ObjectMap<String>();
        LinkedList<String> mcversions = new LinkedList<String>();
        for (Version version : plugin.api.getGameVersion()) mcversions.add(version.toString());
        minecraft.set("version", mcversions);
        minecraft.set("players", plugin.api.getRemotePlayers().size());
        info.set("minecraft", minecraft);


        ObjectMap<String> system = new ObjectMap<String>();
        ObjectMap<String> os = new ObjectMap<String>();
        os.set("name", System.getProperty("os.name"));
        os.set("version", System.getProperty("os.version"));
        system.set("os", os);
        ObjectMap<String> java = new ObjectMap<String>();
        java.set("version",  System.getProperty("java.version"));
        system.set("java", java);
        info.set("system", system);


        data.set(0x0001, info);
        return data;
    }

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        client.sendPacket(new PacketDownloadPlatformInfo(plugin, (data != null && data.contains(0x0000))?data.getUUID(0x0000):null));
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
