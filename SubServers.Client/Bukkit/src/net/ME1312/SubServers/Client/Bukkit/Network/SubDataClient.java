package net.ME1312.SubServers.Client.Bukkit.Network;

import net.ME1312.SubServers.Client.Bukkit.Library.Exception.IllegalPacketException;
import net.ME1312.SubServers.Client.Bukkit.Library.Util;
import net.ME1312.SubServers.Client.Bukkit.Library.Version.Version;
import net.ME1312.SubServers.Client.Bukkit.Network.Packet.*;
import net.ME1312.SubServers.Client.Bukkit.SubPlugin;
import org.bukkit.Bukkit;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * SubData Direct Client Class
 */
public final class SubDataClient {
    private static HashMap<Class<? extends PacketOut>, String> pOut = new HashMap<Class<? extends PacketOut>, String>();
    private static HashMap<String, List<PacketIn>> pIn = new HashMap<String, List<PacketIn>>();
    private static boolean defaults = false;
    private PrintWriter writer;
    private Socket socket;
    private String name;
    private SubPlugin plugin;

    /**
     * SubServers Client Instance
     *
     * @param plugin SubPlugin
     * @param address Bind Address
     * @param port Port
     * @throws IOException
     */
    public SubDataClient(SubPlugin plugin, String name, InetAddress address, int port) throws IOException {
        if (Util.isNull(plugin, name, address, port)) throw new NullPointerException();
        socket = new Socket(address, port);
        this.plugin = plugin;
        this.name = name;
        this.writer = new PrintWriter(socket.getOutputStream(), true);

        if (!defaults) loadDefaults();
        loop();

        Bukkit.getScheduler().runTaskLater(plugin, () -> sendPacket(new PacketAuthorization(plugin)), 10);
    }

    private void loadDefaults() {
        defaults = true;

        registerPacket(new PacketAuthorization(plugin), "Authorization");
        registerPacket(new PacketCommandServer(), "SubCommandServer");
        registerPacket(new PacketCreateServer(), "SubCreateServer");
        registerPacket(new PacketDownloadHostInfo(), "SubDownloadHostInfo");
        registerPacket(new PacketDownloadLang(plugin), "SubDownloadLang");
        registerPacket(new PacketDownloadPlayerList(), "SubDownloadPlayerList");
        registerPacket(new PacketDownloadServerInfo(), "SubDownloadServerInfo");
        registerPacket(new PacketDownloadServerList(), "SubDownloadServerList");
        registerPacket(new PacketInRunEvent(), "SubRunEvent");
        registerPacket(new PacketInShutdown(), "SubShutdown");
        registerPacket(new PacketLinkServer(plugin), "SubLinkServer");
        registerPacket(new PacketStartServer(), "SubStartServer");
        registerPacket(new PacketStopServer(), "SubStopServer");
        registerPacket(new PacketTeleportPlayer(), "SubTeleportPlayer");

        registerPacket(PacketAuthorization.class, "Authorization");
        registerPacket(PacketCommandServer.class, "SubCommandServer");
        registerPacket(PacketCreateServer.class, "SubCreateServer");
        registerPacket(PacketDownloadHostInfo.class, "SubDownloadHostInfo");
        registerPacket(PacketDownloadLang.class, "SubDownloadLang");
        registerPacket(PacketDownloadPlayerList.class, "SubDownloadPlayerList");
        registerPacket(PacketDownloadServerInfo.class, "SubDownloadServerInfo");
        registerPacket(PacketDownloadServerList.class, "SubDownloadServerList");
        registerPacket(PacketLinkServer.class, "SubLinkServer");
        registerPacket(PacketStartServer.class, "SubStartServer");
        registerPacket(PacketStopServer.class, "SubStopServer");
        registerPacket(PacketTeleportPlayer.class, "SubTeleportPlayer");
    }

