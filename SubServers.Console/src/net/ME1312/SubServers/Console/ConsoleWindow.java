package net.ME1312.SubServers.Console;

import net.ME1312.SubServers.Bungee.Host.SubLogFilter;
import net.ME1312.SubServers.Bungee.Host.SubServer;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.logging.Level;

public class ConsoleWindow extends JFrame implements SubLogFilter {
    private JPanel window;
    private JTextField input;
    private TextFieldPopup popup;
    private JTextArea log;
    private JScrollPane scroll;
    private boolean open;
    private SubServer server;

    public ConsoleWindow(SubServer server) {
        this.server = server;
        this.open = false;

        JMenuBar jMenu = new JMenuBar();
        JMenu menu = new JMenu("View");
        JMenuItem item = new JMenuItem("Scroll to Top");
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_UP, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), true));
        item.addActionListener(event -> scroll.getVerticalScrollBar().setValue(0));
        menu.add(item);
        item = new JMenuItem("Scroll to Bottom");
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), true));
        item.addActionListener(event -> scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum() - scroll.getVerticalScrollBar().getVisibleAmount()));
        menu.add(item);
        menu.addSeparator();
        item = new JMenuItem("Reset Text Size");
        item.addActionListener(event -> log.setFont(new Font(log.getFont().getName(), log.getFont().getStyle(), 12)));
        menu.add(item);
        item = new JMenuItem("Bigger Text");
        item.setAccelerator(KeyStroke.getKeyStroke('=', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() + KeyEvent.SHIFT_MASK, true));
        item.addActionListener(event -> log.setFont(new Font(log.getFont().getName(), log.getFont().getStyle(), log.getFont().getSize() + 2)));
        menu.add(item);
        item = new JMenuItem("Smaller Text");
        item.setAccelerator(KeyStroke.getKeyStroke('-', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), true));
        item.addActionListener(event -> log.setFont(new Font(log.getFont().getName(), log.getFont().getStyle(), log.getFont().getSize() - 2)));
        menu.add(item);
        jMenu.add(menu);
        setJMenuBar(jMenu);
        setContentPane(window);
        pack();
        setTitle(server.getName() + " \u2014 SubServers 2");
        setSize(1024, 576);
        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (int) ((dimension.getWidth() - getWidth()) / 2);
        int y = (int) ((dimension.getHeight() - getHeight()) / 2);
        setLocation(x, y);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                close();
            }
        });
        scroll.setBorder(BorderFactory.createLineBorder(new Color(40, 44, 45)));
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        new SmartScroller(scroll, SmartScroller.VERTICAL, SmartScroller.END);
        log.setText("\n");
        log.setForeground(Color.WHITE);
        log.setBorder(BorderFactory.createLineBorder(new Color(40, 44, 45)));
        new TextFieldPopup(log, false);
        input.setForeground(Color.WHITE);
        input.setBorder(BorderFactory.createLineBorder(new Color(40, 44, 45)));
        popup = new TextFieldPopup(input, true);
        input.addActionListener(event -> {
            if (input.getText().length() > 0 && !input.getText().equals("/")) server.command((input.getText().startsWith("/"))?input.getText().substring(1):input.getText());
            popup.commands.add(input.getText());
            popup.current = 0;
            popup.last = true;
            input.setText("/");
        });
        input.setText("/");
    }

    @Override
    public boolean log(Level level, String message) {
        log.setText(log.getText() + ' ' + new SimpleDateFormat("hh:mm:ss").format(Calendar.getInstance().getTime()) + " [" + level.getLocalizedName() + "] " + message + " \n");
        return false;
    }

    public SubServer getServer() {
        return server;
    }

    public void open() {
        if (!open) {
            server.getLogger().registerFilter(this);
            setVisible(true);
            toFront();
            this.open = true;
        }
    }

    public void close() {
        if (open) {
            this.open = false;
            server.getLogger().unregisterFilter(this);
            setVisible(false);
        }
    }

    private class TextFieldPopup extends JPanel {
        protected LinkedList<String> commands = new LinkedList<String>();
        protected int current = 0;
        protected boolean last = true;

        public TextFieldPopup(JTextComponent field, boolean writable) {
            JPopupMenu menu = new JPopupMenu();

            if (writable) {
                Action backward = new TextAction("Previous Command") {
                    public void actionPerformed(ActionEvent e) {
                        JTextComponent field = getFocusedComponent();
                        LinkedList<String> list = new LinkedList<String>(commands);
                        Collections.reverse(list);
                        if (list.size() > current) {
                            if (!last) current++;
                            last = true;
                            field.setText(list.get(current++));
                        }

                    }
                };
                menu.add(backward);

                Action forward = new TextAction("Next Command") {
                    public void actionPerformed(ActionEvent e) {
                        JTextComponent field = getFocusedComponent();
                        LinkedList<String> list = new LinkedList<String>(commands);
                        Collections.reverse(list);
                        if (current > 0) {
                            if (last) current--;
                            last = false;
                            field.setText(list.get(--current));
                        } else field.setText("/");
                    }
                };
                menu.add(forward);
                menu.addSeparator();

                Action cut = new DefaultEditorKit.CutAction();
                cut.putValue(Action.NAME, "Cut");
                cut.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke('X', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), true));
                menu.add(cut);
            }

            Action copy = new DefaultEditorKit.CopyAction();
            copy.putValue(Action.NAME, "Copy");
            copy.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke('C', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), true));
            menu.add(copy);

            if (writable) {
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
                throw new IllegalArgumentException("invalid scroll direction specified");

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
            //  The scroll bar listModel contains information needed to determine
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
            //  distinguish between a user scroll and a program scroll.
            //  (ie. valueChanged will be false on a program scroll)

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
