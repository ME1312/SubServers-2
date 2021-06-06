package net.ME1312.SubServers.Client.Sponge.Library;

/**
 * SubServers.Client Access Mode Enum
 */
public enum AccessMode {
    DEFAULT(0),
    NO_COMMANDS(-1),
    NO_INTEGRATIONS(-2),
    ;
    public final byte value;
    AccessMode(int value) {
        this.value = (byte) value;
    }
}
