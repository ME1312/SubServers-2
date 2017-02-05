package net.ME1312.SubServers.Host.API;

import net.ME1312.SubServers.Host.Library.Config.YAMLSection;
import net.ME1312.SubServers.Host.Library.Config.YAMLValue;
import net.ME1312.SubServers.Host.Library.ExtraDataHandler;
import net.ME1312.SubServers.Host.Library.Log.Logger;
import net.ME1312.SubServers.Host.Library.Util;
import net.ME1312.SubServers.Host.Library.Version.Version;
import net.ME1312.SubServers.Host.SubAPI;
import net.ME1312.SubServers.Host.SubServers;

import java.io.File;
import java.net.URL;
import java.util.*;

/**
 * SubPlugin Info Class
 *
 * @see SubPlugin
 */
public class SubPluginInfo implements ExtraDataHandler {
    private SubServers plugin;
    private Object main;
    private String name;
    private Version version;
    private List<String> authors;
    private String desc;
    private URL website;
    private List<String> loadBefore;
    private List<String> depend;
    private List<String> softDepend;

    private Logger logger;
    private boolean enabled = false;
    private YAMLSection extra = new YAMLSection();

    /**
     * Create a SubPlugin Description
     *
     * @param plugin SubServers.Host
     * @param main Plugin
     * @param name Plugin Name
     * @param version Plugin Version
     * @param authors Authors List
     * @param description Plugin Description
     * @param website Authors' Website
     * @param dependencies Dependencies List
     * @param softDependencies Soft Dependencies List
     */
    public SubPluginInfo(SubServers plugin, Object main, String name, Version version, List<String> authors, String description, URL website, List<String> loadBefore, List<String> dependencies, List<String> softDependencies) {
        if (Util.isNull(plugin, main, name, version, authors)) throw new NullPointerException();
        name = name.replaceAll("#|<|\\$|\\+|%|>|!|`|&|\\*|'|\\||\\{|\\?|\"|=|}|/|\\\\|\\s|@|\\.|\\n", "_");
        if (name.length() == 0) throw new StringIndexOutOfBoundsException("Cannot use an empty name");
        if (version.toString().length() == 0) throw new StringIndexOutOfBoundsException("Cannot use an empty version");
        if (authors.size() == 0) throw new ArrayIndexOutOfBoundsException("Cannot use an empty authors list");
        if (description != null && description.length() == 0) throw new StringIndexOutOfBoundsException("Cannot use an empty description");
        this.plugin = plugin;
        this.main = main;
        this.name = name;
        this.version = version;
        this.authors = authors;
        this.desc = description;
        this.website = website;
        this.loadBefore = (loadBefore == null)?Collections.emptyList():loadBefore;
        this.depend = (dependencies == null)?Collections.emptyList():dependencies;
        this.softDepend = (softDependencies == null)?Collections.emptyList():softDependencies;

        this.logger = new Logger(name);
    }

    public Object get() {
        return main;
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
     * Gets if the Plugin is Enabled
     *
     * @param value Enabled Status
     */
    public void setEnabled(boolean value) {
        enabled = value;
    }

    /**
     * Gets the default Logger for this Plugin
     *
     * @return Logger
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * Gets this Plugin's data folder
     *
     * @return Data Folder
     */
    public File getDataFolder() {
        File dir = new File(plugin.api.getRuntimeDirectory(), "Plugins" + File.separator + name);
        if (!dir.exists()) dir.mkdir();
        return dir;
    }

    @Override
    public void addExtra(String handle, Object value) {
        extra.set(handle, value);
    }

    @Override
    public boolean hasExtra(String handle) {
        return extra.getKeys().contains(handle);
    }

    @Override
    public YAMLValue getExtra(String handle) {
        return extra.get(handle);
    }

    @Override
    public YAMLSection getExtra() {
        return extra.clone();
    }

    @Override
    public void removeExtra(String handle) {
        extra.remove(handle);
    }
}
