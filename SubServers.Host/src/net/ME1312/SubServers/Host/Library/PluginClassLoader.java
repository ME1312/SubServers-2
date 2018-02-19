package net.ME1312.SubServers.Host.Library;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Plugin ClassLoader Class
 */
public class PluginClassLoader extends URLClassLoader {
    private static Set<PluginClassLoader> loaders = new CopyOnWriteArraySet<PluginClassLoader>();
    private Class<?> defaultClass = null;

    /**
     * Load Classes from URLs
     *
     * @param urls URLs
     */
    public PluginClassLoader(URL[] urls) {
        super(urls);
        loaders.add(this);
    }

    /**
     * Load Classes from URLs with a parent loader
     *
     * @param parent Parent loader
     * @param urls URLs
     */
    public PluginClassLoader(ClassLoader parent, URL... urls) {
        super(urls, parent);
        loaders.add(this);
    }

    public void setDefaultClass(Class<?> clazz) {
        this.defaultClass = clazz;
    }

    public Class<?> getDefaultClass() throws ClassNotFoundException {
        if (defaultClass == null) {
            throw new ClassNotFoundException();
        } else {
            return defaultClass;
        }
    }

    private Class<?> getDefaultClass(String name) throws ClassNotFoundException {
        try {
            return getDefaultClass();
        } catch (ClassNotFoundException e) {
            throw new ClassNotFoundException(name);
        }
    }

    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return this.loadClass(name, resolve, true);
    }

    private Class<?> loadClass(String name, boolean resolve, boolean check) throws ClassNotFoundException {
        try {
            return super.loadClass(name, resolve);
        } catch (NoClassDefFoundError | ClassNotFoundException e) {
            if (check) {
                Iterator i = loaders.iterator();

                while (true) {
                    PluginClassLoader loader;
                    do {
                        if (!i.hasNext()) {
                            return getDefaultClass(name);
                        }
                        loader = (PluginClassLoader) i.next();
                    } while (loader == this);

                    try {
                        return loader.loadClass(name, resolve, false);
                    } catch (NoClassDefFoundError | ClassNotFoundException ex) {}
                }
            } else {
                return getDefaultClass(name);
            }
        }
    }
}