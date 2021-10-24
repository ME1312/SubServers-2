package net.ME1312.SubServers.Velocity.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataSender;
import net.ME1312.SubServers.Velocity.ExProxy;
import net.ME1312.SubServers.Velocity.Library.Compatibility.ChatColor;

import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Message External Player Packet
 */
public class PacketExMessagePlayer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private ExProxy plugin;
    private int response;
    private UUID tracker;

    /**
     * New PacketExMessagePlayer (In)
     */
    public PacketExMessagePlayer(ExProxy plugin) {
        this.plugin = plugin;
    }

    /**
     * New PacketExMessagePlayer (Out)
     *
     * @param response Response ID
     * @param tracker Receiver ID
     */
    public PacketExMessagePlayer(int response, UUID tracker) {
        this.response = response;
        this.tracker = tracker;
    }

    @Override
    public ObjectMap<Integer> send(SubDataSender client) {
        ObjectMap<Integer> json = new ObjectMap<Integer>();
        if (tracker != null) json.set(0x0000, tracker);
        json.set(0x0001, response);
        return json;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void receive(SubDataSender client, ObjectMap<Integer> data) {
        UUID tracker =   (data.contains(0x0000)?data.getUUID(0x0000):null);
        List<UUID> ids = (data.contains(0x0001)?data.getUUIDList(0x0001):null);
        try {
                Component[] legacy = null;
                Component[] components = null;

                if (data.contains(0x0002)) {
                    List<String> messages = data.getRawStringList(0x0002);
                    legacy = new Component[messages.size()];
                    for (int i = 0; i < legacy.length; ++i) legacy[i] = ChatColor.convertColor(messages.get(i));
                }
                if (data.contains(0x0003)) {
                    List<String> messages = data.getRawStringList(0x0003);
                    components = new Component[messages.size()];
                    for (int i = 0; i < components.length; ++i) components[i] = GsonComponentSerializer.gson().deserialize(messages.get(i));
                }

                int failures = 0;
                if (ids == null || ids.size() == 0) {
                    if (legacy != null) for (Component c : legacy)
                        ExProxy.getInstance().sendMessage(c);
                    if (components != null) for (Component c : components)
                        ExProxy.getInstance().sendMessage(c);
                } else {
                    for (UUID id : ids) {
                        Optional<Player> local = ExProxy.getInstance().getPlayer(id);
                        if (local.isPresent()) {
                            if (legacy != null) for (Component c : legacy)
                                local.get().sendMessage(c);
                            if (components != null) for (Component c : components)
                                local.get().sendMessage(c);
                        } else {
                            ++failures;
                        }
                    }
                }
                client.sendPacket(new PacketExMessagePlayer(failures, tracker));
        } catch (Throwable e) {
            client.sendPacket(new PacketExMessagePlayer((ids == null || ids.size() == 0)? 1 : ids.size(), tracker));
            e.printStackTrace();
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
