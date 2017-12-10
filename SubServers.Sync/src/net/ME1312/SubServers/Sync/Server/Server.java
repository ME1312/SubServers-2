package net.ME1312.SubServers.Sync.Server;

import net.ME1312.SubServers.Sync.Library.Util;
import net.md_5.bungee.BungeeServerInfo;
import net.md_5.bungee.api.ChatColor;

import java.net.InetSocketAddress;

/**
 * Server Class
 */
public class Server extends BungeeServerInfo {
    private final String signature;
    private String nick = null;
    private String motd;
    private boolean restricted;
    private boolean hidden;

    public Server(String signature, String name, String display, InetSocketAddress address, String motd, boolean hidden, boolean restricted) {
        super(name, address, ChatColor.translateAlternateColorCodes('&', motd), restricted);
        if (Util.isNull(name, address, motd, hidden, restricted)) throw new NullPointerException();
        this.signature = signature;
        this.motd = motd;
        this.restricted = restricted;
        this.hidden = hidden;
        setDisplayName(display);
    }

    /**
     * Get the Display Name of this Server
     *
     * @return Display Name
     */
    public String getDisplayName() {
        return (nick == null)?getName():nick;
    }

    /**
     * Sets the Display Name for this Server
     *
     * @param value Value (or null to reset)
     */
    public void setDisplayName(String value) {
        if (value == null || value.length() == 0 || getName().equals(value)) {
            this.nick = null;
        } else {
            this.nick = value;
        }
    }

    /**
     * If the server is hidden from players
     *
     * @return Hidden Status
     */
    public boolean isHidden() {
        return hidden;
    }

    /**
     * Set if the server is hidden from players
     *
     * @param value Value
     */
    public void setHidden(boolean value) {
        if (Util.isNull(value)) throw new NullPointerException();
        this.hidden = value;
    }

    /**
     * Gets the MOTD of the Server
     *
     * @return Server MOTD
     */
    @Override
    public String getMotd() {
        return motd;
    }

    /**
     * Sets the MOTD of the Server
     *
     * @param value Value
     */
    public void setMotd(String value) {
        if (Util.isNull(value)) throw new NullPointerException();
        this.motd = value;
    }

    /**
     * Gets if the Server is Restricted
     *
     * @return Restricted Status
     */
    @Override
    public boolean isRestricted() {
        return restricted;
    }

    /**
     * Sets if the Server is Restricted
     *
     * @param value Value
     */
    public void setRestricted(boolean value) {
        if (Util.isNull(value)) throw new NullPointerException();
        this.restricted = value;
    }

    /**
     * Get the Signature of this Object
     *
     * @return Object Signature
     */
    public final String getSignature() {
        return signature;
    }
}
