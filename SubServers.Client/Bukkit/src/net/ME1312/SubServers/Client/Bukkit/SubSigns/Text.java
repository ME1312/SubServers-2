package net.ME1312.SubServers.Client.Bukkit.SubSigns;

import net.ME1312.SubServers.Client.Common.Network.API.Server;
import net.ME1312.SubServers.Client.Common.Network.API.SubServer;

public enum Text {
    UNKNOWN(0, "Signs.Text.Unknown"),
    OFFLINE(1, "Signs.Text.Offline"),
    STARTING(3, "Signs.Text.Starting"),
    ONLINE(4, "Signs.Text.Online"),
    STOPPING(2, "Signs.Text.Stopping");

    private final byte priority;
    private final String text;

    Text(int priority, String text) {
        this.priority = (byte) priority;
        this.text = text;
    }

    static Text determine(SubServer server) {
        if (!server.isRunning()) {
            return Text.OFFLINE;
        } else if (server.isStopping()) {
            return Text.STOPPING;
        } else if (server.isOnline()) {
            return Text.ONLINE;
        } else {
            return Text.STARTING;
        }
    }

    public static Text determine(Server server) {
        if (server instanceof SubServer) {
            return determine((SubServer) server);
        } else if (server.getSubData()[0] == null) {
            return Text.UNKNOWN;
        } else {
            return Text.ONLINE;
        }
    }

    public byte getPriority() {
        return priority;
    }

    public String getText() {
        return text;
    }
}