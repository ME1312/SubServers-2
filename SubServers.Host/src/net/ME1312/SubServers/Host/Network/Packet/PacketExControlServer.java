package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataSender;
import net.ME1312.SubServers.Host.ExHost;
import net.ME1312.SubServers.Host.Executable.SubLoggerImpl;
import net.ME1312.SubServers.Host.Executable.SubServerImpl;

import java.util.Arrays;

/**
 * Control Server Packet
 */
public class PacketExControlServer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private ExHost host;
    private SubServerImpl server;
    private Response type;
    private Object[] args;

    public enum Response {
        // Status
        LAUNCH_EXCEPTION(1),
        STOPPED(2, Integer.class, Boolean.class);


        private short value;
        private Class<?>[] args;
        Response(int value, Class<?>... args) {
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
     * @param host ExHost
     */
    public PacketExControlServer(ExHost host) {
        this.host = host;
    }

    /**
     * New PacketExControlServer (Out)
     *
     * @param type Update Type
     * @param arguments Arguments
     */
    public PacketExControlServer(SubServerImpl server, Response type, Object... arguments) {
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
    public ObjectMap<Integer> send(SubDataSender client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0000, server.getName());
        data.set(0x0001, type.getValue());
        data.set(0x0002, Arrays.asList(args));
        return data;
    }

    @Override
    public void receive(SubDataSender client, ObjectMap<Integer> data) {
        try {
            SubServerImpl server = host.servers.get(data.getRawString(0x0000).toLowerCase());
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
                    Util.reflect(SubLoggerImpl.class.getDeclaredField("address"), server.getLogger(), data.getList(0x0002).get(0).asUUID());
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
