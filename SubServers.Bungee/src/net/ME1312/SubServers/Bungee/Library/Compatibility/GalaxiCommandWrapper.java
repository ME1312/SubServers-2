package net.ME1312.SubServers.Bungee.Library.Compatibility;

import net.ME1312.Galaxi.Galaxi;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Plugin.Command.Command;
import net.ME1312.Galaxi.Plugin.Command.CommandSender;
import net.ME1312.Galaxi.Plugin.Command.CompletionHandler;
import net.ME1312.Galaxi.Plugin.PluginManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class GalaxiCommandWrapper extends Command implements CompletionHandler {
    private HashMap<String, Command> forwards = new HashMap<String, Command>();
    private Command data;

    @SafeVarargs
    GalaxiCommandWrapper(Class<? extends Command>... commands) {
        super(Galaxi.getInstance().getAppInfo());

        Map<String, Command> registered = Util.getDespiteException(() -> Util.reflect(PluginManager.class.getDeclaredField("commands"), Galaxi.getInstance().getPluginManager()), null);
        ArrayList<String> tmp = new ArrayList<String>();
        tmp.addAll(registered.keySet());
        for (String alias : tmp) {
            Command command = registered.get(alias);
            for (Class<? extends Command> type : commands) {
                if (type.isInstance(command)) {
                    forwards.put(alias, command);
                    if (data == null) data = command;
                    registered.remove(alias);
                    break;
                }
            }
        }

        register(forwards.keySet().toArray(new String[0]));
    }

    @Override
    public void command(CommandSender sender, String label, String[] args) {
        if (forwards.keySet().contains(label.toLowerCase())) {
            forwards.get(label.toLowerCase()).command(sender, label, args);
        } else {
            throw new IllegalStateException("Command label not recognised in group: " + forwards.keySet());
        }
    }

    @Override
    public String[] complete(CommandSender sender, String label, String[] args) {
        if (forwards.keySet().contains(label.toLowerCase())) {
            Command command = forwards.get(label.toLowerCase());
            if (command.autocomplete() != null) {
                return command.autocomplete().complete(sender, label, args);
            } else return new String[0];
        } else {
            throw new IllegalStateException("Command label not recognised in group: " + forwards.keySet());
        }
    }

    @Override
    public String description() {
        return (data == null)?super.description():data.description();
    }

    @Override
    public String[] help() {
        return (data == null)?super.help():data.help();
    }

    @Override
    public String[] usage() {
        return (data == null)?super.usage():data.usage();
    }
}
