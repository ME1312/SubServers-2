package net.ME1312.SubServers.Proxy.Network;

import net.ME1312.SubServers.Proxy.Libraries.Exception.IllegalPacketException;
import net.ME1312.SubServers.Proxy.Network.Packet.PacketAuthorization;
import net.ME1312.SubServers.Proxy.SubPlugin;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Network Client Class
 *
 * @author ME1312
 */
public class Client {
    private Socket socket;
    private SocketAddress address;
    private ClientHandler handler;
    private List<PacketOut> queue = new ArrayList<PacketOut>();
    private Timer authorized;
    private SubPlugin plugin;
    private Client instance;

    /**
     * Network Client
     *
     * @param plugin SubPlugin
     * @param client Socket to Bind
     */
    public Client(SubPlugin plugin, Socket client) {
        this.plugin = plugin;
        socket = client;
        address = client.getRemoteSocketAddress();
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
        new Thread() {
            public void run() {
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String input;
                    while ((input = in.readLine()) != null) {
                        try {
                            JSONObject json = new JSONObject(input);
                            PacketIn packet = plugin.subdata.decodePacket(json);
                            if (authorized == null || packet instanceof PacketAuthorization) {
                                try {
                                    packet.execute(instance, (json.keySet().contains("c")) ? json.getJSONObject("c") : null);
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
                        plugin.subdata.removeClient(instance);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                } catch (Exception e) {
                    if (e.getMessage() == null || !e.getMessage().equals("Socket closed")) e.printStackTrace();
                    try {
                        plugin.subdata.removeClient(instance);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }.start();
        new Thread() {
            public void run() {
                try {
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    while (!socket.isClosed()) {
                        while (queue.size() > 0) {
                            try {
                                out.println(plugin.subdata.encodePacket(queue.get(0)));
                                queue.remove(0);
                            } catch (IllegalPacketException e) {
                                e.printStackTrace();
                            }
                        }
                        sleep(100);
                    }
                    try {
                        plugin.subdata.removeClient(instance);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                } catch (Exception e) {
                    if (e.getMessage() == null || !e.getMessage().equals("Socket closed")) e.printStackTrace();
                    try {
                        plugin.subdata.removeClient(instance);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }.start();
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
        queue.add(packet);
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
    public SocketAddress getAddress() {
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
     * Sets the Handler
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
