package net.ME1312.SubServers.Host.Library;

import net.ME1312.Galaxi.Library.Util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.regex.Pattern;

/**
 * File Scanner Base Class
 */
public abstract class FileScanner {

    /**
     * Scan a Directory
     *
     * @param dir Directory
     * @param whitelist File Whitelist
     */
    protected void scan(File dir, String... whitelist) throws IOException {
        List<String> files = Util.searchDirectory(dir);
        if (files.size() <= 0 || whitelist.length <= 0)
            return;

        boolean csfs = false;
        {
            long stamp = Math.round(Math.random() * 100000);
            File test1 = new File(dir, '.' + stamp + ".ss_fsc");
            File test2 = new File(dir, '.' + stamp + ".SS_FSC");

            test1.createNewFile();
            if (test2.createNewFile()) {
                csfs = true;
                test2.delete();
            }
            test1.delete();
        }

        LinkedHashMap<Pattern, Boolean> rules = new LinkedHashMap<Pattern, Boolean>();
        for (String entry : whitelist) {
            boolean mode = !entry.startsWith("!");
            if (!mode) entry = entry.substring(1);

            String pattern;
            if (!entry.startsWith("%")) {
                if (entry.startsWith("./"))
                    entry = entry.substring(1);

                StringBuilder rule = new StringBuilder();
                if (entry.startsWith("**")) {
                    entry = entry.substring(2);
                    rule.append("^.*");
                } else if (entry.startsWith("/")) {
                    rule.append("^");
                }

                boolean greedyEnding = false;
                if (entry.endsWith("**")) {
                    entry = entry.substring(0, entry.length() - 2);
                    greedyEnding = true;
                } else if (entry.endsWith("/")) {
                    greedyEnding = true;
                }

                StringBuilder literal = new StringBuilder();
                for (PrimitiveIterator.OfInt i = entry.codePoints().iterator(); i.hasNext(); ) {
                    int c = i.next();
                    if ((c == '*' || c == '?' || c == '[') && literal.length() > 0) {
                        rule.append(Pattern.quote(literal.toString()));
                        literal = new StringBuilder();
                    }
                    switch (c) {
                        case '\\':
                            if (i.hasNext()) c = i.next();
                            literal.appendCodePoint(c);
                        case '[':
                            for (boolean escaped = false; i.hasNext() && (c != ']' || escaped); c = i.next()) {
                                if (c == '\\') escaped = !escaped;
                                else escaped = false;
                                literal.appendCodePoint(c);
                            }
                            if (c == ']' && literal.length() > 1) {
                                literal.appendCodePoint(c);
                                rule.append(literal.toString());
                            }
                            literal = new StringBuilder();
                            break;
                        case '*':
                            rule.append("[^/]+");
                            break;
                        case '?':
                            rule.append("[^/]");
                            break;
                        default:
                            literal.appendCodePoint(c);
                            break;
                    }
                }
                if (literal.length() > 0)
                    rule.append(Pattern.quote(literal.toString()));

                if (greedyEnding)
                    rule.append(".*");
                rule.append("$");
                pattern = rule.toString();
            } else {
                pattern = entry.substring(1);
            }

            if (csfs) rules.put(Pattern.compile(pattern), mode);
            else rules.put(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE), mode);
        }

        for (String file : files) {
            boolean act = false;

            for (Map.Entry<Pattern, Boolean> rule : rules.entrySet()) {
                if (rule.getKey().matcher('/' + file.replace(File.separatorChar, '/')).find()) act = rule.getValue();
            }

            if (act) act(dir, file);
        }
    }

    /**
     * Perform an action on an included file
     *
     * @param dir Parent Directory
     * @param name File Name
     */
    protected abstract void act(File dir, String name) throws IOException;
}
