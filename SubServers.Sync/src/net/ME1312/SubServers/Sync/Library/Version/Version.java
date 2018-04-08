package net.ME1312.SubServers.Sync.Library.Version;

import net.ME1312.SubServers.Sync.Library.Util;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Version Class
 */
@SuppressWarnings("serial")
public class Version implements Serializable, Comparable<Version> {
    private final Version parent;
    private final VersionType type;
    private final String string;

    /**
     * Creates a Version
     *
     * @param string Version String
     */
    public Version(String string) {
        this(VersionType.RELEASE, string);
    }

    /**
     * Creates a Version
     *
     * @param type Version Type
     * @param string Version String
     */
    public Version(VersionType type, String string) {
        this(null, type, string);
    }

    /**
     * Creates a Version (Appending the parent)
     *
     * @param parent Parent Version
     * @param string Version String
     */
    public Version(Version parent, String string) {
        this(parent, VersionType.RELEASE, string);
    }

    /**
     * Creates a Version (Appending the parent)
     *
     * @param parent Parent Version
     * @param type Version Type
     * @param string Version String
     */
    public Version(Version parent, VersionType type, String string) {
        if (Util.isNull(string, type)) throw new NullPointerException();
        this.parent = parent;
        this.type = type;
        this.string = string;
    }

    /**
     * Creates a Version
     *
     * @param ints Version Numbers (Will be separated with dots)
     */
    public Version(int... ints) {
        this(VersionType.RELEASE, ints);
    }

    /**
     * Creates a Version
     *
     * @param type Version Type
     * @param ints Version Numbers (Will be separated with dots)
     */
    public Version(VersionType type, int... ints) {
        this(null, type, ints);
    }

    /**
     * Creates a Version (Appending the parent)
     *
     * @param parent Parent Version
     * @param ints Version Numbers (Will be separated with dots)
     */
    public Version(Version parent, int... ints) {
        this(parent, VersionType.RELEASE, ints);
    }

    /**
     * Creates a Version (Appending the parent)
     *
     * @param parent Parent Version
     * @param type Version Type
     * @param ints Version Numbers (Will be separated with dots)
     */
    public Version(Version parent, VersionType type, int... ints) {
        if (Util.isNull(type)) throw new NullPointerException();
        this.parent = parent;
        this.type = type;
        String string = Integer.toString(ints[0]);
        int i = 0;
        if (ints.length != 1) {
            do {
                i++;
                string = string + "." + ints[i];
            } while ((i + 1) != ints.length);
        }
        this.string = string;
    }

    /**
     * Parse a Version from a string
     *
     * @param string String to parse
     * @see #toFullString() <b>#toFullString()</b> returns a valid string
     * @see #toFullString() <b>#toString()</b> returns a valid string
     */
    public static Version fromString(String string) {
        Matcher regex = Pattern.compile("(rv|(?:p?[abrv])|[u])?([^/]+)", Pattern.CASE_INSENSITIVE).matcher(string);
        Version current = null;
        while (regex.find()) {
            try {
                VersionType type = VersionType.RELEASE;
                if (regex.group(1) != null) switch (regex.group(1).toLowerCase()) {
                    case "pa":
                        type = VersionType.PRE_ALPHA;
                        break;
                    case "a":
                        type = VersionType.ALPHA;
                        break;
                    case "pv":
                        type = VersionType.PREVIEW;
                        break;
                    case "pb":
                        type = VersionType.PRE_BETA;
                        break;
                    case "b":
                        type = VersionType.BETA;
                        break;
                    case "pr":
                        type = VersionType.PRE_RELEASE;
                        break;
                    case "rv":
                        type = VersionType.REVISION;
                        break;
                    case "v":
                        type = VersionType.VERSION;
                        break;
                    case "u":
                        type = VersionType.UPDATE;
                        break;
                }
                current = new Version(current, type, regex.group(2));
            } catch (Throwable e) {}
        }
        if (current == null) throw new IllegalArgumentException("Could not find version in string: " + string);
        return current;
    }

    /**
     * The default toString() method<br>
     * <br>
     * <b style="font-family: consolas">new Version(new Version("1.0.0"), VersionType.PRE_ALPHA, "7")</b> would return:<br>
     * <b style="font-family: consolas">1.0.0/pa7</b>
     *
     * @return Version as a String
     */
    @Override
    public String toString() {
        if (parent != null || type == VersionType.RELEASE) {
            String str = (parent == null)?"":parent.toString() + '/' + type.shortname;
            str += string;
            return str;
        } else return toFullString();
    }

    /**
     * The full toString() method<br>
     * <br>
     * <b style="font-family: consolas">new Version(new Version("1.0.0"), VersionType.PRE_ALPHA, "7")</b> would return:<br>
     * <b style="font-family: consolas">r1.0.0/pa7</b>
     *
     * @return Version as a String
     */
    public String toFullString() {
        String str = type.shortname + string;
        if (parent != null) str = parent.toFullString()+'/'+str;
        return str;
    }

    /**
     * The extended toString() method<br>
     * <br>
     * <b style="font-family: consolas">new Version(new Version("1.0.0"), VersionType.PRE_ALPHA, "7")</b> would return:<br>
     * <b style="font-family: consolas">1.0.0 pre-alpha 7</b>
     *
     * @return Version as a String
     */
    public String toExtendedString() {
        if (parent != null || type == VersionType.RELEASE) {
            String str = (parent == null)?"":parent.toExtendedString() + ' ' + type.longname + ' ';
            str += string;
            return str;
        } else return toFullExtendedString();
    }

