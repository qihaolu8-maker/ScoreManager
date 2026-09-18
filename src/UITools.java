import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ColorUIResource;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class UITools {

    /** Keep windows inside the current monitor's usable logical bounds, including HiDPI displays. */
    public static void fitWindowToScreen(Window window, Dimension preferredSize, Dimension minimumSize) {
        GraphicsConfiguration configuration = window.getGraphicsConfiguration();
        Rectangle bounds = configuration.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration);
        int availableWidth = Math.max(1, bounds.width - insets.left - insets.right);
        int availableHeight = Math.max(1, bounds.height - insets.top - insets.bottom);
        window.setMinimumSize(new Dimension(Math.min(minimumSize.width, availableWidth),
                Math.min(minimumSize.height, availableHeight)));
        window.setSize(Math.min(preferredSize.width, availableWidth),
                Math.min(preferredSize.height, availableHeight));
    }

    /** Preserve working control sizes and expose scrollbars when the screen is too small. */
    public static JScrollPane createAdaptiveScrollPane(JComponent content, Dimension minimumContentSize) {
        class AdaptivePanel extends JPanel implements Scrollable {
            AdaptivePanel() {
                super(new BorderLayout());
                setOpaque(false);
                add(content, BorderLayout.CENTER);
            }
            @Override public Dimension getPreferredSize() {
                return new Dimension(minimumContentSize);
            }
            @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
            @Override public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) { return 24; }
            @Override public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) {
                return Math.max(24, (orientation == SwingConstants.HORIZONTAL ? r.width : r.height) - 24);
            }
            @Override public boolean getScrollableTracksViewportWidth() {
                return getParent() instanceof JViewport && getParent().getWidth() >= minimumContentSize.width;
            }
            @Override public boolean getScrollableTracksViewportHeight() {
                return getParent() instanceof JViewport && getParent().getHeight() >= minimumContentSize.height;
            }
        }
        JScrollPane scrollPane = new JScrollPane(new AdaptivePanel());
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(24);
        scrollPane.getVerticalScrollBar().setUnitIncrement(24);
        return scrollPane;
    }

    // ✨ 1. 开启极致现代化隐藏开关
    public static void setupGlobalAppleStyle() {
        Font globalFont = new Font(Font.DIALOG, Font.PLAIN, 15); // 字体稍微调小一点点，更精致
        Font boldFont = new Font(Font.DIALOG, Font.BOLD, 15);
        String[] fonts = {"defaultFont", "Button.font", "Label.font", "TextField.font", "ComboBox.font", "Table.font", "Tree.font"};
        for (String f : fonts) UIManager.put(f, globalFont);
        UIManager.put("TableHeader.font", boldFont);
        UIManager.put("TabbedPane.font", boldFont);

        // ⚡️ 核心现代化改造
        UIManager.put("Component.arc", 999);        // 让所有输入框、下拉框变成圆润的胶囊状
        UIManager.put("Button.arc", 999);           // 按钮胶囊状
        UIManager.put("ProgressBar.arc", 999);
        UIManager.put("TextComponent.arc", 999);

        // ⚡️ 彻底消灭丑陋的边框
        UIManager.put("ScrollPane.border", BorderFactory.createEmptyBorder()); // 移除滚动面板边框
        UIManager.put("Table.showVerticalLines", false);                       // 移除表格竖线（关键！）
        UIManager.put("Table.showHorizontalLines", true);                      // 保留微弱的横线
        UIManager.put("Table.intercellSpacing", new Dimension(0, 1));

        // 🚀🚀🚀 核心修复：滚动条兼顾现代美学与绝对的实用性！
        UIManager.put("ScrollBar.showButtons", true);                   // ✨ 恢复上下左右的点击按钮
        UIManager.put("ScrollBar.thumbArc", 999);                       // 保持滑块是圆润的胶囊形
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.width", 16);                           // ✨ 从瞎眼的 8px 加粗到饱满的 16px
        UIManager.put("ScrollBar.track", new Color(130, 130, 130, 25)); // ✨ 铺上一层极淡的灰色底板，划定明显的滑动界限

        // ⚡️ Tab 标签页现代化（采用现代下划线风格，而非旧版按钮块）
        UIManager.put("TabbedPane.tabType", "underlined");
        UIManager.put("TabbedPane.tabHeight", 40);
        UIManager.put("TabbedPane.tabInsets", new Insets(0, 20, 0, 20));
        UIManager.put("TabbedPane.selectedBackground", new Color(0, 0, 0, 0));    // 选中背景透明
    }

    // ✨ 2. 注入“弥散阴影”的高级悬浮卡片
    public static JPanel createGlassPanel() {
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                boolean isDark = UIManager.getBoolean("laf.dark");
                String currentTheme = (String) UIManager.get("ScoreManager.CurrentTheme");
                int arc = 30; // 圆角大小

                // 🌟 绘制多层柔和的弥散阴影 (Drop Shadow)
                g2.setColor(isDark ? new Color(0, 0, 0, 20) : new Color(0, 0, 0, 8));
                for (int i = 0; i < 6; i++) {
                    g2.fillRoundRect(6 - i, 6 - i, getWidth() - (6 - i) * 2, getHeight() - (6 - i) * 2, arc, arc);
                }

                // 🌟 绘制实体玻璃面板
                if (isDark) {
                    g2.setColor(new Color(45, 50, 60, 230)); // 深色模式用更高级的实心灰
                } else if ("粉白".equals(currentTheme)) {
                    g2.setColor(new Color(255, 248, 250, 240));
                } else {
                    g2.setColor(new Color(255, 255, 255, 240));
                }

                // 留出边距给阴影，画出主面板
                g2.fillRoundRect(3, 3, getWidth() - 6, getHeight() - 6, arc, arc);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        // 给内部组件留出阴影呼吸空间
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        return p;
    }

    public static void setupTableStyle(JTable table, int rowHeight) {
        table.setRowHeight(rowHeight);
        table.setShowGrid(true);
        // 极淡的横向网格线
        table.setGridColor(UIManager.getBoolean("laf.dark") ? new Color(255, 255, 255, 15) : new Color(0, 0, 0, 10));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        // 移除表头的边框，使其更扁平
        table.getTableHeader().setBorder(BorderFactory.createEmptyBorder());

        table.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                Integer prevHoveredRow = (Integer) table.getClientProperty("hoveredRow");
                if (prevHoveredRow == null || prevHoveredRow != row) {
                    table.putClientProperty("hoveredRow", row);
                    table.repaint();
                }
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                table.putClientProperty("hoveredRow", -1);
                table.repaint();
            }
        });

        DefaultTableCellRenderer hoverRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    Integer hoveredRow = (Integer) table.getClientProperty("hoveredRow");
                    if (hoveredRow != null && hoveredRow == row) {
                        boolean isDark = UIManager.getBoolean("laf.dark");
                        String currentTheme = (String) UIManager.get("ScoreManager.CurrentTheme");

                        if (isDark) c.setBackground(new Color(60, 70, 85));
                        else if ("粉白".equals(currentTheme)) c.setBackground(new Color(255, 235, 240));
                        else c.setBackground(new Color(235, 245, 255));
                    } else {
                        Color altColor = UIManager.getColor("Table.alternateRowColor");
                        Color bgColor = UIManager.getColor("Table.background");
                        if (row % 2 == 1 && altColor != null) c.setBackground(altColor);
                        else c.setBackground(bgColor != null ? bgColor : table.getBackground());
                    }
                }
                // 去掉原本因为 Border 产生的焦点虚线框
                ((JComponent) c).setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                return c;
            }
        };
        hoverRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++)
            table.getColumnModel().getColumn(i).setCellRenderer(hoverRenderer);
    }

    public static JPanel wrap(AbstractButton btn) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(btn);
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
        });
        return wrapper;
    }

    public static void addHoverEffectOnly(AbstractButton btn) {
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
        });
    }

    public static void enableTouchScrolling(JComponent component) {
        MouseAdapter touchAdapter = new MouseAdapter() {
            private Point origin;

            @Override
            public void mousePressed(MouseEvent e) {
                origin = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (origin != null) {
                    JViewport viewPort = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, component);
                    if (viewPort != null) {
                        int deltaX = origin.x - e.getX(), deltaY = origin.y - e.getY();
                        Point viewPos = viewPort.getViewPosition();
                        viewPos.translate(deltaX, deltaY);
                        int maxX = component.getWidth() - viewPort.getWidth(), maxY = component.getHeight() - viewPort.getHeight();
                        viewPos.x = Math.max(0, Math.min(viewPos.x, maxX));
                        viewPos.y = Math.max(0, Math.min(viewPos.y, maxY));
                        viewPort.setViewPosition(viewPos);
                    }
                }
            }
        };
        component.addMouseListener(touchAdapter);
        component.addMouseMotionListener(touchAdapter);
    }

    public static void applyTheme(String themeName, JFrame frame) {
        try {
            UIManager.put("ScoreManager.CurrentTheme", themeName);

            if ("深邃夜间".equals(themeName)) {
                com.formdev.flatlaf.FlatDarkLaf.setup();

                ColorUIResource darkBg = new ColorUIResource(35, 38, 45); // 更深的背景
                ColorUIResource darkAlt = new ColorUIResource(40, 44, 52); // 交替行
                ColorUIResource darkHeader = new ColorUIResource(45, 50, 60);
                ColorUIResource darkSelection = new ColorUIResource(60, 110, 180); // 高亮科技蓝

                UIManager.put("Table.background", darkBg);
                UIManager.put("Table.alternateRowColor", darkAlt);
                UIManager.put("Tree.background", darkBg);
                UIManager.put("Tree.textBackground", darkBg);
                UIManager.put("Viewport.background", darkBg);
                UIManager.put("TextField.background", darkAlt);
                UIManager.put("ComboBox.background", darkAlt);
                UIManager.put("List.background", darkBg);
                UIManager.put("Panel.background", darkBg);
                UIManager.put("TabbedPane.background", darkBg);
                UIManager.put("TabbedPane.contentAreaColor", darkBg);
                UIManager.put("TableHeader.background", darkHeader);

                UIManager.put("Tree.selectionBackground", darkSelection);
                UIManager.put("List.selectionBackground", darkSelection);
                UIManager.put("Table.selectionBackground", darkSelection);
                UIManager.put("Table.selectionInactiveBackground", new ColorUIResource(50, 60, 75));

                if (frame != null) frame.getContentPane().setBackground(new Color(25, 28, 33)); // 极暗的大背板

            } else if ("粉白".equals(themeName)) {
                com.formdev.flatlaf.FlatLightLaf.setup();
                UIManager.put("Component.accentColor", "#FF69B4");

                ColorUIResource softPink = new ColorUIResource(255, 252, 253);
                ColorUIResource softPinkAlt = new ColorUIResource(255, 246, 250);
                ColorUIResource softPinkHeader = new ColorUIResource(255, 240, 245);
                ColorUIResource pinkSelection = new ColorUIResource(255, 182, 193);

                UIManager.put("Table.background", softPink);
                UIManager.put("Table.alternateRowColor", softPinkAlt);
                UIManager.put("Tree.background", softPink);
                UIManager.put("Tree.textBackground", softPink);
                UIManager.put("Viewport.background", softPink);
                UIManager.put("TextField.background", softPink);
                UIManager.put("ComboBox.background", softPink);
                UIManager.put("List.background", softPink);
                UIManager.put("Panel.background", softPink);
                UIManager.put("TabbedPane.background", softPink);
                UIManager.put("TabbedPane.contentAreaColor", softPink);
                UIManager.put("TableHeader.background", softPinkHeader);

                UIManager.put("Tree.selectionBackground", pinkSelection);
                UIManager.put("List.selectionBackground", pinkSelection);
                UIManager.put("Table.selectionBackground", pinkSelection);
                UIManager.put("Table.selectionInactiveBackground", new ColorUIResource(255, 220, 230));

                if (frame != null) frame.getContentPane().setBackground(new Color(255, 240, 245)); // 更粉的底色

            } else if ("白绿".equals(themeName)) {
                com.formdev.flatlaf.FlatLightLaf.setup();
                UIManager.put("Component.accentColor", "#2e8b57");

                ColorUIResource softGreen = new ColorUIResource(250, 254, 250);
                ColorUIResource softGreenAlt = new ColorUIResource(242, 250, 242);
                ColorUIResource softGreenHeader = new ColorUIResource(235, 245, 235);
                ColorUIResource greenSelection = new ColorUIResource(143, 188, 143);

                UIManager.put("Table.background", softGreen);
                UIManager.put("Table.alternateRowColor", softGreenAlt);
                UIManager.put("Tree.background", softGreen);
                UIManager.put("Tree.textBackground", softGreen);
                UIManager.put("Viewport.background", softGreen);
                UIManager.put("TextField.background", softGreen);
                UIManager.put("ComboBox.background", softGreen);
                UIManager.put("List.background", softGreen);
                UIManager.put("Panel.background", softGreen);
                UIManager.put("TabbedPane.background", softGreen);
                UIManager.put("TabbedPane.contentAreaColor", softGreen);
                UIManager.put("TableHeader.background", softGreenHeader);

                UIManager.put("Tree.selectionBackground", greenSelection);
                UIManager.put("List.selectionBackground", greenSelection);
                UIManager.put("Table.selectionBackground", greenSelection);
                UIManager.put("Table.selectionInactiveBackground", new ColorUIResource(200, 225, 200));

                if (frame != null) frame.getContentPane().setBackground(new Color(238, 248, 240));

            } else {
                com.formdev.flatlaf.FlatLightLaf.setup();
                UIManager.put("Component.accentColor", "#007aff");

                ColorUIResource softBlue = new ColorUIResource(255, 255, 255);
                ColorUIResource softBlueAlt = new ColorUIResource(248, 251, 255);
                ColorUIResource softBlueHeader = new ColorUIResource(242, 248, 255);

                UIManager.put("Table.background", softBlue);
                UIManager.put("Table.alternateRowColor", softBlueAlt);
                UIManager.put("Tree.background", softBlue);
                UIManager.put("Tree.textBackground", softBlue);
                UIManager.put("Viewport.background", softBlue);
                UIManager.put("TextField.background", softBlue);
                UIManager.put("ComboBox.background", softBlue);
                UIManager.put("List.background", softBlue);
                UIManager.put("Panel.background", softBlue);
                UIManager.put("TabbedPane.background", softBlue);
                UIManager.put("TabbedPane.contentAreaColor", softBlue);
                UIManager.put("TableHeader.background", softBlueHeader);

                UIManager.put("Tree.selectionBackground", new ColorUIResource(180, 215, 255));
                UIManager.put("List.selectionBackground", new ColorUIResource(180, 215, 255));
                UIManager.put("Table.selectionBackground", new ColorUIResource(180, 215, 255));
                UIManager.put("Table.selectionInactiveBackground", new ColorUIResource(225, 235, 250));

                if (frame != null) frame.getContentPane().setBackground(new Color(238, 245, 255));
            }

            // 重新应用样式
            setupGlobalAppleStyle();
            com.formdev.flatlaf.FlatLaf.updateUI();
            for (Window w : Window.getWindows()) {
                SwingUtilities.updateComponentTreeUI(w);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class Toast extends JDialog {
        public Toast(JFrame owner, String message, Color bgColor) {
            super(owner);
            setUndecorated(true);
            setFocusableWindowState(false);
            setLayout(new BorderLayout());
            setBackground(new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue()));
            GraphicsDevice device = getGraphicsConfiguration().getDevice();
            if (device.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT)
                    && getGraphicsConfiguration().isTranslucencyCapable()) {
                try {
                    setBackground(new Color(0, 0, 0, 0));
                } catch (UnsupportedOperationException ignored) {
                    // Some Linux window managers advertise translucency without implementing it.
                }
            }

            JPanel panel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    // Toast 的高级阴影
                    g2.setColor(new Color(0, 0, 0, 30));
                    g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 40, 40);

                    g2.setColor(bgColor);
                    g2.fillRoundRect(0, 0, getWidth() - 4, getHeight() - 4, 40, 40);
                    g2.dispose();
                }
            };
            panel.setOpaque(false);
            panel.setBorder(new EmptyBorder(12, 25, 12, 25));
            JLabel label = new JLabel(message);
            label.setForeground(Color.WHITE);
            label.setFont(new Font(Font.DIALOG, Font.BOLD, 15));
            panel.add(label);
            add(panel);
            pack();
        }

        public static void showSuccess(JFrame owner, String message) {
            show(owner, message, new Color(60, 179, 113, 230));
        }

        public static void showInfo(JFrame owner, String message) {
            show(owner, message, new Color(100, 149, 237, 230));
        }

        private static void show(JFrame owner, String message, Color bgColor) {
            if (!SwingUtilities.isEventDispatchThread()) {
                SwingUtilities.invokeLater(() -> show(owner, message, bgColor));
                return;
            }
            Toast toast = new Toast(owner, message, bgColor);
            toast.setLocationRelativeTo(owner);
            toast.setLocation(toast.getLocation().x, toast.getLocation().y - owner.getHeight() / 3);
            boolean[] animate = {toast.getGraphicsConfiguration().getDevice()
                    .isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.TRANSLUCENT)};
            if (animate[0]) {
                try {
                    toast.setOpacity(0.0f);
                } catch (UnsupportedOperationException ignored) {
                    animate[0] = false;
                }
            }
            toast.setVisible(true);
            long start = System.nanoTime();
            Timer timer = new Timer(20, e -> {
                long elapsed = (System.nanoTime() - start) / 1_000_000;
                if (elapsed >= 1900 || !toast.isDisplayable()) {
                    ((Timer) e.getSource()).stop();
                    toast.dispose();
                    return;
                }
                if (animate[0]) {
                    float opacity = elapsed < 200 ? elapsed / 200.0f
                            : elapsed > 1700 ? (1900 - elapsed) / 200.0f : 1.0f;
                    try {
                        toast.setOpacity(opacity);
                    } catch (UnsupportedOperationException ignored) {
                        animate[0] = false;
                    }
                }
            });
            timer.start();
        }
    }
}
