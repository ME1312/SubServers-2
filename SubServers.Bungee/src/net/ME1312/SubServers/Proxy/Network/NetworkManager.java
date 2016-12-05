package net.ME1312.SubServers.Proxy.Network;

import net.ME1312.SubServers.Proxy.Libraries.Exception.IllegalPacketException;
import net.ME1312.SubServers.Proxy.Libraries.Version.Version;
import net.ME1312.SubServers.Proxy.Network.Packet.PacketAuthorization;
import net.ME1312.SubServers.Proxy.Network.Packet.PacketLinkServer;
import net.ME1312.SubServers.Proxy.Network.Packet.PacketRequestServerInfo;
import net.ME1312.SubServers.Proxy.Network.Packet.PacketRequestServers;
import net.ME1312.SubServers.Proxy.SubPlugin;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * NetworkManager Class
 *
 * @author ME1312
 */
public final class NetworkManager {
    private HashMap<Class<? extends PacketOut>, String> pOut = new HashMap<Class<? extends PacketOut>, String>();
    private HashMap<String, PacketIn> pIn = new HashMap<String, PacketIn>();
    private HashMap<SocketAddress, Client> clients = new HashMap<SocketAddress, Client>();
    private List<InetAddress> allowedAddresses = new ArrayList<InetAddress>();
    private ServerSocket server;
    private SubPlugin plugin;

    /**
     * SubServers Network Manager
     *
     * @param plugin SubPlugin
     * @param port Port
     * @param backlog Connection Queue
     * @param address Bind Address
     * @throws IOException
     */
    public NetworkManager(SubPlugin plugin, int port, int backlog, InetAddress address) throws IOException {
        server = new ServerSocket(port, backlog, address);
        this.plugin = plugin;

        allowConnection(address);
        loadDefaults();
    }

