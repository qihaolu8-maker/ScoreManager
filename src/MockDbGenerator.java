import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class MockDbGenerator extends JFrame {

    private JComboBox<String> studentCountBox;
    private JComboBox<String> logCountBox;
    private JButton generateBtn;
    private JProgressBar progressBar;
    private JLabel statusLabel;

    public MockDbGenerator() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        setTitle("📦 ScoreManager 高级测试数据生成引擎");
        setSize(520, 360);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(15, 15));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(25, 25, 25, 25));
        getContentPane().setBackground(new Color(245, 247, 250));

        JLabel titleLabel = new JLabel("⚡ 自定义原生 .db 测试存档生成", SwingConstants.CENTER);
        titleLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 22));
        titleLabel.setForeground(new Color(44, 62, 80));
        add(titleLabel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);

        JPanel formPanel = new JPanel(new GridLayout(2, 2, 15, 20));
        formPanel.setOpaque(false);
        formPanel.setBorder(new EmptyBorder(10, 20, 20, 20));

        JLabel stuLabel = new JLabel("🎓 生成学生总数:");
        stuLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 15));
        studentCountBox = new JComboBox<>(new String[]{"1000", "5000", "10000", "50000", "100000"});
        studentCountBox.setEditable(true);
        studentCountBox.setFont(new Font(Font.DIALOG, Font.PLAIN, 15));

        JLabel logLabel = new JLabel("📜 生成历史日志数:");
        logLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 15));
        logCountBox = new JComboBox<>(new String[]{"500", "2000", "10000", "50000", "200000"});
        logCountBox.setEditable(true);
        logCountBox.setFont(new Font(Font.DIALOG, Font.PLAIN, 15));

        formPanel.add(stuLabel);
        formPanel.add(studentCountBox);
        formPanel.add(logLabel);
        formPanel.add(logCountBox);

        JPanel progressPanel = new JPanel(new BorderLayout(0, 10));
        progressPanel.setOpaque(false);
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setFont(new Font(Font.DIALOG, Font.BOLD, 14));
        progressBar.setPreferredSize(new Dimension(0, 25));

        statusLabel = new JLabel("等待开始...", SwingConstants.CENTER);
        statusLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 13));
        statusLabel.setForeground(Color.GRAY);

        progressPanel.add(progressBar, BorderLayout.CENTER);
        progressPanel.add(statusLabel, BorderLayout.SOUTH);

        centerPanel.add(formPanel);
        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(progressPanel);

        add(centerPanel, BorderLayout.CENTER);

        generateBtn = new JButton("🚀 立即生成数据库文件");
        generateBtn.setFont(new Font(Font.DIALOG, Font.BOLD, 16));
        generateBtn.setPreferredSize(new Dimension(0, 45));
        generateBtn.setBackground(new Color(52, 152, 219));
        generateBtn.setForeground(Color.WHITE);
        generateBtn.setFocusPainted(false);
        generateBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        generateBtn.addActionListener(e -> startGeneration());
        add(generateBtn, BorderLayout.SOUTH);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MockDbGenerator().setVisible(true);
        });
    }

    private void startGeneration() {
        int targetStudentCount;
        int targetLogCount;

        try {
            targetStudentCount = Integer.parseInt(studentCountBox.getSelectedItem().toString().trim());
            targetLogCount = Integer.parseInt(logCountBox.getSelectedItem().toString().trim());

            if (targetStudentCount <= 0 || targetLogCount < 0) {
                throw new NumberFormatException();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "数量必须是有效的正整数！", "格式错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("选择生成的 .db 文件保存位置");
        fc.setSelectedFile(new File("MockScoreData_" + targetStudentCount + "人.db"));
        fc.setFileFilter(new FileNameExtensionFilter("SQLite 数据库文件 (*.db)", "db"));

        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File targetFile = fc.getSelectedFile();
            if (!targetFile.getName().toLowerCase().endsWith(".db")) {
                targetFile = new File(targetFile.getParentFile(), targetFile.getName() + ".db");
            }

            final File finalFile = targetFile;
            generateBtn.setEnabled(false);
            studentCountBox.setEnabled(false);
            logCountBox.setEnabled(false);
            generateBtn.setText("⏳ 极速生成中，请稍候...");

            new SwingWorker<Void, Integer>() {
                @Override
                protected Void doInBackground() throws Exception {
                    if (finalFile.exists()) finalFile.delete();

                    String url = "jdbc:sqlite:" + finalFile.getAbsolutePath();
                    try (Connection conn = DriverManager.getConnection(url);
                         Statement stmt = conn.createStatement()) {

                        publish(0);
                        statusLabel.setText("正在建立数据库结构...");

                        stmt.execute("PRAGMA synchronous = OFF;");
                        stmt.execute("PRAGMA journal_mode = MEMORY;");

                        stmt.execute("CREATE TABLE major_classes (major_name TEXT, class_name TEXT);");
                        stmt.execute("CREATE TABLE students (id TEXT PRIMARY KEY, name TEXT, score REAL, class_name TEXT);");
                        stmt.execute("CREATE TABLE logs (time TEXT, class_name TEXT, student_id TEXT, student_name TEXT, change_val TEXT, remark TEXT);");
                        stmt.execute("CREATE TABLE trash (type TEXT, name TEXT, parent_info TEXT, data_json TEXT, delete_time TEXT);");

                        conn.setAutoCommit(false);

                        String[] majors = {"软件工程", "人工智能", "数字媒体", "网络安全", "计算机科学"};
                        Random random = new Random();
                        List<String> allClasses = new ArrayList<>(); // 建立一个总班级池

                        statusLabel.setText("正在编织专业与班级网络...");
                        try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO major_classes VALUES (?, ?)")) {
                            for (String major : majors) {
                                for (int c = 1; c <= 5; c++) {
                                    String className = major + "26级" + c + "班";
                                    allClasses.add(className); // 把所有25个班级收录进池子
                                    pstmt.setString(1, major);
                                    pstmt.setString(2, className);
                                    pstmt.addBatch();
                                }
                            }
                            pstmt.executeBatch();
                        }

                        statusLabel.setText("正在批量写入 " + targetStudentCount + " 条学生档案...");
                        String insertStuSql = "INSERT INTO students (id, name, score, class_name) VALUES (?, ?, ?, ?)";
                        try (PreparedStatement pstmt = conn.prepareStatement(insertStuSql)) {
                            for (int i = 1; i <= targetStudentCount; i++) {
                                String stuId = "2026" + String.format("%06d", i);
                                String stuName = "测试生" + i;
                                // ✨ 核心修复：通过求模运算，让学生绝对均匀地分布在所有25个班级里！
                                String className = allClasses.get(i % allClasses.size());
                                double score = 60.0 + random.nextInt(40);

                                pstmt.setString(1, stuId);
                                pstmt.setString(2, stuName);
                                pstmt.setDouble(3, score);
                                pstmt.setString(4, className);
                                pstmt.addBatch();

                                if (i % 10000 == 0) {
                                    pstmt.executeBatch();
                                    publish((int) ((i / (double) targetStudentCount) * 50));
                                }
                            }
                            pstmt.executeBatch();
                        }

                        statusLabel.setText("正在疯狂写入 " + targetLogCount + " 条加分日志...");
                        String insertLogSql = "INSERT INTO logs (time, class_name, student_id, student_name, change_val, remark) VALUES (?, ?, ?, ?, ?, ?)";
                        try (PreparedStatement pstmt = conn.prepareStatement(insertLogSql)) {
                            long currentTime = System.currentTimeMillis();
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                            for (int i = 1; i <= targetLogCount; i++) {
                                int randomIdx = random.nextInt(targetStudentCount) + 1;
                                String stuId = "2026" + String.format("%06d", randomIdx);
                                String stuName = "测试生" + randomIdx;
                                // ✨ 核心修复：日志里的班级也要和刚才推导出来的班级绝对一致！
                                String className = allClasses.get(randomIdx % allClasses.size());

                                double change = (random.nextDouble() > 0.3 ? 1.0 : -1.0) * (random.nextInt(5) + 1);
                                long randomPastTime = currentTime - (long) (random.nextDouble() * 30L * 86400000L);
                                String time = sdf.format(new Date(randomPastTime));
                                String remark = "系统自动生成的性能测试日志";

                                pstmt.setString(1, time);
                                pstmt.setString(2, className);
                                pstmt.setString(3, stuId);
                                pstmt.setString(4, stuName);
                                pstmt.setString(5, (change > 0 ? "+" : "") + change);
                                pstmt.setString(6, remark);
                                pstmt.addBatch();

                                if (i % 10000 == 0) {
                                    pstmt.executeBatch();
                                    publish(50 + (int) ((i / (double) targetLogCount) * 50));
                                }
                            }
                            pstmt.executeBatch();
                        }

                        statusLabel.setText("正在提交硬盘事务，请勿关闭程序...");
                        conn.commit();
                    }
                    return null;
                }

                @Override
                protected void process(List<Integer> chunks) {
                    int progress = chunks.get(chunks.size() - 1);
                    progressBar.setValue(progress);
                }

                @Override
                protected void done() {
                    try {
                        get();
                        progressBar.setValue(100);
                        statusLabel.setText("✅ 数据库生成大功告成！");
                        JOptionPane.showMessageDialog(MockDbGenerator.this,
                                "🎉 成功生成原生数据库：\n" + finalFile.getAbsolutePath() + "\n\n💡 现在您可以去主程序里，点击控制中心的【切换】按钮打开它进行极限测试了！",
                                "生成完毕", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        progressBar.setString("❌ 生成失败");
                        statusLabel.setText("出现严重错误");
                        JOptionPane.showMessageDialog(MockDbGenerator.this, "生成失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                        ex.printStackTrace();
                    } finally {
                        generateBtn.setEnabled(true);
                        studentCountBox.setEnabled(true);
                        logCountBox.setEnabled(true);
                        generateBtn.setText("🚀 再次生成");
                    }
                }
            }.execute();
        }
    }
}