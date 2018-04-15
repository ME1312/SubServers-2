package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.SubServers.Host.Executable.SubServer;
import net.ME1312.SubServers.Host.Library.Config.YAMLSection;
import net.ME1312.SubServers.Host.Library.Version.Version;
import net.ME1312.SubServers.Host.Network.PacketIn;
import net.ME1312.SubServers.Host.Network.PacketOut;
import net.ME1312.SubServers.Host.ExHost;

import java.util.Arrays;
import java.util.UUID;

/**
 * Update Server Packet
 */
public class PacketExUpdateServer implements PacketIn, PacketOut {
    private ExHost host;
    private SubServer server;
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
     * New PacketExUpdateServer (In)
     * @param host SubPlugin
     */
    public PacketExUpdateServer(ExHost host) {
        this.host = host;
    }

    /**
     * New PacketExUpdateServer (Out)
     *
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
    public YAMLSection generate() {
        YAMLSection data = new YAMLSection();
        data.set("server", server.getName());
        data.set("type", type.getValue());
        data.set("args", Arrays.asList(args));
        return data;
    }

    @Override
    public void execute(YAMLSection data) {
        try {
            SubServer server = host.servers.get(data.getString("server").toLowerCase());
            switch (data.getInt("type")) {
                case 0:
                    server.setEnabled(data.getList("args").get(0).asBoolean());
                    break;
                case 1:
                    server.start(UUID.fromString(data.getList("args").get(0).asRawString()));
                    break;
                case 2:
                    server.command(data.getList("args").get(0).asRawString());
                    break;
                case 3:
                    server.stop();
                    break;
                case 4:
                    server.terminate();
                    break;
                case 5:
                    server.setLogging(data.getList("args").get(0).asBoolean());
                    break;
                case 6:
                    server.setStopCommand(data.getList("args").get(0).asRawString());
                    break;
            }
        } catch (Exception e) {
            host.log.error.println(e);
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}
