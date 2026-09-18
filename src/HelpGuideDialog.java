import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class HelpGuideDialog extends JDialog {
    public HelpGuideDialog(JFrame parentFrame) {
        super(parentFrame, "ℹ️ 快捷键与系统操作指南", true);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        UITools.fitWindowToScreen(this, new Dimension(640, 720), new Dimension(480, 360));

        setLocationRelativeTo(parentFrame);
        setLayout(new BorderLayout(15, 15));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(25, 30, 25, 30));
        getContentPane().setBackground(parentFrame.getContentPane().getBackground());

        JLabel titleLabel = new JLabel("🚀 ScoreManager 核心操作指南");
        titleLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 22));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(titleLabel, BorderLayout.NORTH);

        JPanel glassPanel = UITools.createGlassPanel();
        glassPanel.setLayout(new BorderLayout());
        glassPanel.setBorder(new EmptyBorder(20, 25, 20, 25));

        // 实现 Scrollable 接口，强迫面板必须根据外层窗口宽度进行极限缩放
        class WrapPanel extends JPanel implements Scrollable {
            public WrapPanel() {
                super(new GridBagLayout());
                setOpaque(false);
            }
            @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
            @Override public int getScrollableUnitIncrement(Rectangle v, int o, int d) { return 16; }
            @Override public int getScrollableBlockIncrement(Rectangle v, int o, int d) { return 16; }
            @Override public boolean getScrollableTracksViewportWidth() { return true; }
            @Override public boolean getScrollableTracksViewportHeight() { return false; }
        }

        WrapPanel textPanel = new WrapPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        String shortcutKey = System.getProperty("os.name", "").startsWith("Mac") ? "Command" : "Ctrl";
        String[] helps = {
                "【键盘与基础快捷键】",
                "⌨️ " + shortcutKey + " + S：强制将当前所有数据安全写入本地 SQLite 数据库",
                "⌨️ " + shortcutKey + " + Z：智能撤销上一步操作 (大数据量下自动开启内存保护机制)",
                "🔍 搜索框：输入姓名、学号、班级或专业筛选学生",
                "🗑️ Delete 键：选中学生后移至回收站；Mac 紧凑键盘使用 Fn + Delete",
                "",
                "【鼠标与高级交互体验】",
                "🖱️ 双击排名、专业或班级列：打开该学生的档案与加分时间轴",
                "🖱️ 双击学号、姓名或总分单元格：直接修改内容，按 Enter 确认",
                "🖱️ 左侧树状拖拽：鼠标长按班级节点，可跨专业随意拖拽，实现组织架构重组",
                "🖱️ 表格支持鼠标拖动滚动；小屏幕可使用主窗口外侧滚动条访问完整内容",
                "",
                "【课堂积分与日志追踪】",
                "🎀 极速快捷加分：底部面板选定学生后，通过预设分值下拉框一键完成高频加扣分",
                "✍️ 完整事由录入：支持手动输入精确分值与详细备注，敲击 Enter 极速提交",
                "📜 历史变动日志：记录分数增减、时间及备注，支持筛选和修改备注",
                "",
                "【数据可视化与报表分发】",
                "📈 实时数据大屏：自动渲染各班平均分对比（绝对防欺诈标尺）与全校分数段分布饼图",
                "🖨️ 网页排版导出：生成 HTML 报表，可在浏览器中打开并打印",
                "📊 CSV 导入导出：批量导入学生，或导出当前积分榜供表格软件打开",
                "",
                "【系统调度与容错保护】",
                "🎨 UI 主题：控制中心支持切换蓝白、粉白、白绿三套主题",
                "♻️ 跨维度回收站：任何被删除的班级、专业或学生，均可在此追溯防源并一键满血复活",
                "📁 物理存档管理：支持一键切换、重命名，或将整个海量数据库安全转移至 U盘带走"
        };

        for (String text : helps) {
            if (text.isEmpty()) continue;

            JTextArea l = new JTextArea(text);
            l.setEditable(false);
            l.setOpaque(false);
            l.setLineWrap(true);
            l.setWrapStyleWord(true);
            l.setBorder(null);
            l.setFocusable(false);

            if (text.startsWith("【")) {
                l.setFont(new Font(Font.DIALOG, Font.BOLD, 17));
                l.setForeground(UIManager.getColor("Component.accentColor"));
                gbc.insets = new Insets(gbc.gridy == 0 ? 0 : 15, 0, 8, 0);
            } else {
                l.setFont(new Font(Font.DIALOG, Font.PLAIN, 15));
                l.setForeground(UIManager.getColor("Label.foreground"));
                gbc.insets = new Insets(0, 0, 8, 0);
            }
            textPanel.add(l, gbc);
            gbc.gridy++;
        }

        // 底部透明弹簧
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        JPanel spring = new JPanel();
        spring.setOpaque(false);
        textPanel.add(spring, gbc);

        JScrollPane scrollPane = new JScrollPane(textPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        UITools.enableTouchScrolling(textPanel);

        glassPanel.add(scrollPane, BorderLayout.CENTER);
        add(glassPanel, BorderLayout.CENTER);

        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));

        JButton closeBtn = new JButton("✔️ 我知道了");
        closeBtn.setFont(new Font(Font.DIALOG, Font.BOLD, 16));
        closeBtn.putClientProperty("JButton.buttonType", "default");
        closeBtn.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(closeBtn);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        JPanel btnPanel = new JPanel();
        btnPanel.setOpaque(false);
        btnPanel.add(closeBtn);
        add(btnPanel, BorderLayout.SOUTH);
    }
}
