package net.ME1312.SubServers.Host.Executable;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.Galaxi.Library.Container.Value;
import net.ME1312.Galaxi.Library.Log.LogStream;
import net.ME1312.Galaxi.Library.Log.Logger;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.DataClient;
import net.ME1312.SubData.Client.Library.DisconnectReason;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Host.ExHost;
import net.ME1312.SubServers.Host.Library.TextColor;
import net.ME1312.SubServers.Host.Network.Packet.PacketOutExLogMessage;
import net.ME1312.SubServers.Host.SubAPI;

import java.io.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Internal Process Logger Class
 */
public class SubLoggerImpl {
    Process process;
    private Object handle;
    final Logger logger;
    final String name;
    UUID address;
    Value<Boolean> log;
    static boolean logn = true;
    static boolean logc = true;
    File file;
    private SubDataClient channel = null;
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
    SubLoggerImpl(Process process, Object user, String name, UUID address, Value<Boolean> log, File file) {
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
            } catch (IOException e) {
                logger.error.println(e);
            }
        }
        Process process = this.process;
        ExHost host = SubAPI.getInstance().getInternals();
        if (logn) Util.isException(() -> {
            channel = (SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0].openChannel();
            channel.on.closed(new Callback<Pair<DisconnectReason, DataClient>>() {
                @Override
                public void run(Pair<DisconnectReason, DataClient> client) {
                    if (started && SubLoggerImpl.this.process != null && process == SubLoggerImpl.this.process && process.isAlive()) {
                        int reconnect = host.config.get().getMap("Settings").getMap("SubData").getInt("Reconnect", 60);
                        if (Util.getDespiteException(() -> Util.reflect(ExHost.class.getDeclaredField("reconnect"), host), false) && reconnect > 0
                                && client.key() != DisconnectReason.PROTOCOL_MISMATCH && client.key() != DisconnectReason.ENCRYPTION_MISMATCH) {
                            Timer timer = new Timer(SubAPI.getInstance().getAppInfo().getName() + "::Log_Reconnect_Handler");
                            Callback<Pair<DisconnectReason, DataClient>> run = this;
                            reconnect++;
                            timer.scheduleAtFixedRate(new TimerTask() {
                                @Override
                                public void run() {
                                    if (!started || SubLoggerImpl.this.process == null || process != SubLoggerImpl.this.process || !process.isAlive()) {
                                        timer.cancel();
                                    } else try {
                                        SubDataClient open = (SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0].openChannel();
                                        open.on.closed(run);
                                        channel = open;
                                        timer.cancel();
                                    } catch (NullPointerException | IOException e) {}
                                }
                            }, TimeUnit.SECONDS.toMillis(reconnect), TimeUnit.SECONDS.toMillis(reconnect));
                        }
                    }
                }
            });
        });
        if (out == null) (out = new Thread(() -> start(process.getInputStream(), false), SubAPI.getInstance().getAppInfo().getName() + "::Log_Spooler(" + name + ')')).start();
        if (err == null) (err = new Thread(() -> start(process.getErrorStream(), true), SubAPI.getInstance().getAppInfo().getName() + "::Error_Spooler(" + name + ')')).start();
    }


    @SuppressWarnings("deprecation")
    private void start(InputStream in, boolean isErr) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = br.readLine()) != null) {
                log(line);
            }
        } catch (IOException e) {} finally {
            if (isErr) {
                err = null;
            } else {
                out = null;
            }

            stop();
        }
    }

    private void log(String line) {
        if (!line.startsWith(">")) {
            String msg = line;
            LogStream level;

            // REGEX Formatting
            String type = "";
            Matcher matcher = Pattern.compile("^((?:\\s*\\[?([0-9]{2}:[0-9]{2}:[0-9]{2})]?)?[\\s\\/\\\\\\|]*(?:\\[|\\[.*\\/)?(MESSAGE|INFO|WARNING|WARN|ERROR|ERR|SEVERE)\\]?:?(?:\\s*>)?\\s*)").matcher(msg.replaceAll("\u001B\\[[;\\d]*m", ""));
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
            if (log.value() && channel != null && !channel.isClosed()) channel.sendPacket(new PacketOutExLogMessage(address, line));

            // Log to CONSOLE
            if (log.value() && logc) level.println(TextColor.convertColor(msg));

            // Log to FILE
            if (writer != null) {
                writer.println(line);
                writer.flush();
            }
        }
    }

    /**
     * Stop Logger
     */
    public void stop() {
        try {
            if (out != null) out.interrupt();
            if (err != null) err.interrupt();
            destroy();
        } catch (NullPointerException e) {}
    }

    private void destroy() {
        if (started) {
            started = false;
            if (writer != null) {
                PrintWriter writer = this.writer;
                this.writer = null;
                int l = (int) Math.floor((("---------- LOG START \u2014 " + name + " ----------").length() - 9) / 2);
                String s = "";
                while (s.length() < l) s += '-';
                if (writer != null) {
                    writer.println(s + " LOG END " + s);
                    writer.close();
                }
            }
            if (channel != null && !channel.isClosed()) {
                channel.sendPacket(new PacketOutExLogMessage(address, true));
            }
            channel = null;
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
        return log.value();
    }

    /**
     * Get the Logging Address
     *
     * @return Address
     */
    public UUID getAddress() {
        return address;
    }
}
