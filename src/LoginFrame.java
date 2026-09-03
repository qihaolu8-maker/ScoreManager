import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class LoginFrame extends JFrame {
    private JTextField userField;
    private JPasswordField passField;
    private JButton loginBtn;
    private Runnable onSuccess;
    private DataManager data; // ✨ 新增：持有数据库引擎

    // ✨ 核心修改：构造函数接收 DataManager
    public LoginFrame(DataManager data, Runnable onSuccess) {
        this.data = data;
        this.onSuccess = onSuccess;

        setTitle("ScoreManager Pro - 身份认证");
        setSize(420, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        getContentPane().setBackground(new Color(228, 241, 254));
        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(25, 30, 25, 30));

        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);

        JLabel iconLabel = new JLabel("☁️");
        iconLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 40));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel("系统登录");
        titleLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 22));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(iconLabel);
        headerPanel.add(Box.createVerticalStrut(10));
        headerPanel.add(titleLabel);
        add(headerPanel, BorderLayout.NORTH);

        JPanel formPanel = UITools.createGlassPanel();
        formPanel.setLayout(new GridLayout(2, 1, 0, 15));
        formPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel userRow = new JPanel(new BorderLayout(10, 0));
        userRow.setOpaque(false);
        JLabel userLabel = new JLabel("👤 账号:");
        userLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 15));
        userField = new JTextField("");
        userField.putClientProperty("JTextField.placeholderText", "请输入管理员账号");
        userField.setPreferredSize(new Dimension(0, 40));
        userRow.add(userLabel, BorderLayout.WEST);
        userRow.add(userField, BorderLayout.CENTER);

        JPanel passRow = new JPanel(new BorderLayout(10, 0));
        passRow.setOpaque(false);
        JLabel passLabel = new JLabel("🔑 密码:");
        passLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 15));
        passField = new JPasswordField();
        passField.putClientProperty("JTextField.placeholderText", "请输入密码");
        passField.setPreferredSize(new Dimension(0, 40));
        passRow.add(passLabel, BorderLayout.WEST);
        passRow.add(passField, BorderLayout.CENTER);

        formPanel.add(userRow);
        formPanel.add(passRow);
        add(formPanel, BorderLayout.CENTER);

        loginBtn = new JButton("🚀 登 录 (Enter)");
        loginBtn.setFont(new Font(Font.DIALOG, Font.BOLD, 16));
        loginBtn.setBackground(new Color(135, 206, 250));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setPreferredSize(new Dimension(0, 45));
        loginBtn.setFocusPainted(false);
        UITools.addHoverEffectOnly(loginBtn);
        add(loginBtn, BorderLayout.SOUTH);

        loginBtn.addActionListener(e -> handleLogin());

        KeyAdapter enterListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) handleLogin();
            }
        };
        userField.addKeyListener(enterListener);
        passField.addKeyListener(enterListener);
    }

    private void handleLogin() {
        String username = userField.getText().trim();
        String password = new String(passField.getPassword());

        if (username.isEmpty() || password.isEmpty()) return;

        // 🚀 ✨ 核心起飞：去底层 SQLite 数据库里进行真实匹配！
        if (data.verifyLogin(username, password)) {
            this.dispose();
            if (onSuccess != null) {
                onSuccess.run();
            }
        } else {
            Toolkit.getDefaultToolkit().beep();
            JOptionPane.showMessageDialog(this, "ERROR:账号或密码错误或不存在！", "认证失败", JOptionPane.ERROR_MESSAGE);
            passField.setText("");
            passField.requestFocus();
        }
    }
}