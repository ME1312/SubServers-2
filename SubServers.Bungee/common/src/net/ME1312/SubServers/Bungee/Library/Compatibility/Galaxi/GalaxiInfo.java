package net.ME1312.SubServers.Bungee.Library.Compatibility.Galaxi;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;

import java.lang.annotation.Annotation;
import java.util.jar.Manifest;

/**
 * Galaxi Info Class
 */
public class GalaxiInfo {
    private GalaxiInfo() {}

    @SuppressWarnings("unchecked")
    private static <A extends Annotation> Class<A> asAnnotation(Class<?> clazz) {
        return (Class<A>) clazz;
    }

    /**
     * Get the Galaxi Version
     *
     * @return Galaxi Version
     */
    public static Version getVersion() {
        return Util.getDespiteException(() -> Version.fromString((String) Class.forName("net.ME1312.Galaxi.Plugin.App").getMethod("version").invoke(
                Class.forName("net.ME1312.Galaxi.Engine.Runtime.Engine").getAnnotation(asAnnotation(Class.forName("net.ME1312.Galaxi.Plugin.App"))))), null);
    }

    /**
     * Get the Galaxi Build Version
     *
     * @return Galaxi Build Version
     */
    public static Version getBuild() {
        try {
            Manifest manifest = new Manifest(Class.forName("net.ME1312.Galaxi.Engine.GalaxiEngine").getResourceAsStream("/META-INF/GalaxiEngine.MF"));
            if (manifest.getMainAttributes().getValue("Implementation-Version") != null && manifest.getMainAttributes().getValue("Implementation-Version").length() > 0) {
                return new Version(manifest.getMainAttributes().getValue("Implementation-Version"));
            } else {
                return null;
            }
        } catch (Throwable e) {
            return null;
        }
    }
}
