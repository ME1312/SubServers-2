package net.ME1312.SubServers.Bungee.Library.Compatibility;

import net.ME1312.Galaxi.Library.Container.NamedContainer;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.List;

/**
 * Command Layout Class that implements all possible features (Base Version)
 */
public abstract class CommandX extends Command implements TabExecutor {

    /**
     * Create a Command
     *
     * @param name Command Name
     */
    public CommandX(String name) {
        super(name);
    }

    /**
     * Create a Command
     *
     * @param name Command Name
     * @param permission Command Permission
     * @param aliases Command Aliases
     */
    public CommandX(String name, String permission, String... aliases) {
        super(name, permission, aliases);
    }

    /**
     * Suggest Arguments
     *
     * @param sender Sender
     * @param args Arguments (including the final unfinished one)
     * @return An Error Message (if there was one, otherwise null) and a List of Suggestions
     */
    public abstract NamedContainer<String, List<String>> suggestArguments(CommandSender sender, String[] args);

    /**
     * Override the BungeeCord Method of {@link #suggestArguments(CommandSender, String[])}
     *
     * @param sender Sender
     * @param args Arguments (including the final unfinished one)
     * @return A Collection of Suggestions
     */
    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return suggestArguments(sender, args).get();
    }
}
