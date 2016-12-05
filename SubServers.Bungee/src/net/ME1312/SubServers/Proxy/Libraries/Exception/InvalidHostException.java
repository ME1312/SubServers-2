package net.ME1312.SubServers.Proxy.Libraries.Exception;

public class InvalidHostException extends IllegalStateException {
    public InvalidHostException() {}
    public InvalidHostException(String s) {
        super(s);
    }
}
