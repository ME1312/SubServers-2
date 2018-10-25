package net.ME1312.SubServers.Console;

import org.fusesource.jansi.AnsiOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AnsiHTMLColorStream extends AnsiOutputStream {
    private boolean concealOn = false;
    private static final String[] ANSI_COLOR_MAP = new String[]{"000000", "cd0000", "25bc24", "e1e100", "0000ee", "cd00cd", "00e1e1", "ffffff"};
    private static final byte[] BYTES_NBSP = "&nbsp;".getBytes();
    private static final byte[] BYTES_QUOT = "&quot;".getBytes();
    private static final byte[] BYTES_AMP = "&amp;".getBytes();
    private static final byte[] BYTES_LT = "&lt;".getBytes();
    private static final byte[] BYTES_GT = "&gt;".getBytes();
    private List<String> closingAttributes = new ArrayList();

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
        this.closingAttributes.add(0, s.split(" ", 2)[0]);
    }

    private void closeAttributes() throws IOException {
        Iterator i$ = this.closingAttributes.iterator();

        while(i$.hasNext()) {
            String attr = (String)i$.next();
            this.write("</" + attr + ">");
        }

        this.closingAttributes.clear();
    }

    private boolean nbsp = true;
    public void write(int data) throws IOException {
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

    protected void processSetAttribute(int attribute) throws IOException {
        switch(attribute) {
            case 1:
                this.writeAttribute("b");
                break;
            case 4:
                this.writeAttribute("u");
            case 7:
            case 27:
            default:
                break;
            case 8:
                this.write("\u001b[8m");
                this.concealOn = true;
                break;
            case 22:
                this.closeAttributes();
                break;
            case 24:
                this.closeAttributes();
        }

    }

    protected void processAttributeRest() throws IOException {
        if (this.concealOn) {
            this.write("\u001b[0m");
            this.concealOn = false;
        }

        this.closeAttributes();
    }

    protected void processSetForegroundColor(int color) throws IOException {
        this.writeAttribute("span class=\"ansi\" style=\"color: #" + ANSI_COLOR_MAP[color] + ";\"");
    }

    protected void processSetBackgroundColor(int color) throws IOException {
        this.writeAttribute("span class=\"ansi-background\" style=\"background-color: #" + ANSI_COLOR_MAP[color] + ";\"");
    }
}
