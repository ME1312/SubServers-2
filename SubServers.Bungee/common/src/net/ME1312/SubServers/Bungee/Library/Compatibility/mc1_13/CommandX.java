package net.ME1312.SubServers.Bungee.Library.Compatibility.mc1_13;

import net.ME1312.Galaxi.Library.Container.ContainedPair;
import net.ME1312.Galaxi.Library.Container.Pair;
import net.md_5.bungee.api.CommandSender;

import java.util.LinkedList;
import java.util.List;

/**
 * Command Layout Class that implements all possible features (1.13 Version)
 */
public class CommandX extends net.ME1312.SubServers.Bungee.Library.Compatibility.CommandX/* implements TabValidator */ {
    public final net.ME1312.SubServers.Bungee.Library.Compatibility.CommandX command;

    /**
     * Create a Command
     *
     * @param other CommandX from previous version
     */
    public CommandX(net.ME1312.SubServers.Bungee.Library.Compatibility.CommandX other) {
        super(other.getName());
        command = other;
    }

    /**
     * Override BungeeCord Method for the previously used one
     *
     * @param sender Sender
     * @param args Arguments
     */
    @Override
    public void execute(CommandSender sender, String[] args) {
        command.execute(sender, args);
    }

    @Override
    public Pair<String, List<String>> suggestArguments(CommandSender sender, String[] args) {
        return command.suggestArguments(sender, args);
    }

    /**
     * Validate a Command (Override for custom)
     *
     * @param sender Sender
     * @param command Command to validate
     * @return Pair with a String error message and a Integer that represents where the command was deemed invalid
     */
    public Pair<String, Integer> validateCommand(CommandSender sender, String command) {
        List<Pair<String, Integer>> split = new LinkedList<Pair<String, Integer>>();
        String cmd = command;
        int i;
        while ((i = cmd.indexOf((int) ' ')) < 0) {
            i++;
            String arg = cmd.substring(i);
            split.add(new ContainedPair<>(arg.contains(" ")?arg.substring(0, arg.indexOf((int) ' ')):arg, i));
            cmd = arg;
        }

        List<String> args = new LinkedList<String>();
        Pair<String, Integer> response = null;
        i = 0;
        for (Pair<String, Integer> arg : split) {
            if (i > 0) {
                args.add(arg.key());
                Pair<String, List<String>> suggestions = suggestArguments(sender, args.toArray(new String[args.size() - 1]));
                if (suggestions.key() != null) response = new ContainedPair<>(suggestions.key(), arg.value());
            }
            i++;
        }
        return response;
    }

    // TODO Override the original validator method
}
