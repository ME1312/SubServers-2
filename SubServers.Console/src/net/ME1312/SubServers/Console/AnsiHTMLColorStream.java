package net.ME1312.SubServers.Console;

import org.fusesource.jansi.AnsiOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class AnsiHTMLColorStream extends AnsiOutputStream {
    private static final String[] ANSI_COLOR_MAP = new String[]{"000000", "cd0000", "25bc24", "e1e100", "0000ee", "cd00cd", "00e1e1", "ffffff"};
    private static final byte[] BYTES_NBSP = "&nbsp;".getBytes();
    private static final byte[] BYTES_QUOT = "&quot;".getBytes();
    private static final byte[] BYTES_AMP = "&amp;".getBytes();
    private static final byte[] BYTES_LT = "&lt;".getBytes();
    private static final byte[] BYTES_GT = "&gt;".getBytes();
    private LinkedList<String> closingAttributes = new LinkedList<String>();
    private boolean underline = false;
    private boolean strikethrough = false;

    public void close() throws IOException {
        this.closeAttributes();
        super.close();
    }

    public AnsiHTMLColorStream(OutputStream os) {
        super(os);
    }

    private void write(String s) throws IOException {
        super.out.write(s.getBytes());
    }

    private void writeAttribute(String s) throws IOException {
        this.write("<" + s + ">");
        this.closingAttributes.add(0, s);
    }

    private void closeAttribute(String s) throws IOException {
        LinkedList<String> closedAttributes = new LinkedList<String>();
        LinkedList<String> closingAttributes = new LinkedList<String>();
        LinkedList<String> unclosedAttributes = new LinkedList<String>();

        closingAttributes.addAll(this.closingAttributes);
        for (String attr : closingAttributes) {
            if (attr.toLowerCase().startsWith(s.toLowerCase())) {
                for (String a : unclosedAttributes) {
                    closedAttributes.add(0, a);
                    this.write("</" + a.split(" ", 2)[0] + ">");
                }
                this.closingAttributes.removeFirstOccurrence(attr);
                unclosedAttributes.clear();
                this.write("</" + attr.split(" ", 2)[0] + ">");
            } else {
                unclosedAttributes.add(attr);
            }
        }
        for (String attr : closedAttributes) {
            this.write("<" + attr + ">");
        }
    }

    private void closeAttributes() throws IOException {
        for (String attr : closingAttributes) {
            this.write("</" + attr.split(" ", 2)[0] + ">");
        }

        this.underline = false;
        this.strikethrough = false;
        this.closingAttributes.clear();
    }

    private boolean nbsp = true;
    @Override public void write(int data) throws IOException {
        if (data == 32) {
            if (nbsp) this.out.write(BYTES_NBSP);
            else super.write(data);
            nbsp = !nbsp;
        } else {
            nbsp = false;
            switch(data) {
                case 34:
                    this.out.write(BYTES_QUOT);
                    break;
                case 38:
                    this.out.write(BYTES_AMP);
                    break;
                case 60:
                    this.out.write(BYTES_LT);
                    break;
                case 62:
                    this.out.write(BYTES_GT);
                    break;
                default:
                    super.write(data);
            }
        }
    }

    public void writeLine(byte[] buf, int offset, int len) throws IOException {
        this.write(buf, offset, len);
        this.closeAttributes();
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
        switch(attribute) {
            case 1:
                this.writeAttribute("b");
                break;
            case 3:
                this.writeAttribute("i");
                break;
            case 4:
                this.closeAttribute("span class=\"ansi-decoration");
                this.underline = true;
                this.writeAttribute("span class=\"ansi-decoration\" style=\"text-decoration: " + parseTextDecoration() + ";\"");
                break;
            case 9:
                this.closeAttribute("span class=\"ansi-decoration");
                this.strikethrough = true;
                this.writeAttribute("span class=\"ansi-decoration\" style=\"text-decoration: " + parseTextDecoration() + ";\"");
                break;
            case 22:
                this.closeAttribute("b");
                break;
            case 23:
                this.closeAttribute("i");
                break;
            case 24:
                this.closeAttribute("span class=\"ansi-decoration");
                this.underline = false;
                this.writeAttribute("span class=\"ansi-decoration\" style=\"text-decoration: " + parseTextDecoration() + ";\"");
                break;
            case 29:
                this.closeAttribute("span class=\"ansi-decoration");
                this.strikethrough = false;
                this.writeAttribute("span class=\"ansi-decoration\" style=\"text-decoration: " + parseTextDecoration() + ";\"");
                break;
            default:
                break;
        }
    }

    @Override
    protected void processAttributeRest() throws IOException {
        this.closeAttributes();
    }

    @Override
    protected void processSetForegroundColor(int color) throws IOException {
        this.writeAttribute("span class=\"ansi-foreground\" style=\"color: #" + ANSI_COLOR_MAP[color] + ";\"");
    }

    @Override
    protected void processSetBackgroundColor(int color) throws IOException {
        this.writeAttribute("span class=\"ansi-background\" style=\"background-color: #" + ANSI_COLOR_MAP[color] + ";\"");
    }
}
