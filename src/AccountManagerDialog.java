import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class AccountManagerDialog extends JDialog {
    private DataManager data;
    private JTable table;
    private DefaultTableModel tableModel;

    public AccountManagerDialog(JFrame parent, DataManager data) {
        super(parent, "⚙️ 系统账号与权限管理 (超管专区)", true);
        this.data = data;
        setSize(600, 500); // 稍微加宽一点，用来显示明文密码
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(15, 15));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(20, 25, 20, 25));
        getContentPane().setBackground(parent.getContentPane().getBackground());

        JLabel titleLabel = new JLabel("🛡️ 教师账号调度中心");
        titleLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 20));
        add(titleLabel, BorderLayout.NORTH);

        JPanel glassPanel = UITools.createGlassPanel();
        glassPanel.setLayout(new BorderLayout());
        glassPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // ✨ 升级：表格增加一列“登录密码”
        tableModel = new DefaultTableModel(new String[]{"登录账号", "登录密码 (明文)", "系统权限身份"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        UITools.setupTableStyle(table, 36);
        refreshTable();

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        glassPanel.add(scrollPane, BorderLayout.CENTER);
        add(glassPanel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        JButton addBtn = new JButton("➕ 新增账号");
        JButton editBtn = new JButton("✏️ 编辑选中账号"); // ✨ 新功能按钮
        JButton delBtn = new JButton("🗑️ 删除");
        delBtn.setForeground(new Color(220, 50, 50));

        addBtn.addActionListener(e -> handleAddUser());
        editBtn.addActionListener(e -> handleEditUser());
        delBtn.addActionListener(e -> handleDeleteUser());

        btnPanel.add(UITools.wrap(addBtn));
        btnPanel.add(UITools.wrap(editBtn));
        btnPanel.add(UITools.wrap(delBtn));
        add(btnPanel, BorderLayout.SOUTH);
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        List<String[]> users = data.getAllUsers();
        for (String[] u : users) {
            String roleStr = u[2].equals("admin") ? "🔑 超级管理员" : "👤 普通教师";
            // 填入：账号、明文密码、权限
            tableModel.addRow(new Object[]{u[0], u[1], roleStr});
        }
    }

    private void handleAddUser() {
        JTextField userField = new JTextField();
        JTextField passField = new JTextField(); // 添加时密码也可视
        JComboBox<String> roleBox = new JComboBox<>(new String[]{"👤 普通教师 (user)", "🔑 超级管理员 (admin)"});
        Object[] message = {"新账号名称:", userField, "初始明文密码:", passField, "分配权限:", roleBox};

        int option = JOptionPane.showConfirmDialog(this, message, "分配新账号", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String u = userField.getText().trim();
            String p = passField.getText().trim();
            String r = roleBox.getSelectedIndex() == 0 ? "user" : "admin";

            if (u.isEmpty() || p.isEmpty()) {
                JOptionPane.showMessageDialog(this, "账号或密码不能为空！");
                return;
            }
            if (data.addUser(u, p, r)) {
                UITools.Toast.showSuccess((JFrame) getParent(), "✅ 账号添加成功！");
                refreshTable();
            } else {
                JOptionPane.showMessageDialog(this, "添加失败！该账号可能已存在。", "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ✨ 新增：极其强大的综合编辑功能
    private void handleEditUser() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "请先在表格中选中一个要编辑的账号！");
            return;
        }

        String oldUser = table.getValueAt(row, 0).toString();
        String oldPwd = table.getValueAt(row, 1).toString();
        String roleStr = table.getValueAt(row, 2).toString();
        int roleIndex = roleStr.contains("超级") ? 1 : 0;

        JTextField userField = new JTextField(oldUser);
        JTextField passField = new JTextField(oldPwd); // 密码明文回显
        JComboBox<String> roleBox = new JComboBox<>(new String[]{"👤 普通教师 (user)", "🔑 超级管理员 (admin)"});
        roleBox.setSelectedIndex(roleIndex);

        Object[] message = {"修改账号名称:", userField, "修改明文密码:", passField, "调整系统权限:", roleBox};

        int option = JOptionPane.showConfirmDialog(this, message, "✏️ 账号深度编辑", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String u = userField.getText().trim();
            String p = passField.getText().trim();
            String r = roleBox.getSelectedIndex() == 0 ? "user" : "admin";

            if (u.isEmpty() || p.isEmpty()) {
                JOptionPane.showMessageDialog(this, "账号或密码不能为空！");
                return;
            }

            if (data.updateUser(oldUser, u, p, r)) {
                UITools.Toast.showSuccess((JFrame) getParent(), "✏️ 账号信息已完美更新！");
                refreshTable();
            } else {
                JOptionPane.showMessageDialog(this, "修改失败！新账号名可能与其他现有账号发生冲突。", "更新受阻", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleDeleteUser() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "请先选中要删除的账号！");
            return;
        }
        String targetUser = table.getValueAt(row, 0).toString();

        if (JOptionPane.showConfirmDialog(this, "🚨 警告：确定要永久删除账号 [" + targetUser + "] 吗？", "删除确认", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            if (data.deleteUser(targetUser)) {
                UITools.Toast.showSuccess((JFrame) getParent(), "🗑️ 账号已抹除！");
                refreshTable();
            } else {
                JOptionPane.showMessageDialog(this, "❌ 删除失败！\n原因：禁止删除最高权限创始人或当前正在登录的账号！", "权限拒绝", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}