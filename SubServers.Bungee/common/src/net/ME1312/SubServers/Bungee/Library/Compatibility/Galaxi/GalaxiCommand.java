package net.ME1312.SubServers.Bungee.Library.Compatibility.Galaxi;

import net.ME1312.Galaxi.Library.Util;

import net.md_5.bungee.api.plugin.Command;

/**
 * Galaxi Command Compatibility Class
 */
public class GalaxiCommand {

    /**
     * Group similar Commands
     *
     * @param commands Command Classes
     */
    @SafeVarargs
    public static void group(Class<? extends Command>... commands) {
        Util.isException(() -> Util.reflect(GalaxiCommandWrapper.class.getDeclaredConstructor(Class[].class), (Object) commands));
    }

    /**
     * Set the Description of a Command
     *
     * @param command Command
     * @param value Value
     * @return The Command
     */
    public static Command description(Command command, String value) {
        Util.isException(() -> Class.forName("net.ME1312.Galaxi.Command.Command").getMethod("description", String.class).invoke(command, value));
        return command;
    }

    /**
     * Set the Help Page for a Command
     *
     * @param command Command
     * @param lines Help Page Lines
     * @return The Command
     */
    public static Command help(Command command, String... lines) {
        Util.isException(() -> Class.forName("net.ME1312.Galaxi.Command.Command").getMethod("help", String[].class).invoke(command, (Object) lines));
        return command;
    }

    /**
     * Set the Usage of a Command
     *
     * @param command Command
     * @param args Argument Placeholders
     * @return The Command
     */
    public static Command usage(Command command, String... args) {
        Util.isException(() -> Class.forName("net.ME1312.Galaxi.Command.Command").getMethod("usage", String[].class).invoke(command, (Object) args));
        return command;
    }

}
