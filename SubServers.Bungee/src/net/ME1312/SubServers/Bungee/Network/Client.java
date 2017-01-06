package net.ME1312.SubServers.Bungee.Network;

import net.ME1312.SubServers.Bungee.Library.Exception.IllegalPacketException;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketAuthorization;
import net.ME1312.SubServers.Bungee.SubPlugin;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Network Client Class
 *
 * @author ME1312
 */
public final class Client {
    private Socket socket;
    private InetSocketAddress address;
    private ClientHandler handler;
    private PrintWriter writer;
    private Timer authorized;
    private SubPlugin plugin;
    private Client instance;

    /**
     * Network Client
     *
     * @param plugin SubPlugin
     * @param client Socket to Bind
     */
    public Client(SubPlugin plugin, Socket client) throws IOException {
        this.plugin = plugin;
        socket = client;
        writer = new PrintWriter(client.getOutputStream(), true);
        address = new InetSocketAddress(client.getInetAddress(), client.getPort());
        instance = this;
        authorized = new Timer("auth" + client.getRemoteSocketAddress().toString());
        authorized.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!socket.isClosed()) try {
                    plugin.subdata.removeClient(instance);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 15000);
        loop();
    }

    /**
     * Network Loop
     */
    protected void loop() {
        new Thread(() -> {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String input;
                while ((input = in.readLine()) != null) {
                    try {
                        JSONObject json = new JSONObject(input);
                        for (PacketIn packet : SubDataServer.decodePacket(json)) {
                            if (authorized == null || packet instanceof PacketAuthorization) {
                                try {
                                    packet.execute(instance, (json.keySet().contains("c")) ? json.getJSONObject("c") : null);
                                } catch (Exception e) {
                                    new InvocationTargetException(e, "Exception while executing PacketIn").printStackTrace();
                                }
                            }
                        }
                    } catch (IllegalPacketException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        new IllegalPacketException("Unknown Packet Format: " + input).printStackTrace();
                    }
                }
                try {
                    plugin.subdata.removeClient(instance);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            } catch (Exception e) {
                if (!(e instanceof SocketException)) e.printStackTrace();
                try {
                    plugin.subdata.removeClient(instance);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Authorize Connection
     */
    public void authorize() {
        if (authorized != null) {
            authorized.cancel();
            System.out.println("SubData > " + socket.getRemoteSocketAddress().toString() + " logged in");
        }
        authorized = null;
    }

    /**
     * Send Packet to Client
     *
     * @param packet Packet to send
     */
    public void sendPacket(PacketOut packet) {
        try {
            writer.println(SubDataServer.encodePacket(packet));
        } catch (IllegalPacketException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get Raw Connection
     *
     * @return Socket
     */
    public Socket getConnection() {
        return socket;
    }

    /**
     * Get Remote Address
     *
     * @return Address
     */
    public InetSocketAddress getAddress() {
        return address;
    }

    /**
     * If the connection is authorized
     *
     * @return Authorization Status
     */
    public boolean isAuthorized() {
        return authorized == null;
    }

    /**
     * Gets the Linked Handler
     *
     * @return Handler
     */
    public ClientHandler getHandler() {
        return handler;
    }

    /**
     * Sets the Handler<br>
     * <b>Warning:</b> This method should only be called by ClientHandler methods
     *
     * @see ClientHandler
     * @param obj Handler
     */
    public void setHandler(ClientHandler obj) {
        handler = obj;
    }

    /**
     * Disconnects the Client
     *
     * @throws IOException
     */
    protected void disconnect() throws IOException {
        if (!socket.isClosed()) getConnection().close();
        if (handler != null) handler.linkSubDataClient(null);
        handler = null;

    }
}
