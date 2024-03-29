package net.ME1312.SubServers.Host.Library.Compatibility;

import net.ME1312.Galaxi.Galaxi;
import net.ME1312.Galaxi.Log.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

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
            Logger log = new Logger("JNA");
            File library = new File(Galaxi.getInstance().getRuntimeDirectory(), "Cache/Libraries");
            File jna = new File(library, "jna-" + JNA_VERSION + ".jar");
            jna.getParentFile().mkdirs();
            if (!jna.exists()) {
                log.info.println("Downloading JNA v" + JNA_VERSION);
                announced = true;
                try (InputStream in = new URL(JNA_DOWNLOAD.replace("$1", "jna")).openStream()) {
                    Files.copy(in, jna.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (Throwable e) {
                    jna.delete();
                    log.error.println(e);
                }
            }
            File platform = new File(library, "jna-platform-" + JNA_VERSION + ".jar");
            platform.getParentFile().mkdirs();
            if (!platform.exists()) {
                if (!announced) log.info.println("Downloading JNA platform v" + JNA_VERSION);
                announced = true;
                try (InputStream in = new URL(JNA_DOWNLOAD.replace("$1", "jna-platform")).openStream()) {
                    Files.copy(in, platform.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (Throwable e) {
                    platform.delete();
                    log.error.println(e);
                }
            }
            if (jna.exists() && platform.exists()) {
                if (announced) log.info.println("JNA download complete");
                try {
                    JNA = new URLClassLoader(new URL[]{jna.toURI().toURL(), platform.toURI().toURL()});
                } catch (Throwable e) {
                    log.error.println("Couldn't load JNA:");
                    log.error.println(e);
                }
            } else {
                log.error.println("Couldn't load JNA:");
                log.error.println(new FileNotFoundException());
            }
        }
        return JNA;
    }
}
