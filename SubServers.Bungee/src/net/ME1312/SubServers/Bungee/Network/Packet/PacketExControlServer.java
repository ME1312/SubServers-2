package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Host.External.ExternalSubServer;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.SubServers.Bungee.SubProxy;

import java.util.Arrays;

/**
 * Control External Server Packet
 */
public class PacketExControlServer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private SubProxy plugin;
    private SubServer server;
    private Action type;
    private Object[] args;

    public enum Action {
        // Actions
        START(1, String.class),
        COMMAND(2, String.class),
        STOP(3),
        TERMINATE(4),

        // Data Manipulation
        SET_ENABLED(0, Boolean.class),
        SET_LOGGING(5, Boolean.class),
        SET_LOGGING_ADDRESS(6, String.class),
        SET_STOP_COMMAND(7, String.class);

        private short value;
        private Class<?>[] args;
        Action(int value, Class<?>... args) {
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
     * New PacketExControlServer (In)
     * @param plugin SubPlugin
     */
    public PacketExControlServer(SubProxy plugin) {
        this.plugin = plugin;
    }

    /**
     * New PacketExControlServer (Out)
     *
     * @param server SubServer
     * @param type Update Type
     * @param arguments Arguments
     */
    public PacketExControlServer(SubServer server, Action type, Object... arguments) {
        if (arguments.length < type.getArguments().length) throw new IllegalArgumentException("Not enough arguments for type: " + type);

        this.server = server;
        this.type = type;
        this.args = new Object[type.getArguments().length];

        for (int i = 0; i < type.getArguments().length; ++i) {
            if (!type.getArguments()[i].isInstance(arguments[i])) throw new IllegalArgumentException("Argument " + (i+1) + " is not " + type.getArguments()[i].getTypeName());
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

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        try {
            ExternalSubServer server = (ExternalSubServer) plugin.api.getSubServer(data.getString(0x0000));
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
}
