package net.ME1312.SubServers.Client.Bukkit.Library;

import net.ME1312.SubServers.Client.Common.Network.API.Server;
import net.ME1312.SubServers.Client.Common.Network.API.SubServer;

public enum SignState {
    UNKNOWN(0, "Signs.Text.Unknown"),
    OFFLINE(1, "Signs.Text.Offline"),
    STARTING(3, "Signs.Text.Starting"),
    ONLINE(4, "Signs.Text.Online"),
    STOPPING(2, "Signs.Text.Stopping"),
    ;
    public final byte priority;
    public final String text;

    SignState(int priority, String text) {
        this.priority = (byte) priority;
        this.text = text;
    }

    public static SignState determine(SubServer server) {
        if (!server.isRunning()) {
            return SignState.OFFLINE;
        } else if (server.isStopping()) {
            return SignState.STOPPING;
        } else if (server.isOnline()) {
            return SignState.ONLINE;
        } else {
            return SignState.STARTING;
        }
    }

    public static SignState determine(Server server) {
        if (server instanceof SubServer) {
            return determine((SubServer) server);
        } else if (server.getSubData()[0] == null) {
            return SignState.UNKNOWN;
        } else {
            return SignState.ONLINE;
        }
    }
}
