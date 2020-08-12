package net.ME1312.SubServers.Host.Network.API;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Host.Network.Packet.PacketCreateServer;
import net.ME1312.SubServers.Host.Network.Packet.PacketUpdateServer;
import net.ME1312.SubServers.Host.SubAPI;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Simplified SubCreator Data Class
 */
public class SubCreator {
    HashMap<String, ServerTemplate> templates = new HashMap<String, ServerTemplate>();
    Host host;
    ObjectMap<String> raw;

    SubCreator(Host host, ObjectMap<String> raw) {
        this.host = host;
        this.raw = raw;

        for (String template : raw.getMap("templates").getKeys()) {
            templates.put(template.toLowerCase(), new ServerTemplate(raw.getMap("templates").getMap(template)));
        }
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SubCreator && host.getSignature().equals(((SubCreator) obj).host.getSignature());
    }

    public static class ServerTemplate {
        private ObjectMap<String> raw;
        private ServerType type;

        public ServerTemplate(ObjectMap<String> raw) {
            this.raw = raw;
            this.type = Util.getDespiteException(() -> ServerType.valueOf(raw.getRawString("type").toUpperCase().replace('-', '_').replace(' ', '_')), ServerType.CUSTOM);
        }

        /**
         * Get the Name of this Template
         *
         * @return Template Name
         */
        public String getName() {
            return raw.getRawString("name");
        }

        /**
         * Get the Display Name of this Template
         *
         * @return Display Name
         */
        public String getDisplayName() {
            return raw.getRawString("display");
        }

        /**
         * Get the Enabled Status of this Template
         *
         * @return Enabled Status
         */
        public boolean isEnabled() {
            return raw.getBoolean("enabled");
        }

        /**
         * Get the Item Icon for this Template
         *
         * @return Item Icon Name/ID
         */
        public String getIcon() {
            return raw.getString("icon");
        }

        /**
         * Get the Type of this Template
         *
         * @return Template Type
         */
        public ServerType getType() {
            return type;
        }

        /**
         * Get whether this Template requires the Version argument
         *
         * @return Version Requirement
         */
        public boolean requiresVersion() {
            return raw.getBoolean("version-req");
        }

        /**
         * Get whether this Template can be used to update it's servers
         *
         * @return Updatable Status
         */
        public boolean canUpdate() {
            return raw.getBoolean("can-update");
        }
    }
    public enum ServerType {
        SPIGOT,
        VANILLA,
        FORGE,
        SPONGE,
        CUSTOM;

        @Override
        public String toString() {
            return super.toString().substring(0, 1).toUpperCase()+super.toString().substring(1).toLowerCase();
        }
    }

    /**
     * Create a SubServer
     *
     * @param player Player Creating
     * @param name Server Name
     * @param template Server Template
     * @param version Server Version (may be null)
     * @param port Server Port Number (null to auto-select)
     * @param response Response Code
     */
    public void create(UUID player, String name, ServerTemplate template, Version version, Integer port, Callback<Integer> response) {
        if (Util.isNull(response)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketCreateServer(player, name, host.getName(), template.getName(), version, port, data -> {
            try {
                response.run(data.getInt(0x0001));
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                ew.printStackTrace();
            }
        }));
    }

    /**
     * Create a SubServer
     *
     * @param name Server Name
     * @param template Server Template
     * @param version Server Version (may be null)
     * @param port Server Port Number (null to auto-select)
     * @param response Response Code
     */
    public void create(String name, ServerTemplate template, Version version, Integer port, Callback<Integer> response) {
        create(null, name, template, version, port, response);
    }

    /**
     * Create a SubServer
     *
     * @param player Player Creating
     * @param name Server Name
     * @param template Server Template
     * @param version Server Version (may be null)
     * @param port Server Port Number (null to auto-select)
     */
    public void create(UUID player, String name, ServerTemplate template, Version version, Integer port) {
        create(player, name, template, version, port, i -> {});
    }

    /**
     * Create a SubServer
     *
     * @param name Server Name
     * @param template Server Template
     * @param version Server Version (may be null)
     * @param port Server Port Number (null to auto-select)
     */
    public void create(String name, ServerTemplate template, Version version, Integer port) {
        create(name, template, version, port, i -> {});
    }

    /**
     * Update a SubServer
     *
     * @param player Player Updating
     * @param server Server to Update
     * @param version Server Version (may be null)
     * @param response Response Code
     */
    public void update(UUID player, String server, Version version, Callback<Integer> response) {
        if (Util.isNull(response)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketUpdateServer(player, server, version, data -> {
            try {
                response.run(data.getInt(0x0001));
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                ew.printStackTrace();
            }
        }));
    }

    /**
     * Update a SubServer
     *
     * @param server Server to Update
     * @param version Server Version (may be null)
     * @param response Response Code
     */
    public void update(String server, Version version, Callback<Integer> response) {
        update(null, server, version, response);
    }

    /**
     * Update a SubServer
     *
     * @param player Player Updating
     * @param server Server to Update
     * @param version Server Version (may be null)
     */
    public void update(UUID player, String server, Version version) {
        update(player, server, version, i -> {});
    }

    /**
     * Update a SubServer
     *
     * @param server Server to Update
     * @param version Server Version (may be null)
     */
    public void update(String server, Version version) {
        update(null, server, version);
    }

    /**
     * Gets the host this creator belongs to
     *
     * @return Host
     */
    public Host getHost() {
        return host;
    }

    /**
     * Gets the Templates that can be used in this SubCreator instance
     *
     * @return Template Map
     */
    public Map<String, ServerTemplate> getTemplates() {
        return new TreeMap<String, ServerTemplate>(templates);
    }

    /**
     * Gets a SubCreator Template by name
     *
     * @param name Template Name
     * @return Template
     */
    public ServerTemplate getTemplate(String name) {
        return getTemplates().get(name.toLowerCase());
    }
}
