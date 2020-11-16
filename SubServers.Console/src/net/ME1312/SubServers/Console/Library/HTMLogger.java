package net.ME1312.SubServers.Console.Library;

import net.ME1312.Galaxi.Library.Container.Container;
import net.ME1312.Galaxi.Library.Container.Value;

import org.fusesource.jansi.AnsiOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;

/**
 * HTML Log Stream Class
 */
public class HTMLogger extends AnsiOutputStream {
    private static final String[] ANSI_COLOR_MAP = new String[]{"000000", "cd0000", "25bc24", "d7d700", "0000c3", "be00be", "00a5dc", "cccccc"};
    private static final String[] ANSI_BRIGHT_COLOR_MAP = new String[]{"808080", "ff0000", "31e722", "ffff00", "0000ff", "ff00ff", "00c8ff", "ffffff"};
    private static final byte[] BYTES_NBSP = "&nbsp;".getBytes();
    private static final byte[] BYTES_QUOT = "&quot;".getBytes();
    private static final byte[] BYTES_AMP = "&amp;".getBytes();
    private static final byte[] BYTES_LT = "&lt;".getBytes();
    private static final byte[] BYTES_GT = "&gt;".getBytes();
    private LinkedList<String> closingAttributes = new LinkedList<String>();
    private OutputStream raw;
    protected boolean ansi = true;
    private boolean underline = false;
    private boolean strikethrough = false;

    /**
     * Parse data from an OutputStream
     *
     * @param raw OutputStream
     * @return HTMLogger
     */
    public static HTMLogger wrap(OutputStream raw) {
        return wrap(raw, new HTMConstructor<HTMLogger>() {
            @Override
            public HTMLogger construct(OutputStream raw1, OutputStream wrapped) {
                return new HTMLogger(raw1, wrapped);
            }
        });
    }

    /**
     * Parse data from an OutputStream
     *
     * @param raw OutputStream
     * @param constructor Implementing Constructor
     * @param <T> Logger Type
     * @return HTMLogger
     */
    public static <T extends HTMLogger> T wrap(final OutputStream raw, HTMConstructor<T> constructor) {
        final Value<T> html = new Container<T>(null);
        html.value(constructor.construct(raw, new OutputStream() {
            private boolean nbsp = false;

            @Override
            public void write(int data) throws IOException {
                if (data == 32) {
                    if (nbsp) raw.write(BYTES_NBSP);
                    else raw.write(data);
                    nbsp = !nbsp;
                } else {
                    nbsp = false;
                    switch(data) {
                        case 34:
                            raw.write(BYTES_QUOT);
                            break;
                        case 38:
                            raw.write(BYTES_AMP);
                            break;
                        case 60:
                            raw.write(BYTES_LT);
                            break;
                        case 62:
                            raw.write(BYTES_GT);
                            break;
                        case 10:
                            html.value().closeAttributes();
                        default:
                            raw.write(data);
                    }
                }
            }
        }));
        return html.value();
    }
    protected HTMLogger(final OutputStream raw, OutputStream wrapped) {
        super(wrapped);
        this.raw = raw;
    }
    public interface HTMConstructor<T extends HTMLogger> {
        T construct(OutputStream raw, OutputStream wrapped);
    }

    private void write(String s) throws IOException {
        raw.write(s.getBytes());
    }

    private void writeAttribute(String s) throws IOException {
        write("<" + s + ">");
        closingAttributes.add(0, s);
    }

    protected void closeAttribute(String s) throws IOException {
        LinkedList<String> closedAttributes = new LinkedList<String>();
        LinkedList<String> closingAttributes = new LinkedList<String>();
        LinkedList<String> unclosedAttributes = new LinkedList<String>();

        closingAttributes.addAll(closingAttributes);
        for (String attr : closingAttributes) {
            if (attr.toLowerCase().startsWith(s.toLowerCase())) {
                for (String a : unclosedAttributes) {
                    closedAttributes.add(0, a);
                    write("</" + a.split(" ", 2)[0] + ">");
                }
                closingAttributes.removeFirstOccurrence(attr);
                unclosedAttributes.clear();
                write("</" + attr.split(" ", 2)[0] + ">");
            } else {
                unclosedAttributes.add(attr);
            }
        }
        for (String attr : closedAttributes) {
            write("<" + attr + ">");
        }
    }

    protected void closeAttributes() throws IOException {
        for (String attr : closingAttributes) {
            write("</" + attr.split(" ", 2)[0] + ">");
        }

        underline = false;
        strikethrough = false;
        closingAttributes.clear();
    }

    @Override
    protected void processDeleteLine(int amount) throws IOException {
        super.processDeleteLine(amount);
    }

    private String parseTextDecoration() {
        String dec = "";
        if (underline) dec += " underline";
        if (strikethrough) dec += " line-through";
        if (dec.length() <= 0) dec += " none";

        return dec.substring(1);
    }

