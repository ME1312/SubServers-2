package net.ME1312.SubServers.Bungee.Library.Version;

public enum VersionType {
    PRE_ALPHA(-6, "pa", "pre-alpha"),
    ALPHA(-5, "a", "alpha"),
    PREVIEW(-4, "pv", "preview"),
    PRE_BETA(-4, "pb", "pre-beta"),
    BETA(-3, "b", "beta"),
    SNAPSHOT(-2, "s", "snapshot"),
    PRE_RELEASE(-1, "pr", "pre-release"),
    RELEASE(0, "r", "release"),
    REVISION(0, "rv", "revision"),
    VERSION(0, "v", "version"),
    UPDATE(0, "u", "update"),
    ;
    final short stageid;
    final String shortname, longname;
    VersionType(int stageid, String shortname, String longname) {
        this.stageid = (short) stageid;
        this.shortname = shortname;
        this.longname = longname;
    }
}