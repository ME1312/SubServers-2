package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Host.External.ExternalSubServer;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubServers.Bungee.SubPlugin;

import java.util.Arrays;

/**
 * Update External Server Packet
 */
public class PacketExUpdateServer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private SubPlugin plugin;
    private SubServer server;
    private UpdateType type;
    private Object[] args;

    public enum UpdateType {
        // Actions
        START(1, String.class),
        COMMAND(2, String.class),
        STOP(3),
        TERMINATE(4),

        // Data Manipulation
        SET_ENABLED(0, Boolean.class),
        SET_LOGGING(5, Boolean.class),
        SET_STOP_COMMAND(6, String.class);

        private short value;
        private Class<?>[] args;
        UpdateType(int value, Class<?>... args) {
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
     * New PacketExUpdateServer (In)
     * @param plugin SubPlugin
     */
    public PacketExUpdateServer(SubPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * New PacketExUpdateServer (Out)
     *
     * @param server SubServer
     * @param type Update Type
     * @param arguments Arguments
     */
    public PacketExUpdateServer(SubServer server, UpdateType type, Object... arguments) {
        if (arguments.length != type.getArguments().length) throw new IllegalArgumentException("Not enough arguments for type: " + type.toString());
        int i = 0;
        while (i < arguments.length) {
            if (!type.getArguments()[i].isInstance(arguments[i])) throw new IllegalArgumentException("Argument " + (i+1) + " is not " + type.getArguments()[i].getCanonicalName());
            i++;
        }
        this.server = server;
        this.type = type;
        this.args = arguments;
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0000, server.getName());
        data.set(0x0001, type.getValue());
        data.set(0x0002, Arrays.asList(args));
        return data;
    }

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        try {
            ExternalSubServer server = (ExternalSubServer) plugin.api.getSubServer(data.getRawString(0x0000));
            switch (data.getInt(0x0001)) {
                case 1:
                    Util.reflect(ExternalSubServer.class.getDeclaredMethod("falsestart"), server);
                    break;
                case 2:
                    Util.reflect(ExternalSubServer.class.getDeclaredMethod("stopped", Boolean.class), server, data.getList(0x0002).get(1).asBoolean());
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
