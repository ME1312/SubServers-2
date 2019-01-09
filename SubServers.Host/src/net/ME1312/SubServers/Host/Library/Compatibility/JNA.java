package net.ME1312.SubServers.Host.Library.Compatibility;

import net.ME1312.Galaxi.Galaxi;
import net.ME1312.Galaxi.Library.Log.Logger;
import net.ME1312.Galaxi.Library.UniversalFile;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Load JNA Library
 */
public class JNA {
    private static ClassLoader JNA = null;
    private static final String JNA_VERSION = "5.2.0";
    private static final String JNA_DOWNLOAD = "https://oss.sonatype.org/service/local/repositories/releases/content/net/java/dev/jna/$1/" + JNA_VERSION + "/$1-" + JNA_VERSION + ".jar";

    @SuppressWarnings("deprecation")
    public static ClassLoader get() {
        if (JNA == null) {
            boolean announced = false;
            Logger log = new Logger("JNA");
            UniversalFile library = new UniversalFile(Galaxi.getInstance().getRuntimeDirectory(), "Cache:Libraries");
            library.mkdirs();
            UniversalFile jna = new UniversalFile(library, "jna-" + JNA_VERSION + ".jar");
            if (!jna.exists()) {
                jna.getParentFile().mkdirs();
                if (!jna.exists()) {
                    announced = true;
                    log.info.println("Downloading JNA Library v" + JNA_VERSION);
                    try (InputStream in = new URL(JNA_DOWNLOAD.replace("$1", "jna")).openStream()) {
                        Files.copy(in, jna.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (Throwable e) {
                        jna.delete();
                        log.error.println(e);
                    }
                }
            }
            UniversalFile platform = new UniversalFile(library, "jna-platform-" + JNA_VERSION + ".jar");
            if (!platform.exists()) {
                platform.getParentFile().mkdirs();
                if (!platform.exists()) {
                    if (!announced) log.info.println("Downloading JNA Library v" + JNA_VERSION);
                    try (InputStream in = new URL(JNA_DOWNLOAD.replace("$1", "jna-platform")).openStream()) {
                        Files.copy(in, platform.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (Throwable e) {
                        platform.delete();
                        log.error.println(e);
                    }
                }
            }
            if (jna.exists()) {
                log.info.println("Loading JNA Library");
                try {
                    JNA = new URLClassLoader(new URL[]{jna.toURI().toURL(), platform.toURI().toURL()});
                } catch (Throwable e) {
                    log.error.println(e);
                    throw new IllegalArgumentException("Could not load JNA Library");
                }
            } else {
                throw new IllegalArgumentException("Could not find JNA Library");
            }
        }
        return JNA;
    }
}
