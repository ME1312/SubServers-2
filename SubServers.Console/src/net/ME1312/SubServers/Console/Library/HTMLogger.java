package net.ME1312.SubServers.Console.Library;

import net.ME1312.Galaxi.Library.Container.Container;

import org.fusesource.jansi.AnsiOutputStream;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.Locale;

import static java.nio.charset.StandardCharsets.UTF_8;

public class HTMLogger extends AnsiOutputStream {
    private static final String[] ANSI_COLOR_MAP = new String[]{"000000", "cd0000", "25bc24", "d7d700", "0000c3", "be00be", "00a5dc", "cccccc"};
    private static final String[] ANSI_BRIGHT_COLOR_MAP = new String[]{"808080", "ff0000", "31e722", "ffff00", "0000ff", "ff00ff", "00c8ff", "ffffff"};
    private static final byte[] BYTES_NBSP = "\u00A0".getBytes(UTF_8);
    private static final byte[] BYTES_AMP = "&amp;".getBytes(UTF_8);
    private static final byte[] BYTES_LT = "&lt;".getBytes(UTF_8);
    private LinkedList<String> currentAttributes = new LinkedList<String>();
    private LinkedList<String> queue = new LinkedList<String>();
    private OutputStream raw;
    protected boolean ansi = true;
    protected boolean nbsp = false;
    private boolean underline = false;
    private boolean strikethrough = false;

    public static HTMLogger wrap(OutputStream raw) {
        return wrap(raw, new HTMConstructor<HTMLogger>() {
            @Override
            public HTMLogger construct(OutputStream raw, OutputStream wrapped) {
                return new HTMLogger(raw, wrapped);
            }
        });
    }

    public static <T extends HTMLogger> T wrap(final OutputStream raw, HTMConstructor<T> constructor) {
        final Container<T> html = new Container<T>(null);
        html.value(constructor.construct(raw, new OutputStream() {
            private boolean nbsp = false;

            @Override
            public void write(int data) throws IOException {
                HTMLogger htm = html.value();
                if (htm.queue.size() > 0) {
                    LinkedList<String> queue = htm.queue;
                    htm.queue = new LinkedList<>();
                    for (String attr : queue) {
                        htm.write('<' + attr + '>');
                        htm.currentAttributes.addFirst(attr);
                    }
                }

                if (data == 32) {
                    if (htm.nbsp) {
                        if (nbsp) raw.write(BYTES_NBSP);
                        else raw.write(data);
                        nbsp = !nbsp;
                    } else raw.write(data);
                } else {
                    nbsp = false;
                    switch(data) {
                        case '&':
                            raw.write(BYTES_AMP);
                            break;
                        case '<':
                            raw.write(BYTES_LT);
                            break;
                        case '\n':
                            htm.closeAttributes();
                        default:
                            raw.write(data);
                    }
                }
            }
        }));
        return html.value();
    }
    public HTMLogger(final OutputStream raw, OutputStream wrapped) {
        super(wrapped);
        this.raw = raw;
    }
    public interface HTMConstructor<T extends HTMLogger> {
        T construct(OutputStream raw, OutputStream wrapped);
    }

    private void write(String s) throws IOException {
        raw.write(s.getBytes(UTF_8));
    }

    private void writeAttribute(String attr) throws IOException {
        queue.add(attr);
    }

    public void closeAttribute(String s) throws IOException {

        // Try to remove a tag that doesn't exist yet first
        String[] queue = this.queue.toArray(new String[0]);
        for (int i = queue.length; i > 0;) {
            String attr = queue[--i];
            if (attr.toLowerCase().startsWith(s.toLowerCase())) {
                this.queue.removeLastOccurrence(attr);
                return;
            }
        }

        // Close a tag that we've already written
        LinkedList<String> closedAttributes = new LinkedList<String>();
        LinkedList<String> currentAttributes = new LinkedList<String>(this.currentAttributes);
        LinkedList<String> unclosedAttributes = new LinkedList<String>();

        for (String attr : currentAttributes) {
            if (attr.toLowerCase().startsWith(s.toLowerCase())) {
                for (String a : unclosedAttributes) {
                    closedAttributes.add(a);
                    this.currentAttributes.removeFirst();
                    write("</" + a.split(" ", 2)[0] + '>');
                }
                unclosedAttributes.clear();
                this.currentAttributes.removeFirst();
                write("</" + attr.split(" ", 2)[0] + '>');
                break;
            } else {
                unclosedAttributes.add(attr);
            }
        }

        // Queue unrelated tags to be re-opened
        for (String attr : closedAttributes) {
            this.queue.addFirst(attr);
        }
    }

