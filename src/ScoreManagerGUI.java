import javax.swing.*;
import javax.swing.Timer; // ✨ 核心修复：显式导入，解决与 java.util.Timer 的冲突
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.Collator;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class ScoreManagerGUI {
    private DataManager data;

    // 状态控制变量
    private SortMode currentSortMode = SortMode.SCORE;
    private boolean isAscending = false;
    private String currentSearchText = "";
    private LogSortMode currentLogSortMode = LogSortMode.TIME;
    private boolean isLogAscending = false;
    private String currentLogSearchText = "";

    // ✨ 搜索防抖计时器
    private Timer searchDebounceTimer;
    private Timer logSearchDebounceTimer;

    // UI 组件声明
    private JFrame frame;
    private JTabbedPane tabbedPane;
    private DefaultTableModel tableModel, logTableModel, trashTableModel;
    private JTable table, logTable, trashTable;
    private JTree classTree;
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode rootNode;
    private JComboBox<String> classComboBox;
    private JTextField idField, nameField, scoreField, searchField, remarkField, logSearchField;
    private JComboBox<Double> quickScoreBox;

    // 按钮声明
    private JButton submitBtn, quickBtn, manualSaveBtn, undoBtn, idSortBtn, nameSortBtn, scoreSortBtn;
    private JButton addClassBtn, renameClassBtn, deleteClassBtn, deleteStudentBtn;
    private JButton logTimeSortBtn, logClassSortBtn, logNameSortBtn;
    private JButton changeFileBtn, renameFileBtn, moveFileBtn;
    private JButton exportCsvBtn, importCsvBtn, printPdfBtn, exportHtmlBtn;
    private JButton restoreBtn, cleanTrashBtn, helpBtn;

    private JLabel currentSortLabel, fileDisplayLabel, currentLogSortLabel;
    private DashboardPanel dashboardPanel;
    private List<LogEntry> visibleLogs = new ArrayList<>();
    private boolean closing;

    public ScoreManagerGUI(DataManager dataManager) {
        this.data = dataManager;
        initializeUI();
        fullRefresh();
    }

    private void initializeUI() {
        // 🚀 初始化防抖引擎：300ms 延迟，防止输入时界面抖动卡顿
        searchDebounceTimer = new Timer(300, e -> refreshCurrentView());
        searchDebounceTimer.setRepeats(false);

        logSearchDebounceTimer = new Timer(300, e -> updateLogTable());
        logSearchDebounceTimer.setRepeats(false);

        Font titleFont = new Font(Font.DIALOG, Font.BOLD, 18);

        frame = new JFrame("ScoreManager Pro（V4.5bylqh）");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { handleCloseRequested(); }
        });
        data.setSaveErrorHandler(error -> SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(frame,
                        "保存失败，当前修改仍保留在内存，请重试保存。\n" + errorMessage(error),
                        "存档失败", JOptionPane.ERROR_MESSAGE)));
        frame.setSize(1280, 850);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setLayout(new BorderLayout(15, 15));
        frame.setMinimumSize(new Dimension(1350, 750));
        ((JPanel) frame.getContentPane()).setBorder(new EmptyBorder(15, 20, 15, 20));

        setupGlobalShortcuts();

        JPanel leftPanel = initLeftPanel(titleFont);

        JPanel rightPanel = new JPanel(new BorderLayout(0, 20));
        rightPanel.setMinimumSize(new Dimension(800, 0));
        rightPanel.setOpaque(false);

        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("🏆 实时积分榜", initScoreTab());

        dashboardPanel = new DashboardPanel(data);
        tabbedPane.addTab("📈 数据大屏", dashboardPanel);

        tabbedPane.addTab("📜 历史加分日志", initLogTab());
        tabbedPane.addTab("🗑️ 数据回收站", initTrashTab());

        rightPanel.add(tabbedPane, BorderLayout.CENTER);
        rightPanel.add(initBottomForm(titleFont), BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerSize(10);
        splitPane.setBorder(null);
        splitPane.setOpaque(false);
        splitPane.setResizeWeight(0.0);
        frame.add(splitPane, BorderLayout.CENTER);

        UITools.applyTheme("蓝白", frame);
        paintAccentButtons();

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        SwingUtilities.invokeLater(() -> new HelpGuideDialog(frame).setVisible(true));
    }

    private JPanel initLeftPanel(Font titleFont) {
        JPanel leftPanel = new JPanel(new BorderLayout(0, 15));
        leftPanel.setBorder(new EmptyBorder(0, 0, 0, 5));
        leftPanel.setPreferredSize(new Dimension(420, 0));
        leftPanel.setMinimumSize(new Dimension(390, 0));
        leftPanel.setOpaque(false);

        JPanel leftTopPanel = new JPanel();
        leftTopPanel.setOpaque(false);
        leftTopPanel.setLayout(new BoxLayout(leftTopPanel, BoxLayout.Y_AXIS));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.setBorder(new EmptyBorder(0, 5, 15, 5));
        JLabel settingsTitle = new JLabel("📚 控制中心");
        settingsTitle.setFont(titleFont);
        headerRow.add(settingsTitle, BorderLayout.WEST);

        String[] themes = {"蓝白", "粉白", "白绿"};
        JComboBox<String> themeBox = new JComboBox<>(themes);
        themeBox.setPreferredSize(new Dimension(110, 30));
        themeBox.addActionListener(e -> {
            UITools.applyTheme((String) themeBox.getSelectedItem(), frame);
            paintAccentButtons();
            fullRefresh();
        });

        // ... 原来的代码
        JPanel themeContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        themeContainer.setOpaque(false);
        themeContainer.add(new JLabel("🎨 "));
        themeContainer.add(themeBox);

        // 🚀 ✨ 核心挂载点：只有超级管理员 (admin) 才能看到账号管理按钮！
        if ("admin".equals(data.currentUserRole)) {
            JButton accManageBtn = new JButton("⚙️ 账号");
            accManageBtn.setForeground(new Color(100, 100, 100)); // 低调的高级灰
            accManageBtn.addActionListener(e -> new AccountManagerDialog(frame, data).setVisible(true));
            themeContainer.add(Box.createHorizontalStrut(10));
            themeContainer.add(UITools.wrap(accManageBtn));
        }

        headerRow.add(themeContainer, BorderLayout.EAST);
        // ... 原来的代码继续
        JPanel fileCard = UITools.createGlassPanel();
        fileCard.setLayout(new BorderLayout(10, 10));
        JLabel fileIcon = new JLabel("📄 存档:");
        fileIcon.setFont(new Font(Font.DIALOG, Font.BOLD, 16));
        fileDisplayLabel = new JLabel(data.getCurrentDataFile().getName());
        fileDisplayLabel.setToolTipText(data.getCurrentDataFile().getAbsolutePath());
        fileDisplayLabel.setFont(new Font(Font.DIALOG, Font.ITALIC, 16));

        JPanel fileActionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        fileActionsPanel.setOpaque(false);
        renameFileBtn = new JButton("改名");
        changeFileBtn = new JButton("切换");
        moveFileBtn = new JButton("转移");
        renameFileBtn.addActionListener(e -> handleRenameDataFile());
        changeFileBtn.addActionListener(e -> handleSelectDataFile());
        moveFileBtn.addActionListener(e -> handleMoveDataFile());

        fileActionsPanel.add(UITools.wrap(renameFileBtn));
        fileActionsPanel.add(UITools.wrap(changeFileBtn));
        fileActionsPanel.add(UITools.wrap(moveFileBtn));

        JPanel fileTopRow = new JPanel(new BorderLayout());
        fileTopRow.setOpaque(false);
        fileTopRow.add(fileIcon, BorderLayout.WEST);
        fileTopRow.add(fileActionsPanel, BorderLayout.EAST);
        fileCard.add(fileTopRow, BorderLayout.NORTH);
        fileCard.add(fileDisplayLabel, BorderLayout.CENTER);

        leftTopPanel.add(headerRow);
        leftTopPanel.add(fileCard);
        leftPanel.add(leftTopPanel, BorderLayout.NORTH);

        JPanel classTreePanel = new JPanel(new BorderLayout(0, 12));
        classTreePanel.setOpaque(false);
        JLabel treeTitle = new JLabel("🏫 班级与专业视图");
        treeTitle.setFont(titleFont);
        treeTitle.setBorder(new EmptyBorder(0, 5, 0, 5));
        classTreePanel.add(treeTitle, BorderLayout.NORTH);

        rootNode = new DefaultMutableTreeNode("所有专业与班级");
        treeModel = new DefaultTreeModel(rootNode);
        classTree = new JTree(treeModel);
        classTree.setDragEnabled(true);
        classTree.setDropMode(DropMode.ON_OR_INSERT);
        classTree.setTransferHandler(new TreeTransferHandler());
        classTree.getSelectionModel().setSelectionMode(javax.swing.tree.TreeSelectionModel.SINGLE_TREE_SELECTION);
        classTree.setRowHeight(34);
        classTree.setBorder(new EmptyBorder(10, 10, 10, 10));
        classTree.putClientProperty("JTree.lineStyle", "None");
        classTree.setShowsRootHandles(true);
        classTree.setCellRenderer(createTreeRenderer());
        classTree.addTreeSelectionListener(e -> refreshCurrentView());
        UITools.enableTouchScrolling(classTree);

        JScrollPane treeScroll = new JScrollPane(classTree);
        treeScroll.setBorder(BorderFactory.createLineBorder(new Color(150, 150, 150, 50), 1, true));
        treeScroll.setOpaque(false);
        treeScroll.getViewport().setOpaque(false);
        classTreePanel.add(treeScroll, BorderLayout.CENTER);

        JPanel classManageCard = UITools.createGlassPanel();
        classManageCard.setLayout(new BoxLayout(classManageCard, BoxLayout.Y_AXIS));
        JTextField newClassField = new JTextField();
        newClassField.putClientProperty("JTextField.placeholderText", "输入专业/班级...");
        addClassBtn = new JButton("添加");
        addClassBtn.addActionListener(e -> handleAddNode(newClassField.getText().trim(), newClassField));
        JPanel addRow = new JPanel(new BorderLayout(10, 0));
        addRow.setOpaque(false);
        addRow.add(newClassField);
        addRow.add(UITools.wrap(addClassBtn), BorderLayout.EAST);
        JPanel editRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        editRow.setOpaque(false);
        renameClassBtn = new JButton("修改名称");
        deleteClassBtn = new JButton("删除选中");
        renameClassBtn.addActionListener(e -> handleRenameNode());
        deleteClassBtn.addActionListener(e -> handleDeleteNode());
        editRow.add(UITools.wrap(renameClassBtn));
        editRow.add(UITools.wrap(deleteClassBtn));
        classManageCard.add(addRow);
        classManageCard.add(Box.createVerticalStrut(12));
        classManageCard.add(editRow);

        classTreePanel.add(classManageCard, BorderLayout.SOUTH);
        leftPanel.add(classTreePanel, BorderLayout.CENTER);

        JPanel globalActionsCard = UITools.createGlassPanel();
        globalActionsCard.setLayout(new GridLayout(6, 1, 0, 10));

        undoBtn = new JButton("↩️ 撤销上一步 (Ctrl+Z)");
        undoBtn.addActionListener(e -> handleUndo());

        manualSaveBtn = new JButton("💾 强制安全存档 (Ctrl+S)");
        manualSaveBtn.addActionListener(e -> handleManualSave());

        importCsvBtn = new JButton("📥 批量导入新生 (CSV)");
        importCsvBtn.addActionListener(e -> handleImportCsv());

        exportCsvBtn = new JButton("📊 导出纯数据到 Excel");
        exportCsvBtn.addActionListener(e -> handleExportCsv());

        JPanel printRow = new JPanel(new GridLayout(1, 2, 10, 0));
        printRow.setOpaque(false);
        printPdfBtn = new JButton("🖨️ 原生打印");
        printPdfBtn.addActionListener(e -> handlePrintPdf());
        exportHtmlBtn = new JButton("🌐 网页排版(推荐)");
        exportHtmlBtn.addActionListener(e -> handleExportHtml());
        printRow.add(UITools.wrap(printPdfBtn));
        printRow.add(UITools.wrap(exportHtmlBtn));

        helpBtn = new JButton("ℹ️ 快捷键与操作指南");
        helpBtn.addActionListener(e -> new HelpGuideDialog(frame).setVisible(true));

        UITools.addHoverEffectOnly(undoBtn);
        UITools.addHoverEffectOnly(manualSaveBtn);
        UITools.addHoverEffectOnly(importCsvBtn);
        UITools.addHoverEffectOnly(exportCsvBtn);
        UITools.addHoverEffectOnly(printPdfBtn);
        UITools.addHoverEffectOnly(exportHtmlBtn);
        UITools.addHoverEffectOnly(helpBtn);

        globalActionsCard.add(undoBtn);
        globalActionsCard.add(manualSaveBtn);
        globalActionsCard.add(importCsvBtn);
        globalActionsCard.add(exportCsvBtn);
        globalActionsCard.add(printRow);
        globalActionsCard.add(UITools.wrap(helpBtn));

        leftPanel.add(globalActionsCard, BorderLayout.SOUTH);
        return leftPanel;
    }

    private void triggerSearch() {
        currentSearchText = searchField.getText().trim().toLowerCase();
        searchDebounceTimer.restart();
    }

    private void triggerLogSearch() {
        currentLogSearchText = logSearchField.getText().trim().toLowerCase();
        logSearchDebounceTimer.restart();
    }

    private JPanel initScoreTab() {
        JPanel scorePanel = new JPanel(new BorderLayout(0, 15));
        scorePanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        scorePanel.setOpaque(false);

        JPanel toolBar = new JPanel();
        toolBar.setLayout(new BoxLayout(toolBar, BoxLayout.X_AXIS));
        toolBar.setOpaque(false);

        JPanel sortRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        sortRow.setOpaque(false);
        scoreSortBtn = new JButton("分数排序↓");
        idSortBtn = new JButton("学号排序↑");
        nameSortBtn = new JButton("姓名排序↑");

        scoreSortBtn.addActionListener(e -> {
            currentSortMode = SortMode.SCORE;
            isAscending = !isAscending;
            refreshCurrentView();
        });
        idSortBtn.addActionListener(e -> {
            currentSortMode = SortMode.ID;
            isAscending = !isAscending;
            refreshCurrentView();
        });
        nameSortBtn.addActionListener(e -> {
            currentSortMode = SortMode.NAME;
            isAscending = !isAscending;
            refreshCurrentView();
        });

        currentSortLabel = new JLabel();
        currentSortLabel.setFont(new Font(Font.DIALOG, Font.ITALIC, 14));
        sortRow.add(UITools.wrap(scoreSortBtn));
        sortRow.add(UITools.wrap(idSortBtn));
        sortRow.add(UITools.wrap(nameSortBtn));
        sortRow.add(Box.createHorizontalStrut(5));
        sortRow.add(currentSortLabel);

        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        searchRow.setOpaque(false);
        searchField = new JTextField(15);
        searchField.putClientProperty("JTextField.placeholderText", "🔍 全局过滤搜索...");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { triggerSearch(); }
            public void removeUpdate(DocumentEvent e) { triggerSearch(); }
            public void changedUpdate(DocumentEvent e) { triggerSearch(); }
        });
        searchRow.add(searchField);

        toolBar.add(sortRow);
        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(searchRow);

        tableModel = new DefaultTableModel(new String[]{"排名", "专业", "班级", "学号", "姓名", "总分数"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 3 || column == 4 || column == 5;
            }
            @Override
            public void setValueAt(Object aValue, int row, int column) {
                handleScoreTableEdit(aValue, row, column);
            }
        };
        table = new JTable(tableModel);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        UITools.setupTableStyle(table, 36);
        UITools.enableTouchScrolling(table);

        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "DELETE_STUDENT");
        table.getActionMap().put("DELETE_STUDENT", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) { handleDeleteStudent(); }
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                classComboBox.setSelectedItem(table.getValueAt(table.getSelectedRow(), 2).toString());
                idField.setText(table.getValueAt(table.getSelectedRow(), 3).toString());
                nameField.setText(table.getValueAt(table.getSelectedRow(), 4).toString());
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() != -1) {
                    int row = table.getSelectedRow();
                    String major = table.getValueAt(row, 1).toString();
                    String className = table.getValueAt(row, 2).toString();
                    String id = table.getValueAt(row, 3).toString();
                    String name = table.getValueAt(row, 4).toString();
                    double score = Double.parseDouble(table.getValueAt(row, 5).toString());
                    new StudentProfileDialog(frame, data, major, className, id, name, score).setVisible(true);
                }
            }
        });

        scorePanel.add(toolBar, BorderLayout.NORTH);
        scorePanel.add(new JScrollPane(table), BorderLayout.CENTER);
        return scorePanel;
    }

    private JPanel initLogTab() {
        JPanel historyPanel = new JPanel(new BorderLayout(0, 15));
        historyPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        historyPanel.setOpaque(false);

        JPanel logToolBar = new JPanel();
        logToolBar.setLayout(new BoxLayout(logToolBar, BoxLayout.X_AXIS));
        logToolBar.setOpaque(false);

        JPanel logSortRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        logSortRow.setOpaque(false);
        logTimeSortBtn = new JButton("时间排序↑");
        logClassSortBtn = new JButton("班级排序↑");
        logNameSortBtn = new JButton("姓名排序↑");

        logTimeSortBtn.addActionListener(e -> {
            currentLogSortMode = LogSortMode.TIME;
            isLogAscending = !isLogAscending;
            updateLogTable();
        });
        logClassSortBtn.addActionListener(e -> {
            currentLogSortMode = LogSortMode.CLASS;
            isLogAscending = !isLogAscending;
            updateLogTable();
        });
        logNameSortBtn.addActionListener(e -> {
            currentLogSortMode = LogSortMode.NAME;
            isLogAscending = !isLogAscending;
            updateLogTable();
        });

        currentLogSortLabel = new JLabel();
        currentLogSortLabel.setFont(new Font(Font.DIALOG, Font.ITALIC, 14));
        logSortRow.add(UITools.wrap(logTimeSortBtn));
        logSortRow.add(UITools.wrap(logClassSortBtn));
        logSortRow.add(UITools.wrap(logNameSortBtn));
        logSortRow.add(Box.createHorizontalStrut(5));
        logSortRow.add(currentLogSortLabel);

        JPanel logSearchRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        logSearchRow.setOpaque(false);
        logSearchField = new JTextField(15);
        logSearchField.putClientProperty("JTextField.placeholderText", "🔍 搜索日志...");
        logSearchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { triggerLogSearch(); }
            public void removeUpdate(DocumentEvent e) { triggerLogSearch(); }
            public void changedUpdate(DocumentEvent e) { triggerLogSearch(); }
        });
        logSearchRow.add(logSearchField);

        logToolBar.add(logSortRow);
        logToolBar.add(Box.createHorizontalGlue());
        logToolBar.add(logSearchRow);

        logTableModel = new DefaultTableModel(new String[]{"时间", "班级", "学号", "姓名", "分值变动", "备注"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return column == 5; }
            @Override
            public void setValueAt(Object aValue, int row, int column) { handleLogTableEdit(aValue, row, column); }
        };
        logTable = new JTable(logTableModel);
        logTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        UITools.setupTableStyle(logTable, 36);
        UITools.enableTouchScrolling(logTable);

        historyPanel.add(logToolBar, BorderLayout.NORTH);
        historyPanel.add(new JScrollPane(logTable), BorderLayout.CENTER);
        return historyPanel;
    }

    private JPanel initTrashTab() {
        JPanel trashPanel = new JPanel(new BorderLayout(0, 15));
        trashPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        trashPanel.setOpaque(false);

        JPanel trashToolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        trashToolBar.setOpaque(false);
        restoreBtn = new JButton("🔄 还原选中项");
        cleanTrashBtn = new JButton("💀 彻底清空");
        trashToolBar.add(UITools.wrap(restoreBtn));
        trashToolBar.add(UITools.wrap(cleanTrashBtn));

        trashTableModel = new DefaultTableModel(new String[]{"类型", "被删内容", "原隶属位置", "删除时间"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        trashTable = new JTable(trashTableModel);
        UITools.setupTableStyle(trashTable, 36);
        UITools.enableTouchScrolling(trashTable);

        restoreBtn.addActionListener(e -> {
            int row = trashTable.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(frame, "请先选中要还原的数据行！"); return; }
            try {
                data.restoreItem(data.getRecycleBin().get(row));
                fullRefresh();
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(frame, ex.getMessage(), "还原失败", JOptionPane.ERROR_MESSAGE);
            }
        });
        cleanTrashBtn.addActionListener(e -> {
            if (data.getRecycleBin().isEmpty()) return;
            if (JOptionPane.showConfirmDialog(frame, "确定清空回收站吗？") == JOptionPane.YES_OPTION) {
                data.saveStateForUndo();
                data.getRecycleBin().clear();
                data.saveAllData();
                updateTrashTable();
                updateUndoButtonText();
            }
        });

        trashPanel.add(trashToolBar, BorderLayout.NORTH);
        trashPanel.add(new JScrollPane(trashTable), BorderLayout.CENTER);
        return trashPanel;
    }

    private JPanel initBottomForm(Font titleFont) {
        JPanel bottomFormPanel = UITools.createGlassPanel();
        bottomFormPanel.setLayout(new BorderLayout(0, 10));
        JLabel formTitle = new JLabel("✍️ 快速加分面板");
        formTitle.setFont(titleFont);
        bottomFormPanel.add(formTitle, BorderLayout.NORTH);

        JPanel formWrapper = new JPanel(new GridBagLayout());
        formWrapper.setOpaque(false);

        classComboBox = new JComboBox<>();
        updateClassDropdown();
        idField = new JTextField();
        nameField = new JTextField();
        remarkField = new JTextField();
        remarkField.putClientProperty("JTextField.placeholderText", "加分事由");
        scoreField = new JTextField();
        scoreField.putClientProperty("JTextField.placeholderText", "加/扣分");

        submitBtn = new JButton("💖 提交(Enter)");
        submitBtn.setFont(new Font(Font.DIALOG, Font.BOLD, 16));
        quickScoreBox = new JComboBox<>(new Double[]{0.5, 1.0, 2.0, 5.0, -0.5, -1.0, -2.0, -5.0});
        quickBtn = new JButton("🎀 快捷加分");
        deleteStudentBtn = new JButton("🗑️ 删除该生");

        classComboBox.setPreferredSize(new Dimension(190, 36));
        scoreField.setPreferredSize(new Dimension(110, 36));
        Dimension flexHeight = new Dimension(80, 36);
        idField.setPreferredSize(flexHeight);
        nameField.setPreferredSize(flexHeight);
        remarkField.setPreferredSize(new Dimension(150, 36));
        quickScoreBox.setPreferredSize(new Dimension(80, 36));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridy = 0;

        gbc.gridx = 0; gbc.weightx = 0; gbc.insets = new Insets(8, 0, 8, 5);
        formWrapper.add(new JLabel("班级:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0; gbc.insets = new Insets(8, 0, 8, 20);
        formWrapper.add(classComboBox, gbc);

        gbc.gridx = 2; gbc.weightx = 0; gbc.insets = new Insets(8, 0, 8, 5);
        formWrapper.add(new JLabel("学号:"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.2; gbc.insets = new Insets(8, 0, 8, 20);
        formWrapper.add(idField, gbc);

        gbc.gridx = 4; gbc.weightx = 0; gbc.insets = new Insets(8, 0, 8, 5);
        formWrapper.add(new JLabel("姓名:"), gbc);
        gbc.gridx = 5; gbc.weightx = 0.2; gbc.insets = new Insets(8, 0, 8, 20);
        formWrapper.add(nameField, gbc);

        gbc.gridx = 6; gbc.weightx = 0; gbc.insets = new Insets(8, 0, 8, 5);
        formWrapper.add(new JLabel("备注:"), gbc);
        gbc.gridx = 7; gbc.weightx = 0.6; gbc.insets = new Insets(8, 0, 8, 0);
        formWrapper.add(remarkField, gbc);

        gbc.gridy = 1;
        gbc.gridx = 0; gbc.weightx = 0; gbc.insets = new Insets(8, 0, 8, 5);
        formWrapper.add(new JLabel("分数:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0; gbc.insets = new Insets(8, 0, 8, 20);
        formWrapper.add(scoreField, gbc);

        JPanel actionPanel = new JPanel(new GridBagLayout());
        actionPanel.setOpaque(false);
        GridBagConstraints agbc = new GridBagConstraints();
        agbc.gridy = 0; agbc.fill = GridBagConstraints.NONE; agbc.anchor = GridBagConstraints.WEST;

        agbc.gridx = 0; agbc.weightx = 0; agbc.insets = new Insets(0, 0, 0, 20);
        actionPanel.add(UITools.wrap(submitBtn), agbc);
        agbc.gridx = 1; agbc.weightx = 0; agbc.insets = new Insets(0, 0, 0, 5);
        actionPanel.add(quickScoreBox, agbc);
        agbc.gridx = 2; agbc.weightx = 0; agbc.insets = new Insets(0, 0, 0, 0);
        actionPanel.add(UITools.wrap(quickBtn), agbc);
        agbc.gridx = 3; agbc.weightx = 1.0; agbc.fill = GridBagConstraints.HORIZONTAL;
        actionPanel.add(Box.createHorizontalGlue(), agbc);
        agbc.gridx = 4; agbc.weightx = 0; agbc.fill = GridBagConstraints.NONE; agbc.anchor = GridBagConstraints.EAST; agbc.insets = new Insets(0, 20, 0, 0);
        actionPanel.add(UITools.wrap(deleteStudentBtn), agbc);

        gbc.gridx = 2; gbc.gridwidth = 6; gbc.weightx = 1.0; gbc.insets = new Insets(8, 0, 8, 0); gbc.fill = GridBagConstraints.HORIZONTAL;
        formWrapper.add(actionPanel, gbc);
        bottomFormPanel.add(formWrapper, BorderLayout.CENTER);

        ActionListener enterSubmitAction = e -> handleUpdateScore();
        idField.addActionListener(enterSubmitAction);
        nameField.addActionListener(enterSubmitAction);
        scoreField.addActionListener(enterSubmitAction);
        remarkField.addActionListener(enterSubmitAction);

        submitBtn.addActionListener(e -> handleUpdateScore());
        quickBtn.addActionListener(e -> handleQuickScore());
        deleteStudentBtn.addActionListener(e -> handleDeleteStudent());

        return bottomFormPanel;
    }

    private void paintAccentButtons() {
        boolean isDark = UIManager.getBoolean("laf.dark");
        String currentTheme = (String) UIManager.get("ScoreManager.CurrentTheme");

        Color mainBlue, mainSecondary, warnBg, successBg, importBg, pdfBg, htmlBg, textColor;

        if (isDark) {
            mainBlue = new Color(60, 110, 160); mainSecondary = new Color(160, 80, 100);
            warnBg = new Color(180, 60, 60); successBg = new Color(100, 90, 160);
            importBg = new Color(40, 120, 80); pdfBg = new Color(180, 80, 80);
            htmlBg = new Color(180, 110, 60); textColor = Color.WHITE;
        } else if ("粉白".equals(currentTheme)) {
            mainBlue = new Color(255, 182, 193); mainSecondary = new Color(255, 105, 180);
            warnBg = new Color(255, 110, 130); successBg = new Color(216, 191, 216);
            importBg = new Color(255, 192, 203); pdfBg = new Color(255, 130, 170);
            htmlBg = new Color(255, 160, 120); textColor = Color.WHITE;
        } else if ("白绿".equals(currentTheme)) {
            mainBlue = new Color(110, 180, 140); mainSecondary = new Color(150, 200, 150);
            warnBg = new Color(200, 100, 100); successBg = new Color(120, 160, 200);
            importBg = new Color(80, 160, 120); pdfBg = new Color(220, 110, 90);
            htmlBg = new Color(200, 150, 90); textColor = Color.WHITE;
        } else {
            // ✨ 核心修复：纯正苹果 iOS 科技蓝配色方案
            mainBlue = new Color(0, 122, 255);
            mainSecondary = new Color(106, 175, 255);
            warnBg = new Color(255, 59, 48);
            successBg = new Color(52, 199, 89);
            importBg = new Color(88, 86, 214);
            pdfBg = new Color(255, 105, 97);
            htmlBg = new Color(255, 149, 0);
            textColor = Color.WHITE;
        }

        if (submitBtn != null) { submitBtn.setBackground(mainBlue); submitBtn.setForeground(textColor); }
        if (quickBtn != null) { quickBtn.setBackground(mainSecondary); quickBtn.setForeground(textColor); }
        if (addClassBtn != null) { addClassBtn.setBackground(mainSecondary); addClassBtn.setForeground(textColor); }
        if (exportCsvBtn != null) { exportCsvBtn.setBackground(successBg); exportCsvBtn.setForeground(textColor); }
        if (importCsvBtn != null) { importCsvBtn.setBackground(importBg); importCsvBtn.setForeground(textColor); }
        if (restoreBtn != null) { restoreBtn.setBackground(mainBlue); restoreBtn.setForeground(textColor); }
        if (printPdfBtn != null) { printPdfBtn.setBackground(pdfBg); printPdfBtn.setForeground(textColor); }
        if (exportHtmlBtn != null) { exportHtmlBtn.setBackground(htmlBg); exportHtmlBtn.setForeground(textColor); }
        if (deleteStudentBtn != null) { deleteStudentBtn.putClientProperty("JButton.buttonType", "default"); deleteStudentBtn.setBackground(warnBg); deleteStudentBtn.setForeground(textColor); }
        if (cleanTrashBtn != null) { cleanTrashBtn.putClientProperty("JButton.buttonType", "default"); cleanTrashBtn.setBackground(warnBg); cleanTrashBtn.setForeground(textColor); }

        AbstractButton[] defaultBtns = {manualSaveBtn, undoBtn, scoreSortBtn, idSortBtn, nameSortBtn, renameClassBtn, deleteClassBtn, changeFileBtn, renameFileBtn, moveFileBtn, logTimeSortBtn, logClassSortBtn, logNameSortBtn, helpBtn};
        for (AbstractButton b : defaultBtns) if (b != null) { b.setBackground(null); b.setForeground(null); }
    }

    private void setupGlobalShortcuts() {
        InputMap im = frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "UNDO_ACTION");
        frame.getRootPane().getActionMap().put("UNDO_ACTION", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { handleUndo(); }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "SAVE_ACTION");
        frame.getRootPane().getActionMap().put("SAVE_ACTION", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { manualSaveBtn.doClick(); }
        });
    }

    private DefaultTreeCellRenderer createTreeRenderer() {
        return new DefaultTreeCellRenderer() {
            @Override public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                int level = ((DefaultMutableTreeNode) value).getLevel();
                setIcon(null);
                if (level == 0) { setText("🏫 " + value); setFont(getFont().deriveFont(Font.BOLD, 17f)); }
                else if (level == 1) { setText("📚 " + value); setFont(getFont().deriveFont(Font.BOLD, 15f)); }
                else { setText(" 🏷️ " + value); setFont(getFont().deriveFont(Font.PLAIN, 15f)); }
                setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
                return this;
            }
        };
    }

    private void fullRefresh() {
        updateClassDropdown();
        refreshTree();
        refreshCurrentView();
        updateTrashTable();
        updateUndoButtonText();
        updateFileDisplay();
    }

    private void updateFileDisplay() {
        File file = data.getCurrentDataFile();
        fileDisplayLabel.setText(file.getName());
        fileDisplayLabel.setToolTipText(file.getAbsolutePath());
    }

    private String errorMessage(Throwable error) {
        while (error.getCause() != null) error = error.getCause();
        return error.getMessage() == null ? error.toString() : error.getMessage();
    }

    private boolean finishTableEditing() {
        if (table != null && table.isEditing() && !table.getCellEditor().stopCellEditing()) return false;
        return logTable == null || !logTable.isEditing() || logTable.getCellEditor().stopCellEditing();
    }

    private void handleManualSave() {
        if (closing || !finishTableEditing()) return;
        manualSaveBtn.setEnabled(false);
        manualSaveBtn.setText("正在保存...");
        data.saveAllData().whenComplete((ignored, error) -> SwingUtilities.invokeLater(() -> {
            if (!frame.isDisplayable()) return;
            manualSaveBtn.setEnabled(true);
            manualSaveBtn.setText(error == null ? "💾 强制安全存档 (Ctrl+S)" : "保存失败，请重试 (Ctrl+S)");
            if (error == null) UITools.Toast.showSuccess(frame, "数据已安全存档！");
        }));
    }

    private void handleCloseRequested() {
        if (closing || !finishTableEditing()) return;
        closing = true;
        frame.setEnabled(false);
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        data.saveAllData().whenComplete((ignored, error) -> SwingUtilities.invokeLater(() -> {
            if (error == null) {
                data.shutdown();
                frame.dispose();
                System.exit(0);
            } else {
                closing = false;
                frame.setEnabled(true);
                frame.setCursor(Cursor.getDefaultCursor());
                JOptionPane.showMessageDialog(frame,
                        "保存未完成，程序没有关闭。请处理问题后重试。\n" + errorMessage(error),
                        "无法安全退出", JOptionPane.ERROR_MESSAGE);
            }
        }));
    }

    private void refreshCurrentView() {
        DefaultMutableTreeNode n = (DefaultMutableTreeNode) classTree.getLastSelectedPathComponent();
        String filterMajor = null, filterClass = null;
        if (n != null && n != rootNode) {
            if (n.getParent() == rootNode) filterMajor = n.getUserObject().toString();
            else { filterMajor = ((DefaultMutableTreeNode) n.getParent()).getUserObject().toString(); filterClass = n.getUserObject().toString(); }
        }
        refreshTable(filterMajor, filterClass);
        updateLogTable();
        if (dashboardPanel != null) dashboardPanel.updateData(filterMajor, filterClass);
    }

    private void updateClassDropdown() {
        Object s = classComboBox.getSelectedItem();
        classComboBox.removeAllItems();
        data.getClassesData().keySet().stream().sorted().forEach(classComboBox::addItem);
        if (s != null) classComboBox.setSelectedItem(s);
    }

    private void refreshTree() {
        rootNode.removeAllChildren();
        for (Map.Entry<String, List<String>> entry : data.getMajorToClasses().entrySet()) {
            DefaultMutableTreeNode majorNode = new DefaultMutableTreeNode(entry.getKey());
            for (String className : entry.getValue()) majorNode.add(new DefaultMutableTreeNode(className));
            rootNode.add(majorNode);
        }
        treeModel.reload();
        for (int i = 0; i < classTree.getRowCount(); i++) classTree.expandRow(i);
    }

    private void syncTreeToData() {
        Map<String, List<String>> newMajors = new LinkedHashMap<>();
        for (int i = 0; i < rootNode.getChildCount(); i++) {
            DefaultMutableTreeNode majorNode = (DefaultMutableTreeNode) rootNode.getChildAt(i);
            List<String> classes = new ArrayList<>();
            for (int j = 0; j < majorNode.getChildCount(); j++) classes.add(majorNode.getChildAt(j).toString());
            newMajors.put(majorNode.getUserObject().toString(), classes);
        }
        data.setMajorToClasses(newMajors);
        data.saveAllData();
    }

    private void refreshTable(String filterMajor, String filterClass) {
        List<TableRecord> allRecords = new ArrayList<>();
        for (Map.Entry<String, List<String>> majorEntry : data.getMajorToClasses().entrySet()) {
            if (filterMajor != null && !filterMajor.equals(majorEntry.getKey())) continue;
            for (String className : majorEntry.getValue()) {
                if (filterClass != null && !filterClass.equals(className)) continue;
                Map<String, Student> classMap = data.getClassesData().get(className);
                if (classMap != null) for (Student s : classMap.values()) {
                    if (!currentSearchText.isEmpty() && !s.name.toLowerCase().contains(currentSearchText) && !s.id.toLowerCase().contains(currentSearchText) && !className.toLowerCase().contains(currentSearchText) && !majorEntry.getKey().toLowerCase().contains(currentSearchText)) continue;
                    allRecords.add(new TableRecord(majorEntry.getKey(), className, s));
                }
            }
        }

        allRecords.sort((r1, r2) -> Double.compare(r2.student.score, r1.student.score));
        int rank = 1;
        for (int i = 0; i < allRecords.size(); i++) {
            if (i > 0 && allRecords.get(i).student.score < allRecords.get(i - 1).student.score) rank = i + 1;
            allRecords.get(i).rank = rank;
        }

        allRecords.sort((r1, r2) -> {
            if (currentSortMode == SortMode.SCORE) {
                int comp = Double.compare(r1.student.score, r2.student.score);
                return comp == 0 ? r1.student.id.compareTo(r2.student.id) : (isAscending ? comp : -comp);
            }
            int mc = r1.majorName.compareTo(r2.majorName); if (mc != 0) return mc;
            int cc = r1.className.compareTo(r2.className); if (cc != 0) return cc;
            int fc = currentSortMode == SortMode.ID ? r1.student.id.compareTo(r2.student.id) : Collator.getInstance(Locale.CHINA).compare(r1.student.name, r2.student.name);
            return isAscending ? fc : -fc;
        });

        // 🚀 性能优化：使用矩阵一键注入，避免逐行重绘卡顿
        Object[][] matrixData = new Object[allRecords.size()][6];
        for (int i = 0; i < allRecords.size(); i++) {
            TableRecord r = allRecords.get(i);
            String rs = String.valueOf(r.rank);
            if (r.rank == 1) rs = "🥇 1"; else if (r.rank == 2) rs = "🥈 2"; else if (r.rank == 3) rs = "🥉 3";
            matrixData[i] = new Object[]{rs, r.majorName, r.className, r.student.id, r.student.name, r.student.score};
        }
        tableModel.setDataVector(matrixData, new String[]{"排名", "专业", "班级", "学号", "姓名", "总分数"});

        // ✨ 重要：setDataVector 会重置列属性，必须立刻加回列宽
        table.getColumnModel().getColumn(0).setPreferredWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(5).setPreferredWidth(80);
        updateSortIndicator();
    }

    private void updateLogTable() {
        DefaultMutableTreeNode n = (DefaultMutableTreeNode) classTree.getLastSelectedPathComponent();
        String filterMajor = null, filterClass = null;
        if (n != null && n != rootNode) {
            if (n.getParent() == rootNode) filterMajor = n.getUserObject().toString();
            else { filterMajor = ((DefaultMutableTreeNode) n.getParent()).getUserObject().toString(); filterClass = n.getUserObject().toString(); }
        }
        List<LogEntry> filteredLogs = new ArrayList<>();
        for (LogEntry e : data.getHistoryLogs()) {
            if (filterClass != null && !e.className.equals(filterClass)) continue;
            else if (filterMajor != null) { List<String> cls = data.getMajorToClasses().get(filterMajor); if (cls == null || !cls.contains(e.className)) continue; }
            if (!currentLogSearchText.isEmpty() && !e.name.toLowerCase().contains(currentLogSearchText) && !e.className.toLowerCase().contains(currentLogSearchText) && !e.remark.toLowerCase().contains(currentLogSearchText) && !e.id.toLowerCase().contains(currentLogSearchText)) continue;
            filteredLogs.add(e);
        }

        filteredLogs.sort((e1, e2) -> {
            int comp = currentLogSortMode == LogSortMode.TIME ? e1.time.compareTo(e2.time) : (currentLogSortMode == LogSortMode.CLASS ? e1.className.compareTo(e2.className) : Collator.getInstance(Locale.CHINA).compare(e1.name, e2.name));
            if (comp == 0) comp = e1.time.compareTo(e2.time); return isLogAscending ? comp : -comp;
        });

        visibleLogs = filteredLogs;
        Object[][] logMatrix = new Object[filteredLogs.size()][6];
        for (int i = 0; i < filteredLogs.size(); i++) {
            LogEntry e = filteredLogs.get(i);
            logMatrix[i] = new Object[]{e.time, e.className, e.id, e.name, e.change, e.remark};
        }
        logTableModel.setDataVector(logMatrix, new String[]{"时间", "班级", "学号", "姓名", "分值变动", "备注"});
        logTable.getColumnModel().getColumn(0).setPreferredWidth(180);
        logTable.getColumnModel().getColumn(5).setPreferredWidth(250);

        if (currentLogSortLabel != null) {
            String modeStr = currentLogSortMode == LogSortMode.TIME ? "时间" : (currentLogSortMode == LogSortMode.CLASS ? "班级" : "姓名");
            currentLogSortLabel.setText("当前排序: " + modeStr + (isLogAscending ? "↑" : "↓"));
            String arr = isLogAscending ? "↑" : "↓";
            logTimeSortBtn.setText("时间排序" + (currentLogSortMode == LogSortMode.TIME ? arr : ""));
            logClassSortBtn.setText("班级排序" + (currentLogSortMode == LogSortMode.CLASS ? arr : ""));
            logNameSortBtn.setText("姓名排序" + (currentLogSortMode == LogSortMode.NAME ? arr : ""));
        }
    }

    private void updateTrashTable() {
        Object[][] trashMatrix = new Object[data.getRecycleBin().size()][4];
        for (int i = 0; i < data.getRecycleBin().size(); i++) {
            DataManager.TrashItem item = data.getRecycleBin().get(i);
            trashMatrix[i] = new Object[]{item.type, item.name, item.parentInfo, item.deleteTime};
        }
        trashTableModel.setDataVector(trashMatrix, new String[]{"类型", "被删内容", "原隶属位置", "删除时间"});
    }

    private void updateSortIndicator() {
        if (currentSortLabel == null) return;
        String ms = currentSortMode == SortMode.ID ? "学号" : (currentSortMode == SortMode.NAME ? "姓名" : "分数"), arr = isAscending ? "↑" : "↓";
        currentSortLabel.setText("当前排序: " + ms + arr);
        idSortBtn.setText("学号排序" + (currentSortMode == SortMode.ID ? arr : ""));
        nameSortBtn.setText("姓名排序" + (currentSortMode == SortMode.NAME ? arr : ""));
        if (scoreSortBtn != null) scoreSortBtn.setText("分数排序" + (currentSortMode == SortMode.SCORE ? arr : ""));
    }

    private double parseFiniteScore(String value) {
        double score = Double.parseDouble(value);
        if (!Double.isFinite(score)) throw new NumberFormatException("分值必须是有限数字");
        return score;
    }

    private void handleUpdateScore() {
        if (classComboBox.getSelectedItem() == null) return;
        String id = idField.getText().trim(), name = nameField.getText().trim();
        String scoreText = scoreField.getText().trim(), remark = remarkField.getText().trim();
        if (id.isEmpty() || name.isEmpty() || scoreText.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "请填写完整！"); return;
        }
        String className = classComboBox.getSelectedItem().toString();
        if (data.isStudentIdInUse(id, className, id)) {
            JOptionPane.showMessageDialog(frame, "该学号已在其他班级使用，请检查学号或班级。"); return;
        }
        Map<String, Student> students = data.getClassesData().get(className);
        Student existing = students == null ? null : students.get(id);
        double points, total;
        try {
            points = parseFiniteScore(scoreText);
            total = (existing == null ? 0.0 : existing.score) + points;
            if (!Double.isFinite(total)) throw new NumberFormatException("总分超出有效范围");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(frame, "分值必须是有限数字，且总分不能溢出。", "输入错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        data.saveStateForUndo();
        students = data.getClassesData().computeIfAbsent(className, k -> new HashMap<>());
        if (existing == null) students.put(id, new Student(id, name, total));
        else { existing.name = name; existing.score = total; }
        data.addLog(className, id, name, points, remark);
        fullRefresh();
        scoreField.setText("");
        remarkField.setText("");
    }

    private void handleQuickScore() {
        if (classComboBox.getSelectedItem() == null) return;
        String id = idField.getText().trim(), name = nameField.getText().trim();
        if (id.isEmpty() || name.isEmpty()) { JOptionPane.showMessageDialog(frame, "请选中学生！"); return; }
        String className = classComboBox.getSelectedItem().toString();
        if (data.isStudentIdInUse(id, className, id)) {
            JOptionPane.showMessageDialog(frame, "该学号已在其他班级使用，请检查学号或班级。"); return;
        }
        Map<String, Student> students = data.getClassesData().get(className);
        Student existing = students == null ? null : students.get(id);
        double points = (Double) quickScoreBox.getSelectedItem();
        double total = (existing == null ? 0.0 : existing.score) + points;
        if (!Double.isFinite(total)) { JOptionPane.showMessageDialog(frame, "总分超出有效范围。"); return; }
        data.saveStateForUndo();
        students = data.getClassesData().computeIfAbsent(className, k -> new HashMap<>());
        if (existing == null) students.put(id, new Student(id, name, total));
        else { existing.name = name; existing.score = total; }
        data.addLog(className, id, name, points, remarkField.getText().trim());
        fullRefresh();
        remarkField.setText("");
    }

    private void handleScoreTableEdit(Object value, int row, int column) {
        if (row < 0 || row >= tableModel.getRowCount() || column < 3 || column > 5) return;
        String className = tableModel.getValueAt(row, 2).toString();
        String oldId = tableModel.getValueAt(row, 3).toString();
        String newValue = Objects.toString(value, "").trim();
        Map<String, Student> students = data.getClassesData().get(className);
        if (students == null || !students.containsKey(oldId)) return;
        Student student = students.get(oldId);
        double newScore = student.score;
        if (column == 3) {
            if (newValue.isEmpty() || data.isStudentIdInUse(newValue, className, oldId)) {
                JOptionPane.showMessageDialog(frame, "学号不能为空，也不能与任意班级的学生重复。"); return;
            }
            if (newValue.equals(oldId)) return;
        } else if (column == 4) {
            if (newValue.isEmpty()) { JOptionPane.showMessageDialog(frame, "姓名不能为空。"); return; }
            if (newValue.equals(student.name)) return;
        } else {
            try {
                newScore = parseFiniteScore(newValue);
                if (!Double.isFinite(newScore - student.score)) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "请输入有效的有限分数。"); return;
            }
            if (newScore == student.score) return;
        }
        data.saveStateForUndo();
        if (column == 3) {
            students.remove(oldId);
            student.id = newValue;
            students.put(newValue, student);
            for (LogEntry log : data.getHistoryLogs())
                if (Objects.equals(log.className, className) && Objects.equals(log.id, oldId)) log.id = newValue;
            data.saveAllData();
        } else if (column == 4) {
            student.name = newValue;
            data.saveAllData();
        } else {
            double difference = newScore - student.score;
            student.score = newScore;
            data.addLog(className, student.id, student.name, difference, "表格直接修改");
        }
        refreshCurrentView();
        updateUndoButtonText();
    }

    private void handleLogTableEdit(Object value, int row, int column) {
        if (column != 5 || row < 0 || row >= visibleLogs.size()) return;
        LogEntry target = visibleLogs.get(row);
        if (!data.getHistoryLogs().contains(target)) { updateLogTable(); return; }
        String remark = Objects.toString(value, "").trim();
        if (remark.isEmpty()) remark = "无";
        if (remark.equals(target.remark)) return;
        data.saveStateForUndo();
        target.remark = remark;
        data.saveAllData();
        SwingUtilities.invokeLater(() -> {
            updateLogTable();
            updateUndoButtonText();
        });
    }

    private void handleAddNode(String name, JTextField field) {
        if (name.isEmpty()) return;
        DefaultMutableTreeNode selected = (DefaultMutableTreeNode) classTree.getLastSelectedPathComponent();
        if (selected == null || selected == rootNode) {
            if (data.getMajorToClasses().containsKey(name)) {
                JOptionPane.showMessageDialog(frame, "专业已存在！"); return;
            }
            data.saveStateForUndo();
            data.getMajorToClasses().put(name, new ArrayList<>());
        } else {
            if (data.isClassNameInUse(name)) { JOptionPane.showMessageDialog(frame, "班级已存在！"); return; }
            String major = selected.getParent() == rootNode ? selected.getUserObject().toString()
                    : ((DefaultMutableTreeNode) selected.getParent()).getUserObject().toString();
            data.saveStateForUndo();
            data.getClassesData().put(name, new HashMap<>());
            data.getMajorToClasses().get(major).add(name);
        }
        field.setText("");
        data.saveAllData();
        fullRefresh();
    }

    private void handleRenameNode() {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) classTree.getLastSelectedPathComponent();
        if (node == null || node == rootNode) return;
        String oldName = node.getUserObject().toString();
        boolean isMajor = node.getParent() == rootNode;
        String input = JOptionPane.showInputDialog(frame, isMajor ? "新专业名：" : "新班级名：", oldName);
        if (input == null) return;
        String newName = input.trim();
        if (newName.isEmpty() || newName.equals(oldName)) return;
        if (isMajor ? data.getMajorToClasses().containsKey(newName) : data.isClassNameInUse(newName)) {
            JOptionPane.showMessageDialog(frame, "该名称已存在，未覆盖任何数据。", "名称冲突", JOptionPane.WARNING_MESSAGE);
            return;
        }
        data.saveStateForUndo();
        if (isMajor) {
            Map<String, List<String>> renamed = new LinkedHashMap<>();
            for (Map.Entry<String, List<String>> entry : data.getMajorToClasses().entrySet())
                renamed.put(entry.getKey().equals(oldName) ? newName : entry.getKey(), entry.getValue());
            data.setMajorToClasses(renamed);
            for (DataManager.TrashItem item : data.getRecycleBin())
                if ("班级".equals(item.type) && oldName.equals(item.parentInfo)) item.parentInfo = newName;
        } else {
            String major = ((DefaultMutableTreeNode) node.getParent()).getUserObject().toString();
            Map<String, Student> students = data.getClassesData().remove(oldName);
            data.getClassesData().put(newName, students == null ? new HashMap<>() : students);
            List<String> names = data.getMajorToClasses().get(major);
            names.set(names.indexOf(oldName), newName);
            for (LogEntry log : data.getHistoryLogs())
                if (oldName.equals(log.className)) log.className = newName;
            for (DataManager.TrashItem item : data.getRecycleBin())
                if ("学生".equals(item.type) && oldName.equals(item.parentInfo)) item.parentInfo = newName;
        }
        data.saveAllData();
        fullRefresh();
    }

    private void handleDeleteNode() {
        DefaultMutableTreeNode n = (DefaultMutableTreeNode) classTree.getLastSelectedPathComponent();
        if (n == null || n == rootNode) return;
        String name = n.getUserObject().toString();
        if (JOptionPane.showConfirmDialog(frame, "移至回收站？") == JOptionPane.YES_OPTION) {
            data.saveStateForUndo();
            if (n.getParent() == rootNode) {
                List<String> classes = data.getMajorToClasses().remove(name); Map<String, Map<String, Student>> deletedData = new HashMap<>();
                for (String c : classes) deletedData.put(c, data.getClassesData().remove(c));
                data.moveToTrash("专业", name, "根节点", new DataManager.MajorData(classes, deletedData));
            } else {
                String majorName = ((DefaultMutableTreeNode) n.getParent()).getUserObject().toString();
                data.getMajorToClasses().get(majorName).remove(name); data.moveToTrash("班级", name, majorName, data.getClassesData().remove(name));
            }
            data.saveAllData(); fullRefresh(); updateTrashTable();
        }
    }

    private void handleDeleteStudent() {
        int r = table.getSelectedRow();
        if (r != -1 && JOptionPane.showConfirmDialog(frame, "移至回收站？") == JOptionPane.YES_OPTION) {
            data.saveStateForUndo(); String className = table.getValueAt(r, 2).toString(), studentId = table.getValueAt(r, 3).toString();
            Student s = data.getClassesData().get(className).remove(studentId);
            data.moveToTrash("学生", s.name, className, s); data.saveAllData(); fullRefresh(); updateTrashTable();
        }
    }

    private void handleImportCsv() {
        JFileChooser fc = new JFileChooser(); fc.setDialogTitle("选择要导入的 CSV 表格"); fc.setFileFilter(new FileNameExtensionFilter("CSV 文件 (*.csv)", "csv"));
        if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile(); JDialog loadingDialog = new JDialog(frame, "数据处理中", true);
            loadingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            loadingDialog.setLayout(new BorderLayout()); loadingDialog.setSize(350, 120); loadingDialog.setLocationRelativeTo(frame);
            JProgressBar pb = new JProgressBar(); pb.setIndeterminate(true); pb.setString("正在后台极速解析数万条数据，请稍候..."); pb.setFont(new Font(Font.DIALOG, Font.PLAIN, 14));
            JPanel dp = new JPanel(new BorderLayout()); dp.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); dp.add(pb, BorderLayout.CENTER); loadingDialog.add(dp);
            new SwingWorker<String, Void>() {
                @Override protected String doInBackground() throws Exception { Thread.sleep(500); return data.importFromCsv(file); }
                @Override protected void done() {
                    loadingDialog.dispose();
                    try {
                        String report = get();
                        fullRefresh();
                        JOptionPane.showMessageDialog(frame, report, "导入报告", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        fullRefresh();
                        JOptionPane.showMessageDialog(frame, errorMessage(ex), "导入失败", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
            loadingDialog.setVisible(true);
        }
    }

    private void handleExportCsv() {
        DefaultMutableTreeNode n = (DefaultMutableTreeNode) classTree.getLastSelectedPathComponent();
        String prefix = (n != null && n != rootNode) ? n.getUserObject().toString() : "全局总表";
        if (tableModel.getRowCount() == 0) { JOptionPane.showMessageDialog(frame, "无数据！"); return; }
        String date = new SimpleDateFormat("yyyyMMdd").format(new Date()); JFileChooser fc = new JFileChooser(); fc.setSelectedFile(new File(prefix + "_积分榜_" + date + ".csv"));
        if (fc.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File tf = fc.getSelectedFile(); if (!tf.getName().toLowerCase().endsWith(".csv")) tf = new File(tf.getParentFile(), tf.getName() + ".csv");
            try {
                FileOutputStream fos = new FileOutputStream(tf); fos.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
                try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(fos, "UTF-8"))) {
                    pw.println("排名,专业,班级,学号,姓名,总分数");
                    for (int i = 0; i < tableModel.getRowCount(); i++) pw.println(tableModel.getValueAt(i, 0) + "," + tableModel.getValueAt(i, 1) + "," + tableModel.getValueAt(i, 2) + "," + tableModel.getValueAt(i, 3) + "," + tableModel.getValueAt(i, 4).toString().replace(",", "，") + "," + tableModel.getValueAt(i, 5));
                }
                UITools.Toast.showSuccess(frame, "🎉 导出成功！");
            } catch (Exception ex) { JOptionPane.showMessageDialog(frame, "导出失败！"); }
        }
    }

    private void handleExportHtml() {
        DefaultMutableTreeNode n = (DefaultMutableTreeNode) classTree.getLastSelectedPathComponent();
        String prefix = (n != null && n != rootNode) ? n.getUserObject().toString() : "全局总表";
        if (tableModel.getRowCount() == 0) { JOptionPane.showMessageDialog(frame, "数据为空！"); return; }
        String date = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()); JFileChooser fc = new JFileChooser(); fc.setSelectedFile(new File(prefix + "_精美报表_" + date + ".html"));
        if (fc.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File tf = fc.getSelectedFile(); if (!tf.getName().toLowerCase().endsWith(".html")) tf = new File(tf.getParentFile(), tf.getName() + ".html");
            try {
                try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(tf), "UTF-8"))) {
                    pw.println("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>报表</title><style>body{font-family:sans-serif;padding:40px;}table{width:100%;border-collapse:collapse;}th,td{border:1px solid #eee;padding:12px;text-align:center;}th{background:#f8f9fa;}</style></head><body>");
                    pw.println("<h1>🎓 " + prefix + " 积分考核报表</h1><table><thead><tr><th>排名</th><th>专业</th><th>班级</th><th>学号</th><th>姓名</th><th>总分数</th></tr></thead><tbody>");
                    for (int i = 0; i < tableModel.getRowCount(); i++) { pw.println("<tr>"); for (int j = 0; j < 6; j++) pw.println("<td>" + tableModel.getValueAt(i, j) + "</td>"); pw.println("</tr>"); }
                    pw.println("</tbody></table><script>setTimeout(()=>window.print(),500)</script></body></html>");
                }
                Desktop.getDesktop().browse(tf.toURI());
            } catch (Exception ex) { JOptionPane.showMessageDialog(frame, "生成失败！"); }
        }
    }

    private void handlePrintPdf() {
        DefaultMutableTreeNode n = (DefaultMutableTreeNode) classTree.getLastSelectedPathComponent();
        String prefix = (n != null && n != rootNode) ? n.getUserObject().toString() : "全局总表";
        try {
            MessageFormat h = new MessageFormat("🎓 ScoreManager Pro - " + prefix), f = new MessageFormat("第 {0} 页");
            if (table.print(JTable.PrintMode.FIT_WIDTH, h, f)) UITools.Toast.showSuccess(frame, "🎉 打印成功！");
        } catch (Exception ex) { JOptionPane.showMessageDialog(frame, "打印失败！"); }
    }

    private void handleRenameDataFile() {
        if (!finishTableEditing()) return;
        String current = data.getCurrentDataFile().getName().replaceFirst("(?i)\\.db$", "");
        String input = JOptionPane.showInputDialog(frame, "新文件名：", current);
        if (input == null || input.trim().isEmpty()) return;
        String name = input.trim();
        if (name.matches(".*[\\\\/:*?\"<>|].*")) {
            JOptionPane.showMessageDialog(frame, "请输入文件名，不要包含路径或非法字符。"); return;
        }
        try {
            data.renameDataFile(new File(data.getCurrentDataFile().getParentFile(), name + ".db"));
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(frame, errorMessage(ex), "重命名失败", JOptionPane.ERROR_MESSAGE);
        } finally { updateFileDisplay(); }
    }

    private void handleSelectDataFile() {
        if (!finishTableEditing()) return;
        JFileChooser chooser = new JFileChooser(data.getCurrentDataFile().getParentFile());
        chooser.setFileHidingEnabled(false);
        chooser.setFileFilter(new FileNameExtensionFilter("SQLite 存档 (*.db)", "db"));
        if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            try {
                if (data.changeDataFile(chooser.getSelectedFile())) fullRefresh();
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(frame, "未切换当前存档：\n" + errorMessage(ex), "切换失败", JOptionPane.ERROR_MESSAGE);
            } finally { updateFileDisplay(); }
        }
    }

    private void handleMoveDataFile() {
        if (!finishTableEditing()) return;
        JFileChooser chooser = new JFileChooser(data.getCurrentDataFile().getParentFile());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
            try {
                if (data.moveDataFile(chooser.getSelectedFile())) UITools.Toast.showSuccess(frame, "存档已转移！");
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(frame, errorMessage(ex), "转移失败", JOptionPane.ERROR_MESSAGE);
            } finally { updateFileDisplay(); }
        }
    }

    private void handleUndo() { if (data.undo()) { fullRefresh(); updateTrashTable(); updateUndoButtonText(); UITools.Toast.showInfo(frame, "↩️ 已撤销"); } }

    private void updateUndoButtonText() { if (undoBtn != null) undoBtn.setText(data.getUndoSize() == 0 ? "↩️ 撤销上一步 (Ctrl+Z)" : "↩️ 撤销上一步 (" + data.getUndoSize() + ")"); }

    private enum SortMode {ID, NAME, SCORE}
    private enum LogSortMode {TIME, CLASS, NAME}

    static class TableRecord {
        String majorName, className; Student student; int rank;
        TableRecord(String mn, String cn, Student s) { this.majorName = mn; this.className = cn; this.student = s; }
    }

    private class TreeTransferHandler extends TransferHandler {
        DataFlavor nodesFlavor; DataFlavor[] flavors = new DataFlavor[1]; DefaultMutableTreeNode draggedNode;
        public TreeTransferHandler() { try { nodesFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=\"" + javax.swing.tree.DefaultMutableTreeNode.class.getName() + "\""); flavors[0] = nodesFlavor; } catch (Exception e) {} }
        @Override public int getSourceActions(JComponent c) { return MOVE; }
        @Override protected Transferable createTransferable(JComponent c) { draggedNode = (DefaultMutableTreeNode) ((JTree) c).getSelectionPath().getLastPathComponent(); return draggedNode == rootNode ? null : new Transferable() { @Override public DataFlavor[] getTransferDataFlavors() { return flavors; } @Override public boolean isDataFlavorSupported(DataFlavor f) { return f.equals(nodesFlavor); } @Override public Object getTransferData(DataFlavor f) { return draggedNode; } }; }
        @Override public boolean canImport(TransferSupport s) { if (!s.isDrop() || !s.isDataFlavorSupported(nodesFlavor)) return false; JTree.DropLocation dl = (JTree.DropLocation) s.getDropLocation(); if (dl.getPath() == null) return false; DefaultMutableTreeNode target = (DefaultMutableTreeNode) dl.getPath().getLastPathComponent(); return (draggedNode.getParent() == rootNode) ? (target == rootNode) : (target.getParent() == rootNode); }
        @Override public boolean importData(TransferSupport s) { try { DefaultMutableTreeNode target = (DefaultMutableTreeNode) ((JTree.DropLocation) s.getDropLocation()).getPath().getLastPathComponent(); int idx = ((JTree.DropLocation) s.getDropLocation()).getChildIndex(); data.saveStateForUndo(); treeModel.removeNodeFromParent(draggedNode); treeModel.insertNodeInto(draggedNode, target, idx == -1 ? target.getChildCount() : idx); syncTreeToData(); fullRefresh(); return true; } catch (Exception e) { return false; } }
    }
}
