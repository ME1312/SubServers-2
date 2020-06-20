package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.SubData.Client.Protocol.PacketIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataSender;
import net.ME1312.SubServers.Host.ExHost;
import net.ME1312.SubServers.Host.Executable.SubCreatorImpl;

/**
 * External Host Template Upload Packet
 */
public class PacketExUploadTemplates implements PacketIn, PacketObjectOut<Integer> {
    private boolean first;
    private ExHost host;

    /**
     * New PacketExUploadTemplates
     */
    public PacketExUploadTemplates(ExHost host) {
        this(host, true);
    }
    private PacketExUploadTemplates(ExHost host, boolean first) {
        this.host = host;
        this.first = first;
    }

    @Override
    public ObjectMap<Integer> send(SubDataSender client) {
        host.log.info.println(((first)?"S":"Res") + "ending Local Template Metadata...");
        if (!first) host.creator.load(false);

        ObjectMap<Integer> data = new ObjectMap<Integer>();
        ObjectMap<String> templates = new ObjectMap<String>();
        for (SubCreatorImpl.ServerTemplate template : host.templates.values()) {
            ObjectMap<String> tinfo = new ObjectMap<String>();
            tinfo.set("enabled", template.isEnabled());
            tinfo.set("display", template.getDisplayName());
            tinfo.set("icon", template.getIcon());
            tinfo.set("build", template.getBuildOptions().clone());
            tinfo.set("settings", template.getConfigOptions().clone());
            templates.set(template.getName(), tinfo);
        }
        data.set(0x0000, templates);
        return data;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void receive(SubDataSender client) {
        client.sendPacket(new PacketExUploadTemplates(host, false));
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
