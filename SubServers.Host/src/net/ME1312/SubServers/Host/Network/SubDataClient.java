package net.ME1312.SubServers.Host.Network;

import net.ME1312.SubServers.Host.API.Event.SubNetworkDisconnectEvent;
import net.ME1312.SubServers.Host.Library.Exception.IllegalPacketException;
import net.ME1312.SubServers.Host.Library.Log.Logger;
import net.ME1312.SubServers.Host.Library.Util;
import net.ME1312.SubServers.Host.Library.Version.Version;
import net.ME1312.SubServers.Host.Network.Packet.*;
import net.ME1312.SubServers.Host.SubAPI;
import net.ME1312.SubServers.Host.ExHost;
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
    protected static Logger log;
    private PrintWriter writer;
    private Socket socket;
    private String name;
    private ExHost host;
    private LinkedList<PacketOut> queue;

    /**
     * SubServers Client Instance
     *
     * @param host SubServers.Host
     * @param address Address
     * @param port Port
     * @throws IOException
     */
    public SubDataClient(ExHost host, String name, InetAddress address, int port) throws IOException {
        if (Util.isNull(host, name, address, port)) throw new NullPointerException();
        socket = new Socket(address, port);
        this.host = host;
        this.name = name;
        this.writer = new PrintWriter(socket.getOutputStream(), true);
        this.queue = new LinkedList<PacketOut>();

        if (!defaults) loadDefaults();
        loop();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                sendPacket(new PacketAuthorization(host));
            }
        }, 500);
    }

    private void loadDefaults() {
        defaults = true;
        log = new Logger("SubData");

        registerPacket(new PacketAuthorization(host), "Authorization");
        registerPacket(new PacketCommandServer(), "SubCommandServer");
        registerPacket(new PacketCreateServer(), "SubCreateServer");
        registerPacket(new PacketDownloadBuildScript(), "SubDownloadBuildScript");
        registerPacket(new PacketDownloadHostInfo(), "SubDownloadHostInfo");
        registerPacket(new PacketDownloadLang(host), "SubDownloadLang");
        registerPacket(new PacketDownloadPlayerList(), "SubDownloadPlayerList");
        registerPacket(new PacketDownloadServerInfo(), "SubDownloadServerInfo");
        registerPacket(new PacketDownloadServerList(), "SubDownloadServerList");
        registerPacket(new PacketExAddServer(host), "SubExAddServer");
        registerPacket(new PacketExConfigureHost(host), "SubExConfigureHost");
        registerPacket(new PacketExCreateServer(host), "SubExCreateServer");
        registerPacket(new PacketExDeleteServer(host), "SubExDeleteServer");
        registerPacket(new PacketExRemoveServer(host), "SubExRemoveServer");
        registerPacket(new PacketExUpdateServer(host), "SubExUpdateServer");
        registerPacket(new PacketInReset(host), "SubReset");
        registerPacket(new PacketInRunEvent(), "SubRunEvent");
        registerPacket(new PacketLinkExHost(host), "SubLinkExHost");
        registerPacket(new PacketStartServer(), "SubStartServer");
        registerPacket(new PacketStopServer(), "SubStopServer");
        registerPacket(new PacketTeleportPlayer(), "SubTeleportPlayer");


        registerPacket(PacketAuthorization.class, "Authorization");
        registerPacket(PacketCommandServer.class, "SubCommandServer");
        registerPacket(PacketCreateServer.class, "SubCreateServer");
        registerPacket(PacketDownloadBuildScript.class, "SubDownloadBuildScript");
        registerPacket(PacketDownloadHostInfo.class, "SubDownloadHostInfo");
        registerPacket(PacketDownloadLang.class, "SubDownloadLang");
        registerPacket(PacketDownloadPlayerList.class, "SubDownloadPlayerList");
        registerPacket(PacketDownloadServerInfo.class, "SubDownloadServerInfo");
        registerPacket(PacketDownloadServerList.class, "SubDownloadServerList");
        registerPacket(PacketExAddServer.class, "SubExAddServer");
        registerPacket(PacketExConfigureHost.class, "SubExConfigureHost");
        registerPacket(PacketExCreateServer.class, "SubExCreateServer");
        registerPacket(PacketExDeleteServer.class, "SubExDeleteServer");
        registerPacket(PacketExRemoveServer.class, "SubExRemoveServer");
        registerPacket(PacketExUpdateServer.class, "SubExUpdateServer");
        registerPacket(PacketLinkExHost.class, "SubLinkExHost");
        registerPacket(PacketOutExLogMessage.class, "SubExLogMessage");
        registerPacket(PacketOutExRequestQueue.class, "SubExRequestQueue");
        registerPacket(PacketStartServer.class, "SubStartServer");
        registerPacket(PacketStopServer.class, "SubStopServer");
        registerPacket(PacketTeleportPlayer.class, "SubTeleportPlayer");
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
                                log.error.println(new InvocationTargetException(e, "Exception while executing PacketIn"));
                            }
                        }
                    } catch (IllegalPacketException e) {
                        log.error.println(e);
                    } catch (JSONException e) {
                        log.error.println(new IllegalPacketException("Unknown Packet Format: " + input));
                    }
                }
                try {
                    destroy(true);
                } catch (IOException e1) {
                    log.error.println(e1);
                }
            } catch (Exception e) {
                if (!(e instanceof SocketException)) log.error.println(e);
                try {
                    destroy(true);
                } catch (IOException e1) {
                    log.error.println(e1);
                }
            }
        }).start();
    }

    /**
     * Gets the Assigned Host Name
     *
     * @return Host Name
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
        if (socket == null) {
            queue.add(packet);
        } else {
            try {
                writer.println(encodePacket(packet));
            } catch (IllegalPacketException e) {
                log.error.println(e);
            }
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
                SubAPI.getInstance().getInternals().log.error.println(new IllegalPacketException("Packet Version Mismatch in " + json.getString("h") + ": " + json.getString("v") + " -> " + packet.getVersion().toString()));
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
            host.api.executeEvent(new SubNetworkDisconnectEvent());
            log.info.println("The SubData Connection was closed");
            if (reconnect) {
                log.info.println("Attempting to reconnect in 30 seconds");
                Timer timer = new Timer();
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            host.subdata = new SubDataClient(host, name, socket.getInetAddress(), socket.getPort());
                            timer.cancel();
                            while (queue.size() != 0) {
                                sendPacket(queue.get(0));
                                queue.remove(0);
                            }
                        } catch (IOException e) {
                            log.warn.println("Connection was unsuccessful, retrying in 30 seconds");
                        }
                    }
                }, TimeUnit.SECONDS.toMillis(30), TimeUnit.SECONDS.toMillis(30));
            }
            host.subdata = null;
        }
    }
}
