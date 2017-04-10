package net.ME1312.SubServers.Bungee.Network;

import net.ME1312.SubServers.Bungee.Library.Exception.IllegalPacketException;
import net.ME1312.SubServers.Bungee.Library.Util;
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
 */
public class Client {
    private Socket socket;
    private InetSocketAddress address;
    private ClientHandler handler;
    private PrintWriter writer;
    private Timer authorized;
    protected SubDataServer subdata;

    /**
     * Network Client
     *
     * @param subdata SubData Direct Server
     * @param client Socket to Bind
     */
    public Client(SubDataServer subdata, Socket client) throws IOException {
        if (Util.isNull(subdata, client)) throw new NullPointerException();
        this.subdata = subdata;
        socket = client;
        writer = new PrintWriter(client.getOutputStream(), true);
        address = new InetSocketAddress(client.getInetAddress(), client.getPort());
        authorized = new Timer("__subdata_auth_" + client.getRemoteSocketAddress().toString());
        authorized.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!socket.isClosed()) try {
                    subdata.removeClient(Client.this);
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
                    recievePacket(input);
                }
                try {
                    subdata.removeClient(Client.this);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            } catch (Exception e) {
                if (!(e instanceof SocketException)) e.printStackTrace();
                try {
                    subdata.removeClient(Client.this);
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

    protected void recievePacket(String raw) {
        try {
            JSONObject json = new JSONObject(raw);
            for (PacketIn packet : SubDataServer.decodePacket(json)) {
                if (authorized == null || packet instanceof PacketAuthorization) {
                    try {
                        packet.execute(Client.this, (json.keySet().contains("c")) ? json.getJSONObject("c") : null);
                    } catch (Throwable e) {
                        new InvocationTargetException(e, "Exception while executing PacketIn").printStackTrace();
                    }
                } else sendPacket(new PacketAuthorization(-1, "Unauthorized"));
            }
        } catch (IllegalPacketException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            new IllegalPacketException("Unknown Packet Format: " + raw).printStackTrace();
        }
    }

    /**
     * Send Packet to Client
     *
     * @param packet Packet to send
     */
    public void sendPacket(PacketOut packet) {
        if (Util.isNull(packet)) throw new NullPointerException();
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
