package net.ME1312.SubServers.Host.Library.Event;

/**
 * Event Order Defaults Class<br>
 * Events will be run from Short.MIN_VALUE to Short.MAX_VALUE
 */
public final class EventOrder {
    private EventOrder() {}
    public static final short FIRST = Short.MIN_VALUE;
    public static final short VERY_EARLY = (Short.MIN_VALUE / 3) * 2;
    public static final short EARLY = Short.MIN_VALUE / 3;
    public static final short NORMAL = 0;
    public static final short LATE = Short.MAX_VALUE / 3;
    public static final short VERY_LATE = (Short.MAX_VALUE / 3) * 2;
    public static final short LAST = Short.MAX_VALUE;
}
