import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

public class AccountManagerDialog extends JDialog {
    private final DataManager data;
    private final JTable table;
    private final DefaultTableModel tableModel;
    private final JButton addBtn = new JButton("新增账号");
    private final JButton editBtn = new JButton("编辑选中账号");
    private final JButton delBtn = new JButton("删除");

    public AccountManagerDialog(JFrame parent, DataManager data) {
        super(parent, "系统账号与权限管理", true);
        this.data = data;
        if (!data.isAdmin()) throw new SecurityException("需要管理员权限");
        setSize(600, 500);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(15, 15));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(20, 25, 20, 25));
        getContentPane().setBackground(parent.getContentPane().getBackground());

        JLabel titleLabel = new JLabel("账号管理（密码不显示，可重新设置）");
        titleLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 18));
        add(titleLabel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new String[]{"登录账号", "系统权限身份"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        UITools.setupTableStyle(table, 36);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        refreshTable();
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        delBtn.setForeground(new Color(220, 50, 50));
        addBtn.addActionListener(e -> handleUserForm(false));
        editBtn.addActionListener(e -> handleUserForm(true));
        delBtn.addActionListener(e -> handleDeleteUser());
        buttons.add(addBtn);
        buttons.add(editBtn);
        buttons.add(delBtn);
        add(buttons, BorderLayout.SOUTH);
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (String[] user : data.getAllUsers()) {
            tableModel.addRow(new Object[]{user[0], "admin".equals(user[1]) ? "管理员" : "普通教师"});
        }
    }

    private void handleUserForm(boolean edit) {
        int row = table.getSelectedRow();
        if (edit && row < 0) {
            JOptionPane.showMessageDialog(this, "请先选中要编辑的账号。");
            return;
        }
        String oldUsername = edit ? table.getValueAt(row, 0).toString() : "";
        JTextField userField = new JTextField(oldUsername);
        JPasswordField passField = new JPasswordField();
        JPasswordField confirmation = new JPasswordField();
        JComboBox<String> roleBox = new JComboBox<>(new String[]{"普通教师", "管理员"});
        if (edit) roleBox.setSelectedIndex("管理员".equals(table.getValueAt(row, 1)) ? 1 : 0);
        if (edit && oldUsername.equals(data.getCurrentUsername())) roleBox.setEnabled(false);
        Object[] message = {"账号名称：", userField,
                edit ? "新密码（留空保留原密码；新密码至少 8 个字符）：" : "初始密码（至少 8 个字符）：", passField,
                "确认新密码：", confirmation, "权限：", roleBox};
        if (JOptionPane.showConfirmDialog(this, message, edit ? "编辑账号" : "新增账号",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        char[] password = passField.getPassword();
        char[] confirm = confirmation.getPassword();
        if (!Arrays.equals(password, confirm)) {
            Arrays.fill(password, '\0');
            Arrays.fill(confirm, '\0');
            JOptionPane.showMessageDialog(this, "两次输入的密码不一致。");
            return;
        }
        Arrays.fill(confirm, '\0');
        String username = userField.getText().trim();
        String role = roleBox.getSelectedIndex() == 1 ? "admin" : "user";
        runOperation(() -> {
            try {
                String supplied = new String(password);
                return edit ? data.updateUser(oldUsername, username, supplied, role)
                        : data.addUser(username, supplied, role);
            } finally { Arrays.fill(password, '\0'); }
        }, edit ? "账号已更新。" : "账号已创建。", "操作失败：账号名可能重复，账号已不存在，或数据库无法写入。");
    }

    private void handleDeleteUser() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请先选中要删除的账号。");
            return;
        }
        String username = table.getValueAt(row, 0).toString();
        if (JOptionPane.showConfirmDialog(this, "确定删除账号 [" + username + "]？", "删除账号",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            runOperation(() -> data.deleteUser(username), "账号已删除。", "无法删除当前登录账号，或账号已不存在/数据库无法写入。");
        }
    }

    private void runOperation(Supplier<Boolean> operation, String success, String failure) {
        setBusy(true);
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() { return operation.get(); }
            @Override protected void done() {
                setBusy(false);
                try {
                    if (get()) {
                        refreshTable();
                        UITools.Toast.showSuccess((JFrame) getParent(), success);
                    } else JOptionPane.showMessageDialog(AccountManagerDialog.this, failure, "操作失败", JOptionPane.ERROR_MESSAGE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException | RuntimeException e) {
                    Throwable cause = e instanceof ExecutionException ? e.getCause() : e;
                    JOptionPane.showMessageDialog(AccountManagerDialog.this, cause.getMessage(), "操作失败", JOptionPane.ERROR_MESSAGE);
                    if (cause instanceof SecurityException) dispose();
                }
            }
        }.execute();
    }

    private void setBusy(boolean busy) {
        addBtn.setEnabled(!busy);
        editBtn.setEnabled(!busy);
        delBtn.setEnabled(!busy);
        table.setEnabled(!busy);
    }
}
