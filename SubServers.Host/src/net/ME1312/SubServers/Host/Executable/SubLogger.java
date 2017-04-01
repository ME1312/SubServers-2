package net.ME1312.SubServers.Host.Executable;

import net.ME1312.SubServers.Host.Library.Container;
import net.ME1312.SubServers.Host.Library.Log.LogStream;
import net.ME1312.SubServers.Host.Library.Log.Logger;
import net.ME1312.SubServers.Host.Network.Packet.PacketOutExLogMessage;
import net.ME1312.SubServers.Host.SubAPI;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Internal Process Logger Class
 */
public class SubLogger {
    protected Process process;
    private Object handle;
    private Logger logger;
    private String name;
    protected UUID address;
    protected Container<Boolean> log;
    protected File file;
    private PrintWriter writer = null;
    private boolean started = false;
    private Thread out = null;
    private Thread err = null;

    /**
     * Creates a new Internal Process Logger
     *
     * @param process Process
     * @param user Object using this logger (or null)
     * @param name Prefix
     * @param address External Logger Address
     * @param log Console Logging Status
     * @param file File to log to (or null for disabled)
     */
    protected SubLogger(Process process, Object user, String name, UUID address, Container<Boolean> log, File file) {
        this.process = process;
        this.handle = user;
        this.logger = new Logger(name);
        this.name = name;
        this.address = address;
        this.log = log;
        this.file = file;
    }

    /**
     * Start Logger
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


    @SuppressWarnings("deprecation")
    private void start(InputStream in, boolean isErr) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith(">")) {
                    String msg = line;
                    LogStream level;

                    // REGEX Formatting
                    String type = "";
                    Matcher matcher = Pattern.compile("^((?:\\s*\\[?([0-9]{2}:[0-9]{2}:[0-9]{2})]?)?[\\s\\/\\\\\\|]*(?:\\[|\\[.*\\/)?(MESSAGE|INFO|WARNING|WARN|ERROR|ERR|SEVERE)\\]?:?(?:\\s*>)?\\s*)").matcher(msg);
                    while (matcher.find()) {
                        type = matcher.group(3).toUpperCase();
                    }

                    msg = msg.replaceAll("^((?:\\s*\\[?([0-9]{2}:[0-9]{2}:[0-9]{2})]?)?[\\s\\/\\\\\\|]*(?:\\[|\\[.*\\/)?(MESSAGE|INFO|WARNING|WARN|ERROR|ERR|SEVERE)\\]?:?(?:\\s*>)?\\s*)", "");

                    // Determine LOG LEVEL
                    switch (type) {
                        case "WARNING":
                        case "WARN":
                            level = logger.warn;
                            break;
                        case "SEVERE":
                            level = logger.severe;
                            break;
                        case "ERROR":
                        case "ERR":
                            level = logger.error;
                            break;
                        default:
                            level = logger.info;
                    }

                    // Log to NETWORK
                    if (log.get()) SubAPI.getInstance().getSubDataNetwork().sendPacket(new PacketOutExLogMessage(address, line));

                    // Log to CONSOLE
                    if (log.get() && SubAPI.getInstance().getInternals().config.get().getSection("Settings").getBoolean("Log")) level.println(msg);

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
     * Stop Logger
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

    /**
     * Gets the Object using this Logger
     *
     * @return Object
     */
    public Object getHandler() {
        return handle;
    }

    /**
     * Gets the Name of the task logging
     *
     * @return Log Task Name
     */
    public String getName() {
        return name;
    }

    /**
     * Get if the Logger is currently logging
     *
     * @return Logging Status
     */
    public boolean isLogging() {
        return log.get();
    }
}
