import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class HelpGuideDialog extends JDialog {
    public HelpGuideDialog(JFrame parentFrame) {
        super(parentFrame, "ℹ️ 快捷键与系统操作指南", true);

        setSize(520, 600);

        setMinimumSize(new Dimension(600, 720));

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

        String[] helps = {
                "【键盘与基础快捷键】",
                "⌨️ Ctrl + S：强制将当前所有数据安全写入本地 SQLite 数据库",
                "⌨️ Ctrl + Z：智能撤销上一步操作 (大数据量下自动开启内存保护机制)",
                "⌨️ Ctrl + F / 🔍搜索：搭载 300ms 防抖引擎，海量数据下打字亦丝滑流畅",
                "🗑️ Delete 键：在主表格中选中学生后，按此键直接将其移至数据回收站",
                "",
                "【鼠标与高级交互体验】",
                "🖱️ 双击表格整行：瞬间弹出该学生的「专属数字档案」与「加分时间轴」",
                "🖱️ 双击单元格：支持直接在表格内就地修改学号、姓名或总分（全自动记入日志）",
                "🖱️ 左侧树状拖拽：鼠标长按班级节点，可跨专业随意拖拽，实现组织架构重组",
                "🖱️ 物理阻尼滑动：所有数据表格与可视化大屏均支持触控板级别的平滑惯性拖拽",
                "",
                "【课堂积分与日志追踪】",
                "🎀 极速快捷加分：底部面板选定学生后，通过预设分值下拉框一键完成高频加扣分",
                "✍️ 完整事由录入：支持手动输入精确分值与详细备注，敲击 Enter 极速提交",
                "📜 历史变动审计：全自动记录全校每一笔分数的增减时间、操作人及具体变更轨迹",
                "",
                "【数据可视化与报表分发】",
                "📈 实时数据大屏：自动渲染各班平均分对比（绝对防欺诈标尺）与全校分数段分布饼图",
                "🖨️ 网页排版导出：生成自带高亮奖牌的精美 HTML 报表，全平台无乱码完美打印",
                "📊 工业级导入导出：支持自带高级正则过滤的 CSV 新生批量导入，以及纯数据导出",
                "",
                "【系统调度与容错保护】",
                "🎨 沉浸式 UI 主题：控制中心支持一键无缝切换 蓝白/粉白/白绿/夜间 等多套视觉风格",
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
        JPanel btnPanel = new JPanel();
        btnPanel.setOpaque(false);
        btnPanel.add(closeBtn);
        add(btnPanel, BorderLayout.SOUTH);
    }
}