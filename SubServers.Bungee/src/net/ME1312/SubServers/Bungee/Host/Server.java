package net.ME1312.SubServers.Bungee.Host;

import net.ME1312.Galaxi.Library.ExtraDataHandler;
import net.ME1312.SubData.Server.ClientHandler;
import net.ME1312.SubData.Server.DataClient;

import net.md_5.bungee.api.config.ServerInfo;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Server Interface
 */
public interface Server extends ServerInfo, ClientHandler, ExtraDataHandler<String> {

    /**
     * Link a SubData Client to this Object
     *
     * @param client Client to Link
     * @param channel Channel ID
     */
    void setSubData(DataClient client, int channel);

    /**
     * Get the Display Name of this Server
     *
     * @return Display Name
     */
    String getDisplayName();

    /**
     * Sets the Display Name for this Server
     *
     * @param value Value (or null to reset)
     */
    void setDisplayName(String value);

    /**
     * Get this Server's Groups
     *
     * @return Group names
     */
    List<String> getGroups();

    /**
     * Add this Server to a Group
     *
     * @param value Group name
     */
    void addGroup(String value);

    /**
     * Remove this Server from a Group
     *
     * @param value value Group name
     */
    void removeGroup(String value);

    /**
     * Commands the Server
     *
     * @param player Player who's Commanding
     * @param target Player who will Send
     * @param command Command to Send
     */
    boolean command(UUID player, UUID target, String command);

    /**
     * Commands the Server
     *
     * @param player Player who's Commanding
     * @param command Command to Send
     */
    default boolean command(UUID player, String command) {
        return command(player, null, command);
    }

    /**
     * Commands the Server
     *
     * @param command Command to Send
     */
    default boolean command(String command) {
        return command(null, command);
    }

    /**
     * Get players on this server across all known proxies
     *
     * @return Remote Player Collection
     */
    Collection<RemotePlayer> getRemotePlayers();

    /**
     * If the server is hidden from players
     *
     * @return Hidden Status
     */
    boolean isHidden();

    /**
     * Set if the server is hidden from players
     *
     * @param value Value
     */
    void setHidden(boolean value);

    /**
     * Gets the MOTD of the Server
     *
     * @return Server MOTD
     */
    String getMotd();

    /**
     * Sets the MOTD of the Server
     *
     * @param value Value
     */
    void setMotd(String value);

    /**
     * Gets if the Server is Restricted
     *
     * @return Restricted Status
     */
    boolean isRestricted();

    /**
     * Sets if the Server is Restricted
     *
     * @param value Value
     */
    void setRestricted(boolean value);

    /**
     * Get a copy of the current whitelist
     *
     * @return Player Whitelist
     */
    Collection<UUID> getWhitelist();

    /**
     * See if a player is whitelisted
     *
     * @param player Player to check
     * @return Whitelisted Status
     */
    boolean isWhitelisted(UUID player);

    /**
     * Add a player to the whitelist (for use with restricted servers)
     *
     * @param player Player to add
     */
    void whitelist(UUID player);

    /**
     * Remove a player to the whitelist
     *
     * @param player Player to remove
     */
    void unwhitelist(UUID player);

    /**
     * Makes it so the server object will still exist within the server manager even if it is disconnected
     */
    void persist();

    /**
     * Get the Signature of this Object
     *
     * @return Object Signature
     */
    String getSignature();
}
