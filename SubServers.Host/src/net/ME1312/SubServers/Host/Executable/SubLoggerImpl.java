package net.ME1312.SubServers.Host.Executable;

import net.ME1312.Galaxi.Library.Container.ContainedPair;
import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.Galaxi.Library.Container.Value;
import net.ME1312.Galaxi.Library.Try;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Log.LogStream;
import net.ME1312.Galaxi.Log.Logger;
import net.ME1312.SubData.Client.DataClient;
import net.ME1312.SubData.Client.DataSender;
import net.ME1312.SubData.Client.Library.DisconnectReason;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubData.Client.SubDataSender;
import net.ME1312.SubServers.Host.ExHost;
import net.ME1312.SubServers.Host.Library.TextColor;
import net.ME1312.SubServers.Host.Network.Packet.PacketOutExLogMessage;
import net.ME1312.SubServers.Host.SubAPI;

import java.io.*;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
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
    private LinkedList<Pair<Byte, String>> ccache = null;
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

        logger.addFilter((stream, message) -> {
            // Log to NETWORK
            if (logn) {
                if (this.address != null && channel != null && !channel.isClosed()) {
                    flushCache(channel);
                    channel.sendPacket(new PacketOutExLogMessage(this.address, stream.getLevel().getID(), message));
                } else {
                    if (ccache == null) ccache = new LinkedList<Pair<Byte, String>>();
                    ccache.add(new ContainedPair<>(stream.getLevel().getID(), message));
                }
            }

            // Log to CONSOLE
            return logc || !started;
        });
    }

    private void flushCache(DataSender sender) {
        if (ccache != null) {
            SubDataSender channel = (SubDataSender) sender;
            for (Pair<Byte, String> val : ccache) channel.sendPacket(new PacketOutExLogMessage(this.address, val.key(), val.value()));
            ccache = null;
        }
    }

    @SuppressWarnings("deprecation")
    void init() {
        if (logn) Try.all.run(() -> {
            Process process = this.process;
            ExHost host = SubAPI.getInstance().getInternals();
            channel = (SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0].newChannel();
            channel.on.closed(new Consumer<Pair<DisconnectReason, DataClient>>() {
                @Override
                public void accept(Pair<DisconnectReason, DataClient> client) {
                    if (started && SubLoggerImpl.this.process != null && process == SubLoggerImpl.this.process && process.isAlive()) {
                        int reconnect = host.config.get().getMap("Settings").getMap("SubData").getInt("Reconnect", 60);
                        if (Try.all.get(() -> Util.reflect(ExHost.class.getDeclaredField("reconnect"), host), false) && reconnect > 0
                                && client.key() != DisconnectReason.PROTOCOL_MISMATCH && client.key() != DisconnectReason.ENCRYPTION_MISMATCH) {
                            Timer timer = new Timer(SubAPI.getInstance().getAppInfo().getName() + "::Log_Reconnect_Handler");
                            Consumer<Pair<DisconnectReason, DataClient>> run = this;
                            reconnect++;
                            timer.scheduleAtFixedRate(new TimerTask() {
                                @Override
                                public void run() {
                                    if (!started || SubLoggerImpl.this.process == null || process != SubLoggerImpl.this.process || !process.isAlive()) {
                                        timer.cancel();
                                    } else try {
                                        SubDataClient open = (SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0].newChannel();
                                        open.on.ready(SubLoggerImpl.this::flushCache);
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
    }

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
        if (level == null) level = logger.info;
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

    private LogStream level;
    private static final String PATTERN = "^((?:\\s*\\[?([0-9]{2}:[0-9]{2}:[0-9]{2})]?)?[\\s\\/\\\\\\|]*(?:\\[|\\[.*\\/)?(MESSAGE|MSG|" + Pattern.quote(Level.INFO.getLocalizedName()) + "|INFO|" + Pattern.quote(Level.WARNING.getLocalizedName()) + "|WARNING|WARN|ERROR|ERR|" + Pattern.quote(Level.SEVERE.getLocalizedName()) + "|SEVERE)\\]?(?::|\\s*>)?\\s*)";
    private void log(String line) {
        if (!line.startsWith(">")) {
            String msg = line;

            // REGEX Formatting
            String type = null;
            Matcher matcher = Pattern.compile(PATTERN).matcher(msg.replaceAll("\u001B\\[[;\\d]*m", ""));
            if (matcher.find()) {
                type = matcher.group(3).toUpperCase();
            }

            msg = msg.replaceAll(PATTERN, "");

            // Determine LOG LEVEL
            if (type != null) {
                if (type.equalsIgnoreCase(Level.INFO.getLocalizedName())) {
                    level = logger.info;
                } else if (type.equalsIgnoreCase(Level.WARNING.getLocalizedName())) {
                    level = logger.warn;
                } else if (type.equalsIgnoreCase(Level.SEVERE.getLocalizedName())) {
                    level = logger.severe;
                } else switch (type) {
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
                    case "MSG":
                    case "MESSAGE":
                        level = logger.message;
                        break;
                    default:
                        level = logger.info;
                }
            }

            // Log to FILTER
            if (log.value()) level.println(TextColor.convertColor(msg));

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
            level = null;

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
            }
        } catch (NullPointerException e) {}
    }

    void destroy() {
        if (channel != null && !channel.isClosed()) {
            channel.sendPacket(new PacketOutExLogMessage(address));
        }
        channel = null;
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
