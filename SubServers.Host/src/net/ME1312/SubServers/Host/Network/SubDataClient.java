package net.ME1312.SubServers.Host.Network;

import net.ME1312.SubServers.Host.API.Event.SubNetworkConnectEvent;
import net.ME1312.SubServers.Host.API.Event.SubNetworkDisconnectEvent;
import net.ME1312.SubServers.Host.Library.Exception.IllegalPacketException;
import net.ME1312.SubServers.Host.Library.Log.Logger;
import net.ME1312.SubServers.Host.Library.NamedContainer;
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
import java.net.InetSocketAddress;
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
    private NamedContainer<Boolean, Socket> socket;
    private String name;
    private Encryption encryption;
    private ExHost host;
    private LinkedList<NamedContainer<String, PacketOut>> queue;

    public enum Encryption {
        NONE,
        AES,
        AES_128,
        AES_192,
        AES_256
    }

    /**
     * SubServers Client Instance
     *
     * @param host SubServers.Host
     * @param name Name of Host
     * @param address Address
     * @param port Port
     * @param encryption Encryption Type
     * @throws IOException
     */
    public SubDataClient(ExHost host, String name, InetAddress address, int port, Encryption encryption) throws IOException {
        if (Util.isNull(host, name, address, port, encryption)) throw new NullPointerException();
        socket = new NamedContainer<>(false, new Socket(address, port));
        this.host = host;
        this.name = name;
        this.writer = new PrintWriter(socket.get().getOutputStream(), true);
        this.encryption = encryption;
        this.queue = new LinkedList<NamedContainer<String, PacketOut>>();

        if (!defaults) loadDefaults();
        loop();

        sendPacket(new NamedContainer<>(null, new PacketAuthorization(host)));
    }

    private void init() {
        sendPacket(new PacketExConfigureHost(host));
        sendPacket(new PacketDownloadLang());
        sendPacket(new PacketOutExRequestQueue());
        while (queue.size() != 0) {
            sendPacket(queue.get(0));
            queue.remove(0);
        }
        socket.rename(true);
        host.api.executeEvent(new SubNetworkConnectEvent(host.subdata));
    }

    private void loadDefaults() {
        defaults = true;
        log = new Logger("SubData");

        registerPacket(new PacketAuthorization(host), "Authorization");
        registerPacket(new PacketCommandServer(), "SubCommandServer");
        registerPacket(new PacketCreateServer(), "SubCreateServer");
        registerPacket(new PacketDownloadHostInfo(), "SubDownloadHostInfo");
        registerPacket(new PacketDownloadLang(host), "SubDownloadLang");
        registerPacket(new PacketDownloadNetworkList(), "SubDownloadNetworkList");
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
        registerPacket(PacketDownloadHostInfo.class, "SubDownloadHostInfo");
        registerPacket(PacketDownloadLang.class, "SubDownloadLang");
        registerPacket(PacketDownloadNetworkList.class, "SubDownloadNetworkList");
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
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.get().getInputStream()));
                String input;
                while ((input = in.readLine()) != null) {
                    String decoded = null;
                    try {
                        switch (getEncryption()) {
                            case AES:
                            case AES_128:
                            case AES_192:
                            case AES_256:
                                decoded = AES.decrypt(host.config.get().getSection("Settings").getSection("SubData").getRawString("Password"), Base64.getDecoder().decode(input)).get();
                                break;
                            default:
                                decoded = input;
                        }
                        JSONObject json = new JSONObject(decoded);
                        for (PacketIn packet : decodePacket(json)) {
                            try {
                                packet.execute((json.keySet().contains("c"))?json.getJSONObject("c"):null);
                            } catch (Throwable e) {
                                log.error.println(new InvocationTargetException(e, "Exception while executing PacketIn"));
                            }
                        }
                    } catch (JSONException e) {
                        log.error.println(new IllegalPacketException("Unknown Packet Format: " + ((decoded == null || decoded.length() <= 0)?input:decoded)));
                    } catch (IllegalPacketException e) {
                        log.error.println(e);
                    } catch (Exception e) {
                        log.error.println(new InvocationTargetException(e, "Exception while decoding packet"));
                    }
                }
                try {
                    destroy(host.config.get().getSection("Settings").getSection("SubData").getInt("Reconnect", 30));
                } catch (IOException e1) {
                    log.error.println(e1);
                }
            } catch (Exception e) {
                if (!(e instanceof SocketException)) log.error.println(e);
                try {
                    destroy(host.config.get().getSection("Settings").getSection("SubData").getInt("Reconnect", 30));
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
        return socket.get();
    }

    /**
     * Gets the Connection's Encryption method
     *
     * @return Encryption method
     */
    public Encryption getEncryption() {
        return encryption;
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
        if (socket.get() == null || !socket.name()) {
            queue.add(new NamedContainer<>(null, packet));
        } else {
            sendPacket(new NamedContainer<>(null, packet));
        }
    }

    private void sendPacket(NamedContainer<String, PacketOut> packet) {
        try {
            JSONObject json = encodePacket(packet.get());
            if (packet.name() != null) json.put("f", packet.name());
            switch (getEncryption()) {
                case AES:
                case AES_128:
                    writer.println(Base64.getEncoder().encodeToString(AES.encrypt(128, host.config.get().getSection("Settings").getSection("SubData").getRawString("Password"), json.toString())));
                    break;
                case AES_192:
                    writer.println(Base64.getEncoder().encodeToString(AES.encrypt(192, host.config.get().getSection("Settings").getSection("SubData").getRawString("Password"), json.toString())));
                    break;
                case AES_256:
                    writer.println(Base64.getEncoder().encodeToString(AES.encrypt(256, host.config.get().getSection("Settings").getSection("SubData").getRawString("Password"), json.toString())));
                    break;
                default:
                    writer.println(json.toString());
            }
        } catch (Throwable e) {
            log.error.println(e);
        }
    }
    /**
     * Forward Packet to Server
     *
     * @param packet Packet to send
     * @param location Where to send
     */
    public void forwardPacket(PacketOut packet, String location) {
        if (Util.isNull(packet, location)) throw new NullPointerException();
        if (socket == null || !socket.name()) {
            queue.add(new NamedContainer<>(location, packet));
        } else {
            sendPacket(new NamedContainer<>(location, packet));
        }
    }

    /**
     * Broadcast packet to all Clients
     *
     * @param packet Packet to send
     */
    public void broadcastPacket(PacketOut packet) {
        forwardPacket(packet, "");
    }

    /**
     * JSON Encode PacketOut
     *
     * @param packet PacketOut
     * @return JSON Formatted Packet
     * @throws IllegalPacketException
     */
    private static JSONObject encodePacket(PacketOut packet) throws IllegalPacketException, InvocationTargetException {
        JSONObject json = new JSONObject();

        if (!pOut.keySet().contains(packet.getClass())) throw new IllegalPacketException("Unknown PacketOut Channel: " + packet.getClass().getCanonicalName());
        if (packet.getVersion().toString() == null) throw new NullPointerException("PacketOut Version cannot be null: " + packet.getClass().getCanonicalName());

        try {
            JSONObject contents = packet.generate();
            json.put("h", pOut.get(packet.getClass()));
            json.put("v", packet.getVersion().toString());
            if (contents != null) json.put("c", contents);
        } catch (Throwable e) {
            throw new InvocationTargetException(e, "Exception while encoding packet");
        }
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
    public void destroy(int reconnect) throws IOException {
        if (Util.isNull(reconnect)) throw new NullPointerException();
        if (socket.get() != null) {
            final Socket socket = this.socket.get();
            this.socket.set(null);
            if (!socket.isClosed()) socket.close();
            host.api.executeEvent(new SubNetworkDisconnectEvent());
            log.info.println("The SubData Connection was closed");
            if (reconnect > 0) {
                log.info.println("Attempting to reconnect in " + reconnect + " seconds");
                Timer timer = new Timer();
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            host.subdata = new SubDataClient(host, name, socket.getInetAddress(), socket.getPort(), encryption);
                            timer.cancel();
                            while (queue.size() != 0) {
                                if (queue.get(0).name() != null) {
                                    host.subdata.forwardPacket(queue.get(0).get(), queue.get(0).name());
                                } else {
                                    host.subdata.sendPacket(queue.get(0).get());
                                }
                                queue.remove(0);
                            }
                        } catch (IOException e) {
                            log.warn.println("Connection was unsuccessful, retrying in " + reconnect + " seconds");
                        }
                    }
                }, TimeUnit.SECONDS.toMillis(reconnect), TimeUnit.SECONDS.toMillis(reconnect));
            }
            host.subdata = null;
        }
    }
}
