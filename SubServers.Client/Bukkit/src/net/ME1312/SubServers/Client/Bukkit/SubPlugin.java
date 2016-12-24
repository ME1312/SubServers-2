package net.ME1312.SubServers.Client.Bukkit;

import net.ME1312.SubServers.Client.Bukkit.Graphic.UIListener;
import net.ME1312.SubServers.Client.Bukkit.Library.Config.YAMLConfig;
import net.ME1312.SubServers.Client.Bukkit.Library.Config.YAMLSection;
import net.ME1312.SubServers.Client.Bukkit.Library.UniversalFile;
import net.ME1312.SubServers.Client.Bukkit.Library.Version.Version;
import net.ME1312.SubServers.Client.Bukkit.Network.SubDataClient;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.InetAddress;
import java.nio.file.Files;
import java.util.Arrays;

public final class SubPlugin extends JavaPlugin {
    public YAMLConfig pluginconf;
    public YAMLSection lang = null;
    public SubDataClient subdata = null;

    public UIListener gui = null;
    public Version version;
    protected Version bversion = new Version(4);
    
    //public final SubAPI api = new SubAPI(this);

    @Override
    public void onEnable() {
        version = new Version(getDescription().getVersion());
        try {
            Bukkit.getLogger().info("SubServers > Loading SubServers v" + version.toString() + " Libraries... ");
            getDataFolder().mkdirs();
            if (!(new UniversalFile(getDataFolder(), "config.yml").exists())) {
                copyFromJar("config.yml", new UniversalFile(getDataFolder(), "config.yml").getPath());
                Bukkit.getLogger().info("SubServers > Created ~/plugins/SubServers/config.yml");
            } else if ((new Version((new YAMLConfig(new UniversalFile(getDataFolder(), "config.yml"))).get().getSection("Settings").getString("Version", "0")).compareTo(new Version("2.11.0a+"))) != 0) {
                Files.move(new UniversalFile(getDataFolder(), "config.yml").toPath(), new UniversalFile(getDataFolder(), "config.old" + Math.round(Math.random() * 100000) + ".yml").toPath());

                copyFromJar("config.yml", new UniversalFile(getDataFolder(), "config.yml").getPath());
                Bukkit.getLogger().info("SubServers > Updated ~/plugins/SubServers/config.yml");
            }
            pluginconf = new YAMLConfig(new UniversalFile(getDataFolder(), "config.yml"));
            subdata = new SubDataClient(this, pluginconf.get().getSection("Settings").getSection("SubData").getString("Name", "~no_name"),
                    InetAddress.getByName(pluginconf.get().getSection("Settings").getSection("SubData").getString("Address", "127.0.0.1:4391").split(":")[0]),
                    Integer.parseInt(pluginconf.get().getSection("Settings").getSection("SubData").getString("Address", "127.0.0.1:4391").split(":")[1]));
            Bukkit.getLogger().info("SubServers > SubData Connected to " + subdata.getClient().getRemoteSocketAddress().toString());

            gui = new UIListener(this);
            getCommand("subservers").setExecutor(new SubCommand(this));
            getCommand("subserver").setExecutor(new SubCommand(this));
            getCommand("sub").setExecutor(new SubCommand(this));
        } catch (IOException e) {
            setEnabled(false);
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        if (subdata != null)
            try {
                subdata.destroy(false);
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    private void copyFromJar(String resource, String destination) {
        InputStream resStreamIn = SubPlugin.class.getClassLoader().getResourceAsStream(resource);
        File resDestFile = new File(destination);
        try {
            OutputStream resStreamOut = new FileOutputStream(resDestFile);
            int readBytes;
            byte[] buffer = new byte[4096];
            while ((readBytes = resStreamIn.read(buffer)) > 0) {
                resStreamOut.write(buffer, 0, readBytes);
            }
            resStreamOut.close();
            resStreamIn.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