    /**
     * The full extended toString() method<br>
     * <br>
     * <b style="font-family: consolas">new Version(new Version("1.0.0"), VersionType.PRE_ALPHA, "7")</b> would return:<br>
     * <b style="font-family: consolas">release 1.0.0 pre-alpha 7</b>
     *
     * @return Version as a String
     */
    public String toFullExtendedString() {
        String str = type.longname + ' ' + string;
        if (parent != null) str = parent.toFullExtendedString()+' '+str;
        return str;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof Version) {
            return equals((Version) object);
        } else {
            return super.equals(object);
        }
    }

    /**
     * See if Versions are Equal
     *
     * @param version Version to Compare to
     * @return
     */
    public boolean equals(Version version) {
        return compareTo(version) == 0;
    }

    /*
     * Returns 1 if Greater than
     * Returns 0 if Equal
     * Returns -1 if Less than
     *//**
     *
     * Compare Versions
     *
     * @param version Version to Compare to
     */
    public int compareTo(Version version) {
        return compare(this, version);
    }

    /**
     * See if Versions are Equal
     *
     * @param ver1 Version to Compare
     * @param ver2 Version to Compare
     * @return
     */
    public static boolean equals(Version ver1, Version ver2) {
        return compare(ver1, ver2) == 0;
    }

    /*
     * Returns 1 if Greater than
     * Returns 0 if Equal
     * Returns -1 if Less than
     *//**
     * Compare Versions
     *
     * @param ver1 Version to Compare
     * @param ver2 Version to Compare
     */
    public static int compare(Version ver1, Version ver2) {
        if (ver1 == null && ver2 == null) {
            // Both versions are null
            return 0;
        }

        if (ver1 == null) {
            // Version one is null
            return -1;
        }

        if (ver2 == null) {
            // Version two is null
            return 1;
        }

        LinkedList<Version> stack1 = new LinkedList<Version>();
        stack1.add(ver1);
        while (ver1.parent != null) {
            ver1 = ver1.parent;
            stack1.add(ver1);
        }
        Collections.reverse(stack1);

        LinkedList<Version> stack2 = new LinkedList<Version>();
        stack2.add(ver2);
        while (ver2.parent != null) {
            ver2 = ver2.parent;
            stack2.add(ver2);
        }
        Collections.reverse(stack2);

        int id;
        for (id = 0; id < stack1.size(); id++) {
            if (id >= stack2.size()) {
                // Version one still has children when version two does not...
                if (stack1.get(id).type.stageid < 0) {
                    // ...making version two the official version
                    return -1;
                } else {
                    // ...however the direct child of version one has a stageid higher than or equal to a release
                    return 1;
                }
            }

            int result = stack1.get(id).compare(stack2.get(id));
            if (result != 0) {
                // Versions are not the same, return the result
                return result;
            }
        }
        if (id < stack2.size()) {
            // Version one does not children when version two still does...
            if (stack2.get(id).type.stageid < 0) {
                // ...making version one the official version
                return 1;
            } else {
                // ...however the direct child of version two has a stageid higher than or equal to a release
                return -1;
            }
        }
        return 0;
    }

    /*
     * Compares versions ignoring parent/child relationships
     */
    private int compare(Version version) {
        if (this.type.stageid > version.type.stageid) {
            // Version one has a type of a later stage than version two
            return 1;
        }

        if (this.type.stageid < version.type.stageid) {
            // Version one has a type of an earlier stage than version two
            return -1;
        }

        VersionTokenizer tokenizer1 = new VersionTokenizer(string);
        VersionTokenizer tokenizer2 = new VersionTokenizer(version.string);

        int number1, number2;
        String suffix1, suffix2;

        while (tokenizer1.MoveNext()) {
            if (!tokenizer2.MoveNext()) {
                do {
                    number1 = tokenizer1.getNumber();
                    suffix1 = tokenizer1.getSuffix();
                    if (number1 != 0 || suffix1.length() != 0) {
                        // Version one is longer than number two, and non-zero
                        return 1;
                    }
                }
                while (tokenizer1.MoveNext());

                // Version one is longer than version two, but zero
                return 0;
            }

            number1 = tokenizer1.getNumber();
            suffix1 = tokenizer1.getSuffix();
            number2 = tokenizer2.getNumber();
            suffix2 = tokenizer2.getSuffix();

            if (number1 < number2) {
                // Number one is less than number two
                return -1;
            }
            if (number1 > number2) {
                // Number one is greater than number two
                return 1;
            }

            boolean empty1 = suffix1.length() == 0;
            boolean empty2 = suffix2.length() == 0;

            if (empty1 && empty2) continue; // No suffixes
            if (empty1) return 1; // First suffix is empty (1.2 > 1.2b)
            if (empty2) return -1; // Second suffix is empty (1.2a < 1.2)

            // Lexical comparison of suffixes
            int result = suffix1.compareTo(suffix2);
            if (result != 0) return result;

        }
        if (tokenizer2.MoveNext()) {
            do {
                number2 = tokenizer2.getNumber();
                suffix2 = tokenizer2.getSuffix();
                if (number2 != 0 || suffix2.length() != 0) {
                    // Version one is longer than version two, and non-zero
                    return -1;
                }
            }
            while (tokenizer2.MoveNext());

            // Version two is longer than version one, but zero
            return 0;
        }
        return 0;
    }
}