package net.ME1312.SubServers.Host;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Plugin.PluginInfo;
import net.ME1312.SubData.Client.DataClient;
import net.ME1312.SubData.Client.DataProtocol;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Client.Common.ClientAPI;

import java.util.*;

/**
 * SubAPI Class
 */
public final class SubAPI extends ClientAPI {
    private final ExHost host;
    private static SubAPI api;
    String name;

    SubAPI(ExHost host) {
        this.host = host;
        api = this;
    }

    /**
     * Gets the SubAPI Methods
     *
     * @return SubAPI
     */
    public static SubAPI getInstance() {
        return api;
    }

    /**
     * Gets the SubServers Internals
     *
     * @deprecated Use SubAPI Methods when available
     * @return ExHost Internals
     */
    @Deprecated
    public ExHost getInternals() {
        return host;
    }

    /**
     * Get the Host Name
     *
     * @return Host Name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the SubData Network Connections
     *
     * @return SubData Network Connections
     */
    public DataClient[] getSubDataNetwork() {
        LinkedList<Integer> keys = new LinkedList<Integer>(host.subdata.keySet());
        LinkedList<SubDataClient> channels = new LinkedList<SubDataClient>();
        Collections.sort(keys);
        for (Integer channel : keys) channels.add(host.subdata.get(channel));
        return channels.toArray(new DataClient[0]);
    }

    /**
     * Gets the SubData Network Protocol
     *
     * @return SubData Network Protocol
     */
    public DataProtocol getSubDataProtocol() {
        return host.subprotocol;
    }

    /**
     * Gets the current SubServers Lang Channels
     *
     * @return SubServers Lang Channel list
     */
    public Collection<String> getLangChannels() {
        return host.lang.get().keySet();
    }

    /**
     * Gets values from the SubServers Lang
     *
     * @param channel Lang Channel
     * @return Lang Value
     */
    public Map<String, String> getLang(String channel) {
        if (Util.isNull(channel)) throw new NullPointerException();
        return new LinkedHashMap<>(host.lang.get().get(channel.toLowerCase()));
    }

    /**
     * Gets the SubServers App Info
     *
     * @return SubServers App Info
     */
    public PluginInfo getAppInfo() {
        return host.info;
    }
}
