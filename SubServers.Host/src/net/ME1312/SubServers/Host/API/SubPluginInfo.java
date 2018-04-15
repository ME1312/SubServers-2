package net.ME1312.SubServers.Host.API;

import net.ME1312.SubServers.Host.Library.Config.YAMLSection;
import net.ME1312.SubServers.Host.Library.Config.YAMLValue;
import net.ME1312.SubServers.Host.Library.ExtraDataHandler;
import net.ME1312.SubServers.Host.Library.Log.Logger;
import net.ME1312.SubServers.Host.Library.Util;
import net.ME1312.SubServers.Host.Library.Version.Version;
import net.ME1312.SubServers.Host.ExHost;

import java.io.File;
import java.net.URL;
import java.util.*;

/**
 * SubPlugin Info Class
 *
 * @see SubPlugin
 */
public class SubPluginInfo implements ExtraDataHandler {
    private ExHost host;
    private Object plugin;
    private String name;
    private Version version;
    private List<String> authors;
    private String desc;
    private URL website;
    private List<String> loadBefore;
    private List<String> depend;
    private List<String> softDepend;

    private Logger logger = null;
    private boolean enabled = false;
    private YAMLSection extra = new YAMLSection();

    /**
     * Create a SubPlugin Description
     *
     * @param host SubServers.Host
     * @param plugin Plugin
     * @param name Plugin Name
     * @param version Plugin Version
     * @param authors Authors List
     * @param description Plugin Description
     * @param website Authors' Website
     * @param loadBefore Load Before Plugins List
     * @param dependencies Dependencies List
     * @param softDependencies Soft Dependencies List
     */
    public SubPluginInfo(ExHost host, Object plugin, String name, Version version, List<String> authors, String description, URL website, List<String> loadBefore, List<String> dependencies, List<String> softDependencies) {
        if (Util.isNull(host, plugin, name, version, authors)) throw new NullPointerException();
        name = name.replaceAll("#|<|\\$|\\+|%|>|!|`|&|\\*|'|\\||\\{|\\?|\"|=|}|/|\\\\|\\s|@|\\.|\\n", "_");
        if (name.length() == 0) throw new StringIndexOutOfBoundsException("Cannot use an empty name");
        if (version.toString().length() == 0) throw new StringIndexOutOfBoundsException("Cannot use an empty version");
        if (authors.size() == 0) throw new ArrayIndexOutOfBoundsException("Cannot use an empty authors list");
        if (description != null && description.length() == 0) throw new StringIndexOutOfBoundsException("Cannot use an empty description");
        this.host = host;
        this.plugin = plugin;
        this.name = name;
        this.version = version;
        this.authors = authors;
        this.desc = description;
        this.website = website;
        this.loadBefore = (loadBefore == null)?Collections.emptyList():loadBefore;
        this.depend = (dependencies == null)?Collections.emptyList():dependencies;
        this.softDepend = (softDependencies == null)?Collections.emptyList():softDependencies;
    }

    /**
     * Get the Plugin's ClassLoader
     *
     * @return Plugin ClassLoader
     */
    public ClassLoader getLoader() {
        return plugin.getClass().getClassLoader();
    }

    /**
     * Get Plugin Object
     *
     * @return Plugin Object
     */
    public Object get() {
        return plugin;
    }

    /**
     * Get Plugin's Name
     *
     * @return Plugin Name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get Plugin's Version
     *
     * @return Plugin Version
     */
    public Version getVersion() {
        return this.version;
    }

    /**
     * Get Authors List
     *
     * @return Authors List
     */
    public List<String> getAuthors() {
        return this.authors;
    }

    /**
     * Get Plugin Description
     *
     * @return Plugin Description
     */
    public String getDescription() {
        return this.desc;
    }

    /**
     * Get Authors' Website
     *
     * @return Authors' Website
     */
    public URL getWebsite() {
        return this.website;
    }

    /**
     * Gets the Load Before Plugins List
     *
     * @return Load Before Plugins List
     */
    public List<String> getLoadBefore() {
        return this.loadBefore;
    }

    /**
     * Gets the Dependencies List
     *
     * @return Dependencies List
     */
    public List<String> getDependancies() {
        return this.depend;
    }

    /**
     * Gets the Soft Dependencies List
     *
     * @return Soft Dependencies List
     */
    public List<String> getSoftDependancies() {
        return this.softDepend;
    }

    /**
     * Sets the Plugin's Enabled Status
     *
     * @return Enabled Status
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets if the Plugin is Enabled
     *
     * @param value Value
     */
    public void setEnabled(boolean value) {
        enabled = value;
    }

    /**
     * Replace this Plugin's Logger with a custom one
     *
     * @param value Value
     */
    public void setLogger(Logger value) {
        logger = value;
    }

    /**
     * Gets the default Logger for this Plugin
     *
     * @return Logger
     */
    public Logger getLogger() {
        if (logger == null) logger = new Logger(name);
        return logger;
    }

    /**
     * Gets this Plugin's data folder
     *
     * @return Data Folder
     */
    public File getDataFolder() {
        File dir = new File(host.api.getRuntimeDirectory(), "Plugins" + File.separator + name);
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    @Override
    public void addExtra(String handle, Object value) {
        if (Util.isNull(handle, value)) throw new NullPointerException();
        extra.set(handle, value);
    }

    @Override
    public boolean hasExtra(String handle) {
        if (Util.isNull(handle)) throw new NullPointerException();
        return extra.getKeys().contains(handle);
    }

    @Override
    public YAMLValue getExtra(String handle) {
        if (Util.isNull(handle)) throw new NullPointerException();
        return extra.get(handle);
    }

    @Override
    public YAMLSection getExtra() {
        return extra.clone();
    }

    @Override
    public void removeExtra(String handle) {
        if (Util.isNull(handle)) throw new NullPointerException();
        extra.remove(handle);
    }
}
