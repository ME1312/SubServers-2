package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Host.Server;
import net.ME1312.SubServers.Bungee.SubProxy;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * External Server Edit Notification Packet
 */
public class PacketOutExEditServer implements PacketObjectOut<Integer> {
    private SubProxy plugin;
    private Server server;
    private Edit type;
    private Object[] args;

    public enum Edit {
        // Generic
        DISPLAY_NAME(0, String.class),
        MOTD(1, String.class),
        RESTRICTED(2, Boolean.class),
        HIDDEN(3, Boolean.class),

        // SubData
        CONNECTED(4, Integer.class, UUID.class),
        DISCONNECTED(5, Integer.class),

        // Whitelist
        WHITELIST_SET(6, List.class),
        WHITELIST_ADD(7, UUID.class),
        WHITELIST_REMOVE(8, UUID.class);

        private short value;
        private Class<?>[] args;
        Edit(int value, Class<?>... args) {
            this.value = (short) value;
            this.args = args;
        }

        public Class<?>[] getArguments() {
            return args;
        }

        public short getValue() {
            return value;
        }
    }

    /**
     * New PacketExEditServer (In)
     * @param plugin SubPlugin
     */
    public PacketOutExEditServer(SubProxy plugin) {
        this.plugin = plugin;
    }

    /**
     * New PacketExEditServer (Out)
     *
     * @param server SubServer
     * @param type Update Type
     * @param arguments Arguments
     */
    public PacketOutExEditServer(Server server, Edit type, Object... arguments) {
        if (arguments.length < type.getArguments().length) throw new IllegalArgumentException("Not enough arguments for type: " + type);

        this.server = server;
        this.type = type;
        this.args = new Object[type.getArguments().length];

        for (int i = 0; i < type.getArguments().length; ++i) {
            if (!type.getArguments()[i].isInstance(arguments[i])) throw new IllegalArgumentException("Argument " + (i+1) + " is not " + type.getArguments()[i].getCanonicalName());
            args[i] = arguments[i];
        }
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0000, server.getName());
        data.set(0x0001, type.getValue());
        data.set(0x0002, Arrays.asList(args));
        return data;
    }
}
