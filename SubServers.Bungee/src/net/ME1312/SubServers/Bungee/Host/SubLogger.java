package net.ME1312.SubServers.Bungee.Host;

import net.ME1312.SubServers.Bungee.Library.NamedContainer;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

/**
 * SubLogger Layout Class
 */
public abstract class SubLogger {
    /**
     * Log Message Storage Class
     */
    public static class LogMessage {
        private Date date;
        private Level level;
        private String message;

        /**
         * Store a Message
         *
         * @param message Message
         */
        public LogMessage(String message) {
            this.date = Calendar.getInstance().getTime();
            this.level = Level.INFO;
            this.message = message;
        }

        /**
         * Store a Message
         *
         * @param level Log Level
         * @param message Message
         */
        public LogMessage(Level level, String message) {
            this.date = Calendar.getInstance().getTime();
            this.level = level;
            this.message = message;
        }

        /**
         * Store a Message
         *
         * @param date Date
         * @param level Log Level
         * @param message Message
         */
        public LogMessage(Date date, Level level, String message) {
            this.date = date;
            this.level = level;
            this.message = message;
        }

        /**
         * Get the date this message was logged
         *
         * @return Date
         */
        public Date getDate() {
            return date;
        }

        /**
         * Get the level this message was logged on
         *
         * @return Log Level
         */
        public Level getLevel() {
            return level;
        }

        /**
         * Get the message
         *
         * @return Message
         */
        public String getMessage() {
            return message;
        }
    }


    /**
     * Gets the Name of the task logging
     *
     * @return Log Task Name
     */
    public abstract String getName();

    /**
     * Gets the Object using this Logger
     *
     * @return Object
     */
    public abstract Object getHandler();

    /**
     * Start Logger
     */
    public abstract void start();

    /**
     * Stop Logger
     */
    public abstract void stop();

    /**
     * Get if the Logger is currently logging
     *
     * @return Logging Status
     */
    public abstract boolean isLogging();

    /**
     * Gets a list of all the messages logged by this logger
     *
     * @return Log Messages (named by log level)
     */
    public abstract List<LogMessage> getMessages();

    /**
     * Register Filter
     *
     * @param filter Filter
     */
    public abstract void registerFilter(SubLogFilter filter);

    /**
     * Unregister Filter
     *
     * @param filter Filter
     */
    public abstract void unregisterFilter(SubLogFilter filter);
}
