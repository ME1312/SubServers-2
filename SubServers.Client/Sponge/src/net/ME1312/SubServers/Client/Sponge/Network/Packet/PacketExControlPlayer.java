package net.ME1312.SubServers.Client.Sponge.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataSender;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;

import java.util.UUID;

/**
 * Player Control Packet
 */
public class PacketExControlPlayer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private int response;
    private UUID tracker;

    /**
     * New PacketExControlPlayer (In)
     */
    public PacketExControlPlayer() {}

    /**
     * New PacketExControlPlayer (Out)
     *
     * @param response Response ID
     * @param tracker Tracker ID
     */
    public PacketExControlPlayer(int response, UUID tracker) {
        this.response = response;
        this.tracker = tracker;
    }

    @Override
    public ObjectMap<Integer> send(SubDataSender client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        if (tracker != null) data.set(0x0000, tracker);
        data.set(0x0001, response);
        return data;
    }

    @Override
    public void receive(SubDataSender client, ObjectMap<Integer> data) {
        UUID tracker =       (data.contains(0x0000)?data.getUUID(0x0000):null);
        try {
            String command = data.getString(0x0001);
            UUID target =    (data.contains(0x0002)?data.getUUID(0x0002):null);


            CommandSource sender = Sponge.getServer().getConsole();
            if (target != null && (sender = Sponge.getServer().getPlayer(target).orElse(null)) == null) {
                client.sendPacket(new PacketExControlPlayer(6, tracker));
            } else {
                Sponge.getCommandManager().process(sender, command);
                client.sendPacket(new PacketExControlPlayer(0, tracker));
            }
        } catch (Throwable e) {
            client.sendPacket(new PacketExControlPlayer(2, tracker));
            e.printStackTrace();
        }
    }
}
