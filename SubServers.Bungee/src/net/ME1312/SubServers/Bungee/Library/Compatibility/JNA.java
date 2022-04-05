package net.ME1312.SubServers.Bungee.Library.Compatibility;


import net.ME1312.SubServers.Bungee.SubAPI;

import com.google.common.io.Resources;
import net.md_5.bungee.api.ProxyServer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.Logger;

import static java.util.logging.Level.SEVERE;

/**
 * JNA Library Loader Class
 */
public class JNA {
    private JNA() {}
    private static ClassLoader JNA = null;
    private static final String JNA_VERSION = "5.2.0";
    private static final String JNA_DOWNLOAD = "https://oss.sonatype.org/service/local/repositories/releases/content/net/java/dev/jna/$1/" + JNA_VERSION + "/$1-" + JNA_VERSION + ".jar";

    /**
     * Get/Load JNA Library
     *
     * @return JNA ClassLoader
     */
    @SuppressWarnings("deprecation")
    public static ClassLoader get() {
        if (JNA == null) {
            boolean announced = false;
            Logger log = ProxyServer.getInstance().getLogger();
            File library = new File(SubAPI.getInstance().getInternals().dir, "SubServers/Cache/Libraries");
            File jna = new File(library, "jna-" + JNA_VERSION + ".jar");
            jna.getParentFile().mkdirs();
            if (!jna.exists()) {
                announced = true;
                log.info(">> Downloading JNA v" + JNA_VERSION);
                try (FileOutputStream fin = new FileOutputStream(jna)) {
                    Resources.copy(new URL(JNA_DOWNLOAD.replace("$1", "jna")), fin);
                } catch (Throwable e) {
                    jna.delete();
                    e.printStackTrace();
                }
            }
            File platform = new File(library, "jna-platform-" + JNA_VERSION + ".jar");
            platform.getParentFile().mkdirs();
            if (!platform.exists()) {
                if (!announced) log.info(">> Downloading JNA platform v" + JNA_VERSION);
                announced = true;
                try (FileOutputStream fin = new FileOutputStream(platform)) {
                    Resources.copy(new URL(JNA_DOWNLOAD.replace("$1", "jna-platform")), fin);
                } catch (Throwable e) {
                    platform.delete();
                    e.printStackTrace();
                }
            }
            if (jna.exists() && platform.exists()) {
                if (announced) log.info(">> JNA download complete");
                try {
                    JNA = new URLClassLoader(new URL[]{jna.toURI().toURL(), platform.toURI().toURL()});
                } catch (Throwable e) {
                    log.log(SEVERE, ">> Couldn't load JNA:", e);
                }
            } else {
                log.log(SEVERE, ">> Couldn't load JNA:", new FileNotFoundException());
            }
        }
        return JNA;
    }
}
