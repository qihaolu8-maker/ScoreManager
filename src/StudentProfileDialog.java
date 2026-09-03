import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class StudentProfileDialog extends JDialog {
    public StudentProfileDialog(JFrame parentFrame, DataManager data, String major, String className, String id, String name, double score) {
        super(parentFrame, "🎓 学生个人数字档案", true);
        setSize(580, 650);
        setLocationRelativeTo(parentFrame);
        setLayout(new BorderLayout(15, 15));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(25, 30, 25, 30));
        getContentPane().setBackground(parentFrame.getContentPane().getBackground());

        // 计算班级排名和人数
        int classRank = 1;
        List<Student> cStus = new ArrayList<>(data.getClassesData().get(className).values());
        cStus.sort((s1, s2) -> Double.compare(s2.score, s1.score));
        for (int i = 0; i < cStus.size(); i++) {
            if (i > 0 && cStus.get(i).score < cStus.get(i - 1).score) classRank = i + 1;
            if (cStus.get(i).id.equals(id)) break;
        }
        int totalClassCount = cStus.size();

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        JLabel nl = new JLabel("🎓 " + name + " (" + id + ")");
        nl.setFont(new Font(Font.DIALOG, Font.BOLD, 26));
        nl.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel il = new JLabel(major + " | " + className);
        il.setFont(new Font(Font.DIALOG, Font.PLAIN, 16));
        il.setForeground(new Color(120, 130, 140));
        il.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel stats = new JPanel(new GridLayout(1, 3, 15, 0));
        stats.setOpaque(false);
        stats.setBorder(new EmptyBorder(25, 0, 20, 0));

        stats.add(createStatCard("当前总积分", String.valueOf(score), new Color(135, 206, 250)));
        stats.add(createStatCard("班级排名", classRank + " / " + totalClassCount, new Color(255, 166, 193)));
        stats.add(createStatCard("身份状态", "在校", new Color(180, 160, 255)));

        header.add(nl);
        header.add(Box.createVerticalStrut(10));
        header.add(il);
        header.add(stats);

        JPanel timeline = new JPanel(new BorderLayout(0, 10));
        timeline.setOpaque(false);
        DefaultTableModel logM = new DefaultTableModel(new String[]{"时间", "变动", "事由"}, 0);
        for (LogEntry le : data.getHistoryLogs()) {
            if (le.id.equals(id)) logM.addRow(new Object[]{le.time, le.change, le.remark});
        }
        JTable lt = new JTable(logM);
        UITools.setupTableStyle(lt, 16);
        UITools.enableTouchScrolling(lt);
        timeline.add(new JScrollPane(lt), BorderLayout.CENTER);

        add(header, BorderLayout.NORTH);
        add(timeline, BorderLayout.CENTER);
    }

    private JPanel createStatCard(String title, String val, Color c) {
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 35));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(18, 10, 18, 10));
        JLabel tL = new JLabel(title);
        tL.setFont(new Font(Font.DIALOG, Font.PLAIN, 14));
        tL.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel vL = new JLabel(val);
        vL.setFont(new Font(Font.DIALOG, Font.BOLD, 20));
        vL.setForeground(c.darker());
        vL.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(tL);
        p.add(Box.createVerticalStrut(10));
        p.add(vL);
        return p;
    }
}