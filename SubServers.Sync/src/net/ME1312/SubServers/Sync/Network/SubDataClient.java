package net.ME1312.SubServers.Sync.Network;

import net.ME1312.SubServers.Sync.Event.SubNetworkDisconnectEvent;
import net.ME1312.SubServers.Sync.Library.Exception.IllegalPacketException;
import net.ME1312.SubServers.Sync.Library.NamedContainer;
import net.ME1312.SubServers.Sync.Library.Util;
import net.ME1312.SubServers.Sync.Library.Version.Version;
import net.ME1312.SubServers.Sync.Network.Packet.*;
import net.ME1312.SubServers.Sync.SubPlugin;
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
    private Encryption encryption;
    private SubPlugin plugin;
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
     * @param plugin SubPlugin
     * @param address Address
     * @param port Port
     * @param encryption Encryption Type
     * @throws IOException
     */
    public SubDataClient(SubPlugin plugin, InetAddress address, int port, Encryption encryption) throws IOException {
        if (Util.isNull(plugin, address, port)) throw new NullPointerException();
        socket = new Socket(address, port);
        this.plugin = plugin;
        this.writer = new PrintWriter(socket.getOutputStream(), true);
        this.encryption = encryption;
        this.queue = new LinkedList<NamedContainer<String, PacketOut>>();

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
        registerPacket(new PacketCommandServer(), "SubCommandServer");
        registerPacket(new PacketCreateServer(), "SubCreateServer");
        registerPacket(new PacketDownloadHostInfo(), "SubDownloadHostInfo");
        registerPacket(new PacketDownloadLang(plugin), "SubDownloadLang");
        registerPacket(new PacketDownloadNetworkList(), "SubDownloadNetworkList");
        registerPacket(new PacketDownloadPlayerList(), "SubDownloadPlayerList");
        registerPacket(new PacketDownloadServerInfo(), "SubDownloadServerInfo");
        registerPacket(new PacketDownloadServerList(), "SubDownloadServerList");
        registerPacket(new PacketInRunEvent(), "SubRunEvent");
        registerPacket(new PacketInReset(), "SubReset");
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
                    String decoded = null;
                    try {
                        switch (getEncryption()) {
                            case AES:
                            case AES_128:
                            case AES_192:
                            case AES_256:
                                decoded = AES.decrypt(plugin.config.get().getSection("Settings").getSection("SubData").getRawString("Password"), Base64.getDecoder().decode(input)).get();
                                break;
                            default:
                                decoded = input;
                        }
                        JSONObject json = new JSONObject(decoded);
                        for (PacketIn packet : decodePacket(json)) {
                            try {
                                packet.execute((json.keySet().contains("c")) ? json.getJSONObject("c") : null);
                            } catch (Throwable e) {
                                new InvocationTargetException(e, "Exception while executing PacketIn").printStackTrace();
                            }
                        }
                    } catch (JSONException e) {
                        new IllegalPacketException("Unknown Packet Format: " + ((decoded == null || decoded.length() <= 0) ? input : decoded)).printStackTrace();
                    } catch (IllegalPacketException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        new InvocationTargetException(e, "Exception while decoding packet").printStackTrace();
                    }
                }
                try {
                    destroy(plugin.config.get().getSection("Settings").getSection("SubData").getInt("Reconnect", 30));
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            } catch (Exception e) {
                if (!(e instanceof SocketException)) e.printStackTrace();
                try {
                    destroy(plugin.config.get().getSection("Settings").getSection("SubData").getInt("Reconnect", 30));
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }).start();
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
        if (!list.contains(packet)) {
            list.add(packet);
            pIn.put(handle, list);
        }
    }

    /**
     * Unregister PacketIn from the Network
     *
     * @param packet PacketIn to unregister
     */
    public static void unregisterPacket(PacketIn packet) {
        if (Util.isNull(packet)) throw new NullPointerException();
        List<String> search = new ArrayList<String>();
        search.addAll(pIn.keySet());
        for (String handle : search) if (pIn.get(handle).contains(packet)) {
            List<PacketIn> list = pIn.get(handle);
            list.remove(packet);
            if (list.isEmpty()) {
                pIn.remove(handle);
            } else {
                pIn.put(handle, list);
            }
        }
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
     * Send Packet to Server
     *
     * @param packet Packet to send
     */
    public void sendPacket(PacketOut packet) {
        if (Util.isNull(packet)) throw new NullPointerException();
        if (socket == null) {
            queue.add(new NamedContainer<>(null, packet));
        } else {
            try {
                switch (getEncryption()) {
                    case AES:
                    case AES_128:
                        writer.println(Base64.getEncoder().encodeToString(AES.encrypt(128, plugin.config.get().getSection("Settings").getSection("SubData").getRawString("Password"), encodePacket(packet).toString())));
                        break;
                    case AES_192:
                        writer.println(Base64.getEncoder().encodeToString(AES.encrypt(192, plugin.config.get().getSection("Settings").getSection("SubData").getRawString("Password"), encodePacket(packet).toString())));
                        break;
                    case AES_256:
                        writer.println(Base64.getEncoder().encodeToString(AES.encrypt(256, plugin.config.get().getSection("Settings").getSection("SubData").getRawString("Password"), encodePacket(packet).toString())));
                        break;
                    default:
                        writer.println(encodePacket(packet));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Forward Packet to Client
     *
     * @param packet Packet to send
     * @param location Where to send
     */
    public void forwardPacket(PacketOut packet, String location) {
        if (Util.isNull(packet, location)) throw new NullPointerException();
        if (socket == null) {
            queue.add(new NamedContainer<>(location, packet));
        } else {
            try {
                JSONObject json = encodePacket(packet);
                json.put("f", location);
                switch (getEncryption()) {
                    case AES:
                    case AES_128:
                        writer.println(Base64.getEncoder().encodeToString(AES.encrypt(128, plugin.config.get().getSection("Settings").getSection("SubData").getRawString("Password"), json.toString())));
                        break;
                    case AES_192:
                        writer.println(Base64.getEncoder().encodeToString(AES.encrypt(192, plugin.config.get().getSection("Settings").getSection("SubData").getRawString("Password"), json.toString())));
                        break;
                    case AES_256:
                        writer.println(Base64.getEncoder().encodeToString(AES.encrypt(256, plugin.config.get().getSection("Settings").getSection("SubData").getRawString("Password"), json.toString())));
                        break;
                    default:
                        writer.println(json.toString());
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
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
            return json;
        } catch (Throwable e) {
            throw new InvocationTargetException(e, "Exception while encoding packet");
        }
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
    public void destroy(int reconnect) throws IOException {
        if (Util.isNull(reconnect)) throw new NullPointerException();
        if (socket != null) {
            final Socket socket = this.socket;
            this.socket = null;
            if (!socket.isClosed()) socket.close();
            plugin.getPluginManager().callEvent(new SubNetworkDisconnectEvent());
            System.out.println("SubServers > The SubData Connection was closed");
            if (reconnect > 0) {
                System.out.println("SubServers > Attempting to reconnect in " + reconnect + " seconds");
                Timer timer = new Timer();
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            plugin.subdata = new SubDataClient(plugin, socket.getInetAddress(), socket.getPort(), encryption);
                            timer.cancel();
                            while (queue.size() != 0) {
                                if (queue.get(0).name() != null) {
                                    plugin.subdata.forwardPacket(queue.get(0).get(), queue.get(0).name());
                                } else {
                                    plugin.subdata.sendPacket(queue.get(0).get());
                                }
                                queue.remove(0);
                            }
                        } catch (IOException e) {
                            System.out.println("SubServers > Connection was unsuccessful, retrying in " + reconnect + " seconds");
                        }
                    }
                }, TimeUnit.SECONDS.toMillis(reconnect), TimeUnit.SECONDS.toMillis(reconnect));
            }
            plugin.subdata = null;
        }
    }
}
