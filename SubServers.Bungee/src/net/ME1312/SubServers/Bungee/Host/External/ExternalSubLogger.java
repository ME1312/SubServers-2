package net.ME1312.SubServers.Bungee.Host.External;

import net.ME1312.Galaxi.Library.Container.Value;
import net.ME1312.Galaxi.Library.Try;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubServers.Bungee.Host.SubLogFilter;
import net.ME1312.SubServers.Bungee.Host.SubLogger;
import net.ME1312.SubServers.Bungee.Library.Compatibility.Logger;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketInExLogMessage;
import net.ME1312.SubServers.Bungee.SubAPI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

/**
 * External Process Logger Class
 */
public class ExternalSubLogger extends SubLogger {
    private Object handle;
    UUID id = null;
    String name;
    Value<Boolean> log;
    private List<SubLogFilter> filters = new CopyOnWriteArrayList<>();
    File file;
    private PrintWriter writer = null;
    private boolean started = false;

    /**
     * Creates a new External Logger
     *
     * @param user Object using this logger (or null)
     * @param name Prefix
     * @param log Console Logging Status
     * @param file File to log to (or null for disabled)
     */
    ExternalSubLogger(Object user, String name, Value<Boolean> log, File file) {
        this.handle = user;
        this.name = name;
        this.log = log;
        this.file = file;
    }

    @Override
    public void start() {
        id = PacketInExLogMessage.register(this);
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
        List<SubLogFilter> filters = new ArrayList<SubLogFilter>();
        filters.addAll(this.filters);
        for (SubLogFilter filter : filters) try {
            filter.start();
        } catch (Throwable e) {
            new InvocationTargetException(e, "Exception while running SubLogger Event").printStackTrace();
        }
    }

    @SuppressWarnings("deprecation")
    private void log(int type, String msg) {
        if (started) {
            Level level;

            // Determine LOG LEVEL
            switch (type) {
                case 80:
                    level = Level.FINE;
                    break;
                case 40:
                    level = Level.WARNING;
                    break;
                case 30:
                case 20:
                    level = Level.SEVERE;
                    break;
                default:
                    level = Level.INFO;
            }

            // Filter Message
            boolean allow = (SubAPI.getInstance().getInternals().sudo == getHandler() && SubAPI.getInstance().getInternals().canSudo) || (log.value() && (SubAPI.getInstance().getInternals().sudo == null || !SubAPI.getInstance().getInternals().canSudo));
            List<SubLogFilter> filters = new ArrayList<SubLogFilter>();
            filters.addAll(this.filters);
            for (SubLogFilter filter : filters)
                try {
                    allow = (filter.log(level, msg) && allow);
                } catch (Throwable e) {
                    new InvocationTargetException(e, "Exception while running SubLogger Event").printStackTrace();
                }

            // Log to CONSOLE
            if (allow) Logger.get(name).log(level, msg);

            // Log to FILE
            if (writer != null) {
                writer.println('[' + new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()) + "] [" + level + "] > " + msg);
                writer.flush();
            }
        }
    }

    /**
     * Get the External Logger Address
     *
     * @return External Address
     */
    public UUID getExternalAddress() {
        return id;
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
    public void stop() {
        if (started) {
            id = null;
            started = false;
            List<SubLogFilter> filters = new ArrayList<SubLogFilter>();
            filters.addAll(this.filters);
            for (SubLogFilter filter : filters) try {
                filter.stop();
            } catch (Throwable e) {
                new InvocationTargetException(e, "Exception while running SubLogger Event").printStackTrace();
            }
            if (writer != null) {
                PrintWriter writer = this.writer;
                this.writer = null;
                int l = (("---------- LOG START \u2014 " + name + " ----------").length() - 9) / 2;
                String s = "";
                while (s.length() < l) s += '-';
                writer.println(s + " LOG END " + s);
                writer.close();
            }
        }
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
