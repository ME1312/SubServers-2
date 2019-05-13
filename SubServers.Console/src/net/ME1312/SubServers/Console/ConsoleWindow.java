package net.ME1312.SubServers.Console;

import net.ME1312.Galaxi.Library.Callback.ExceptionReturnRunnable;
import net.ME1312.Galaxi.Library.Callback.ExceptionRunnable;
import net.ME1312.SubServers.Bungee.Host.SubCreator;
import net.ME1312.SubServers.Bungee.Host.SubLogFilter;
import net.ME1312.SubServers.Bungee.Host.SubLogger;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubServers.Console.Library.HTMLogger;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.*;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.logging.Level;

public final class ConsoleWindow implements SubLogFilter {
    private static final int MAX_SCROLLBACK = (Integer.getInteger("subservers.console.max_scrollback", 0) >= 128)?Integer.getInteger("subservers.console.max_scrollback"):7500;
    private static final String RESET_VALUE = "\n\u00A0\n\u00A0";
    private ConsolePlugin plugin;
    private JFrame window;
    private JPanel panel;
    private JTextField input;
    private boolean ifocus = false;
    private TextFieldPopup popup;
    private JTextPane log;
    private JScrollPane vScroll;
    private JScrollBar hScroll;
    private List<Integer> eScroll = new ArrayList<Integer>();
    private JPanel find;
    private JTextField findT;
    private JButton findN;
    private JButton findP;
    private JButton findD;
    private int findO = 0;
    private int findI = 0;
    private boolean open = false;
    private boolean running = true;
    private LinkedList<Object> messages = new LinkedList<Object>();
    private SubLogger logger;
    private int fontSize = 12;
    private File file = null;
    private FileOutputStream filewriter = null;
    private List<Runnable> spost = new LinkedList<Runnable>();
    private ByteArrayOutputStream scache = new ByteArrayOutputStream();
    private AnsiUIOutputStream stream = AnsiUIOutputStream.wrap(new OutputStream() {

        private int countLines(String str) {
            int count = 0;
            for (int i = 0; i < str.length(); i++) if (str.charAt(i) == '\n') count++;
            return count;
        }

        @Override
        public void write(int b) throws IOException {
            scache.write(b);
            if (b == '\n') {
                try {
                    HTMLEditorKit kit = (HTMLEditorKit) log.getEditorKit();
                    HTMLDocument doc = (HTMLDocument) log.getDocument();
                    kit.insertHTML(doc, doc.getLength() - 2, new String(scache.toByteArray(), "UTF-8"), 0, 0, null);
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            hScroll();
                        }
                    });
                } catch (Exception e) {} try {
                    int lines;
                    String content;
                    if (log.getSelectionStart() == log.getSelectionEnd() && (lines = countLines(content = log.getDocument().getText(0, log.getDocument().getLength()))) > MAX_SCROLLBACK + 2) {
                        int lineBreak = 1;
                        for (lines -= MAX_SCROLLBACK; lines > 0; lines--) lineBreak = content.indexOf('\n', lineBreak + 1);
                        if (lineBreak <= log.getDocument().getLength() - 2 && log.getSelectionStart() == log.getSelectionEnd()) {
                            log.getDocument().remove(0, lineBreak);
                        }
                    }
                } catch (Exception e) {}
                for (Runnable post : spost) try {
                    post.run();
                } catch (Throwable e) {}
                spost.clear();
                scache = new ByteArrayOutputStream();
            }
        }
    }, new HTMLogger.HTMConstructor<AnsiUIOutputStream>() {
        @Override
        public AnsiUIOutputStream construct(OutputStream raw, OutputStream wrapped) {
            return new AnsiUIOutputStream(raw, wrapped);
        }
    });
    private boolean[] kpressed = new boolean[65535];
    private KeyEventDispatcher keys = new KeyEventDispatcher() {
        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            switch (event.getID()) {
                case KeyEvent.KEY_PRESSED:
                    kpressed[event.getKeyCode()] = true;
                    break;
                case KeyEvent.KEY_RELEASED:
                    kpressed[event.getKeyCode()] = false;
                    break;
            }
            if (window.isVisible() && window.isFocused()) {
                if (event.getID() == KeyEvent.KEY_PRESSED) switch (event.getKeyCode()) {
                    case KeyEvent.VK_UP:
                        if (ifocus)
                            popup.prev(input);
                        break;
                    case KeyEvent.VK_DOWN:
                        if (ifocus)
                            popup.next(input);
                        break;
                    case KeyEvent.VK_ESCAPE:
                        if (find.isVisible()) {
                            find.setVisible(false);
                            findI = 0;
                            findO = 0;
                        }
                        break;
                    case KeyEvent.VK_ENTER:
                        if (find.isVisible() && !ifocus)
                            ConsoleWindow.this.find(kpressed[KeyEvent.VK_SHIFT] != Boolean.TRUE);
                        break;
                    case KeyEvent.VK_TAB:
                        if (!ifocus) input.requestFocusInWindow();
                        break;
                }

            }
            return false;
        }
    };

    public ConsoleWindow(final ConsolePlugin plugin, final SubLogger logger) {
        this.plugin = plugin;
        this.logger = logger;

        try {
            file = File.createTempFile("SubServers.Console.", ".log");
            file.deleteOnExit();
            filewriter = new FileOutputStream(file, false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        window = new JFrame();

        JMenuBar jMenu = new JMenuBar();
        JMenu menu = new JMenu("\u00A0Log\u00A0");
        JMenuItem item = new JMenuItem("Clear Screen");
        item.setAccelerator(KeyStroke.getKeyStroke('L', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), true));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                ConsoleWindow.this.clear();
            }
        });
        menu.add(item);
        item = new JMenuItem("Reload Log");
        item.setAccelerator(KeyStroke.getKeyStroke('R', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), true));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                log.setText(RESET_VALUE);
                ConsoleWindow.this.loadContent();
            }
        });
        menu.add(item);
        if (logger.getHandler() instanceof SubServer || logger.getHandler() instanceof SubCreator) {
            item = new JCheckBoxMenuItem("Auto Popout Log");
            item.setSelected((logger.getHandler() instanceof SubServer && (plugin.config.get().getStringList("Enabled-Servers").contains(((SubServer) logger.getHandler()).getName().toLowerCase()))) || (logger.getHandler() instanceof SubCreator && (plugin.config.get().getStringList("Enabled-Creators").contains(((SubCreator) logger.getHandler()).getHost().getName().toLowerCase()))));
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    try {
                        if (logger.getHandler() instanceof SubServer) {
                            List<String> list = plugin.config.get().getStringList("Enabled-Servers");
                            if (((AbstractButton) event.getSource()).getModel().isSelected()) {
                                list.add(((SubServer) logger.getHandler()).getName().toLowerCase());
                            } else {
                                list.remove(((SubServer) logger.getHandler()).getName().toLowerCase());
                            }
                            plugin.config.get().set("Enabled-Servers", list);
                            plugin.config.save();
                        } else if (logger.getHandler() instanceof SubCreator) {
                            List<String> list = plugin.config.get().getStringList("Enabled-Servers");
                            if (((AbstractButton) event.getSource()).getModel().isSelected()) {
                                list.add(((SubCreator) logger.getHandler()).getHost().getName().toLowerCase());
                            } else {
                                list.remove(((SubCreator) logger.getHandler()).getHost().getName().toLowerCase());
                            }
                            plugin.config.get().set("Enabled-Servers", list);
                            plugin.config.save();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            menu.add(item);
            jMenu.add(menu);
        }

        menu = new JMenu("\u00A0Search\u00A0");
        item = new JMenuItem("Find");
        item.setAccelerator(KeyStroke.getKeyStroke('F', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), true));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (find.isVisible()) {
                    find.setVisible(false);
                    findI = 0;
                    findO = 0;
                } else {
                    find.setVisible(true);
                    findT.selectAll();
                    findT.requestFocusInWindow();
                }
            }
        });
        menu.add(item);
        item = new JMenuItem("Find Next");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (find.isVisible()) {
                    ConsoleWindow.this.find(true);
                } else {
                    find.setVisible(true);
                    findT.selectAll();
                    findT.requestFocusInWindow();
                }
            }
        });
        menu.add(item);
        item = new JMenuItem("Find Previous");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (find.isVisible()) {
                    ConsoleWindow.this.find(false);
                } else {
                    find.setVisible(true);
                    findT.selectAll();
                    findT.requestFocusInWindow();
                }
            }
        });
        menu.add(item);
        jMenu.add(menu);

        menu = new JMenu("\u00A0View\u00A0");
        item = new JMenuItem("Scroll to Top");
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_UP, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), true));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                vScroll.getVerticalScrollBar().setValue(0);
            }
        });
        menu.add(item);
        item = new JMenuItem("Scroll to Bottom");
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), true));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                vScroll.getVerticalScrollBar().setValue(vScroll.getVerticalScrollBar().getMaximum() - vScroll.getVerticalScrollBar().getVisibleAmount());
            }
        });
        menu.add(item);
        menu.addSeparator();
        item = new JCheckBoxMenuItem("Use ANSI Formatting");
        item.setSelected(true);
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                stream.ansi(((AbstractButton) event.getSource()).getModel().isSelected());
                log.setText(RESET_VALUE);
                ConsoleWindow.this.loadContent();
            }
        });
        menu.add(item);
        item = new JMenuItem("Reset Text Size");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                HTMLDocument doc = (HTMLDocument) log.getDocument();
                fontSize = 12;
                doc.getStyleSheet().addRule("body {font-size: " + fontSize + ";}\n");
                ConsoleWindow.this.hScroll();
            }
        });
        menu.add(item);
        item = new JMenuItem("Bigger Text");
        item.setAccelerator(KeyStroke.getKeyStroke('=', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), true));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                HTMLDocument doc = (HTMLDocument) log.getDocument();
                fontSize += 2;
                doc.getStyleSheet().addRule("body {font-size: " + fontSize + ";}\n");
                ConsoleWindow.this.hScroll();
            }
        });
        menu.add(item);
        item = new JMenuItem("Smaller Text");
        item.setAccelerator(KeyStroke.getKeyStroke('-', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), true));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                HTMLDocument doc = (HTMLDocument) log.getDocument();
                fontSize -= 2;
                doc.getStyleSheet().addRule("body {font-size: " + fontSize + ";}\n");
                ConsoleWindow.this.hScroll();
            }
        });
        menu.add(item);
        jMenu.add(menu);

        window.setJMenuBar(jMenu);
        window.setContentPane(panel);
        window.pack();
        Util.isException(new ExceptionRunnable() {
            @Override
            public void run() throws Throwable {
                window.setIconImage(ImageIO.read(ConsolePlugin.class.getResourceAsStream("/net/ME1312/SubServers/Console/ConsoleIcon.png")));
            }
        });
        window.setTitle(logger.getName() + " \u2014 SubServers 2");
        window.setSize(1024, 576);
        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (int) ((dimension.getWidth() - window.getWidth()) / 2);
        int y = (int) ((dimension.getHeight() - window.getHeight()) / 2);
        window.setLocation(x, y);
        window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                close();
            }
        });
        window.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                hScroll();
            }
        });
        vScroll.setBorder(BorderFactory.createEmptyBorder());
        hScroll.setVisible(false);
        new SmartScroller(vScroll, SmartScroller.VERTICAL, SmartScroller.END);
        log.setContentType("text/html");
        log.setEditorKit(new HTMLEditorKit());
        StyleSheet style = new StyleSheet();
        String font;
        try {
            Font f = Font.createFont(Font.TRUETYPE_FONT, ConsoleWindow.class.getResourceAsStream("/net/ME1312/SubServers/Console/ConsoleFont.ttf"));
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(f);
            font = f.getFontName();
            input.setFont(f.deriveFont(14f));
        } catch (Exception e) {
            font = "Courier";
        }
        style.addRule("body {color: #dcdcdc; font-family: " + font + "; font-size: 12;}\n");
        log.setDocument(new HTMLDocument(style));
        log.setBorder(BorderFactory.createLineBorder(new Color(40, 44, 45)));
        new TextFieldPopup(log, false);
        ((AbstractDocument) log.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                super.insertString(fb, offset, string, attr);
                hScroll();
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                super.replace(fb, offset, length, text, attrs);
                hScroll();
            }

            @Override
            public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
                super.remove(fb, offset, length);
                hScroll();
            }
        });


        popup = new TextFieldPopup(input, true);
        input.setBorder(BorderFactory.createLineBorder(new Color(40, 44, 45), 4));
        input.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (logger.getHandler() instanceof SubServer && input.getText().length() > 0 && !input.getText().equals(">")) {
                    if (((SubServer) logger.getHandler()).command((input.getText().startsWith(">")) ? input.getText().substring(1) : input.getText())) {
                        popup.commands.add((input.getText().startsWith(">")) ? input.getText().substring(1) : input.getText());
                        input.setText("");
                    }
                }
            }
        });
        ((AbstractDocument) input.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                if (offset < 1) {
                    return;
                }
                super.insertString(fb, offset, string, attr);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (offset < 1) {
                    length = Math.max(0, length - 1);
                    offset = input.getDocument().getLength();
                    input.setCaretPosition(offset);
                }

                if (popup.history == Boolean.TRUE) {
                    popup.history = null;
                } else if (popup.history == null) {
                    popup.history = false;
                }

                try {
                    super.replace(fb, offset, length, text, attrs);
                } catch (BadLocationException e) {
                    super.replace(fb, 1, length, text, attrs);
                }
            }

            @Override
            public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
                if (offset < 1) {
                    length = Math.max(0, length + offset - 1);
                    offset = 1;
                }
                if (length > 0) {
                    super.remove(fb, offset, length);
                }
            }
        });
        input.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                ifocus = true;
            }

            @Override
            public void focusLost(FocusEvent e) {
                ifocus = false;
            }
        });

        vScroll.getHorizontalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(AdjustmentEvent event) {
                if (!eScroll.contains(event.getValue())) {
                    eScroll.add(event.getValue());
                    hScroll.setValue(event.getValue());
                } else {
                    eScroll.remove((Object) event.getValue());
                }
            }
        });
        hScroll.addAdjustmentListener(new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(AdjustmentEvent event) {
                if (!eScroll.contains(event.getValue())) {
                    eScroll.add(event.getValue());
                    vScroll.getHorizontalScrollBar().setValue(event.getValue());
                } else {
                    eScroll.remove((Object) event.getValue());
                }
            }
        });

        new TextFieldPopup(findT, false);
        findT.setBorder(BorderFactory.createLineBorder(new Color(40, 44, 45), 4));
        ((AbstractDocument) findT.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                super.insertString(fb, offset, string, attr);
                findI = 0;
                findO = 0;
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                super.replace(fb, offset, length, text, attrs);
                findI = 0;
                findO = 0;
            }

            @Override
            public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
                super.remove(fb, offset, length);
                findI = 0;
                findO = 0;
            }
        });
        findP.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (findP.getModel().isPressed()) findP.setBackground(new Color(40, 44, 45));
                else findP.setBackground(new Color(69, 73, 74));
            }
        });
        findP.setBorder(new ButtonBorder(40, 44, 45, 4));
        findP.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                ConsoleWindow.this.find(false);
            }
        });
        findN.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (findN.getModel().isPressed()) findN.setBackground(new Color(40, 44, 45));
                else findN.setBackground(new Color(69, 73, 74));
            }
        });
        findN.setBorder(new ButtonBorder(40, 44, 45, 4));
        findN.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                ConsoleWindow.this.find(true);
            }
        });
        findD.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (findD.getModel().isPressed()) findD.setBackground(new Color(40, 44, 45));
                else findD.setBackground(new Color(69, 73, 74));
            }
        });
        findD.setBorder(new ButtonBorder(40, 44, 45, 4));
        findD.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                find.setVisible(false);
                findI = 0;
                findO = 0;
            }
        });


        if (logger.getHandler() instanceof SubServer) {
            for (SubServer.LoggedCommand command : ((SubServer) logger.getHandler()).getCommandHistory()) popup.commands.add(command.getCommand());
        } else {
            input.setVisible(false);
            hScroll.setVisible(false);
            vScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        }

        logger.registerFilter(this);
        log.setText(RESET_VALUE);
        loadContent();
        log();
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keys);
    }
    private void hScroll() {
        hScroll.setMaximum(vScroll.getHorizontalScrollBar().getMaximum());
        hScroll.setMinimum(vScroll.getHorizontalScrollBar().getMinimum());
        hScroll.setVisibleAmount(vScroll.getHorizontalScrollBar().getVisibleAmount());
        hScroll.setVisible(input.isVisible() && hScroll.getVisibleAmount() < hScroll.getMaximum());
    }

    public SubLogger getLogger() {
        return logger;
    }

    public void log(Date date, String message) {
        try {
            messages.add(('\u00A0' + new SimpleDateFormat("hh:mm:ss").format(date) + ' ' + message + "\u00A0\n").getBytes("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void log(String message) {
        log(Calendar.getInstance().getTime(), message);
    }
    public void log(Date date, Level level, String message) {
        log(date, "[" + level.getLocalizedName() + "] " + message);
    }
    @Override
    public boolean log(Level level, String message) {
        log(Calendar.getInstance().getTime(), level, message);
        return !open;
    }

    private void log() {
        new Thread("SubServers.Console::Log_Spooler(" + logger.getName() + ")") {
            @Override
            public void run() {
                while (running) {
                    while (running && messages.size() > 0) try {
                        byte[] msg = (byte[]) Util.getDespiteException(new ExceptionReturnRunnable<Object>() {
                            @Override
                            public Object run() throws Throwable {
                                return messages.get(0);
                            }
                        }, null);
                        if (msg != null) {
                            filewriter.write(msg);
                            stream.write(msg);
                        }
                        try { ConsoleWindow.this.messages.remove(0); } catch (Throwable e) {}
                    } catch (Throwable e) {
                        try { ConsoleWindow.this.messages.remove(0); } catch (Throwable ex) {}
                        e.printStackTrace();
                    }
                    try { Thread.sleep(32); } catch (Throwable e) {}
                }
            }
        }.start();
    }

    public void clear() {
        log.setText(RESET_VALUE);
        hScroll();
    }

    public void open() {
        if (!open) {
            window.setVisible(true);
            this.open = true;
        }
        window.toFront();
    }

    public boolean isOpen() {
        return open;
    }

    @Override
    public void start() {}
    private void loadContent() {
        if (file != null) {
            try (FileInputStream reader = new FileInputStream(file)) {
                int b;
                while ((b = reader.read()) != -1) {
                    stream.write(b);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            hScroll();
        }
    }

    @Override
    public void stop() {
        plugin.onClose(this);
    }

    public void close() {
        if (open) {
            this.open = false;
            if (find.isVisible()) {
                find.setVisible(false);
                findI = 0;
                findO = 0;
            }
            window.setVisible(false);
        }
    }

    public void destroy() {
        close();
        running = false;
        logger.unregisterFilter(this);
        if (filewriter != null) try {
            filewriter.close();
        } catch (Exception e) {}
        if (file != null) file.delete();
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(keys);
    }

    private void find(boolean direction) {
        if (!direction) findI -= findO + 1;
        String find = findT.getText().toLowerCase();
        log.requestFocusInWindow();

        if (find.length() > 0) {
            Document document = log.getDocument();
            int findLength = find.length();
            try {
                boolean found = false;

                if (findI < 0 || findI + findLength >= document.getLength()) {
                    if (direction) {
                        findI = 1;
                    } else {
                        findI = document.getLength() - findLength;
                    }
                }

                while (findLength <= document.getLength()) {
                    String match = document.getText(findI, findLength).toLowerCase();
                    if (match.equals(find)) {
                        found = true;
                        break;
                    }
                    if (direction) findI++;
                    else findI--;
                }

                if (found) {
                    Rectangle viewRect = log.modelToView(findI);
                    log.scrollRectToVisible(viewRect);

                    log.setCaretPosition(findI + findLength);
                    log.moveCaretPosition(findI);

                    findI += findLength;
                    findO = findLength;
                }

            } catch (BadLocationException e) {
                findI = -2;
                JOptionPane.showMessageDialog(window,
                        ((findO > 0)?"There are no more results\nSearch again to start from the " + ((direction)?"top":"bottom"):"Couldn't find \"" + findT.getText() + "\""),
                        "Find",
                        JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private class TextFieldPopup extends JPanel {
        protected LinkedList<String> commands = new LinkedList<String>();
        protected Boolean history = false;
        protected int hpos = -1;
        protected String hcache = "";

        public TextFieldPopup(JTextComponent field, boolean command) {
            JPopupMenu menu = new JPopupMenu();

            if (field.isEditable()) {
                if (command) {
                    Action backward = new TextAction("Previous Command") {
                        public void actionPerformed(ActionEvent e) {
                            prev(getFocusedComponent());
                        }
                    };
                    menu.add(backward);

                    Action forward = new TextAction("Next Command") {
                        public void actionPerformed(ActionEvent e) {
                            next(getFocusedComponent());
                        }
                    };
                    menu.add(forward);
                    menu.addSeparator();
                }

                Action cut = new DefaultEditorKit.CutAction();
                cut.putValue(Action.NAME, "Cut");
                cut.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke('X', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), true));
                menu.add(cut);
            }

            Action copy = new DefaultEditorKit.CopyAction();
            copy.putValue(Action.NAME, "Copy");
            copy.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke('C', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), true));
            menu.add(copy);

            if (field.isEditable()) {
                Action paste = new DefaultEditorKit.PasteAction();
                paste.putValue(Action.NAME, "Paste");
                paste.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke('V', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), true));
                menu.add(paste);
            }

            Action find = new TextAction("Find Selection") {
                public void actionPerformed(ActionEvent e) {
                    JTextComponent field = getFocusedComponent();
                    if (field.getSelectedText() != null && field.getSelectedText().length() > 0) {
                        findT.setText(field.getSelectedText());
                        findI = 0;
                        findO = 0;
                        ConsoleWindow.this.find.setVisible(true);
                        find(true);
                    }
                }
            };
            find.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke('F', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() + KeyEvent.SHIFT_MASK, true));
            menu.add(find);

            Action selectAll = new TextAction("Select All") {
                public void actionPerformed(ActionEvent e) {
                    JTextComponent field = getFocusedComponent();
                    field.selectAll();
                    field.requestFocusInWindow();
                }
            };
            selectAll.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke('A', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), true));
            menu.add(selectAll);

            field.setComponentPopupMenu(menu);
        }

        public void next(JTextComponent field) {
            if (field.isEditable()) {
                LinkedList<String> list = new LinkedList<String>(commands);
                Collections.reverse(list);
                if (history == Boolean.FALSE) {
                    hcache = (field.getText().startsWith(">"))?field.getText().substring(1):field.getText();
                    hpos = -1;
                } else {
                    hpos--;
                    if (hpos < -1) hpos = -1;
                }
                if (hpos >= 0) {
                    history = true;
                    field.setText(list.get(hpos));
                } else field.setText(hcache);
                field.setCaretPosition(field.getText().length());
            }
        }

        public void prev(JTextComponent field) {
            if (field.isEditable()) {
                LinkedList<String> list = new LinkedList<String>(commands);
                Collections.reverse(list);
                if (history == Boolean.FALSE) {
                    hcache = (field.getText().startsWith(">"))?field.getText().substring(1):field.getText();
                    hpos = 0;
                } else {
                    hpos++;
                }
                if (hpos >= list.size()) hpos = list.size() - 1;
                if (hpos >= 0) {
                    history = true;
                    field.setText(list.get(hpos));
                }
            }
        }
    }
    private class AnsiUIOutputStream extends HTMLogger {
        public AnsiUIOutputStream(OutputStream raw, OutputStream wrapped) {
            super(raw, wrapped);
        }

        public void ansi(boolean value) {
            ansi = value;
        }

        @Override
        protected void processChangeWindowTitle(String label) {
            window.setTitle(logger.getName() + " \u2014 SubServers 2" + ((label.length() <= 0)?"":" \u2014 " + label));
        }

        @Override
        protected void processEraseLine(int mode) throws IOException {
            processDeleteLine(1);
        }

        @Override
        protected void processEraseScreen(int mode) throws IOException {
            if (ansi) spost.add(new Runnable() {
                @Override
                public void run() {
                    log.setText(RESET_VALUE);
                }
            });
        }

        @Override
        protected void processDeleteLine(final int amount) throws IOException {
            if (ansi) spost.add(new Runnable() {
                @Override
                public void run() {
                    try {
                        String content = log.getDocument().getText(0, log.getDocument().getLength());
                        int lineBreak = log.getDocument().getLength() - 2;
                        for (int lines = 0; lines < amount; lines++) lineBreak = content.lastIndexOf('\n', lineBreak - 1);
                        if (lineBreak < 2) lineBreak = 2;
                        int length = log.getDocument().getLength() - lineBreak - 2;
                        if (length > 0) {
                            while (log.getSelectionStart() != log.getSelectionEnd()) Thread.sleep(100);
                            log.getDocument().remove(lineBreak, length);
                        }
                    } catch (Exception e) {}
                }
            });
        }

        @Override
        public void close() throws IOException {
            open = false;
            super.close();
        }
    }
    private class SmartScroller implements AdjustmentListener {
        public final static int HORIZONTAL = 0;
        public final static int VERTICAL = 1;

        public final static int START = 0;
        public final static int END = 1;

        private int viewportPosition;

        private JScrollBar scrollBar;
        private boolean adjustScrollBar = true;

        private int previousValue = -1;
        private int previousMaximum = -1;

        public SmartScroller(JScrollPane scrollPane)
        {
            this(scrollPane, VERTICAL, END);
        }

        public SmartScroller(JScrollPane scrollPane, int viewportPosition)
        {
            this(scrollPane, VERTICAL, viewportPosition);
        }

        public SmartScroller(JScrollPane scrollPane, int scrollDirection, int viewportPosition)
        {
            if (scrollDirection != HORIZONTAL
                    &&  scrollDirection != VERTICAL)
                throw new IllegalArgumentException("invalid vScroll direction specified");

            if (viewportPosition != START
                    &&  viewportPosition != END)
                throw new IllegalArgumentException("invalid viewport position specified");

            this.viewportPosition = viewportPosition;

            if (scrollDirection == HORIZONTAL)
                scrollBar = scrollPane.getHorizontalScrollBar();
            else
                scrollBar = scrollPane.getVerticalScrollBar();

            scrollBar.addAdjustmentListener( this );

            //  Turn off automatic scrolling for text components

            Component view = scrollPane.getViewport().getView();

            if (view instanceof JTextComponent)
            {
                JTextComponent textComponent = (JTextComponent)view;
                DefaultCaret caret = (DefaultCaret)textComponent.getCaret();
                caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
            }
        }

        @Override
        public void adjustmentValueChanged(final AdjustmentEvent e)
        {
            checkScrollBar(e);
        }

        /*
         *  Analyze every adjustment event to determine when the viewport
         *  needs to be repositioned.
         */
        private void checkScrollBar(AdjustmentEvent e)
        {
            //  The vScroll bar listModel contains information needed to determine
            //  whether the viewport should be repositioned or not.

            JScrollBar scrollBar = (JScrollBar)e.getSource();
            BoundedRangeModel listModel = scrollBar.getModel();
            int value = listModel.getValue();
            int extent = listModel.getExtent();
            int maximum = listModel.getMaximum();

            boolean valueChanged = previousValue != value;
            boolean maximumChanged = previousMaximum != maximum;

            //  Check if the user has manually repositioned the scrollbar

            if (valueChanged && !maximumChanged)
            {
                if (viewportPosition == START)
                    adjustScrollBar = value != 0;
                else
                    adjustScrollBar = value + extent >= maximum;
            }

            //  Reset the "value" so we can reposition the viewport and
            //  distinguish between a user vScroll and a program vScroll.
            //  (ie. valueChanged will be false on a program vScroll)

            if (adjustScrollBar && viewportPosition == END)
            {
                //  Scroll the viewport to the end.
                scrollBar.removeAdjustmentListener( this );
                value = maximum - extent;
                scrollBar.setValue( value );
                scrollBar.addAdjustmentListener( this );
            }

            if (adjustScrollBar && viewportPosition == START)
            {
                //  Keep the viewport at the same relative viewportPosition
                scrollBar.removeAdjustmentListener( this );
                value = value + maximum - previousMaximum;
                scrollBar.setValue( value );
                scrollBar.addAdjustmentListener( this );
            }

            previousValue = value;
            previousMaximum = maximum;
        }
    }
    private class ButtonBorder implements Border {
        private int radius;
        private Color color;

        public ButtonBorder(int red, int green, int blue, int radius) {
            this.color = new Color(red, green, blue);
            this.radius = radius;
        }

        public Insets getBorderInsets(Component c) {
            return new Insets(this.radius+1, this.radius+1, this.radius+2, this.radius);
        }

        public boolean isBorderOpaque() {
            return true;
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            g.setColor(color);
            g.drawRoundRect(x, y, width-1, height-1, radius, radius);
        }
    }
}
