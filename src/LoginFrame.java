import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

public class LoginFrame extends JFrame {
    private final JTextField userField = new JTextField();
    private final JPasswordField passField = new JPasswordField();
    private final JPasswordField confirmField = new JPasswordField();
    private final JButton loginBtn;
    private final Runnable onSuccess;
    private final DataManager data;
    private final boolean firstRun;
    private boolean busy;

    public LoginFrame(DataManager data, Runnable onSuccess) {
        this.data = data;
        this.onSuccess = onSuccess;
        this.firstRun = !data.hasUsers();

        setTitle("ScoreManager - " + (firstRun ? "创建管理员" : "身份认证"));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        getContentPane().setBackground(new Color(228, 241, 254));
        setLayout(new BorderLayout(10, 18));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(25, 30, 25, 30));

        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);
        JLabel titleLabel = new JLabel(firstRun ? "首次使用：创建管理员" : "系统登录");
        titleLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 22));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(12));
        JLabel hint = new JLabel(firstRun
                ? "<html>账号只保存在当前本地数据库中。<br>请妥善保管密码，新密码至少 8 个字符。</html>"
                : "<html>请使用当前存档的账号登录。<br>升级用户继续使用原来的账号和密码。</html>");
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(hint);
        add(headerPanel, BorderLayout.NORTH);

        JPanel formPanel = UITools.createGlassPanel();
        formPanel.setLayout(new GridLayout(firstRun ? 3 : 2, 1, 0, 15));
        formPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        userField.putClientProperty("JTextField.placeholderText", firstRun ? "设置管理员账号" : "请输入账号");
        passField.putClientProperty("JTextField.placeholderText", firstRun ? "至少 8 个字符" : "请输入密码");
        formPanel.add(formRow("账号：", userField));
        formPanel.add(formRow("密码：", passField));
        if (firstRun) formPanel.add(formRow("确认：", confirmField));
        add(formPanel, BorderLayout.CENTER);

        loginBtn = new JButton(firstRun ? "创建管理员并进入" : "登录");
        loginBtn.setFont(new Font(Font.DIALOG, Font.BOLD, 16));
        loginBtn.setPreferredSize(new Dimension(0, 45));
        loginBtn.addActionListener(e -> handleLogin());
        add(loginBtn, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(loginBtn);
        setSize(470, firstRun ? 440 : 385);
        setLocationRelativeTo(null);
    }

    private JPanel formRow(String label, JTextField field) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.add(new JLabel(label), BorderLayout.WEST);
        field.setPreferredSize(new Dimension(250, 38));
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    private void handleLogin() {
        if (busy) return;
        String username = userField.getText().trim();
        char[] password = passField.getPassword();
        char[] confirmation = confirmField.getPassword();
        if (username.isEmpty() || password.length == 0) {
            Arrays.fill(password, '\0');
            Arrays.fill(confirmation, '\0');
            JOptionPane.showMessageDialog(this, "请输入账号和密码。");
            return;
        }
        if (firstRun && !Arrays.equals(password, confirmation)) {
            Arrays.fill(password, '\0');
            Arrays.fill(confirmation, '\0');
            JOptionPane.showMessageDialog(this, "两次输入的密码不一致。");
            return;
        }
        Arrays.fill(confirmation, '\0');
        busy = true;
        loginBtn.setEnabled(false);
        userField.setEnabled(false);
        passField.setEnabled(false);
        confirmField.setEnabled(false);
        loginBtn.setText("正在验证...");
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() {
                try {
                    String supplied = new String(password);
                    return firstRun ? data.createFirstAdmin(username, supplied) : data.verifyLogin(username, supplied);
                } finally { Arrays.fill(password, '\0'); }
            }
            @Override protected void done() {
                busy = false;
                loginBtn.setEnabled(true);
                userField.setEnabled(true);
                passField.setEnabled(true);
                confirmField.setEnabled(true);
                loginBtn.setText(firstRun ? "创建管理员并进入" : "登录");
                passField.setText("");
                confirmField.setText("");
                try {
                    if (get()) {
                        dispose();
                        if (onSuccess != null) onSuccess.run();
                    } else if (firstRun && data.hasUsers()) {
                        JOptionPane.showMessageDialog(LoginFrame.this, "当前数据库已创建账号，请使用已有账号登录。");
                        dispose();
                        new LoginFrame(data, onSuccess).setVisible(true);
                    } else {
                        JOptionPane.showMessageDialog(LoginFrame.this, "账号或密码错误。", "认证失败", JOptionPane.ERROR_MESSAGE);
                        passField.requestFocusInWindow();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException | RuntimeException e) {
                    Throwable cause = e instanceof ExecutionException ? e.getCause() : e;
                    JOptionPane.showMessageDialog(LoginFrame.this, cause.getMessage(), "登录失败", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}
