package net.ME1312.SubServers.Host.Library;

import net.ME1312.SubServers.Host.Library.Config.YAMLSection;
import net.ME1312.SubServers.Host.Library.Config.YAMLValue;

/**
 * Extra Data Handler Layout Class
 */
public interface ExtraDataHandler {
    /**
     * Add an extra value to this Server
     *
     * @param handle Handle
     * @param value Value
     */
    void addExtra(String handle, Object value);

    /**
     * Determine if an extra value exists
     *
     * @param handle Handle
     * @return Value Status
     */
    boolean hasExtra(String handle);

    /**
     * Get an extra value
     *
     * @param handle Handle
     * @return Value
     */
    YAMLValue getExtra(String handle);

    /**
     * Get the extra value section
     *
     * @return Extra Value Section
     */
    YAMLSection getExtra();

    /**
     * Remove an extra value from this Server
     *
     * @param handle Handle
     */
    void removeExtra(String handle);
}
