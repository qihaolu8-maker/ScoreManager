import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Tests the actual import/save path with synthetic data and an isolated database. */
public final class CsvPersistenceTest {
    private static void check(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }

    private static void verify(DataManager manager, List<String[]> expected) {
        check(manager.getMajorToClasses().size() == 2, "two majors");
        check(manager.getClassesData().size() == 5, "five classes");
        check(manager.getClassesData().values().stream().mapToInt(Map::size).sum() == 50, "50 students");
        for (Map<String, Student> students : manager.getClassesData().values())
            check(students.size() == 10, "ten students per class");
        for (String[] row : expected) {
            check(manager.getMajorToClasses().get(row[0]).contains(row[1]), "class ownership");
            Student student = manager.getClassesData().get(row[1]).get(row[2]);
            check(student != null && student.id.equals(row[2]) && student.name.equals(row[3]), "student identity");
            check(Double.compare(student.score, Double.parseDouble(row[4])) == 0, "exact stored score");
        }
    }

    public static void main(String[] args) throws Exception {
        Path scratch = Files.createTempDirectory("scoremanager-csv-");
        System.setProperty("user.home", scratch.resolve("isolated home").toString());
        Path csv = Paths.get("examples", "ScoreManager-50-students.csv").toAbsolutePath();
        List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);
        check(lines.size() == 51, "one header plus 50 records");
        check(lines.get(0).replace("\uFEFF", "").equals("专业,班级,学号,姓名,总分数"), "UTF-8 header");
        List<String[]> expected = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        boolean negative = false, zero = false, fractional = false;
        for (int i = 1; i < lines.size(); i++) {
            String[] row = lines.get(i).split(",", -1);
            check(row.length == 5 && ids.add(row[2]), "five fields and unique ID");
            check(row[2].equals(String.format(Locale.ROOT, "TEST%04d", i)), "stable example IDs");
            double score = Double.parseDouble(row[4]);
            negative |= score < 0; zero |= score == 0; fractional |= score != Math.rint(score);
            expected.add(row);
        }
        check(negative && zero && fractional, "representative score boundaries");
        Path database = scratch.resolve("中文路径 with spaces").resolve("ScoreData.db");
        DataManager manager = new DataManager(database.toString());
        try {
            check(manager.createFirstAdmin("test-admin", "test-only-password"), "create isolated account");
            String imported = manager.importFromCsv(csv.toFile());
            check(imported.contains("成功导入：50 名新生") && imported.contains("忽略/跳过：0 条"), imported);
            verify(manager, expected);
            manager.saveAllData().join();
        } finally { manager.shutdown(); }

        DataManager reopened = new DataManager(database.toString());
        try {
            check(reopened.verifyLogin("test-admin", "test-only-password"), "reopen account");
            verify(reopened, expected);
            String duplicate = reopened.importFromCsv(csv.toFile());
            check(duplicate.contains("成功导入：0 名新生") && duplicate.contains("忽略/跳过：50 条"), duplicate);
            Path malformed = scratch.resolve("malformed.csv");
            Files.writeString(malformed, "专业,班级,学号,姓名,总分数\n"
                    + "测试计算机科学,测试计科1班,BAD001,无效分数,NaN\n"
                    + "测试计算机科学,测试计科1班,BAD002,无穷分数,Infinity\n"
                    + "其他专业,测试计科1班,BAD003,冲突归属,10\n"
                    + "测试计算机科学,测试计科1班,,缺少学号,10\n", StandardCharsets.UTF_8);
            String rejected = reopened.importFromCsv(malformed.toFile());
            check(rejected.contains("成功导入：0 名新生") && rejected.contains("忽略/跳过：4 条"), rejected);
            verify(reopened, expected);
            check(Files.isDirectory(database.getParent().resolve("backups")), "backup directory");
        } finally { reopened.shutdown(); }
        System.out.println("PASS CsvPersistenceTest: 50 students, Unicode paths, exact persistence, duplicates and invalid rows.");
        // importFromCsv initializes Swing's event thread even in a headless test process.
        System.exit(0);
    }
}