    @Override
    protected void processSetAttribute(int attribute) throws IOException {
        if (ansi) switch(attribute) {
            case 1:
                closeAttribute("b");
                writeAttribute("b");
                break;
            case 3:
                closeAttribute("i");
                writeAttribute("i");
                break;
            case 4:
                closeAttribute("span class=\"ansi-decoration");
                underline = true;
                writeAttribute("span class=\"ansi-decoration\" style=\"text-decoration: " + parseTextDecoration() + ";\"");
                break;
            case 9:
                closeAttribute("span class=\"ansi-decoration");
                strikethrough = true;
                writeAttribute("span class=\"ansi-decoration\" style=\"text-decoration: " + parseTextDecoration() + ";\"");
                break;
            case 22:
                closeAttribute("b");
                break;
            case 23:
                closeAttribute("i");
                break;
            case 24:
                closeAttribute("span class=\"ansi-decoration");
                underline = false;
                writeAttribute("span class=\"ansi-decoration\" style=\"text-decoration: " + parseTextDecoration() + ";\"");
                break;
            case 29:
                closeAttribute("span class=\"ansi-decoration");
                strikethrough = false;
                writeAttribute("span class=\"ansi-decoration\" style=\"text-decoration: " + parseTextDecoration() + ";\"");
                break;
        }
    }

    @Override
    protected void processUnknownOperatingSystemCommand(int label, String arg) {
        try {
            if (ansi) switch (label) {
                case 99900: // Galaxi Console Exclusives 99900-99999
                    closeAttribute("a");
                    writeAttribute("a href=\"" + arg + "\" target=\"_blank\"");
                    break;
                case 99901:
                    closeAttribute("a");
                    break;
            }
        } catch (Exception e) {}
    }

    @Override
    protected void processAttributeRest() throws IOException {
        closeAttributes();
    }

    private String parse8BitColor(int color) throws IOException {
        if (color < 8) {
            return ANSI_COLOR_MAP[color];
        } else if (color < 16) {
            return ANSI_BRIGHT_COLOR_MAP[color - 8];
        } else if (color < 232) {
            int r = (int) (Math.floor((color - 16) / 36d) * (255 / 5));
            int g = (int) (Math.floor(((color - 16) % 36d) / 6d) * (255 / 5));
            int b = (int) (Math.floor(((color - 16) % 36d) % 6d) * (255 / 5));
            return ((r >= 16)?"":"0") + Integer.toString(r, 16) + ((g >= 16)?"":"0") + Integer.toString(g, 16) + ((b >= 16)?"":"0") + Integer.toString(b, 16);
        } else if (color < 256) {
            int gray = (int) ((255 / 25d) * (color - 232 + 1));
            return ((gray >= 16)?"":"0") + Integer.toString(gray, 16) + ((gray >= 16)?"":"0") + Integer.toString(gray, 16) + ((gray >= 16)?"":"0") + Integer.toString(gray, 16);
        } else {
            throw new IOException("Invalid 8 Bit Color: " + color);
        }
    }

    @Override
    protected void processDefaultTextColor() throws IOException {
        closeAttribute("span class=\"ansi-foreground");
    }

    @Override
    protected void processSetForegroundColor(int color) throws IOException {
        processSetForegroundColor(color, false);
    }

    @Override
    protected void processSetForegroundColor(int color, boolean bright) throws IOException {
        if (ansi) {
            processDefaultTextColor();
            writeAttribute("span class=\"ansi-foreground\" style=\"color: #" + ((!bright)?ANSI_COLOR_MAP:ANSI_BRIGHT_COLOR_MAP)[color] + ";\"");
        }
    }

    @Override
    protected void processSetForegroundColorExt(int index) throws IOException {
        if (ansi) {
            processDefaultTextColor();
            writeAttribute("span class=\"ansi-foreground\" style=\"color: #" + parse8BitColor(index) + ";\"");
        }
    }

    @Override
    protected void processSetForegroundColorExt(int r, int g, int b) throws IOException {
        if (ansi) {
            processDefaultTextColor();
            writeAttribute("span class=\"ansi-foreground\" style=\"color: #" + ((r >= 16)?"":"0") + Integer.toString(r, 16) + ((g >= 16)?"":"0") + Integer.toString(g, 16) + ((b >= 16)?"":"0") + Integer.toString(b, 16) + ";\"");
        }
    }

    @Override
    protected void processDefaultBackgroundColor() throws IOException {
        closeAttribute("span class=\"ansi-background");
    }

    @Override
    protected void processSetBackgroundColor(int color) throws IOException {
        processSetBackgroundColor(color, false);
    }

    @Override
    protected void processSetBackgroundColor(int color, boolean bright) throws IOException {
        if (ansi) {
            processDefaultBackgroundColor();
            writeAttribute("span class=\"ansi-background\" style=\"background-color: #" + ((!bright)?ANSI_COLOR_MAP:ANSI_BRIGHT_COLOR_MAP)[color] + ";\"");
        }
    }

    @Override
    protected void processSetBackgroundColorExt(int index) throws IOException {
        if (ansi) {
            processDefaultBackgroundColor();
            writeAttribute("span class=\"ansi-background\" style=\"background-color: #" + parse8BitColor(index) + ";\"");
        }
    }

    @Override
    protected void processSetBackgroundColorExt(int r, int g, int b) throws IOException {
        if (ansi) {
            processDefaultBackgroundColor();
            writeAttribute("span class=\"ansi-background\" style=\"background-color: #" + ((r >= 16)?"":"0") + Integer.toString(r, 16) + ((g >= 16)?"":"0") + Integer.toString(g, 16) + ((b >= 16)?"":"0") + Integer.toString(b, 16) + ";\"");
        }
    }

    @Override
    public void close() throws IOException {
        closeAttributes();
        super.close();
        raw.close();
    }
}
