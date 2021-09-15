package net.ME1312.SubServers.Host;

/**
 * SubServers/GalaxiEngine Launch Class
 */
public final class Launch {

    /**
     * Launch SubServers/GalaxiEngine
     *
     * @param args Launch Arguments
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        System.setProperty("jdk.lang.Process.allowAmbiguousCommands", "true");
        System.setProperty("jdk.util.jar.enableMultiRelease", "force");
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        boolean exists = false;
        try {
            exists = Class.forName("net.ME1312.Galaxi.Engine.GalaxiEngine") != null;
        } catch (Throwable e) {}

        if (!exists) {
            System.out.println(">> GalaxiEngine.jar Doesn't Exist");
            System.out.println(">> ");
            System.out.println(">> Please download a build from:");
            System.out.println(">> https://github.com/ME1312/GalaxiEngine/releases");
            System.exit(1);
        } else {
            ExHost.main(args);
        }
    }
}