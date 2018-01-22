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
    private static final Set<PluginClassLoader> loaders = new CopyOnWriteArraySet<PluginClassLoader>();

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

    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return this.loadClass(name, resolve, true);
    }

    private Class<?> loadClass(String name, boolean resolve, boolean check) throws ClassNotFoundException {
        try {
            return super.loadClass(name, resolve);
        } catch (ClassNotFoundException e) {
            if (check) {
                Iterator i = loaders.iterator();

                while (true) {
                    PluginClassLoader loader;
                    do {
                        if (!i.hasNext()) {
                            throw new ClassNotFoundException(name);
                        }
                        loader = (PluginClassLoader) i.next();
                    } while (loader == this);

                    try {
                        return loader.loadClass(name, resolve, false);
                    } catch (ClassNotFoundException ex) {}
                }
            } else {
                throw new ClassNotFoundException(name);
            }
        }
    }
}