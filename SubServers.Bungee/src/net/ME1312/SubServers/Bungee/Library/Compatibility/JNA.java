package net.ME1312.SubServers.Bungee.Library.Compatibility;

import com.google.common.io.Resources;
import net.ME1312.SubServers.Bungee.Library.UniversalFile;
import net.ME1312.SubServers.Bungee.SubAPI;

import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLClassLoader;

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
            UniversalFile library = new UniversalFile(SubAPI.getInstance().getInternals().dir, "SubServers:Cache:Libraries");
            library.mkdirs();
            UniversalFile jna = new UniversalFile(library, "JNA.jar");
            if (!jna.exists()) {
                jna.getParentFile().mkdirs();
                if (!jna.exists()) {
                    System.out.println(">> Downloading JNA Library v" + JNA_VERSION);
                    try (FileOutputStream fin = new FileOutputStream(jna)) {
                        Resources.copy(new URL(JNA_DOWNLOAD.replace("$1", "jna")), fin);
                    } catch (Throwable e) {
                        jna.delete();
                        e.printStackTrace();
                    }
                    announced = true;
                }
            }
            UniversalFile platform = new UniversalFile(library, "JNA-Platform.jar");
            if (!platform.exists()) {
                platform.getParentFile().mkdirs();
                if (!platform.exists()) {
                    if (!announced) System.out.println(">> Downloading JNA Library v" + JNA_VERSION);
                    try (FileOutputStream fin = new FileOutputStream(platform)) {
                        Resources.copy(new URL(JNA_DOWNLOAD.replace("$1", "jna-platform")), fin);
                    } catch (Throwable e) {
                        platform.delete();
                        e.printStackTrace();
                    }
                }
            }
            if (jna.exists()) {
                System.out.println(">> Loading JNA Library");
                try {
                    JNA = new URLClassLoader(new URL[]{jna.toURI().toURL(), platform.toURI().toURL()});
                } catch (Throwable e) {
                    e.printStackTrace();
                    throw new IllegalArgumentException("Could not load JNA Library");
                }
            } else {
                throw new IllegalArgumentException("Could not find JNA Library");
            }
        }
        return JNA;
    }
}
