package net.ME1312.SubServers.Velocity.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.SubDataSender;
import net.ME1312.SubServers.Velocity.ExProxy;
import net.ME1312.SubServers.Velocity.Library.Compatibility.ChatColor;
import net.ME1312.SubServers.Velocity.Server.ServerData;

import com.velocitypowered.api.proxy.server.RegisteredServer;

/**
 * Server Edit Notification Packet
 */
public class PacketInExEditServer implements PacketObjectIn<Integer> {
    private ExProxy plugin;

    /**
     * New PacketExControlServer (In)
     */
    public PacketInExEditServer(ExProxy plugin) {
        this.plugin = plugin;
    }

    @Override
    public void receive(SubDataSender client, ObjectMap<Integer> data) {
        ServerData server = ExProxy.getInstance().getServer(data.getString(0x0000)).map(RegisteredServer::getServerInfo).map(plugin::getData).orElse(null);
        if (server != null) {
            switch (data.getInt(0x0001)) {
                case 0:
                    server.setDisplayName(data.getList(0x0002).get(0).asString());
                    break;
                case 1:
                    server.setMotd(ChatColor.parseColor('&', data.getList(0x0002).get(0).asString()));
                    break;
                case 2:
                    server.setRestricted(data.getList(0x0002).get(0).asBoolean());
                    break;
                case 3:
                    server.setHidden(data.getList(0x0002).get(0).asBoolean());
                    break;
                case 4:
                    server.setSubData(data.getList(0x0002).get(1).asUUID(), data.getList(0x0002).get(0).asInt());
                    break;
                case 5:
                    server.setSubData(null, data.getList(0x0002).get(0).asInt());
                    break;
                case 6:
                    server.whitelist = data.getList(0x0002).get(0).asUUIDList();
                    break;
                case 7:
                    server.whitelist(data.getList(0x0002).get(0).asUUID());
                    break;
                case 8:
                    server.unwhitelist(data.getList(0x0002).get(0).asUUID());
                    break;
            }
        }
    }

    @Override
    public int version() {
        return 0x0002;
    }
}
