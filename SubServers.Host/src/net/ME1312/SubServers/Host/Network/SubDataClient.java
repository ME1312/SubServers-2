package net.ME1312.SubServers.Host.Network;

import net.ME1312.SubServers.Host.Library.Container;
import net.ME1312.SubServers.Host.Library.Exception.IllegalPacketException;
import net.ME1312.SubServers.Host.Library.Util;
import net.ME1312.SubServers.Host.Library.Version.Version;
import net.ME1312.SubServers.Host.Network.Packet.PacketAuthorization;
import net.ME1312.SubServers.Host.Network.Packet.PacketDownloadLang;
import net.ME1312.SubServers.Host.SubAPI;
import net.ME1312.SubServers.Host.SubServers;
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
import java.util.*;
import java.util.concurrent.TimeUnit;

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
    private SubServers plugin;

    /**
     * SubServers Client Instance
     *
     * @param plugin SubPlugin
     * @param address Bind Address
     * @param port Port
     * @throws IOException
     */
    public SubDataClient(SubServers plugin, String name, InetAddress address, int port) throws IOException {
        if (Util.isNull(plugin, name, address, port)) throw new NullPointerException();
        socket = new Socket(address, port);
        this.plugin = plugin;
        this.name = name;
        this.writer = new PrintWriter(socket.getOutputStream(), true);

        if (!defaults) loadDefaults();
        loop();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                sendPacket(new PacketAuthorization(plugin));
            }
        }, 500);
    }

    private void loadDefaults() {
        defaults = true;

        registerPacket(new PacketAuthorization(plugin), "Authorization");
        registerPacket(new PacketDownloadLang(plugin), "SubDownloadLang");

        registerPacket(PacketAuthorization.class, "Authorization");
        registerPacket(PacketDownloadLang.class, "SubDownloadLang");
    }

    private void loop() {
        new Thread(() -> {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String input;
                while ((input = in.readLine()) != null) {
                    try {
                        JSONObject json = new JSONObject(input);
                        for (PacketIn packet : decodePacket(json)) {
                            try {
                                packet.execute((json.keySet().contains("c"))?json.getJSONObject("c"):null);
                            } catch (Exception e) {
                                plugin.log.error(new InvocationTargetException(e, "Exception while executing PacketIn"));
                            }
                        }
                    } catch (IllegalPacketException e) {
                        plugin.log.error(e);
                    } catch (JSONException e) {
                        plugin.log.error(new IllegalPacketException("Unknown Packet Format: " + input));
                    }
                }
                try {
                    destroy(true);
                } catch (IOException e1) {
                    plugin.log.error(e1);
                }
            } catch (Exception e) {
                if (!(e instanceof SocketException)) plugin.log.error(e);
                try {
                    destroy(true);
                } catch (IOException e1) {
                    plugin.log.error(e1);
                }
            }
        }).start();
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
            plugin.log.error(e);
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
    @SuppressWarnings("deprecation")
    private static List<PacketIn> decodePacket(JSONObject json) throws IllegalPacketException, InvocationTargetException {
        if (!json.keySet().contains("h") || !json.keySet().contains("v")) throw new IllegalPacketException("Unknown Packet Format: " + json.toString());
        if (!pIn.keySet().contains(json.getString("h"))) throw new IllegalPacketException("Unknown PacketIn Channel: " + json.getString("h"));

        List<PacketIn> list = new ArrayList<PacketIn>();
        for (PacketIn packet : pIn.get(json.getString("h"))) {
            if (new Version(json.getString("v")).equals(packet.getVersion())) {
                list.add(packet);
            } else {
                SubAPI.getInstance().getInternals().log.error(new IllegalPacketException("Packet Version Mismatch in " + json.getString("h") + ": " + json.getString("v") + " -> " + packet.getVersion().toString()));
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
            plugin.log.info("The SubData Connection was closed");
            if (reconnect) {
                plugin.log.info("Attempting to reconnect in 30 seconds");
                final Container<Timer> timer = new Container<Timer>(new Timer());
                timer.get().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            plugin.subdata = new SubDataClient(plugin, name, socket.getInetAddress(), socket.getPort());
                        } catch (IOException e) {
                            plugin.log.info("Connection was unsuccessful, retrying in 30 seconds");
                            timer.set(new Timer());
                            timer.get().schedule(this, TimeUnit.SECONDS.toMillis(30));
                        }
                    }
                }, TimeUnit.SECONDS.toMillis(30));
            }
            plugin.subdata = null;
        }
    }
}
