package net.ME1312.SubServers.Client.Bukkit.Library;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubServers.Client.Common.Network.API.Host;
import net.ME1312.SubServers.Client.Common.Network.API.Proxy;
import net.ME1312.SubServers.Client.Common.Network.API.Server;
import net.ME1312.SubServers.Client.Common.Network.API.SubServer;

import org.bukkit.permissions.Permissible;

import java.util.List;

/**
 * Object Permissions Class
 */
public class ObjectPermission {
    private ObjectPermission() {}

    /**
     * Determine if an <i>object</i> can perform some action on this proxy using possible permissions
     *
     * @param proxy Proxy to check against
     * @param object Object to check against
     * @param permissions Permissions to check (use <b>%</b> as a placeholder for the proxy name)
     * @return Permission Check Result
     */
    public static boolean permits(Proxy proxy, Permissible object, String... permissions) {
        return permits(proxy.getName(), object, permissions);
    }

    /**
     * Determine if an <i>object</i> can perform some action on a host using possible permissions
     *
     * @param host Host to check against
     * @param object Object to check against
     * @param permissions Permissions to check (use <b>%</b> as a placeholder for the host name)
     * @return Permission Check Result
     */
    public static boolean permits(Host host, Permissible object, String... permissions) {
        return permits(host.getName(), object, permissions);
    }

    /**
     * Determine if an <i>object</i> can perform some action on another object using possible permissions
     *
     * @param string String to check against
     * @param object Object to check against
     * @param permissions Permissions to check (use <b>%</b> as a placeholder for the object name)
     * @return Permission Check Result
     */
    public static boolean permits(String string, Permissible object, String... permissions) {
        Util.nullpo(object);
        boolean permitted = false;

        for (int p = 0; !permitted && p < permissions.length; ++p) {
            String perm = permissions[p];
            if (perm != null) {
                // Check all objects & individual objects permission
                permitted = object.hasPermission(perm.replace("%", "*"))
                        || object.hasPermission(perm.replace("%", string.toLowerCase()));
            }
        }

        return permitted;
    }

    /**
     * Determine if an <i>object</i> can perform some action on a server using possible permissions
     *
     * @param server Server to check against
     * @param object Object to check against
     * @param permissions Permissions to check (use <b>%</b> as a placeholder for the server name)
     * @return Permission Check Result
     */
    public static boolean permits(Server server, Permissible object, String... permissions) {
        Util.nullpo(object);
        boolean permitted = false;

        for (int p = 0; !permitted && p < permissions.length; ++p) {
            String perm = permissions[p];
            if (perm != null) {
                // Check all servers & individual servers permission
                permitted = object.hasPermission(perm.replace("%", "*"))
                        || object.hasPermission(perm.replace("%", server.getName().toLowerCase()));

                // Check all hosts & individual hosts permission
                if (server instanceof SubServer) {
                    permitted = permitted || object.hasPermission(perm.replace("%", "::*"))
                            || object.hasPermission(perm.replace("%", "::" + ((SubServer) server).getHost().toLowerCase()));
                }

                // Check all groups & individual groups permission
                List<String> groups = server.getGroups();
                if (groups.size() > 0) {
                    permitted = permitted || object.hasPermission(perm.replace("%", ":*"));
                    for (int g = 0; !permitted && g < groups.size(); ++g) {
                        permitted = object.hasPermission(perm.replace("%", ":" + groups.get(g).toLowerCase()));
                    }
                }
            }
        }

        return permitted;
    }
}