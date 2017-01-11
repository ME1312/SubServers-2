package net.ME1312.SubServers.Console;

import net.ME1312.SubServers.Bungee.Host.SubLogFilter;
import net.ME1312.SubServers.Bungee.Host.SubLogger;
import net.ME1312.SubServers.Bungee.Host.SubServer;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.logging.Level;

public class ConsoleWindow implements SubLogFilter {
    private ConsolePlugin plugin;
    private JFrame window;
    private JPanel panel;
    private JTextField input;
    private boolean ifocus = false;
    private TextFieldPopup popup;
    private JTextArea log;
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
    private List<Runnable> events;
    private boolean running = false;
    private boolean open = false;
    private SubLogger logger;
    private KeyEventDispatcher keys = event -> {
        if (window.isVisible() && window.isFocused()) {
            if (event.getID() == KeyEvent.KEY_RELEASED) switch (event.getKeyCode()) {
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
                        find(true);
                    break;
            }

        }
        return false;
    };

    public ConsoleWindow(ConsolePlugin plugin, SubLogger logger) {
        this.plugin = plugin;
        this.logger = logger;
        this.events = new LinkedList<Runnable>();

        events.add(() -> {
            window = new JFrame();

            JMenuBar jMenu = new JMenuBar();
            JMenu menu = new JMenu("View");
            JMenuItem item = new JMenuItem("Scroll to Top");
            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_UP, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), true));
            item.addActionListener(event -> vScroll.getVerticalScrollBar().setValue(0));
            menu.add(item);
            item = new JMenuItem("Scroll to Bottom");
            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), true));
            item.addActionListener(event -> vScroll.getVerticalScrollBar().setValue(vScroll.getVerticalScrollBar().getMaximum() - vScroll.getVerticalScrollBar().getVisibleAmount()));
            menu.add(item);
            item = new JMenuItem("Find");
            item.setAccelerator(KeyStroke.getKeyStroke('F', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), true));
            item.addActionListener(event -> {
                if (find.isVisible()) {
                    find.setVisible(false);
                    findI = 0;
                    findO = 0;
                } else {
                    find.setVisible(true);
                    findT.selectAll();
                    findT.requestFocusInWindow();
                }
            });
            menu.add(item);
            menu.addSeparator();
            item = new JMenuItem("Reset Text Size");
            item.addActionListener(event -> {
                log.setFont(new Font(log.getFont().getName(), log.getFont().getStyle(), 12));
                SwingUtilities.invokeLater(this::hScroll);
            });
            menu.add(item);
            item = new JMenuItem("Bigger Text");
            item.setAccelerator(KeyStroke.getKeyStroke('=', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), true));
            item.addActionListener(event -> {
                log.setFont(new Font(log.getFont().getName(), log.getFont().getStyle(), log.getFont().getSize() + 2));
                SwingUtilities.invokeLater(this::hScroll);
            });
            menu.add(item);
            item = new JMenuItem("Smaller Text");
            item.setAccelerator(KeyStroke.getKeyStroke('-', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), true));
            item.addActionListener(event -> {
                log.setFont(new Font(log.getFont().getName(), log.getFont().getStyle(), log.getFont().getSize() - 2));
                SwingUtilities.invokeLater(this::hScroll);
            });
            menu.add(item);
            menu.addSeparator();
            item = new JMenuItem("Clear Screen");
            item.setAccelerator(KeyStroke.getKeyStroke('L', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), true));
            item.addActionListener(event -> clear());
            menu.add(item);
            item = new JMenuItem("Reload Log");
            item.setAccelerator(KeyStroke.getKeyStroke('R', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), true));
            item.addActionListener(event -> {
                log.setText("\n");
                for (SubLogger.LogMessage message : logger.getMessages()) log(message.getDate(), message.getLevel(), message.getMessage());
                SwingUtilities.invokeLater(this::hScroll);
            });
            menu.add(item);
            jMenu.add(menu);
            window.setJMenuBar(jMenu);
            window.setContentPane(panel);
            window.pack();
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
                    SwingUtilities.invokeLater(ConsoleWindow.this::hScroll);
                }
            });
            vScroll.setBorder(BorderFactory.createLineBorder(new Color(40, 44, 45)));
            new SmartScroller(vScroll, SmartScroller.VERTICAL, SmartScroller.END);
            log.setText("\n");
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
            input.setBorder(BorderFactory.createLineBorder(new Color(69, 73, 74)));
            input.addActionListener((ActionEvent event) -> {
                if (logger.getHandler() instanceof SubServer && input.getText().length() > 0 && !input.getText().equals(">")) {
                    if (((SubServer) logger.getHandler()).command((input.getText().startsWith(">")) ? input.getText().substring(1) : input.getText())) {
                        popup.commands.add((input.getText().startsWith(">")) ? input.getText().substring(1) : input.getText());
                        popup.current = 0;
                        popup.last = true;
                        input.setText("");
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
                        offset = 1;
                    }
                    super.replace(fb, offset, length, text, attrs);
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

            vScroll.getHorizontalScrollBar().addAdjustmentListener(event -> {
                if (!eScroll.contains(event.getValue())) {
                    eScroll.add(event.getValue());
                    hScroll.setValue(event.getValue());
                } else {
                    eScroll.remove((Object) event.getValue());
                }
            });
            hScroll.addAdjustmentListener(event -> {
                if (!eScroll.contains(event.getValue())) {
                    eScroll.add(event.getValue());
                    vScroll.getHorizontalScrollBar().setValue(event.getValue());
                } else {
                    eScroll.remove((Object) event.getValue());
                }
            });

            new TextFieldPopup(findT, false);
            findT.setBorder(BorderFactory.createLineBorder(new Color(69, 73, 74)));
            findP.addActionListener(event -> find(false));
            findN.addActionListener(event -> find(true));
            findD.addActionListener(event -> {
                find.setVisible(false);
                findI = 0;
                findO = 0;
            });


            if (!(logger.getHandler() instanceof SubServer)) {
                input.setVisible(false);
                hScroll.setVisible(false);
                vScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
            }

            logger.registerFilter(this);
            for (SubLogger.LogMessage message : logger.getMessages()) log(message.getDate(), message.getLevel(), message.getMessage());
            KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keys);
            events.add(() -> {
                if (logger.isLogging() && !open) open();
            });
            hScroll();
        });
        runEvents();
    }
    private void hScroll() {
        hScroll.setMaximum(vScroll.getHorizontalScrollBar().getMaximum());
        hScroll.setMinimum(vScroll.getHorizontalScrollBar().getMinimum());
        hScroll.setVisibleAmount(vScroll.getHorizontalScrollBar().getVisibleAmount());
    }

    public SubLogger getLogger() {
        return logger;
    }

    public void log(Date date, String message) {
        log.append(' ' + new SimpleDateFormat("hh:mm:ss").format(date) + ' ' + message + " \n");
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

    public void clear() {
        log.setText("\n");
        SwingUtilities.invokeLater(this::hScroll);
    }

    public boolean runEvents() {
        return runEvents(false);
    }

    private boolean runEvents(boolean force) {
        if (events.size() > 0 && (force || !running)) {
            try {
                running = true;
                final Runnable event = events.get(0);
                if (event != null) {
                    SwingUtilities.invokeLater(() -> {
                        try {
                            event.run();
                        } catch (Throwable e) {
                            new InvocationTargetException(e, "Exception while running SubServers Console Window Event").printStackTrace();
                        }
                        events.remove(0);
                        if (!runEvents(true)) running = false;
                    });
                } else {
                    events.remove(0);
                    if (!runEvents(true)) running = false;
                }
                return true;
            } catch (NullPointerException e) {
                return false;
            }
        } else return false;
    }

    @Override
    public void start() {
        open();
    }
    public void open() {
        events.add(() -> {
            if (!open) {
                window.setVisible(true);
                this.open = true;
            }
            window.toFront();
        });
        runEvents();
    }

    public boolean isOpen() {
        return open;
    }

    @Override
    public void stop() {
        close();
    }
    public void close() {
        events.add(() -> {
            if (open) {
                this.open = false;
                if (find.isVisible()) {
                    find.setVisible(false);
                    findI = 0;
                    findO = 0;
                }
                window.setVisible(false);
                plugin.onClose(this);
            }
        });
        runEvents();
    }

    public void destroy() {
        close();
        logger.unregisterFilter(this);
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

                if (findI + findLength >= document.getLength()) findI = 1;

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
                findI = log.getText().length() - 1;
                JOptionPane.showMessageDialog(window,
                        "There are no more results\nSearch again to start from the " + ((direction)?"top":"bottom"),
                        "Find",
                        JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private class TextFieldPopup extends JPanel {
        protected LinkedList<String> commands = new LinkedList<String>();
        protected int current = 0;
        protected boolean last = true;

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
                if (current > 0) {
                    if (last && current != 1) current--;
                    last = false;
                    field.setText(list.get(--current));
                } else field.setText("");
                field.setCaretPosition(field.getText().length());
            }
        }

        public void prev(JTextComponent field) {
            if (field.isEditable()) {
                LinkedList<String> list = new LinkedList<String>(commands);
                Collections.reverse(list);
                if (list.size() > current) {
                    if (!last) current++;
                    last = true;
                    field.setText(list.get(current++));
                    field.setCaretPosition(field.getText().length());
                }
            }
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
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    checkScrollBar(e);
                }
            });
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
}
