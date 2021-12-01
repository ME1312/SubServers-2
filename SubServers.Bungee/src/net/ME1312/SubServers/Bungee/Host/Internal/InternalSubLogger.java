package net.ME1312.SubServers.Bungee.Host.Internal;

import net.ME1312.Galaxi.Library.Container.Value;
import net.ME1312.Galaxi.Library.Try;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubServers.Bungee.Host.SubLogFilter;
import net.ME1312.SubServers.Bungee.Host.SubLogger;
import net.ME1312.SubServers.Bungee.Library.Compatibility.Logger;
import net.ME1312.SubServers.Bungee.SubAPI;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Internal Process Logger Class
 */
public class InternalSubLogger extends SubLogger {
    Process process;
    private Object handle;
    String name;
    Value<Boolean> log;
    private List<SubLogFilter> filters = new CopyOnWriteArrayList<>();
    File file;
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
     * @param log Console Logging Status
     * @param file File to log to (or null for disabled)
     */
    InternalSubLogger(Process process, Object user, String name, Value<Boolean> log, File file) {
        this.process = process;
        this.handle = user;
        this.name = name;
        this.log = log;
        this.file = file;
    }

    void init() {
        List<SubLogFilter> filters = new ArrayList<SubLogFilter>();
        filters.addAll(this.filters);
        for (SubLogFilter filter : filters) try {
            filter.start();
        } catch (Throwable e) {
            new InvocationTargetException(e, "Exception while running SubLogger Event").printStackTrace();
        }
    }

    @Override
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
        if (out == null) (out = new Thread(() -> start(process.getInputStream(), false), "SubServers.Bungee::Internal_Log_Spooler(" + name + ')')).start();
        if (err == null) (err = new Thread(() -> start(process.getErrorStream(), true), "SubServers.Bungee::Internal_Error_Spooler(" + name + ')')).start();
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

    private Level level = Level.INFO;
    private static final String PATTERN = "^((?:\\s*\\[?([0-9]{2}:[0-9]{2}:[0-9]{2})]?)?[\\s\\/\\\\\\|]*(?:\\[|\\[.*\\/)?(DEBUG|MESSAGE|MSG|" + Pattern.quote(Level.INFO.getLocalizedName()) + "|INFO|" + Pattern.quote(Level.WARNING.getLocalizedName()) + "|WARNING|WARN|ERROR|ERR|" + Pattern.quote(Level.SEVERE.getLocalizedName()) + "|SEVERE)\\]?(?::|\\s*>)?\\s*)";
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
                    level = Level.INFO;
                } else if (type.equalsIgnoreCase(Level.WARNING.getLocalizedName())) {
                    level = Level.WARNING;
                } else if (type.equalsIgnoreCase(Level.SEVERE.getLocalizedName())) {
                    level = Level.SEVERE;
                } else switch (type) {
                    case "WARNING":
                    case "WARN":
                        level = Level.WARNING;
                        break;
                    case "SEVERE":
                    case "ERROR":
                    case "ERR":
                        level = Level.SEVERE;
                        break;
                    default:
                        level = Level.INFO;
                }
            }

            // Log to FILTER
            log(level, msg);

            // Log to FILE
            if (writer != null) {
                writer.println(line);
                writer.flush();
            }
        }
    }

    void log(Level level, String message) {
        // Filter Message
        boolean allow = (SubAPI.getInstance().getInternals().sudo == getHandler() && SubAPI.getInstance().getInternals().canSudo) || (log.value() && (SubAPI.getInstance().getInternals().sudo == null || !SubAPI.getInstance().getInternals().canSudo));
        List<SubLogFilter> filters = new ArrayList<SubLogFilter>();
        filters.addAll(this.filters);
        for (SubLogFilter filter : filters) {
            try {
                allow = (filter.log(level, message) && allow);
            } catch (Throwable e) {
                new InvocationTargetException(e, "Exception while running SubLogger Event").printStackTrace();
            }
        }

        // Log to CONSOLE
        if (allow || !started) {
            Logger.get(name).log(level, message);
        }
    }

    @Override
    public void stop() {
        try {
            if (out != null) out.interrupt();
            if (err != null) err.interrupt();
            level = Level.INFO;

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
        filters.addAll(this.filters);
        for (SubLogFilter filter : filters) try {
            filter.stop();
        } catch (Throwable e) {
            new InvocationTargetException(e, "Exception while running SubLogger Event").printStackTrace();
        }
    }

    @Override
    public void registerFilter(SubLogFilter filter) {
        Util.nullpo(filter);
        filters.add(filter);
    }

    @Override
    public void unregisterFilter(SubLogFilter filter) {
        Util.nullpo(filter);
        Try.all.run(() -> filters.remove(filter));
    }

    @Override
    public Object getHandler() {
        return handle;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isLogging() {
        return log.value();
    }
}
