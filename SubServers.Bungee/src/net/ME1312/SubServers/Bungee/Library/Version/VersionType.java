package net.ME1312.SubServers.Bungee.Library.Version;

public enum VersionType {
    PRE_ALPHA(-5, "pa", "pre-alpha"),
    ALPHA(-4, "a", "alpha"),
    PREVIEW(-3, "pv", "preview"),
    PRE_BETA(-3, "pb", "pre-beta"),
    BETA(-2, "b", "beta"),
    PRE_RELEASE(-1, "pr", "pre-release"),
    RELEASE(0, "r", "release"),
    REVISION(0, "rv", "revision"),
    VERSION(0, "v", "version"),
    UPDATE(0, "u", "update"),
    PATCH(0, "p", "patch"),
    ;
    final short stageid;
    final String shortname, longname;
    VersionType(int stageid, String shortname, String longname) {
        this.stageid = (short) stageid;
        this.shortname = shortname;
        this.longname = longname;
    }
}