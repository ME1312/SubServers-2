package net.ME1312.SubServers.Bungee.Network;

import net.ME1312.SubServers.Bungee.Library.Exception.IllegalPacketException;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketAuthorization;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Base64;
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
    private SubDataServer subdata;

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
        authorized = new Timer();
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
    private void loop() {
        new Thread(() -> {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String input;
                while ((input = in.readLine()) != null) {
                    String decoded = null;
                    try {
                        switch (subdata.getEncryption()) {
                            case AES:
                            case AES_128:
                            case AES_192:
                            case AES_256:
                                decoded = AES.decrypt(subdata.plugin.config.get().getSection("Settings").getSection("SubData").getRawString("Password"), Base64.getDecoder().decode(input)).get();
                                break;
                            default:
                                decoded = input;
                        }
                        JSONObject json = new JSONObject(decoded);
                        for (PacketIn packet : SubDataServer.decodePacket(json)) {
                            if (authorized == null || packet instanceof PacketAuthorization) {
                                try {
                                    if (json.keySet().contains("f")) {
                                        Client client = subdata.getClient(new InetSocketAddress(json.getString("f").split(":")[0], Integer.parseInt(json.getString("f").split(":")[1])));
                                        json.remove("f");
                                        client.writer.println(json);
                                    } else {
                                        packet.execute(Client.this, (json.keySet().contains("c"))?json.getJSONObject("c"):null);
                                    }
                                } catch (Throwable e) {
                                    new InvocationTargetException(e, "Exception while executing PacketIn").printStackTrace();
                                }
                            } else sendPacket(new PacketAuthorization(-1, "Unauthorized"));
                        }
                    } catch (JSONException e) {
                        new IllegalPacketException("Unknown Packet Format: " + ((decoded == null)?input:decoded)).printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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

    /**
     * Send Packet to Client
     *
     * @param packet Packet to send
     */
    public void sendPacket(PacketOut packet) {
        if (Util.isNull(packet)) throw new NullPointerException();
        try {
            switch (subdata.getEncryption()) {
                case AES:
                case AES_128:
                    writer.println(Base64.getEncoder().encodeToString(AES.encrypt(128, subdata.plugin.config.get().getSection("Settings").getSection("SubData").getRawString("Password"), SubDataServer.encodePacket(packet).toString())));
                    break;
                case AES_192:
                    writer.println(Base64.getEncoder().encodeToString(AES.encrypt(192, subdata.plugin.config.get().getSection("Settings").getSection("SubData").getRawString("Password"), SubDataServer.encodePacket(packet).toString())));
                    break;
                case AES_256:
                    writer.println(Base64.getEncoder().encodeToString(AES.encrypt(256, subdata.plugin.config.get().getSection("Settings").getSection("SubData").getRawString("Password"), SubDataServer.encodePacket(packet).toString())));
                    break;
                default:
                    writer.println(SubDataServer.encodePacket(packet));
            }
        } catch (Exception e) {
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
     * Sets the Handler
     *
     * @param obj Handler
     */
    public void setHandler(ClientHandler obj) {
        if (handler != null) handler.setSubData(null);
        handler = obj;
        handler.setSubData(this);
    }

    /**
     * Disconnects the Client (does not remove them from the server)
     *
     * @throws IOException
     */
    public void disconnect() throws IOException {
        if (!socket.isClosed()) getConnection().close();
        if (handler != null) handler.setSubData(null);
        handler = null;

    }
}
