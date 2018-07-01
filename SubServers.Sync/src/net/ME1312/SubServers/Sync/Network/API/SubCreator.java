package net.ME1312.SubServers.Sync.Network.API;

import net.ME1312.SubServers.Sync.Library.Callback;
import net.ME1312.SubServers.Sync.Library.Config.YAMLSection;
import net.ME1312.SubServers.Sync.Library.Util;
import net.ME1312.SubServers.Sync.Library.Version.Version;
import net.ME1312.SubServers.Sync.Network.Packet.PacketCreateServer;
import net.ME1312.SubServers.Sync.SubAPI;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class SubCreator {
    HashMap<String, ServerTemplate> templates = new HashMap<String, ServerTemplate>();
    Host host;
    YAMLSection raw;

    SubCreator(Host host, YAMLSection raw) {
        this.host = host;
        this.raw = raw;

        for (String template : raw.getSection("templates").getKeys()) {
            templates.put(template.toLowerCase(), new ServerTemplate(raw.getSection("templates").getSection(template)));
        }
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SubCreator && host.getSignature().equals(((SubCreator) obj).host.getSignature());
    }

    public static class ServerTemplate {
        private YAMLSection raw;
        private ServerType type;

        private ServerTemplate(YAMLSection raw) {
            this.raw = raw;
            this.type = (Util.isException(() -> ServerType.valueOf(raw.getRawString("type").toUpperCase())))? ServerType.valueOf(raw.getRawString("type").toUpperCase()): ServerType.CUSTOM;
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

        @Override
        public String toString() {
            YAMLSection tinfo = new YAMLSection();
            tinfo.set("enabled", isEnabled());
            tinfo.set("display", getDisplayName());
            tinfo.set("icon", getIcon());
            tinfo.set("type", getType().toString());
            return tinfo.toJSON();
        }
    }
    public enum ServerType {
        SPIGOT,
        VANILLA,
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
     * @param version Server Version
     * @param port Server Port Number
     * @param response Response Code
     */
    public void create(UUID player, String name, ServerTemplate template, Version version, int port, Callback<Integer> response) {
        if (Util.isNull(response)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        SubAPI.getInstance().getSubDataNetwork().sendPacket(new PacketCreateServer(player, name, host.getName(), template.getName(), version, port, data -> {
            try {
                response.run(data.getInt("r"));
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
     * @param version Server Version
     * @param port Server Port Number
     * @param response Response Code
     */
    public void create(String name, ServerTemplate template, Version version, int port, Callback<Integer> response) {
        create(null, name, template, version, port, response);
    }

    /**
     * Create a SubServer
     *
     * @param player Player Creating
     * @param name Server Name
     * @param template Server Template
     * @param version Server Version
     * @param port Server Port Number
     */
    public void create(UUID player, String name, ServerTemplate template, Version version, int port) {
        create(player, name, template, version, port, i -> {});
    }

    /**
     * Create a SubServer
     *
     * @param name Server Name
     * @param template Server Template
     * @param version Server Version
     * @param port Server Port Number
     */
    public void create(String name, ServerTemplate template, Version version, int port) {
        create(name, template, version, port, i -> {});
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
        if (Util.isNull(name)) throw new NullPointerException();
        return getTemplates().get(name.toLowerCase());
    }

    @Override
    @SuppressWarnings("unchecked")
    public String toString() {
        return raw.toJSON().toString();
    }
}
