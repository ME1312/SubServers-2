package net.ME1312.SubServers.Host;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.xml.parsers.DocumentBuilderFactory;

import jline.console.ConsoleReader;
import jline.console.CursorBuffer;
import net.ME1312.SubServers.Host.Library.Util;
import org.fusesource.jansi.AnsiConsole;
import org.w3c.dom.NodeList;

/**
 * SubServers.Host Launcher Class
 */
public final class Launch {

    /**
     * Prepare and launch SubServers.Host
     *
     * @param args Args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        String plugins = "";
        File rtdir = new File(System.getProperty("user.dir"));
        File tmpdir = File.createTempFile("SubServers.Host.", ".jar");
        File pldir = new File(rtdir, "Plugins");
        tmpdir.delete();
        tmpdir.mkdir();
        System.out.println(">> Created " + tmpdir.getPath().replace(File.separator, "/"));
        extractJar(getCodeSourceLocation(), tmpdir);
        System.out.println(">> Extracted ~/" + getCodeSourceLocation().getName());
        if (pldir.isDirectory() && pldir.listFiles().length > 0) {
            for (File plugin : Arrays.asList(pldir.listFiles())) {
                try {
                    boolean success = false;
                    if (getFileExtension(plugin.getName()).equalsIgnoreCase("zip")) {
                        Util.unzip(new FileInputStream(plugin), tmpdir);
                        success = true;
                    } else if (getFileExtension(plugin.getName()).equalsIgnoreCase("jar")) {
                        extractJar(plugin, tmpdir);
                        success = true;
                    }
                    if (new File(tmpdir, "package.xml").exists()) {
                        NodeList xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(tmpdir, "package.xml")).getElementsByTagName("class");
                        if (xml.getLength() > 0) {
                            for (int i = 0; i < xml.getLength(); i++) {
                                plugins += ((plugins.length() == 0)?"":" ") + xml.item(i).getTextContent().replace(' ', '_');
                            }
                        }
                        new File(tmpdir, "package.xml").delete();
                    }
                    if (success) System.out.println(">> Extracted ~/plugins/" + plugin.getName());
                } catch (Exception e) {
                    System.out.println(">> Couldn't extract ~/plugins/" + plugin.getName());
                    e.printStackTrace();
                }
            }
        }
        ArrayList<String> arguments = new ArrayList<String>();
        String javaPath = String.valueOf(System.getProperty("java.home")) + File.separator + "bin" + File.separator + "java";
        arguments.add(javaPath);
        arguments.addAll(getVmArgs());
        arguments.add("-Dsubservers.host.runtime=" + URLEncoder.encode(tmpdir.getPath(), "UTF-8"));
        if (!plugins.equals(""))
            arguments.add("-Dsubservers.host.plugins=" + URLEncoder.encode(plugins, "UTF-8"));
        arguments.add("-cp");
        arguments.add(tmpdir.getPath());
        arguments.add("net.ME1312.SubServers.Host.ExHost");
        arguments.addAll(Arrays.asList(args));
        ProcessBuilder processBuilder = new ProcessBuilder(arguments);
        processBuilder.directory(new File(System.getProperty("user.dir")));
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        syncConsole(process);
        System.out.println(">> Cleaning up");
        deleteDir(tmpdir);
        System.exit(process.exitValue());
    }

    private static void syncConsole(final Process process) throws Exception {
        AnsiConsole.systemInstall();
        ConsoleReader console = new ConsoleReader();
        console.setExpandEvents(false);
        try {
            new Thread(() -> {
                try {
                    String line;
                    BufferedWriter cmd = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
                    while (process.isAlive() && (line = console.readLine(">")) != null) {
                        if (line.equals("")) continue;
                        cmd.write(line);
                        cmd.newLine();
                        cmd.flush();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
            new Thread(() -> {
                try {
                    String line;
                    BufferedReader obr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                    while (process.isAlive() && (line = obr.readLine()) != null) {
                        stashLine(console);
                        if (System.getProperty("subservers.host.log.color", "true").equalsIgnoreCase("true")) {
                            console.println(ConsoleColor.parseColor(line) + ConsoleColor.RESET);
                        } else {
                            console.println(ConsoleColor.stripColor(line.replaceAll("\u001B\\[[;\\d]*m", "")));
                        }
                        unstashLine(console);
                        console.flush();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
            String line;
            BufferedReader obr = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while (process.isAlive() && (line = obr.readLine()) != null) {
                stashLine(console);
                if (System.getProperty("subservers.host.log.color", "true").equalsIgnoreCase("true")) {
                    console.println(ConsoleColor.parseColor(line) + ConsoleColor.RESET);
                } else {
                   console.println(ConsoleColor.stripColor(line.replaceAll("\u001B\\[[;\\d]*m", "")));
                }
                unstashLine(console);
                console.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        stashLine(console);
        AnsiConsole.systemUninstall();
    }

    private static void extractJar(File jarFile, File dir) throws Exception {
        JarFile jar = new JarFile(jarFile);
        Enumeration<JarEntry> files = jar.entries();
        ArrayList<JarEntry> entries = new ArrayList<JarEntry>();
        while (files.hasMoreElements()) {
            entries.add(files.nextElement());
        }
        for (JarEntry file : entries) {
            File f = new File(dir, file.getName());
            if (f.exists()) {
                continue;
            }
            if (file.isDirectory()) {
                f.mkdirs();
                continue;
            }
            if (!f.getParentFile().exists()) {
                f.getParentFile().mkdirs();
            }
            InputStream is = jar.getInputStream(file);
            FileOutputStream fos = new FileOutputStream(f);
            while (is.available() > 0) {
                fos.write(is.read());
            }
            fos.close();
            is.close();
        }
    }

    private static void deleteDir(File f) {
        if (f.isDirectory()) {
            for (File c : f.listFiles()) {
                deleteDir(c);
            }
        }
        f.delete();
    }

    private static List<String> getVmArgs() {
        ArrayList<String> values = new ArrayList<String>();
        values.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());
        return values;
    }

    private static File getCodeSourceLocation() {
        try {
            return new File(Launch.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String getFileExtension(String fileName) {
        String extension = "";
        int i = fileName.lastIndexOf(46);
        if (i >= 0) {
            extension = fileName.substring(i + 1);
        }
        return extension;
    }

    private static CursorBuffer stashed;
    private static void stashLine(ConsoleReader console) {
        stashed = console.getCursorBuffer().copy();
        try {
            console.getOutput().write("\u001b[1G\u001b[K");
            console.flush();
        } catch (IOException e) {
            // ignore
        }
    }

    private static void unstashLine(ConsoleReader console) {
        try {
            console.resetPromptLine(console.getPrompt(),
                    stashed.toString(), stashed.cursor);
        } catch (IOException e) {
            // ignore
        }
    }

    private enum ConsoleColor {
        AQUA('b', "[0;36;1m"),
        BLACK('0', "[0;30;22m"),
        BLUE('9', "[0;34;1m"),
        BOLD('l', "[21m"),
        DARK_AQUA('3', "[0;36;22m"),
        DARK_BLUE('1', "[0;34;22m"),
        DARK_GRAY('8', "[0;30;1m"),
        DARK_GREEN('2', "[0;32;22m"),
        DARK_PURPLE('5', "[0;35;22m"),
        DARK_RED('4', "[0;31;22m"),
        GOLD('6', "[0;33;22m"),
        GRAY('7', "[0;37;22m"),
        GREEN('a', "[0;32;1m"),
        ITALIC('o', "[3m"),
        LIGHT_PURPLE('d', "[0;35;1m"),
        MAGIC('k', "[5m"),
        RED('c', "[0;31;1m"),
        RESET('r', "[m"),
        STRIKETHROUGH('m', "[9m"),
        UNDERLINE('n', "[4m"),
        WHITE('f', "[0;37;1m"),
        YELLOW('e', "[0;33;1m");

        private final Character color;
        private final String value;

        ConsoleColor(Character color, String value) {
            this.color = color;
            this.value = value;
        }

        @Override
        public String toString() {
            return getConsoleString();
        }

        public String getValue() {
            return "\u00A7" + color;
        }

        public String getConsoleString() {
            return "\u001B" + value;
        }

        public static String parseColor(String str) {
            for (ConsoleColor color : Arrays.asList(ConsoleColor.values())) {
                str = str.replace(color.getValue(), color.getConsoleString());
            }
            return str;
        }

        public static String stripColor(String str) {
            for (ConsoleColor color : Arrays.asList(ConsoleColor.values())) {
                str = str.replace(color.getValue(), "");
            }
            return str;
        }
    }
}