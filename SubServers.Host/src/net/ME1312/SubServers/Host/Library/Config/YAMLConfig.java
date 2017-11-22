package net.ME1312.SubServers.Host.Library.Config;

import net.ME1312.SubServers.Host.Library.Util;
import org.json.JSONObject;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * YAML Config Class
 */
@SuppressWarnings("unused")
public class YAMLConfig {
    private File file;
    private Yaml yaml;
    private YAMLSection config;

    /**
     * Creates/Loads a YAML Formatted Config
     *
     * @param file
     * @throws IOException
     * @throws YAMLException
     */
    @SuppressWarnings("unchecked")
    public YAMLConfig(File file) throws IOException, YAMLException {
        if (Util.isNull(file)) throw new NullPointerException();
        this.file = file;
        this.yaml = new Yaml(getDumperOptions());
        if (file.exists()) {
            this.config = new YAMLSection((LinkedHashMap<String, ?>) yaml.loadAs(new FileInputStream(file), LinkedHashMap.class), null, null, yaml);
        } else {
            this.config = new YAMLSection(null, null, null, yaml);
        }
    }

    /**
     * Get Config Contents
     *
     * @return Config Contents
     */
    public YAMLSection get() {
        return config;
    }

    /**
     * Set Config Contents
     *
     * @param value Value
     */
    public void set(YAMLSection value) {
        if (Util.isNull(value)) throw new NullPointerException();
        config = value;
    }

    /**
     * Reload Config Contents
     *
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public void reload() throws IOException {
        if (file.exists()) {
            this.config = new YAMLSection((LinkedHashMap<String, ?>) yaml.loadAs(new FileInputStream(file), LinkedHashMap.class), null, null, yaml);
        } else {
            this.config = new YAMLSection(null, null, null, yaml);
        }
    }

    /**
     * Save Config Contents
     *
     * @throws IOException
     */
    public void save() throws IOException {
        if (!file.exists()) file.createNewFile();
        FileWriter writer = new FileWriter(file);
        yaml.dump(config.map, writer);
        writer.close();
    }

    @Override
    public String toString() {
        return yaml.dump(config.map);
    }

    /**
     * Converts Config Contents to JSON
     *
     * @return JSON Formatted Config Contents
     */
    public JSONObject toJSON() {
        return new JSONObject(config.map);
    }

    protected static DumperOptions getDumperOptions() {
        DumperOptions options = new DumperOptions();
        options.setAllowUnicode(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setIndent(4);

        return options;
    }
}