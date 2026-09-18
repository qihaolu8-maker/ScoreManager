import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Standalone regression tests. All databases and backups are created in a fresh temporary directory. */
public class AccountStorageTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        Path temporary = Files.createTempDirectory("scoremanager-account-test-");
        List<DataManager> managers = new ArrayList<>();
        boolean success = false;
        try {
            testPaths(temporary);
            testPasswordHasher();
            testNewAccounts(temporary, managers);
            testLegacyMigration(temporary, managers);
            testDatabaseChanges(temporary, managers);
            success = true;
            System.out.println("AccountStorageTest: " + checks + " checks passed");
        } finally {
            for (DataManager manager : managers) manager.shutdown();
            if (success) {
                try (var paths = Files.walk(temporary)) {
                    for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
                }
            } else System.err.println("Temporary test files retained at: " + temporary);
        }
    }

    private static void testPaths(Path temporary) {
        Path home = temporary.resolve("home"), xdg = temporary.resolve("xdg");
        check(DataManager.applicationDataDirectory("Mac OS X", home, xdg.toString())
                .equals(home.resolve("Library/Application Support/ScoreManager")), "macOS ignores XDG");
        check(DataManager.applicationDataDirectory("Windows 11", home, xdg.toString())
                .equals(home.resolve("AppData/LocalLow/ScoreManager")), "Windows path remains compatible");
        check(DataManager.applicationDataDirectory("Linux", home, null)
                .equals(home.resolve(".local/share/ScoreManager")), "Linux default path");
        check(DataManager.applicationDataDirectory("Linux", home, xdg.toString())
                .equals(xdg.resolve("ScoreManager")), "Linux accepts absolute XDG_DATA_HOME");
        check(DataManager.applicationDataDirectory("Linux", home, "relative/data")
                .equals(home.resolve(".local/share/ScoreManager")), "Linux ignores relative XDG_DATA_HOME");
        check(DataManager.applicationDataDirectory("Linux", home, "bad\u0000path")
                .equals(home.resolve(".local/share/ScoreManager")), "Linux ignores invalid XDG_DATA_HOME");
    }

    private static void testPasswordHasher() {
        String first = PasswordHasher.hash("  密码 long secret  ");
        String second = PasswordHasher.hash("  密码 long secret  ");
        check(!first.equals(second), "password hashes use fresh random salts");
        check(PasswordHasher.verify("  密码 long secret  ", first), "Unicode and surrounding spaces preserved");
        check(!PasswordHasher.verify("密码 long secret", first), "passwords are not trimmed");
        check(!PasswordHasher.verify("wrong", first), "wrong password rejected");
        check(!PasswordHasher.verify("secret", "v1$2147483647$invalid$invalid"), "unbounded work factors rejected");
        check(!PasswordHasher.verify("secret", "malformed"), "malformed hash rejected");
    }

    private static void testNewAccounts(Path root, List<DataManager> managers) throws Exception {
        Path database = root.resolve("fresh.db");
        DataManager data = open(database, managers);
        check(!data.hasUsers(), "empty database has no preinstalled accounts");
        check(!data.verifyLogin("lqh", "050128"), "old default admin is not created");
        check(!data.verifyLogin("teacher", "teacher123"), "old default teacher is not created");
        expect(IllegalArgumentException.class, () -> data.createFirstAdmin("owner", "short"), "new password length validation");
        check(data.createFirstAdmin("owner", "owner secret"), "first administrator creation");
        check(data.isAdmin(), "first administrator has authenticated session");
        check(!data.createFirstAdmin("intruder", "intruder secret"), "first admin creation locked after setup");
        check(PasswordHasher.SCHEME.equals(query(database, "SELECT password_scheme FROM users WHERE username='owner'")), "new accounts stored as hashes");
        check(!"owner secret".equals(query(database, "SELECT password FROM users WHERE username='owner'")), "plaintext not saved");
        check(data.addUser("teacher", "teacher secret", "user"), "administrator can add teacher");
        check(!data.addUser("teacher", "teacher secret", "user"), "duplicate username rejected");
        check(data.getAllUsers().stream().allMatch(row -> row.length == 2), "account list returns no passwords or hashes");
        check(!data.deleteUser("owner"), "cannot delete active administrator");
        check(!data.updateUser("owner", "owner", "", "user"), "cannot demote active administrator");
        check(data.updateUser("owner", "renamed-owner", "", "admin"), "administrator rename without password reset");
        check("renamed-owner".equals(data.getCurrentUsername()) && data.isAdmin(), "session follows administrator rename");
        check(data.verifyLogin("renamed-owner", "owner secret"), "blank edit retains prior password");
        check(data.updateUser("teacher", "teacher", "changed secret", "user"), "administrator can reset teacher password");
        check(!data.verifyLogin("teacher", "teacher secret"), "reset invalidates old password");
        check(!data.isAdmin() && data.getCurrentUsername().isEmpty(), "failed login clears prior administrator session");
        check(data.verifyLogin("teacher", "changed secret"), "teacher logs in with reset password");
        expect(SecurityException.class, data::getAllUsers, "teacher cannot list accounts");
        expect(SecurityException.class, () -> data.addUser("injected", "injected secret", "admin"), "teacher cannot create administrator");
        expect(SecurityException.class, () -> data.updateUser("teacher", "teacher", "", "admin"), "teacher cannot elevate self");
        expect(SecurityException.class, () -> data.deleteUser("renamed-owner"), "teacher cannot delete administrator");
        check(data.verifyLogin("renamed-owner", "owner secret"), "administrator can log in again");
        check(data.addUser("second-admin", "second secret", "admin"), "second administrator creation");
        check(data.updateUser("second-admin", "second-admin", "", "user"), "administrator may demote another administrator");
        check(data.deleteUser("second-admin"), "administrator may remove another account");
        check(!data.deleteUser("absent"), "deletion reports missing account");
        check(!data.updateUser("absent", "absent", "", "user"), "update reports missing account");
        execute(database, "UPDATE users SET role='user' WHERE username='renamed-owner'");
        check(!data.isAdmin(), "role is checked in current database rather than cached");
        expect(SecurityException.class, data::getAllUsers, "stale administrator session cannot manage accounts");
    }

    private static void testLegacyMigration(Path root, List<DataManager> managers) throws Exception {
        Path database = root.resolve("legacy.db");
        execute(database, "CREATE TABLE users (username TEXT PRIMARY KEY, password TEXT, role TEXT)",
                "INSERT INTO users VALUES ('legacy', 'short', 'admin')",
                "INSERT INTO users VALUES ('old-teacher', 'v1$looks-like-a-hash', 'user')",
                "CREATE TABLE students (id TEXT PRIMARY KEY, name TEXT, score REAL, class_name TEXT)",
                "INSERT INTO students VALUES ('test-1', '测试学生', 42.5, '测试班')");
        DataManager data = open(database, managers);
        check(data.hasUsers(), "legacy accounts retained at startup");
        check("plain".equals(query(database, "SELECT password_scheme FROM users WHERE username='legacy'")), "old schema migration marks legacy credentials");
        check(!data.verifyLogin("legacy", "wrong"), "wrong legacy password rejected");
        check("short".equals(query(database, "SELECT password FROM users WHERE username='legacy'")), "failed login does not rewrite legacy credential");
        check(data.verifyLogin("legacy", "short"), "short legacy password remains usable");
        String encoded = query(database, "SELECT password FROM users WHERE username='legacy'");
        check(PasswordHasher.verify("short", encoded), "successful legacy login upgrades password hash");
        check(PasswordHasher.SCHEME.equals(query(database, "SELECT password_scheme FROM users WHERE username='legacy'")), "legacy scheme upgraded");
        check(data.verifyLogin("legacy", "short"), "upgraded legacy account continues to log in");
        check(data.getClassesData().get("测试班").get("test-1").score == 42.5, "student scores survive account migration");
        check(data.verifyLogin("old-teacher", "v1$looks-like-a-hash"), "plaintext resembling a hash is migrated correctly");
        check(!data.isAdmin(), "legacy teacher role retained");
        data.logout();
        check(data.getCurrentUsername().isEmpty(), "logout clears username");
        execute(database, "UPDATE users SET password_scheme='unsupported-future-version' WHERE username='legacy'");
        check(!data.verifyLogin("legacy", "short"), "unknown password scheme fails closed");
    }

    private static void testDatabaseChanges(Path root, List<DataManager> managers) throws Exception {
        Path first = root.resolve("switch-first.db"), second = root.resolve("switch-second.db");
        DataManager data = open(first, managers);
        DataManager other = open(second, managers);
        check(data.createFirstAdmin("same-name", "first secret"), "source administrator created");
        check(other.createFirstAdmin("same-name", "second secret"), "target administrator created");
        check(data.changeDataFile(second.toFile()), "database switch succeeds");
        check(data.getCurrentUsername().isEmpty() && !data.isAdmin(), "database switch clears identity even when username matches");
        expect(SecurityException.class, data::getAllUsers, "old database session grants no access to target accounts");
        check(!data.verifyLogin("same-name", "first secret"), "target rejects source password");
        check(data.verifyLogin("same-name", "second secret"), "target requires its own password");
        check(data.changeDataFile(first.toFile()), "source database remains usable");
        check(data.verifyLogin("same-name", "first secret"), "source credentials preserved");
        Path destination = Files.createDirectory(root.resolve("moved"));
        check(data.moveDataFile(destination.toFile()), "move current database");
        check(data.isAdmin(), "moving same database preserves identity");
        check(data.renameDataFile(destination.resolve("renamed.db").toFile()), "rename current database");
        check(data.isAdmin() && data.verifyLogin("same-name", "first secret"), "renaming preserves account data and identity");
    }

    private static DataManager open(Path database, List<DataManager> managers) {
        DataManager data = new DataManager(database.toString());
        managers.add(data);
        return data;
    }

    private static String query(Path database, String sql) throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + database);
             Statement statement = conn.createStatement(); ResultSet rows = statement.executeQuery(sql)) {
            return rows.next() ? rows.getString(1) : null;
        }
    }

    private static void execute(Path database, String... sql) throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + database);
             Statement statement = conn.createStatement()) {
            for (String command : sql) statement.execute(command);
        }
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void expect(Class<? extends Throwable> expected, Runnable operation, String message) {
        checks++;
        try { operation.run(); }
        catch (Throwable failure) {
            if (expected.isInstance(failure)) return;
            throw new AssertionError(message + ": unexpected exception", failure);
        }
        throw new AssertionError(message + ": expected " + expected.getSimpleName());
    }
}
