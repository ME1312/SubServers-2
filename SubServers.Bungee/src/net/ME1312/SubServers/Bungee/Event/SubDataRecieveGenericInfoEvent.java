package net.ME1312.SubServers.Bungee.Event;

import net.ME1312.SubServers.Bungee.Library.SubEvent;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.md_5.bungee.api.plugin.Event;
import org.json.JSONObject;

/**
 * SubData Generic Info Event
 */
public class SubDataRecieveGenericInfoEvent extends Event implements SubEvent{
    private String handle;
    private Version version;
    private JSONObject content;

    /**
     * SubData Generic Info Event
     *
     * @param handle Content Handle
     * @param version Content Version
     * @param content Content
     */
    public SubDataRecieveGenericInfoEvent(String handle, Version version, JSONObject content) {
        this.handle = handle;
        this.version = version;
        this.content = content;
    }

    /**
     * Get Content Handle
     *
     * @return Content Handle
     */
    public String getHandle() {
        return handle;
    }

    /**
     * Get Content Version
     *
     * @return Content Version
     */
    public Version getVersion() {
        return version;
    }

    /**
     * Get Content
     *
     * @return Content
     */
    public JSONObject getContent() {
        return content;
    }
}
