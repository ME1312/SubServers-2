package net.ME1312.SubServers.Bungee.Host;

import net.ME1312.Galaxi.Library.Container.ContainedPair;
import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Bungee.SubAPI;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * SubServer Layout Class
 */
public abstract class SubServerImpl extends ServerImpl implements SubServer {
    protected static final Pattern DISALLOWED_COMMANDS = Pattern.compile("^/?(?:[^\\s]+:)?sub(?:servers?)?(?:\\s|$)", Pattern.CASE_INSENSITIVE & Pattern.UNICODE_CASE);
    private List<Pair<String, String>> incompatibilities = new ArrayList<Pair<String, String>>();
    private SubCreator.ServerTemplate templateV = null;
    private String templateS = null;
    protected boolean registered, started, stopping, updating;

    /**
     * Creates a SubServer
     *
     * @param host Host
     * @param name Server Name
     * @param port Port Number
     * @param motd Server MOTD
     * @param hidden Hidden Status
     * @param restricted Restricted Status
     *
     * @see ServerImpl#ServerImpl(String, SocketAddress, String, boolean, boolean) Super Method 2
     * @throws InvalidServerException
     */
    protected SubServerImpl(Host host, String name, int port, String motd, boolean hidden, boolean restricted) throws InvalidServerException {
        super(name, (SocketAddress) new InetSocketAddress(host.getAddress().getHostAddress(), port), motd, hidden, restricted);
    }

    /**
     * Creates a SubServer
     *
     * @param host Host
     * @param name Server Name
     * @param port Port Number
     * @param motd Server MOTD
     * @param hidden Hidden Status
     * @param restricted Restricted Status
     *
     * @see ServerImpl#ServerImpl(String, InetSocketAddress, String, boolean, boolean) Super Method 1
     * @throws InvalidServerException
     */
    protected SubServerImpl(Host host, String name, Integer port, String motd, boolean hidden, boolean restricted) throws InvalidServerException {
        super(name, new InetSocketAddress(host.getAddress().getHostAddress(), port), motd, hidden, restricted);
    }

    public int edit(UUID player, ObjectMap<String> edit) {
        return edit(player, edit, false);
    }

    public int permaEdit(UUID player, ObjectMap<String> edit) {
        return edit(player, edit, true);
    }

    /**
     * Edits the Server
     *
     * @param player Player Editing
     * @param edit Edits
     * @param perma Saves Changes
     * @return Success Status
     */
    protected int edit(UUID player, ObjectMap<String> edit, boolean perma) {
        return -1;
    }

    @Override
    protected final boolean isRegistered() {
        return registered;
    }

    @Override
    public boolean isAvailable() {
        return registered && !updating && getHost().isAvailable();
    }

    @Override
    public boolean isOnline() {
        return isRunning() && started;
    }

    @Override
    public boolean isStopping() {
        return isRunning() && stopping;
    }

    @Override
    public void setTemplate(String template) {
        this.templateV = null;
        this.templateS = template;
    }

    @Override
    public void setTemplate(SubCreator.ServerTemplate template) {
        this.templateV = template;
        this.templateS = (template != null)?template.getName():null;
    }

    @Override
    public SubCreator.ServerTemplate getTemplate() {
        if (templateV != null) {
            return templateV;
        } else if (templateS != null && getHost().getCreator().getTemplates().containsKey(templateS.toLowerCase())) {
            return getHost().getCreator().getTemplate(templateS.toLowerCase());
        } else {
            return null;
        }
    }

    @Override
    public void toggleCompatibility(SubServer... server) {
        for (SubServer s : server) {
            if (!equals(s)) {
                Pair<String, String> info = new ContainedPair<String, String>(s.getHost().getName(), s.getName());
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
        return !incompatibilities.contains(new ContainedPair<String, String>(server.getHost().getName(), server.getName()));
    }

    @Override
    public List<SubServer> getIncompatibilities() {
        List<SubServer> servers = new ArrayList<SubServer>();
        List<Pair<String, String>> temp = new ArrayList<Pair<String, String>>();
        temp.addAll(incompatibilities);
        for (Pair<String, String> info : temp) {
            try {
                SubServer server = SubAPI.getInstance().getHost(info.key()).getSubServer(info.value());
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

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SubServerImpl && super.equals(obj);
    }

    @SuppressWarnings("deprecation")
    @Override
    public ObjectMap<String> forSubData() {
        ObjectMap<String> sinfo = super.forSubData();
        sinfo.set("type", "SubServer");
        sinfo.set("host", getHost().getName());
        sinfo.set("template", (getTemplate() != null)?getTemplate().getName():null);
        sinfo.set("available", isAvailable());
        sinfo.set("enabled", isEnabled());
        sinfo.set("editable", isEditable());
        sinfo.set("log", isLogging());
        sinfo.set("dir", getPath());
        sinfo.set("exec", getExecutable());
        sinfo.set("running", isRunning());
        sinfo.set("online", isOnline());
        sinfo.set("stopping", isStopping());
        sinfo.set("stop-cmd", getStopCommand());
        sinfo.set("stop-action", getStopAction().toString());
        sinfo.set("auto-run", SubAPI.getInstance().getInternals().servers.get().getMap("Servers").getMap(getName(), new ObjectMap<String>()).getBoolean("Run-On-Launch", false));
        List<String> incompatibleCurrent = new ArrayList<String>();
        List<String> incompatible = new ArrayList<String>();
        for (SubServer server : getCurrentIncompatibilities()) incompatibleCurrent.add(server.getName());
        for (SubServer server : getIncompatibilities()) incompatible.add(server.getName());
        sinfo.set("incompatible", incompatibleCurrent);
        sinfo.set("incompatible-list", incompatible);
        return sinfo;
    }
}
