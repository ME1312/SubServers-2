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
import net.ME1312.SubServers.Host.Library.TextColor;
import net.ME1312.SubServers.Host.Library.Util;
import org.fusesource.jansi.Ansi;
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
        ConsoleReader console = new ConsoleReader(System.in, (System.getProperty("subservers.host.log.color", "true").equalsIgnoreCase("true"))?AnsiConsole.out:System.out);
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
        AQUA(TextColor.AQUA, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.CYAN).bold().toString()),
        BLACK(TextColor.BLACK, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLACK).boldOff().toString()),
        BLUE(TextColor.BLUE, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLUE).bold().toString()),
        BOLD(TextColor.BOLD, Ansi.ansi().a(Ansi.Attribute.UNDERLINE_DOUBLE).toString()),
        DARK_AQUA(TextColor.DARK_AQUA, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.CYAN).boldOff().toString()),
        DARK_BLUE(TextColor.DARK_BLUE, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLUE).boldOff().toString()),
        DARK_GRAY(TextColor.DARK_GRAY, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLACK).bold().toString()),
        DARK_GREEN(TextColor.DARK_GREEN, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.GREEN).boldOff().toString()),
        DARK_PURPLE(TextColor.DARK_PURPLE, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.MAGENTA).boldOff().toString()),
        DARK_RED(TextColor.DARK_RED, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.RED).boldOff().toString()),
        GOLD(TextColor.GOLD, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.YELLOW).boldOff().toString()),
        GRAY(TextColor.GRAY, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.WHITE).boldOff().toString()),
        GREEN(TextColor.GREEN, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.GREEN).bold().toString()),
        ITALIC(TextColor.ITALIC, Ansi.ansi().a(Ansi.Attribute.ITALIC).toString()),
        LIGHT_PURPLE(TextColor.LIGHT_PURPLE, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.MAGENTA).bold().toString()),
        MAGIC(TextColor.MAGIC, Ansi.ansi().a(Ansi.Attribute.BLINK_SLOW).toString()),
        RED(TextColor.RED, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.RED).bold().toString()),
        RESET(TextColor.RESET, Ansi.ansi().a(Ansi.Attribute.RESET).toString()),
        STRIKETHROUGH(TextColor.STRIKETHROUGH, Ansi.ansi().a(Ansi.Attribute.STRIKETHROUGH_ON).toString()),
        UNDERLINE(TextColor.UNDERLINE, Ansi.ansi().a(Ansi.Attribute.UNDERLINE).toString()),
        WHITE(TextColor.WHITE, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.WHITE).bold().toString()),
        YELLOW(TextColor.YELLOW, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.YELLOW).bold().toString());

        private final TextColor color;
        private final String value;

        ConsoleColor(TextColor color, String value) {
            this.color = color;
            this.value = value;
        }

        @Override
        public String toString() {
            return getConsoleString();
        }

        public String getValue() {
            return color.getValue();
        }

        public String getConsoleString() {
            return value;
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