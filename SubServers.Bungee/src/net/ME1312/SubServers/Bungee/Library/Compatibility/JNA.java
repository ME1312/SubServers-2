package net.ME1312.SubServers.Bungee.Library.Compatibility;

import com.google.common.io.Resources;
import net.ME1312.SubServers.Bungee.Library.UniversalFile;
import net.ME1312.SubServers.Bungee.SubAPI;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLClassLoader;

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
            UniversalFile library = new UniversalFile(SubAPI.getInstance().getInternals().dir, "SubServers:Cache:Libraries");
            UniversalFile jna = new UniversalFile(library, "jna-" + JNA_VERSION + ".jar");
            jna.getParentFile().mkdirs();
            if (!jna.exists()) {
                announced = true;
                System.out.println(">> Downloading JNA Library v" + JNA_VERSION);
                try (FileOutputStream fin = new FileOutputStream(jna)) {
                    Resources.copy(new URL(JNA_DOWNLOAD.replace("$1", "jna")), fin);
                } catch (Throwable e) {
                    jna.delete();
                    e.printStackTrace();
                }
            }
            UniversalFile platform = new UniversalFile(library, "jna-platform-" + JNA_VERSION + ".jar");
            platform.getParentFile().mkdirs();
            if (!platform.exists()) {
                if (!announced) System.out.println(">> Downloading JNA Library v" + JNA_VERSION);
                announced = true;
                try (FileOutputStream fin = new FileOutputStream(platform)) {
                    Resources.copy(new URL(JNA_DOWNLOAD.replace("$1", "jna-platform")), fin);
                } catch (Throwable e) {
                    platform.delete();
                    e.printStackTrace();
                }
            }
            if (jna.exists() && platform.exists()) {
                if (announced) System.out.println(">> Loading JNA Library");
                try {
                    JNA = new URLClassLoader(new URL[]{jna.toURI().toURL(), platform.toURI().toURL()});
                } catch (Throwable e) {
                    System.out.println(">> Could not load JNA Library:");
                    e.printStackTrace();
                }
            } else {
                System.out.println(">> Could not load JNA Library:");
                new FileNotFoundException().printStackTrace();
            }
        }
        return JNA;
    }
}
