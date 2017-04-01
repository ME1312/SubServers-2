package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Host.External.ExternalSubServer;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.Client;
import net.ME1312.SubServers.Bungee.Network.PacketIn;
import net.ME1312.SubServers.Bungee.Network.PacketOut;
import net.ME1312.SubServers.Bungee.SubPlugin;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Update External Server Packet
 */
public class PacketExUpdateServer implements PacketIn, PacketOut {
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
    public JSONObject generate() {
        JSONObject json = new JSONObject();
        json.put("server", server.getName());
        json.put("type", type.getValue());
        json.put("args", Arrays.asList(args));
        return json;
    }

    @Override
    public void execute(Client client, JSONObject data) {
        try {
            ExternalSubServer server = (ExternalSubServer) plugin.api.getSubServer(data.getString("server"));
            switch (data.getInt("type")) {
                case 1:
                    Method falsestart = ExternalSubServer.class.getDeclaredMethod("falsestart");
                    falsestart.setAccessible(true);
                    falsestart.invoke(server);
                    falsestart.setAccessible(false);
                    break;
                case 2:
                    Method stopped = ExternalSubServer.class.getDeclaredMethod("stopped", Boolean.class);
                    stopped.setAccessible(true);
                    stopped.invoke(server, data.getJSONArray("args").getBoolean(1));
                    stopped.setAccessible(false);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}
