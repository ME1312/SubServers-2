package net.ME1312.SubServers.Host;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilderFactory;

import jline.console.ConsoleReader;
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
            for (File mod : Arrays.asList(pldir.listFiles())) {
                try {
                    boolean success = false;
                    if (getFileExtension(mod.getName()).equalsIgnoreCase("zip")) {
                        extractZip(mod, tmpdir);
                        success = true;
                    } else if (getFileExtension(mod.getName()).equalsIgnoreCase("jar")) {
                        extractJar(mod, tmpdir);
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
                    if (success) System.out.println(">> Extracted ~/plugins/" + mod.getName());
                } catch (Exception e) {
                    System.out.println(">> Couldn't extract ~/plugins/" + mod.getName());
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
        try {
            String line;
            new Thread(() -> {
                try {
                    String line1;
                    ConsoleReader console = new ConsoleReader();
                    BufferedWriter cmd = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
                    while (process.isAlive() && (line1 = console.readLine()) != null) {
                        if (line1.equals("")) continue;
                        cmd.write(line1);
                        cmd.newLine();
                        cmd.flush();
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
            BufferedReader obr = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while (process.isAlive() && (line = obr.readLine()) != null) {
                System.out.println(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    private static void extractZip(File zipFile, File dir) {
        byte[] buffer = new byte[1024];
        try {
            ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
            ZipEntry entry = zis.getNextEntry();
            ArrayList<ZipEntry> entries = new ArrayList<ZipEntry>();
            while (entry != null) {
                entries.add(entry);
                entry = zis.getNextEntry();
            }
            for (ZipEntry ze : entries) {
                int len;
                File newFile = new File(dir, ze.getName());
                if (newFile.exists()) {
                    continue;
                }
                if (ze.isDirectory()) {
                    newFile.mkdirs();
                    continue;
                }
                if (!newFile.getParentFile().exists()) {
                    newFile.getParentFile().mkdirs();
                }
                FileOutputStream fos = new FileOutputStream(newFile);
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zis.closeEntry();
            zis.close();
        }
        catch (IOException ex) {
            ex.printStackTrace();
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
}