    private void loop() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String input;
                while ((input = in.readLine()) != null) {
                    try {
                        JSONObject json = new JSONObject(input);
                        for (PacketIn packet : decodePacket(json)) {
                            try {
                                Bukkit.getScheduler().runTask(plugin, () -> packet.execute((json.keySet().contains("c"))?json.getJSONObject("c"):null));
                            } catch (Exception e) {
                                new InvocationTargetException(e, "Exception while executing PacketIn").printStackTrace();
                            }
                        }
                    } catch (IllegalPacketException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        new IllegalPacketException("Unknown Packet Format: " + input).printStackTrace();
                    }
                }
                try {
                    destroy(true);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            } catch (Exception e) {
                if (!(e instanceof SocketException)) e.printStackTrace();
                try {
                    destroy(true);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
    }

    /**
     * Gets the Assigned Server Name
     *
     * @return Server Name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the Server Socket
     *
     * @return Server Socket
     */
    public Socket getClient() {
        return socket;
    }

    /**
     * Register PacketIn to the Network
     *
     * @param packet PacketIn to register
     * @param handle Handle to Bind
     */
    public static void registerPacket(PacketIn packet, String handle) {
        if (Util.isNull(packet, handle)) throw new NullPointerException();
        List<PacketIn> list = (pIn.keySet().contains(handle))?pIn.get(handle):new ArrayList<PacketIn>();
        if (!list.contains(packet)) list.add(packet);
        pIn.put(handle, list);
    }

    /**
     * Unregister PacketIn from the Network
     *
     * @param packet PacketIn to unregister
     */
    public static void unregisterPacket(PacketIn packet) {
        if (Util.isNull(packet)) throw new NullPointerException();
        for (String handle : pIn.keySet()) if (pIn.get(handle).contains(packet)) pIn.get(handle).remove(packet);
    }

    /**
     * Register PacketOut to the Network
     *
     * @param packet PacketOut to register
     * @param handle Handle to bind
     */
    public static void registerPacket(Class<? extends PacketOut> packet, String handle) {
        if (Util.isNull(packet, handle)) throw new NullPointerException();
        pOut.put(packet, handle);
    }

    /**
     * Unregister PacketOut to the Network
     *
     * @param packet PacketOut to unregister
     */
    public static void unregisterPacket(Class<? extends PacketOut> packet) {
        if (Util.isNull(packet)) throw new NullPointerException();
        pOut.remove(packet);
    }

    /**
     * Grab PacketIn Instances via handle
     *
     * @param handle Handle
     * @return PacketIn
     */
    public static List<? extends PacketIn> getPacket(String handle) {
        if (Util.isNull(handle)) throw new NullPointerException();
        return new ArrayList<PacketIn>(pIn.get(handle));
    }

    /**
     * Send Packet to Client
     *
     * @param packet Packet to send
     */
    public void sendPacket(PacketOut packet) {
        if (Util.isNull(packet)) throw new NullPointerException();
        try {
            writer.println(encodePacket(packet));
        } catch (IllegalPacketException e) {
            e.printStackTrace();
        }
    }

    /**
     * JSON Encode PacketOut
     *
     * @param packet PacketOut
     * @return JSON Formatted Packet
     * @throws IllegalPacketException
     */
    private static JSONObject encodePacket(PacketOut packet) throws IllegalPacketException {
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
    private static List<PacketIn> decodePacket(JSONObject json) throws IllegalPacketException, InvocationTargetException {
        if (!json.keySet().contains("h") || !json.keySet().contains("v")) throw new IllegalPacketException("Unknown Packet Format: " + json.toString());
        if (!pIn.keySet().contains(json.getString("h"))) throw new IllegalPacketException("Unknown PacketIn Channel: " + json.getString("h"));

        List<PacketIn> list = new ArrayList<PacketIn>();
        for (PacketIn packet : pIn.get(json.getString("h"))) {
            if (new Version(json.getString("v")).equals(packet.getVersion())) {
                list.add(packet);
            } else {
                new IllegalPacketException("Packet Version Mismatch in " + json.getString("h") + ": " + json.getString("v") + " -> " + packet.getVersion().toString()).printStackTrace();
            }
        }

        return list;
    }

    /**
     * Drops All Connections and Stops the SubData Listener
     *
     * @throws IOException
     */
    public void destroy(boolean reconnect) throws IOException {
        if (Util.isNull(reconnect)) throw new NullPointerException();
        if (socket != null) {
            final Socket socket = this.socket;
            this.socket = null;
            if (!socket.isClosed()) socket.close();
            Bukkit.getLogger().info("SubServers > The SubData Connection was closed");
            if (reconnect) {
                Bukkit.getLogger().info("SubServers > Attempting to reconnect in 10 seconds");
                Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            plugin.subdata = new SubDataClient(plugin, name, socket.getInetAddress(), socket.getPort());
                        } catch (IOException e) {
                            Bukkit.getLogger().info("SubServers > Connection was unsuccessful, retrying in 10 seconds");
                            Bukkit.getScheduler().runTaskLater(plugin, this, 30 * 20);
                        }
                    }
                }, 30 * 20);
            }
            plugin.subdata = null;
        }
    }
}
