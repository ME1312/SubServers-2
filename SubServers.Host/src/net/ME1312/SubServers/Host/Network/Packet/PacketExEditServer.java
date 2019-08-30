package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Host.Executable.SubLogger;
import net.ME1312.SubServers.Host.Executable.SubServerImpl;
import net.ME1312.SubServers.Host.ExHost;

import java.util.Arrays;

/**
 * Edit Server Packet
 */
public class PacketExEditServer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private ExHost host;
    private SubServerImpl server;
    private UpdateType type;
    private Object[] args;

    public enum UpdateType {
        // Status
        LAUNCH_EXCEPTION(1),
        STOPPED(2, Integer.class, Boolean.class);


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
     * New PacketExEditServer (In)
     * @param host ExHost
     */
    public PacketExEditServer(ExHost host) {
        this.host = host;
    }

    /**
     * New PacketExEditServer (Out)
     *
     * @param type Update Type
     * @param arguments Arguments
     */
    public PacketExEditServer(SubServerImpl server, UpdateType type, Object... arguments) {
        if (arguments.length != type.getArguments().length) throw new IllegalArgumentException(((arguments.length > type.getArguments().length)?"Too many":"Not enough") + " arguments for type: " + type.toString());
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
            SubServerImpl server = host.servers.get(data.getString(0x0000).toLowerCase());
            switch (data.getInt(0x0001)) {
                case 0:
                    server.setEnabled(data.getList(0x0002).get(0).asBoolean());
                    break;
                case 1:
                    server.start(data.getList(0x0002).get(0).asUUID());
                    break;
                case 2:
                    server.command(data.getList(0x0002).get(0).asRawString());
                    break;
                case 3:
                    server.stop();
                    break;
                case 4:
                    server.terminate();
                    break;
                case 5:
                    server.setLogging(data.getList(0x0002).get(0).asBoolean());
                    break;
                case 6:
                    Util.reflect(SubLogger.class.getDeclaredField("address"), server.getLogger(), data.getList(0x0002).get(0).asUUID());
                    break;
                case 7:
                    server.setStopCommand(data.getList(0x0002).get(0).asRawString());
                    break;
            }
        } catch (Exception e) {
            host.log.error.println(e);
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
