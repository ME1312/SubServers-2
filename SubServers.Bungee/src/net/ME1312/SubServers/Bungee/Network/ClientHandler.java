package net.ME1312.SubServers.Bungee.Network;

/**
 * Client Handler Layout Class
 */
public interface ClientHandler {
    /**
     * Gets the SubData Client
     *
     * @return SubData Client (or null if not linked)
     */
    Client getSubData();

    /**
     * Link a SubData Client to this Object
     *
     * @see Client#setHandler(ClientHandler)
     * @param client Client to Link
     */
    void setSubData(Client client);
}
