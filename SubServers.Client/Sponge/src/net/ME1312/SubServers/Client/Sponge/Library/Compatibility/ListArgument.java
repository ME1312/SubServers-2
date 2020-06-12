package net.ME1312.SubServers.Client.Sponge.Library.Compatibility;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.text.Text;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Comma Separated List Argument Class
 */
public class ListArgument extends CommandElement {

    /**
     * Parse a Comma Separated List Argument
     *
     * @param key Key
     */
    public ListArgument(Text key) {
        super(key);
    }

    @Nullable
    @Override
    protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
        LinkedList<String> selection = new LinkedList<String>();

        for (boolean run = true; run && args.hasNext(); ) {
            String current = args.next();
            if (current.endsWith(",")) {
                current = current.substring(0, current.length() - 1);
            } else run = false;
            selection.add(current.toLowerCase());
        }

        return selection.toArray(new String[0]);
    }

    @Override
    public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
        return Collections.emptyList();
    }
}
