package net.ME1312.SubServers.Bungee.Host;

import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Bungee.Library.NamedContainer;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.SubAPI;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.json.JSONObject;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.*;

/**
 * SubServer Layout Class
 */
public abstract class SubServerContainer extends ServerContainer implements SubServer {
    private List<NamedContainer<String, String>> incompatibilities = new ArrayList<NamedContainer<String, String>>();

    /**
     * Creates a SubServer
     *
     * @param host Host
     * @param name Server Name
     * @param port Port Number
     * @param motd Server MOTD
     * @param restricted Players will need a permission to join if true
     * @throws InvalidServerException
     */
    public SubServerContainer(Host host, String name, int port, String motd, boolean hidden, boolean restricted) throws InvalidServerException {
        super(name, new InetSocketAddress(host.getAddress().getHostAddress(), port), motd, hidden, restricted);
    }

    @Override
    public boolean start() {
        return start(null);
    }

    @Override
    public boolean stop() {
        return stop(null);
    }

    @Override
    public boolean terminate() {
        return terminate(null);
    }

    @Override
    public boolean command(String command) {
        return command(null, command);
    }

    @Override
    public int edit(YAMLSection edit) {
        return edit(null, edit);
    }

    @Override
    public String getFullPath() {
        return new File(getHost().getPath(), getPath()).getPath();
    }

    @Override
    public void toggleCompatibility(SubServer... server) {
        for (SubServer s : server) {
            if (!equals(s)) {
                NamedContainer<String, String> info = new NamedContainer<String, String>(s.getHost().getName(), s.getName());
                if (isCompatible(s)) {
                    incompatibilities.add(info);
                    if (s.isCompatible(this)) toggleCompatibility(this);
                } else {
                    incompatibilities.remove(info);
                    if (!s.isCompatible(this)) toggleCompatibility(this);
                }
            }
        }
    }

    @Override
    public boolean isCompatible(SubServer server) {
        return !incompatibilities.contains(new NamedContainer<String, String>(server.getHost().getName(), server.getName()));
    }

    @Override
    public List<SubServer> getIncompatibilities() {
        List<SubServer> servers = new ArrayList<SubServer>();
        List<NamedContainer<String, String>> temp = new ArrayList<NamedContainer<String, String>>();
        temp.addAll(incompatibilities);
        for (NamedContainer<String, String> info : temp) {
            try {
                SubServer server = SubAPI.getInstance().getHost(info.name()).getSubServer(info.get());
                if (server == null) throw new NullPointerException();
                servers.add(server);
            } catch (Throwable e) {
                incompatibilities.remove(info);
            }
        }
        return servers;
    }

    @Override
    public List<SubServer> getCurrentIncompatibilities() {
        List<SubServer> servers = new ArrayList<SubServer>();
        for (SubServer server : getIncompatibilities()) {
            if (server.isRunning()) servers.add(server);
        }
        return servers;
    }

    @SuppressWarnings("deprecation")
    @Override
    public String toString() {
        JSONObject sinfo = new JSONObject(super.toString());
        sinfo.put("type", "SubServer");
        sinfo.put("host", getHost().getName());
        sinfo.put("enabled", isEnabled() && getHost().isEnabled());
        sinfo.put("editable", isEditable());
        sinfo.put("log", isLogging());
        sinfo.put("dir", getPath());
        sinfo.put("exec", getExecutable());
        sinfo.put("running", isRunning());
        sinfo.put("stop-cmd", getStopCommand());
        sinfo.put("auto-run", SubAPI.getInstance().getInternals().config.get().getSection("Servers").getSection(getName()).getKeys().contains("Run-On-Launch") && SubAPI.getInstance().getInternals().config.get().getSection("Servers").getSection(getName()).getBoolean("Run-On-Launch"));
        sinfo.put("auto-restart", willAutoRestart());
        List<String> incompatibleCurrent = new ArrayList<String>();
        List<String> incompatible = new ArrayList<String>();
        for (SubServer server : getCurrentIncompatibilities()) incompatibleCurrent.add(server.getName());
        for (SubServer server : getIncompatibilities()) incompatible.add(server.getName());
        sinfo.put("incompatible", incompatibleCurrent);
        sinfo.put("incompatible-list", incompatible);
        sinfo.put("temp", isTemporary());
        return sinfo.toString();
    }
}