    public void closeAttributes() throws IOException {
        queue.clear();

        for (String attr : currentAttributes) {
            write("</" + attr.split(" ", 2)[0] + ">");
        }

        underline = false;
        strikethrough = false;
        currentAttributes.clear();
    }

    @Override
    protected void processDeleteLine(int amount) throws IOException {
        super.processDeleteLine(amount);
    }

    private void renderTextDecoration() throws IOException {
        String dec = "";
        if (underline) dec += " underline";
        if (strikethrough) dec += " line-through";

        closeAttribute("span style=\"text-decoration:");
        if (dec.length() != 0) writeAttribute("span style=\"text-decoration:" + dec.substring(1) + "\"");
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
                underline = true;
                renderTextDecoration();
                break;
            case 9:
                strikethrough = true;
                renderTextDecoration();
                break;
            case 22:
                closeAttribute("b");
                break;
            case 23:
                closeAttribute("i");
                break;
            case 24:
                underline = false;
                renderTextDecoration();
                break;
            case 29:
                strikethrough = false;
                renderTextDecoration();
                break;
            case 73:
                closeAttribute("su");
                writeAttribute("sup");
                break;
            case 74:
                closeAttribute("su");
                writeAttribute("sub");
                break;
            case 75:
                closeAttribute("su");
                break;
        }
    }

    @Override
    protected void processUnknownOperatingSystemCommand(int label, String arg) {
        try {
            if (ansi && label == 8) {
                closeAttribute("a");
                String[] args = arg.split(";", 3);
                if (args.length > 1 && args[1].length() > 0 && allowHyperlink(args[1])) {
                    writeAttribute("a href=\"" + args[1].replace("&", "&amp;").replace("<", "&lt;").replace("\"", "&quot;") + "\" target=\"_blank\"");
                }
            }
        } catch (Exception e) {}
    }

    protected boolean allowHyperlink(String link) {
        if (link.toLowerCase(Locale.ENGLISH).startsWith("mailto:execute@galaxi.engine")) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    protected void processAttributeRest() throws IOException {
        closeAttributes();
    }

    private String parse256(int color) throws IOException {
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
        closeAttribute("span style=\"color:");
    }

    @Override
    protected void processSetForegroundColor(int color) throws IOException {
        processSetForegroundColor(color, false);
    }

    @Override
    protected void processSetForegroundColor(int color, boolean bright) throws IOException {
        if (ansi) {
            processDefaultTextColor();
            writeAttribute("span style=\"color:#" + ((!bright)?ANSI_COLOR_MAP:ANSI_BRIGHT_COLOR_MAP)[color] + "\"");
            renderTextDecoration();
        }
    }

    @Override
    protected void processSetForegroundColorExt(int index) throws IOException {
        if (ansi) {
            processDefaultTextColor();
            writeAttribute("span style=\"color:#" + parse256(index) + "\"");
            renderTextDecoration();
        }
    }

    @Override
    protected void processSetForegroundColorExt(int r, int g, int b) throws IOException {
        if (ansi) {
            processDefaultTextColor();
            writeAttribute("span style=\"color:#" + ((r >= 16)?"":"0") + Integer.toString(r, 16) + ((g >= 16)?"":"0") + Integer.toString(g, 16) + ((b >= 16)?"":"0") + Integer.toString(b, 16) + "\"");
            renderTextDecoration();
        }
    }

    @Override
    protected void processDefaultBackgroundColor() throws IOException {
        closeAttribute("span style=\"background-color:");
    }

    @Override
    protected void processSetBackgroundColor(int color) throws IOException {
        processSetBackgroundColor(color, false);
    }

    @Override
    protected void processSetBackgroundColor(int color, boolean bright) throws IOException {
        if (ansi) {
            processDefaultBackgroundColor();
            writeAttribute("span style=\"background-color:#" + ((!bright)?ANSI_COLOR_MAP:ANSI_BRIGHT_COLOR_MAP)[color] + "\"");
        }
    }

    @Override
    protected void processSetBackgroundColorExt(int index) throws IOException {
        if (ansi) {
            processDefaultBackgroundColor();
            writeAttribute("span style=\"background-color:#" + parse256(index) + "\"");
        }
    }

    @Override
    protected void processSetBackgroundColorExt(int r, int g, int b) throws IOException {
        if (ansi) {
            processDefaultBackgroundColor();
            writeAttribute("span style=\"background-color:#" + ((r >= 16)?"":"0") + Integer.toString(r, 16) + ((g >= 16)?"":"0") + Integer.toString(g, 16) + ((b >= 16)?"":"0") + Integer.toString(b, 16) + "\"");
        }
    }

    @Override
    public void flush() throws IOException {
        super.flush();
        raw.flush();
    }

    @Override
    public void close() throws IOException {
        closeAttributes();
        super.close();
        raw.close();
    }
}