    private void loadDefaults() {
        for (String s : plugin.config.get().getSection("Settings").getSection("SubData").getStringList("Allowed-Connections")) {
            try {
                allowedAddresses.add(InetAddress.getByName(s));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        registerPacket(new PacketAuthorization(plugin), "Authorization");
        registerPacket(new PacketLinkServer(plugin), "LinkServer");
        registerPacket(new PacketRequestServerInfo(plugin), "RequestServerInfo");
        registerPacket(new PacketRequestServers(plugin), "RequestServers");

        registerPacket(PacketAuthorization.class, "Authorization");
        registerPacket(PacketLinkServer.class, "LinkServer");
        registerPacket(PacketRequestServerInfo.class, "RequestServerInfo");
        registerPacket(PacketRequestServers.class, "RequestServers");
    }

    /**
     * Gets the Server Socket
     *
     * @return Server Socket
     */
    public ServerSocket getServer() {
        return server;
    }

    /**
     * Add a Client to the Network
     *
     * @param socket Client to add
     * @throws IOException
     */
    public Client addClient(Socket socket) throws IOException {
        if (allowedAddresses.contains(socket.getInetAddress())) {
            Client client = new Client(plugin, socket);
            System.out.println("SubData > " + client.getAddress().toString() + " has connected");
            clients.put(client.getAddress(), client);
            return client;
        } else {
            System.out.println("SubData > " + socket.getRemoteSocketAddress().toString() + " attempted to connect, but isn't whitelisted");
            socket.close();
            return null;
        }
    }

    /**
     * Grabs a Client from the Network
     *
     * @param socket Socket to search
     * @return Client
     */
    public Client getClient(Socket socket) {
        return clients.get(socket.getRemoteSocketAddress());
    }

    /**
     * Grabs a Client from the Network
     *
     * @param address Address to search
     * @return Client
     */
    public Client getClient(SocketAddress address) {
        return clients.get(address);
    }

    /**
     * Remove a Client from the Network
     *
     * @param client Client to Kick
     * @throws IOException
     */
    public void removeClient(Client client) throws IOException {
        SocketAddress address = client.getAddress();
        if (clients.keySet().contains(address)) {
            clients.remove(address);
            client.disconnect();
            System.out.println("SubData > " + client.getAddress().toString() + " has disconnected");
        }
    }

    /**
     * Remove a Client from the Network
     *
     * @param address Address to Kick
     * @throws IOException
     */
    public void removeClient(SocketAddress address) throws IOException {
        Client client = clients.get(address);
        if (clients.keySet().contains(address)) {
            clients.remove(address);
            client.disconnect();
            System.out.println("SubData > " + client.getAddress().toString() + " has disconnected");
        }
    }

    /**
     * Register Packet to the Network
     *
     * @param packet PacketIn to register
     * @param handle Handle to Bind
     */
    public void registerPacket(PacketIn packet, String handle) {
        pIn.put(handle, packet);
    }

    /**
     * Register Packet to the Network
     *
     * @param packet PacketOut to register
     * @param handle Handle to bind
     */
    public void registerPacket(Class<? extends PacketOut> packet, String handle) {
        pOut.put(packet, handle);
    }

    /**
     * Broadcast a Packet to everything on the Network
     * <b>Warning:</b> There are usually different types of applications on the network at once, they may not recognise the same packet handles
     *
     * @param packet Packet to send
     */
    public void broadcastPacket(PacketOut packet) {
        for (Client client : clients.values()) {
            client.sendPacket(packet);
        }
    }

    /**
     * Allow Connections from an Address
     *
     * @param address Address to allow
     */
    public void allowConnection(InetAddress address) {
        if (!allowedAddresses.contains(address)) allowedAddresses.add(address);
    }

    /**
     * Deny Connections from an Address
     *
     * @param address Address to deny
     */
    public void denyConnection(InetAddress address) {
        allowedAddresses.remove(address);
    }

    /**
     * JSON Encode PacketOut
     *
     * @param packet PacketOut
     * @return JSON Formatted Packet
     * @throws IllegalPacketException
     */
    protected JSONObject encodePacket(PacketOut packet) throws IllegalPacketException {
        JSONObject json = new JSONObject();

        if (!pOut.keySet().contains(packet.getClass())) throw new IllegalPacketException("Unknown PacketOut Channel: " + packet.getClass().getCanonicalName());
        if (packet.getVersion().toString() == null) throw new NullPointerException("PacketOut Version cannot be null: " + packet.getClass().getCanonicalName());

        JSONObject contents = packet.generate();
        json.put("h", pOut.get(packet.getClass()));
        json.put("v", packet.getVersion().toString());
        if (contents != null) json.put("c", contents);
        return json;
    }

    /**
     * JSON Decode PacketIn
     *
     * @param json JSON to Decode
     * @return PacketIn
     * @throws IllegalPacketException
     * @throws InvocationTargetException
     */
    protected PacketIn decodePacket(JSONObject json) throws IllegalPacketException, InvocationTargetException {
        if (!json.keySet().contains("h") || !json.keySet().contains("v")) throw new IllegalPacketException("Unknown Packet Format: " + json.toString());
        if (!pIn.keySet().contains(json.getString("h"))) throw new IllegalPacketException("Unknown PacketIn Channel: " + json.getString("h"));

        PacketIn packet = pIn.get(json.getString("h"));
        if (!new Version(json.getString("v")).equals(packet.getVersion())) throw new IllegalPacketException("Packet Version Mismatch in " + json.getString("h") + ": " + json.getString("v") + "->" + packet.getVersion().toString());
        return packet;
    }

    /**
     * Drops All Connections and Stops the SubData Listener
     *
     * @throws IOException
     */
    public void destroy() throws IOException {
        while(clients.size() > 0) {
            removeClient((Client) clients.values().toArray()[0]);
        }
        server.close();
        System.out.println("SubServers > The SubData Listener has been closed");
        plugin.subdata = null;
    }
}
