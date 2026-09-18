import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import javax.swing.SwingUtilities;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class DataManager {
    private File currentDataFile;
    private String dbUrl;
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    // UI state stays on the Swing event thread; database workers receive deep copies.
    private AppStorage storage = new AppStorage();
    private final Stack<StateSnapshot> undoStack = new Stack<>();
    private final Gson gson = new GsonBuilder().create();
    private final Path settingsFile;
    private final boolean rememberLastFile;
    private final AtomicBoolean saveFailureReported = new AtomicBoolean();
    private volatile Consumer<Throwable> saveErrorHandler = Throwable::printStackTrace;
    private ProgressListener listener = (p, m) -> {};

    public DataManager(String defaultFilePath) {
        this(defaultFilePath, null);
    }

    public DataManager(String defaultFilePath, ProgressListener listener) {
        if (listener != null) this.listener = listener;
        this.listener.onProgress(10, "正在初始化环境配置...");
        File appDataDir = applicationDataDirectory(
                System.getProperty("os.name", ""), Paths.get(System.getProperty("user.home"))).toFile();
        settingsFile = new File(appDataDir, "settings.properties").toPath();
        File passedFile = new File(defaultFilePath);
        rememberLastFile = passedFile.getParent() == null;
        String fileName = passedFile.getName().replaceFirst("(?i)\\.(json|txt)$", ".db");
        File selectedFile = rememberLastFile ? new File(appDataDir, fileName)
                : new File(passedFile.getParentFile(), fileName);
        if (rememberLastFile && Files.exists(settingsFile)) {
            Properties settings = readSettings();
            String remembered = settings.getProperty("dataFile");
            if (remembered != null && !remembered.trim().isEmpty()) {
                selectedFile = new File(remembered);
                if (!selectedFile.isFile()) {
                    throw new IllegalStateException("上次使用的存档暂时不可用，请恢复该文件或接入存储设备："
                            + selectedFile.getAbsolutePath());
                }
            }
        }
        currentDataFile = selectedFile.getAbsoluteFile();
        dbUrl = databaseUrl(currentDataFile);
        this.listener.onProgress(30, "正在连接数据库...");
        initDatabase(currentDataFile);
        this.listener.onProgress(50, "正在加载班级与学生...");
        loadAllData();
        rememberDataFile(currentDataFile);
        this.listener.onProgress(95, "正在生成本地安全备份...");
        autoBackup();
        this.listener.onProgress(100, "加载完成");
    }

    static Path applicationDataDirectory(String osName, Path userHome) {
        return applicationDataDirectory(osName, userHome, System.getenv("XDG_DATA_HOME"));
    }

    static Path applicationDataDirectory(String osName, Path userHome, String xdgDataHome) {
        String platform = osName.toLowerCase(Locale.ROOT);
        if (platform.startsWith("mac")) {
            return userHome.resolve("Library").resolve("Application Support").resolve("ScoreManager");
        }
        if (platform.startsWith("windows")) {
            return userHome.resolve("AppData").resolve("LocalLow").resolve("ScoreManager");
        }
        if (xdgDataHome != null && !xdgDataHome.isBlank()) {
            try {
                Path configured = Paths.get(xdgDataHome);
                if (configured.isAbsolute()) return configured.resolve("ScoreManager");
            } catch (InvalidPathException ignored) {
                // The XDG specification requires an absolute, usable path.
            }
        }
        return userHome.resolve(".local").resolve("share").resolve("ScoreManager");
    }

    private String databaseUrl(File file) { return "jdbc:sqlite:" + file.getAbsolutePath(); }
    public File getCurrentDataFile() { return currentDataFile; }
    public Map<String, Map<String, Student>> getClassesData() { return storage.classesData; }
    public Map<String, List<String>> getMajorToClasses() { return storage.majorToClasses; }
    public void setMajorToClasses(Map<String, List<String>> newMap) {
        storage.majorToClasses = new LinkedHashMap<>(newMap);
    }
    public List<LogEntry> getHistoryLogs() { return storage.historyLogs; }
    public List<TrashItem> getRecycleBin() { return storage.recycleBin; }
    public int getUndoSize() { return undoStack.size(); }
    public void setSaveErrorHandler(Consumer<Throwable> handler) { saveErrorHandler = handler; }
    public void shutdown() { dbExecutor.shutdown(); }

    private Properties readSettings() {
        Properties settings = new Properties();
        if (Files.exists(settingsFile)) {
            try (Reader reader = Files.newBufferedReader(settingsFile, StandardCharsets.UTF_8)) {
                settings.load(reader);
            } catch (IOException e) {
                throw new IllegalStateException("无法读取存档路径设置", e);
            }
        }
        return settings;
    }

    private void rememberDataFile(File file) {
        if (!rememberLastFile) return;
        Path temporary = null;
        try {
            Properties settings = readSettings();
            settings.setProperty("dataFile", file.getAbsolutePath());
            Files.createDirectories(settingsFile.getParent());
            temporary = Files.createTempFile(settingsFile.getParent(), "settings-", ".tmp");
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                settings.store(writer, "ScoreManager");
            }
            try {
                Files.move(temporary, settingsFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, settingsFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("无法保存存档路径设置", e);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) {}
            }
        }
    }

    private void initDatabase(File file) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("无法创建存档目录：" + parent);
        }
        try (Connection conn = DriverManager.getConnection(databaseUrl(file)); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS majors (name TEXT PRIMARY KEY)");
            stmt.execute("CREATE TABLE IF NOT EXISTS major_classes (major_name TEXT, class_name TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS students (id TEXT PRIMARY KEY, name TEXT, score REAL, class_name TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS logs (time TEXT, class_name TEXT, student_id TEXT, student_name TEXT, change_val TEXT, remark TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS trash (type TEXT, name TEXT, parent_info TEXT, data_json TEXT, delete_time TEXT)");
            stmt.execute("INSERT OR IGNORE INTO majors (name) SELECT DISTINCT major_name FROM major_classes WHERE major_name IS NOT NULL");
            stmt.execute("CREATE TABLE IF NOT EXISTS users (username TEXT PRIMARY KEY, password TEXT, role TEXT, "
                    + "password_scheme TEXT NOT NULL DEFAULT 'plain')");
            boolean hasPasswordScheme = false;
            try (ResultSet columns = stmt.executeQuery("PRAGMA table_info(users)")) {
                while (columns.next()) {
                    if ("password_scheme".equals(columns.getString("name"))) hasPasswordScheme = true;
                }
            }
            if (!hasPasswordScheme)
                stmt.execute("ALTER TABLE users ADD COLUMN password_scheme TEXT NOT NULL DEFAULT 'plain'");

            try { Files.setAttribute(file.toPath(), "dos:hidden", true); } catch (Exception ignored) {}
        } catch (SQLException e) {
            throw new IllegalStateException("无法打开数据库：" + file.getAbsolutePath(), e);
        }
    }

    public void loadAllData() { storage = readAllData(dbUrl); }

    private AppStorage readAllData(String url) {
        AppStorage loaded = new AppStorage();
        try (Connection conn = DriverManager.getConnection(url); Statement stmt = conn.createStatement()) {
            ResultSet majors = stmt.executeQuery("SELECT name FROM majors ORDER BY rowid");
            while (majors.next()) loaded.majorToClasses.put(majors.getString("name"), new ArrayList<>());
            ResultSet classes = stmt.executeQuery("SELECT * FROM major_classes ORDER BY rowid");
            while (classes.next()) {
                String majorName = classes.getString("major_name"), className = classes.getString("class_name");
                List<String> names = loaded.majorToClasses.computeIfAbsent(majorName, k -> new ArrayList<>());
                if (!names.contains(className)) names.add(className);
                loaded.classesData.putIfAbsent(className, new HashMap<>());
            }
            ResultSet students = stmt.executeQuery("SELECT * FROM students");
            while (students.next()) {
                String id = students.getString("id");
                loaded.classesData.computeIfAbsent(students.getString("class_name"), k -> new HashMap<>())
                        .put(id, new Student(id, students.getString("name"), students.getDouble("score")));
            }
            ResultSet logs = stmt.executeQuery("SELECT * FROM logs ORDER BY rowid");
            while (logs.next()) {
                loaded.historyLogs.add(new LogEntry(logs.getString("time"), logs.getString("class_name"),
                        logs.getString("student_id"), logs.getString("student_name"),
                        logs.getString("change_val"), logs.getString("remark")));
            }
            ResultSet trash = stmt.executeQuery("SELECT * FROM trash ORDER BY rowid");
            while (trash.next()) {
                String type = trash.getString("type");
                loaded.recycleBin.add(new TrashItem(type, trash.getString("name"), trash.getString("parent_info"),
                        decodeTrashData(type, trash.getString("data_json")), trash.getString("delete_time")));
            }
            return loaded;
        } catch (SQLException | RuntimeException e) {
            throw new IllegalStateException("读取存档失败，未切换当前数据", e);
        }
    }

    private Object decodeTrashData(String type, String json) {
        if ("学生".equals(type)) return gson.fromJson(json, Student.class);
        if ("班级".equals(type)) return gson.fromJson(json, new TypeToken<Map<String, Student>>() {}.getType());
        if ("专业".equals(type)) return gson.fromJson(json, MajorData.class);
        throw new IllegalArgumentException("未知回收站数据类型：" + type);
    }

    private AppStorage copyStorage(AppStorage source) {
        AppStorage copy = new AppStorage();
        for (Map.Entry<String, List<String>> entry : source.majorToClasses.entrySet())
            copy.majorToClasses.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        for (Map.Entry<String, Map<String, Student>> entry : source.classesData.entrySet()) {
            Map<String, Student> students = new HashMap<>();
            for (Map.Entry<String, Student> student : entry.getValue().entrySet()) {
                Student s = student.getValue();
                students.put(student.getKey(), new Student(s.id, s.name, s.score));
            }
            copy.classesData.put(entry.getKey(), students);
        }
        for (LogEntry log : source.historyLogs)
            copy.historyLogs.add(new LogEntry(log.time, log.className, log.id, log.name, log.change, log.remark));
        for (TrashItem item : source.recycleBin)
            copy.recycleBin.add(new TrashItem(item.type, item.name, item.parentInfo,
                    decodeTrashData(item.type, gson.toJson(item.data)), item.deleteTime));
        return copy;
    }

    public synchronized CompletableFuture<Void> saveAllData() {
        CompletableFuture<Void> future;
        try {
            final String targetUrl = dbUrl;
            final AppStorage snapshot = copyStorage(storage);
            future = CompletableFuture.runAsync(() -> writeSnapshot(targetUrl, snapshot), dbExecutor);
        } catch (RuntimeException e) {
            future = new CompletableFuture<>();
            future.completeExceptionally(e);
        }
        future.whenComplete((ignored, error) -> {
            if (error == null) saveFailureReported.set(false);
            else if (saveFailureReported.compareAndSet(false, true)) saveErrorHandler.accept(error);
        });
        return future;
    }

    private void writeSnapshot(String url, AppStorage snapshot) {
        Set<String> ids = new HashSet<>();
        for (Map<String, Student> students : snapshot.classesData.values()) {
            for (Student s : students.values()) {
                if (s.id == null || s.id.trim().isEmpty() || !ids.add(s.id))
                    throw new IllegalArgumentException("学号为空或在多个班级重复：" + s.id);
                if (!Double.isFinite(s.score)) throw new IllegalArgumentException("分数不是有效的有限数字：" + s.id);
            }
        }
        try (Connection conn = DriverManager.getConnection(url)) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM major_classes");
                stmt.execute("DELETE FROM students");
                stmt.execute("DELETE FROM logs");
                stmt.execute("DELETE FROM trash");
                stmt.execute("DELETE FROM majors");
                try (PreparedStatement p = conn.prepareStatement("INSERT INTO majors (name) VALUES (?)")) {
                    for (String major : snapshot.majorToClasses.keySet()) { p.setString(1, major); p.addBatch(); }
                    p.executeBatch();
                }
                try (PreparedStatement p = conn.prepareStatement("INSERT INTO major_classes VALUES (?, ?)")) {
                    for (Map.Entry<String, List<String>> entry : snapshot.majorToClasses.entrySet()) {
                        for (String className : entry.getValue()) {
                            p.setString(1, entry.getKey()); p.setString(2, className); p.addBatch();
                        }
                    }
                    p.executeBatch();
                }
                try (PreparedStatement p = conn.prepareStatement("INSERT INTO students VALUES (?, ?, ?, ?)")) {
                    for (Map.Entry<String, Map<String, Student>> entry : snapshot.classesData.entrySet()) {
                        for (Student s : entry.getValue().values()) {
                            p.setString(1, s.id); p.setString(2, s.name); p.setDouble(3, s.score);
                            p.setString(4, entry.getKey()); p.addBatch();
                        }
                    }
                    p.executeBatch();
                }
                try (PreparedStatement p = conn.prepareStatement("INSERT INTO logs VALUES (?, ?, ?, ?, ?, ?)")) {
                    for (LogEntry log : snapshot.historyLogs) {
                        p.setString(1, log.time); p.setString(2, log.className); p.setString(3, log.id);
                        p.setString(4, log.name); p.setString(5, log.change); p.setString(6, log.remark); p.addBatch();
                    }
                    p.executeBatch();
                }
                try (PreparedStatement p = conn.prepareStatement("INSERT INTO trash VALUES (?, ?, ?, ?, ?)")) {
                    for (TrashItem item : snapshot.recycleBin) {
                        p.setString(1, item.type); p.setString(2, item.name); p.setString(3, item.parentInfo);
                        p.setString(4, gson.toJson(item.data)); p.setString(5, item.deleteTime); p.addBatch();
                    }
                    p.executeBatch();
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                try { conn.rollback(); } catch (SQLException rollbackError) { e.addSuppressed(rollbackError); }
                throw e;
            }
        } catch (SQLException e) {
            throw new CompletionException(e);
        }
    }

    private void autoBackup() {
        try {
            if (!currentDataFile.exists()) return;
            Path backupDir = Paths.get(currentDataFile.getParent(), "backups");
            if (!Files.exists(backupDir)) {
                Files.createDirectories(backupDir);
                try {
                    Files.setAttribute(backupDir, "dos:hidden", true);
                } catch (Exception ignored) {
                }
            }
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date());
            Path backupPath = backupDir.resolve("Backup_" + timestamp + ".db");
            Files.copy(currentDataFile.toPath(), backupPath, StandardCopyOption.REPLACE_EXISTING);
            File[] files = backupDir.toFile().listFiles((dir, name) -> name.endsWith(".db"));
            if (files != null && files.length > 10) {
                Arrays.sort(files, Comparator.comparingLong(File::lastModified));
                for (int i = 0; i < files.length - 10; i++) files[i].delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized boolean changeDataFile(File newFile) {
        if (newFile == null) return false;
        File target = newFile.getAbsoluteFile();
        if (!target.isFile()) throw new IllegalArgumentException("请选择存在的数据库文件");
        saveAllData().join();
        initDatabase(target);
        AppStorage loaded = readAllData(databaseUrl(target));
        rememberDataFile(target);
        currentDataFile = target;
        dbUrl = databaseUrl(target);
        storage = loaded;
        clearUndoStack();
        logout();
        return true;
    }

    public synchronized boolean moveDataFile(File targetDir) {
        if (targetDir == null || !targetDir.isDirectory()) throw new IllegalArgumentException("请选择存在的目标目录");
        relocateDataFile(new File(targetDir, currentDataFile.getName()));
        return true;
    }

    public synchronized boolean renameDataFile(File newFile) {
        relocateDataFile(newFile);
        return true;
    }

    private void relocateDataFile(File newFile) {
        try {
            File source = currentDataFile.getCanonicalFile(), target = newFile.getCanonicalFile();
            if (source.equals(target)) return;
            if (target.exists()) throw new IllegalArgumentException("目标位置已有同名文件，未覆盖：" + target);
            saveAllData().join();
            Files.move(source.toPath(), target.toPath());
            try {
                rememberDataFile(target);
            } catch (RuntimeException e) {
                try { Files.move(target.toPath(), source.toPath()); }
                catch (IOException rollbackError) {
                    currentDataFile = target;
                    dbUrl = databaseUrl(target);
                    e.addSuppressed(rollbackError);
                    throw new IllegalStateException("文件已移至 " + target + "，但无法保存新路径，请记下此位置", e);
                }
                throw e;
            }
            currentDataFile = target;
            dbUrl = databaseUrl(target);
            try { Files.setAttribute(target.toPath(), "dos:hidden", true); } catch (Exception ignored) {}
        } catch (IOException e) {
            throw new IllegalStateException("移动或重命名存档失败", e);
        }
    }

    public boolean isStudentIdInUse(String id, String excludedClass, String excludedId) {
        for (Map.Entry<String, Map<String, Student>> entry : storage.classesData.entrySet()) {
            if (entry.getValue().containsKey(id)
                    && !(Objects.equals(entry.getKey(), excludedClass) && Objects.equals(id, excludedId))) return true;
        }
        return false;
    }

    public boolean isClassNameInUse(String name) {
        if (storage.classesData.containsKey(name)) return true;
        for (List<String> classes : storage.majorToClasses.values()) if (classes.contains(name)) return true;
        return false;
    }

    public void addLog(String className, String id, String name, double change, String remark) {
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        storage.historyLogs.add(0, new LogEntry(time, className, id, name, (change > 0 ? "+" : "") + change,
                (remark == null || remark.isEmpty()) ? "无" : remark));
        saveAllData();
    }

    public void moveToTrash(String type, String name, String parentInfo, Object dataObj) {
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        storage.recycleBin.add(0, new TrashItem(type, name, parentInfo,
                decodeTrashData(type, gson.toJson(dataObj)), time));
        saveAllData();
    }

    private void checkRestoredStudents(Map<String, Map<String, Student>> restored) {
        Set<String> ids = new HashSet<>();
        for (Map<String, Student> students : storage.classesData.values()) ids.addAll(students.keySet());
        for (Map<String, Student> students : restored.values()) {
            if (students == null) throw new IllegalArgumentException("回收站中的学生数据不完整");
            for (Map.Entry<String, Student> entry : students.entrySet()) {
                Student s = entry.getValue();
                if (s == null || s.id == null || s.id.trim().isEmpty() || !Objects.equals(entry.getKey(), s.id)
                        || s.name == null || !Double.isFinite(s.score))
                    throw new IllegalArgumentException("回收站中的学生数据格式有误");
                if (!ids.add(s.id)) throw new IllegalArgumentException("学号已存在，未覆盖现有学生：" + s.id);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void restoreItem(TrashItem item) {
        if (!storage.recycleBin.contains(item)) throw new IllegalArgumentException("该回收站记录已不存在");
        Object restored = decodeTrashData(item.type, gson.toJson(item.data));
        if ("学生".equals(item.type)) {
            Student s = (Student) restored;
            if (s == null) throw new IllegalArgumentException("回收站中的学生数据不完整");
            if (!storage.classesData.containsKey(item.parentInfo))
                throw new IllegalArgumentException("请先还原所属班级：" + item.parentInfo);
            Map<String, Student> students = new HashMap<>();
            students.put(s.id, s);
            checkRestoredStudents(Collections.singletonMap(item.parentInfo, students));
            saveStateForUndo();
            storage.classesData.get(item.parentInfo).put(s.id, s);
        } else if ("班级".equals(item.type)) {
            if (isClassNameInUse(item.name)) throw new IllegalArgumentException("同名班级已存在，未覆盖：" + item.name);
            Map<String, Student> students = (Map<String, Student>) restored;
            checkRestoredStudents(Collections.singletonMap(item.name, students));
            saveStateForUndo();
            storage.classesData.put(item.name, students);
            storage.majorToClasses.computeIfAbsent(item.parentInfo, k -> new ArrayList<>()).add(item.name);
        } else if ("专业".equals(item.type)) {
            if (storage.majorToClasses.containsKey(item.name))
                throw new IllegalArgumentException("同名专业已存在，未覆盖：" + item.name);
            MajorData major = (MajorData) restored;
            if (major == null || major.classList == null || major.allStudents == null)
                throw new IllegalArgumentException("回收站中的专业数据不完整");
            Map<String, Map<String, Student>> students = new HashMap<>();
            for (String className : major.classList) {
                if (isClassNameInUse(className) || students.containsKey(className))
                    throw new IllegalArgumentException("班级名冲突，未覆盖：" + className);
                students.put(className, major.allStudents.get(className));
            }
            checkRestoredStudents(students);
            saveStateForUndo();
            storage.majorToClasses.put(item.name, new ArrayList<>(major.classList));
            storage.classesData.putAll(students);
        } else {
            throw new IllegalArgumentException("未知回收站类型：" + item.type);
        }
        storage.recycleBin.remove(item);
        saveAllData();
    }

    public void saveStateForUndo() {
        AppStorage snapshot = copyStorage(storage);
        if (undoStack.size() >= 30) undoStack.removeElementAt(0);
        undoStack.push(new StateSnapshot(snapshot));
    }

    public boolean undo() {
        if (undoStack.isEmpty()) return false;
        storage = undoStack.pop().storage;
        saveAllData();
        return true;
    }

    public void clearUndoStack() { undoStack.clear(); }

    public String importFromCsv(File file) {
        final List<String[]> rows = new ArrayList<>();
        int malformed = 0;
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                line = line.trim().replace("\uFEFF", "");
                if (line.isEmpty()) continue;
                if (firstLine) {
                    firstLine = false;
                    if (line.contains("姓名") || line.contains("学号")) continue;
                }
                String[] parts = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                for (int i = 0; i < parts.length; i++)
                    parts[i] = parts[i].replaceAll("^\"|\"$", "").replace("\"\"", "\"").trim();
                if (parts.length < 4 || parts.length > 6) { malformed++; continue; }
                int len = parts.length;
                try {
                    double score = Double.parseDouble(parts[len - 1]);
                    if (!Double.isFinite(score) || parts[len - 2].isEmpty() || parts[len - 3].isEmpty()
                            || parts[len - 4].isEmpty() || (len >= 5 && parts[len - 5].isEmpty())) {
                        malformed++; continue;
                    }
                    rows.add(parts);
                } catch (NumberFormatException e) { malformed++; }
            }
        } catch (IOException e) {
            return "导入失败，当前数据未修改：\n" + e.getMessage();
        }
        final int skippedRows = malformed;
        try {
            FutureTask<ImportResult> task = new FutureTask<>(() -> applyImportedRows(rows, skippedRows));
            if (SwingUtilities.isEventDispatchThread()) task.run();
            else SwingUtilities.invokeAndWait(task);
            ImportResult result = task.get();
            try {
                result.saved.join();
                return "导入完成！\n成功导入：" + result.added + " 名新生\n忽略/跳过：" + result.skipped + " 条";
            } catch (CompletionException e) {
                return "已导入 " + result.added + " 名学生到内存，但保存失败。\n请重试保存或撤销本次导入。";
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "导入任务被中断，请检查当前名单和存档状态。";
        } catch (Exception e) {
            return "导入失败：\n" + e.getMessage();
        }
    }

    private ImportResult applyImportedRows(List<String[]> rows, int skipped) {
        Set<String> ids = new HashSet<>();
        for (Map<String, Student> students : storage.classesData.values()) ids.addAll(students.keySet());
        Map<String, String> owners = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : storage.majorToClasses.entrySet())
            for (String className : entry.getValue()) owners.put(className, entry.getKey());
        int added = 0;
        for (String[] parts : rows) {
            int len = parts.length;
            String id = parts[len - 3], className = parts[len - 4];
            String major = len >= 5 ? parts[len - 5] : owners.getOrDefault(className, "默认专业");
            if (ids.contains(id) || (owners.containsKey(className) && !owners.get(className).equals(major))) {
                skipped++; continue;
            }
            if (added == 0) saveStateForUndo();
            List<String> classes = storage.majorToClasses.computeIfAbsent(major, k -> new ArrayList<>());
            if (!classes.contains(className)) classes.add(className);
            storage.classesData.computeIfAbsent(className, k -> new HashMap<>())
                    .put(id, new Student(id, parts[len - 2], Double.parseDouble(parts[len - 1])));
            ids.add(id);
            owners.put(className, major);
            added++;
        }
        return new ImportResult(added, skipped, added == 0 ? CompletableFuture.completedFuture(null) : saveAllData());
    }

    private static class ImportResult {
        final int added, skipped;
        final CompletableFuture<Void> saved;
        ImportResult(int added, int skipped, CompletableFuture<Void> saved) {
            this.added = added; this.skipped = skipped; this.saved = saved;
        }
    }

    public interface ProgressListener { void onProgress(int percent, String message); }

    public static class AppStorage {
        public Map<String, List<String>> majorToClasses = new LinkedHashMap<>();
        public Map<String, Map<String, Student>> classesData = new HashMap<>();
        public List<LogEntry> historyLogs = new ArrayList<>();
        public List<TrashItem> recycleBin = new ArrayList<>();
    }

    public static class TrashItem {
        public String type, name, parentInfo, deleteTime;
        public Object data;
        public TrashItem(String type, String name, String parentInfo, Object data, String deleteTime) {
            this.type = type; this.name = name; this.parentInfo = parentInfo;
            this.data = data; this.deleteTime = deleteTime;
        }
    }

    public static class MajorData {
        public List<String> classList;
        public Map<String, Map<String, Student>> allStudents;
        public MajorData(List<String> classes, Map<String, Map<String, Student>> students) {
            classList = classes; allStudents = students;
        }
    }

    private static class StateSnapshot {
        final AppStorage storage;
        StateSnapshot(AppStorage storage) { this.storage = storage; }
    }
    private String currentUsername = "";

    public synchronized String getCurrentUsername() { return currentUsername; }

    public synchronized void logout() { currentUsername = ""; }

    public synchronized boolean hasUsers() {
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1 FROM users LIMIT 1")) {
            return rs.next();
        } catch (SQLException e) { throw new IllegalStateException("无法读取本地账号", e); }
    }

    /** Only an empty database can create its first administrator without an existing session. */
    public synchronized boolean createFirstAdmin(String username, String password) {
        validateCredentials(username, password);
        String sql = "INSERT INTO users (username, password, role, password_scheme) "
                + "SELECT ?, ?, 'admin', ? WHERE NOT EXISTS (SELECT 1 FROM users)";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement p = conn.prepareStatement(sql)) {
            p.setString(1, username); p.setString(2, PasswordHasher.hash(password));
            p.setString(3, PasswordHasher.SCHEME);
            if (p.executeUpdate() != 1) return false;
            currentUsername = username;
            return true;
        } catch (SQLException e) { throw new IllegalStateException("无法创建管理员账号", e); }
    }

    public synchronized boolean verifyLogin(String username, String password) {
        logout();
        if (username == null || password == null) return false;
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            String stored, scheme, role;
            try (PreparedStatement p = conn.prepareStatement(
                    "SELECT password, password_scheme, role FROM users WHERE username = ?")) {
                p.setString(1, username);
                try (ResultSet rs = p.executeQuery()) {
                    if (!rs.next()) return false;
                    stored = rs.getString("password"); scheme = rs.getString("password_scheme");
                    role = rs.getString("role");
                }
            }
            if (!isValidRole(role)) return false;
            if (PasswordHasher.SCHEME.equals(scheme)) {
                if (!PasswordHasher.verify(password, stored)) return false;
            } else if ("plain".equals(scheme)) {
                if (!PasswordHasher.verifyLegacy(password, stored)) return false;
                // A successful legacy login upgrades only this account, without changing its password.
                try (PreparedStatement p = conn.prepareStatement("UPDATE users SET password = ?, password_scheme = ? "
                        + "WHERE username = ? AND password = ? AND password_scheme = 'plain'")) {
                    p.setString(1, PasswordHasher.hash(password)); p.setString(2, PasswordHasher.SCHEME);
                    p.setString(3, username); p.setString(4, stored);
                    if (p.executeUpdate() != 1) return false;
                }
            } else return false;
            currentUsername = username;
            return true;
        } catch (SQLException e) { throw new IllegalStateException("无法读取或更新本地账号", e); }
    }

    public synchronized boolean isAdmin() {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            return isAdmin(conn);
        } catch (SQLException e) { throw new IllegalStateException("无法检查账号权限", e); }
    }

    private boolean isAdmin(Connection conn) throws SQLException {
        if (currentUsername.isEmpty()) return false;
        try (PreparedStatement p = conn.prepareStatement("SELECT role FROM users WHERE username = ?")) {
            p.setString(1, currentUsername);
            try (ResultSet rs = p.executeQuery()) { return rs.next() && "admin".equals(rs.getString(1)); }
        }
    }

    private void requireAdmin(Connection conn) throws SQLException {
        if (!isAdmin(conn)) throw new SecurityException("需要使用当前存档的管理员账号登录");
    }

    private static boolean isValidRole(String role) { return "admin".equals(role) || "user".equals(role); }

    private static void validateUsername(String username) {
        if (username == null || username.isBlank() || username.length() > 64
                || !username.equals(username.trim()) || username.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("账号需为 1–64 个字符，不能包含控制字符或首尾空格");
    }

    private static void validateCredentials(String username, String password) {
        validateUsername(username);
        if (password == null || password.length() < 8 || password.length() > 256)
            throw new IllegalArgumentException("新密码长度需为 8–256 个字符");
    }

    /** Returns usernames and roles only. Passwords and hashes never leave the account service. */
    public synchronized List<String[]> getAllUsers() {
        List<String[]> users = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            requireAdmin(conn);
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT username, role FROM users ORDER BY username")) {
                while (rs.next()) users.add(new String[]{rs.getString("username"), rs.getString("role")});
            }
            return users;
        } catch (SQLException e) { throw new IllegalStateException("无法读取账号列表", e); }
    }

    public synchronized boolean addUser(String username, String password, String role) {
        validateCredentials(username, password);
        if (!isValidRole(role)) throw new IllegalArgumentException("无效的账号角色");
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            conn.setAutoCommit(false);
            requireAdmin(conn);
            try (PreparedStatement p = conn.prepareStatement(
                    "INSERT INTO users (username, password, role, password_scheme) VALUES (?, ?, ?, ?)")) {
                p.setString(1, username); p.setString(2, PasswordHasher.hash(password));
                p.setString(3, role); p.setString(4, PasswordHasher.SCHEME);
                p.executeUpdate();
            }
            conn.commit();
            return true;
        } catch (SQLException e) { return false; }
    }

    /** A null or empty newPassword preserves the existing password, including unmigrated legacy ones. */
    public synchronized boolean updateUser(String oldUsername, String newUsername, String newPassword, String newRole) {
        validateUsername(newUsername);
        boolean changePassword = newPassword != null && !newPassword.isEmpty();
        if (changePassword) validateCredentials(newUsername, newPassword);
        if (!isValidRole(newRole)) throw new IllegalArgumentException("无效的账号角色");
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            conn.setAutoCommit(false);
            requireAdmin(conn);
            // Keep the current administrator usable until another administrator performs the change.
            if (Objects.equals(oldUsername, currentUsername) && !"admin".equals(newRole)) return false;
            String sql = changePassword
                    ? "UPDATE users SET username = ?, role = ?, password = ?, password_scheme = ? WHERE username = ?"
                    : "UPDATE users SET username = ?, role = ? WHERE username = ?";
            try (PreparedStatement p = conn.prepareStatement(sql)) {
                p.setString(1, newUsername); p.setString(2, newRole);
                if (changePassword) {
                    p.setString(3, PasswordHasher.hash(newPassword)); p.setString(4, PasswordHasher.SCHEME);
                    p.setString(5, oldUsername);
                } else p.setString(3, oldUsername);
                if (p.executeUpdate() != 1) return false;
            }
            conn.commit();
            if (Objects.equals(oldUsername, currentUsername)) currentUsername = newUsername;
            return true;
        } catch (SQLException e) { return false; }
    }

    public synchronized boolean deleteUser(String username) {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            conn.setAutoCommit(false);
            requireAdmin(conn);
            if (Objects.equals(username, currentUsername)) return false;
            try (PreparedStatement p = conn.prepareStatement("DELETE FROM users WHERE username = ?")) {
                p.setString(1, username);
                if (p.executeUpdate() != 1) return false;
            }
            conn.commit();
            return true;
        } catch (SQLException e) { return false; }
    }
}
