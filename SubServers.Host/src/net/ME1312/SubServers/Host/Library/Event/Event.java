package net.ME1312.SubServers.Host.Library.Event;

import net.ME1312.SubServers.Host.API.SubPluginInfo;
import net.ME1312.SubServers.Host.SubAPI;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * SubEvent Layout Class
 */
public abstract class Event {
    private SubPluginInfo plugin = null;

    /**
     * Gets SubAPI
     *
     * @return SubAPI
     */
    public SubAPI getAPI() {
        return SubAPI.getInstance();
    }

    /**
     * Gets your Plugin's Info
     *
     * @return Plugin Info
     */
    public SubPluginInfo getPlugin() {
        return plugin;
    }

    /**
     * Get the handlers for this event
     *
     * @return Handler Map
     */
    @SuppressWarnings({"deprecation", "unchecked"})
    public Map<SubPluginInfo, List<Method>> getHandlers() throws IllegalAccessException {
        try {
            Field f = SubAPI.class.getDeclaredField("listeners");
            f.setAccessible(true);
            HashMap<EventPriority, HashMap<Class<? extends Event>, HashMap<SubPluginInfo, HashMap<Object, List<Method>>>>> listeners = (HashMap<EventPriority, HashMap<Class<? extends Event>, HashMap<SubPluginInfo, HashMap<Object, List<Method>>>>>) f.get(getAPI());
            HashMap<SubPluginInfo, List<Method>> map = new LinkedHashMap<SubPluginInfo, List<Method>>();
            f.setAccessible(false);
            for (EventPriority priority : listeners.keySet()) {
                if (!listeners.get(priority).keySet().contains(getClass())) continue;
                for (SubPluginInfo plugin : listeners.get(priority).get(getClass()).keySet()) {
                    for (Object listener : listeners.get(priority).get(getClass()).get(plugin).keySet()) {
                        for (Method method : listeners.get(priority).get(getClass()).get(plugin).get(listener)) {
                            List<Method> methods = (map.keySet().contains(plugin))?map.get(plugin):new LinkedList<Method>();
                            methods.add(method);
                            map.put(plugin, methods);
                        }
                    }
                }
            }
            return map;
        } catch (NoSuchFieldException e) {
            getAPI().getInternals().log.error(new InvocationTargetException(e, "Couldn't get handler list for event: " + toString()));
            return null;
        }
    }

    @Override
    public String toString() {
        return getClass().getTypeName();
    }
}
