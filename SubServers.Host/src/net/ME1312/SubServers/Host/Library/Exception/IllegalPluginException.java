package net.ME1312.SubServers.Host.Library.Exception;

import java.lang.reflect.InvocationTargetException;

/**
 * Illegal Plugin Exception
 */
public class IllegalPluginException extends InvocationTargetException {
    public IllegalPluginException(Throwable e) {
        super(e);
    }
    public IllegalPluginException(Throwable e, String s) {
        super(e, s);
    }
}
