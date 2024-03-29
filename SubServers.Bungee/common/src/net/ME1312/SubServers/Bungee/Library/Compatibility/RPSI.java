package net.ME1312.SubServers.Bungee.Library.Compatibility;

import net.md_5.bungee.api.chat.BaseComponent;

import java.util.UUID;
import java.util.function.IntConsumer;

/**
 * RemotePlayer Static Implementation Layout Class
 */
public abstract class RPSI {
    protected static RPSI instance;
    protected RPSI() {
        if (instance == null) instance = this;
    }

    /**
     * Sends messages to this player
     *
     * @param players Players to send to
     * @param messages Messages to send
     * @param response Success Status
     */
    protected abstract void sendMessage(UUID[] players, String[] messages, IntConsumer response);

    /**
     * Sends messages to this player
     *
     * @param players Players to send to
     * @param messages Messages to send
     * @param response Success Status
     */
    protected abstract void sendMessage(UUID[] players, BaseComponent[][] messages, IntConsumer response);

    /**
     * Transfers this player to another server
     *
     * @param players Players to send to
     * @param server Target server
     * @param response Success Status
     */
    protected abstract void transfer(UUID[] players, String server, IntConsumer response);

    /**
     * Disconnects this player from the network
     *
     * @param players Players to send to
     * @param reason Disconnect Reason
     * @param response Success status
     */
    protected abstract void disconnect(UUID[] players, String reason, IntConsumer response);
}
