package net.ME1312.SubServers.Host.Library.Log;

import jline.console.ConsoleReader;
import jline.console.CursorBuffer;
import net.ME1312.SubServers.Host.Library.Container;
import net.ME1312.SubServers.Host.Library.TextColor;

import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;

/**
 * Console Log Stream Class
 */
public class ConsoleStream extends PrintStream {
    private ConsoleReader jline;
    private PrintStream original;
    private LinkedList<Integer> buffer;
    private CursorBuffer hidden;

    public ConsoleStream(ConsoleReader jline, PrintStream original) {
        super(original);
        this.jline = jline;
        this.buffer = new LinkedList<Integer>();
        this.original = original;
    }

    @Override
    public void write(int i) {
        try {
            if (((char) i) == '\n') {
                LinkedList<Integer> buffer = this.buffer;
                this.buffer = new LinkedList<Integer>();
                hide();
                for (Integer b : buffer) original.write(b);
                original.print(TextColor.RESET.asAnsiCode());
                original.write(i);
                show();
            } else {
                buffer.add(i);
            }
        } catch (Exception e) {}
    }

    private void hide() {
        hidden = jline.getCursorBuffer().copy();
        try {
            jline.getOutput().write("\u001b[1G\u001b[K");
            jline.flush();
        } catch (IOException e) {}
    }

    private void show() {
        try {
            jline.resetPromptLine(jline.getPrompt(), hidden.toString(), hidden.cursor);
            jline.flush();
        } catch (IOException e) {}
    }
}
