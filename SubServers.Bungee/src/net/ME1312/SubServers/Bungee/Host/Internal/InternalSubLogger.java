package net.ME1312.SubServers.Bungee.Host.Internal;

import net.ME1312.SubServers.Bungee.Library.Container;
import net.md_5.bungee.api.ProxyServer;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Internal Process Logger Class
 */
public class InternalSubLogger {
    private Process process;
    private String name;
    private Container<Boolean> log;
    private File file;
    private PrintWriter writer = null;
    private boolean started = false;
    private Thread out = null;
    private Thread err = null;

    /**
     * Creates a new Internal Process Logger
     *
     * @param process Process
     * @param name Prefix
     * @param log Console Logging Status
     * @param file File to log to (or null for disabled)
     */
    public InternalSubLogger(Process process, String name, Container<Boolean> log, File file) {
        this.process = process;
        this.name = name;
        this.log = log;
        this.file = file;
    }

    /**
     * Start the Logger
     */
    public void start() {
        started = true;
        if (file != null && writer == null) {
            try {
                this.writer = new PrintWriter(file, "UTF-8");
                this.writer.println("---------- LOG START \u2014 " + name + " ----------");
                this.writer.flush();
            } catch (UnsupportedEncodingException | FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        if (out == null) (out = new Thread(() -> start(process.getInputStream(), false))).start();
        if (err == null) (err = new Thread(() -> start(process.getErrorStream(), true))).start();
    }

    private void start(InputStream in, boolean isErr) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith(">")) {
                    if (log.get()) {
                        String msg = line;

                        // REGEX Formatting
                        String type = "INFO";
                        Matcher matcher = Pattern.compile("^((?:\\s*\\[?([0-9]{2}:[0-9]{2}:[0-9]{2})]?)?[\\s\\/\\\\\\|]*(?:\\[|\\[.*\\/)?(MESSAGE|INFO|WARN|WARNING|ERROR|ERR|SEVERE)\\]?:?\\s*)").matcher(msg);
                        while (matcher.find()) {
                            type = matcher.group(3).toUpperCase();
                        }

                        msg = msg.replaceAll("^((?:\\s*\\[?([0-9]{2}:[0-9]{2}:[0-9]{2})]?)?[\\s\\/\\\\\\|]*(?:\\[|\\[.*\\/)?(MESSAGE|INFO|WARN|WARNING|ERROR|ERR|SEVERE)\\]?:?\\s*)", "");

                        // Determine LOG LEVEL
                        switch (type) {
                            case "INFO":
                            case "MESSAGE":
                                ProxyServer.getInstance().getLogger().info(name + " > " + msg);
                                break;
                            case "WARNING":
                            case "WARN":
                                ProxyServer.getInstance().getLogger().warning(name + " > " + msg);
                                break;
                            case "SEVERE":
                            case "ERROR":
                            case "ERR":
                                ProxyServer.getInstance().getLogger().severe(name + " > " + msg);
                                break;
                        }
                    }

                    // Log to FILE
                    if (writer != null) {
                        writer.println(line);
                        writer.flush();
                    }
                }
            }
        } catch (IOException e) {} finally {
            if (isErr) {
                err = null;
            } else {
                out = null;
            }

            destroy();
        }
    }

    /**
     * Stop the Logger
     */
    public void stop() {
        if (out != null) out.interrupt();
        if (err != null) err.interrupt();
        destroy();
    }

    private void destroy() {
        if (started) {
            started = false;
            if (writer != null) {
                int l = (int) Math.floor((("---------- LOG START \u2014 " + name + " ----------").length() - 9) / 2);
                String s = "";
                while (s.length() < l) s += '-';
                writer.println(s + " LOG END " + s);
                writer.close();
                writer = null;
            }
        }
    }
}
