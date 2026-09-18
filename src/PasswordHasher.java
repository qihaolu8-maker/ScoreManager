import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/** Password storage format is versioned independently of the database schema. */
final class PasswordHasher {
    static final String SCHEME = "pbkdf2-sha256";
    private static final int ITERATIONS = 210_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {}

    static String hash(String password) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] hash = derive(password, salt, ITERATIONS);
        return "v1$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(hash);
    }

    static boolean verify(String password, String stored) {
        if (password == null || stored == null) return false;
        try {
            String[] parts = stored.split("\\$", -1);
            if (parts.length != 4 || !"v1".equals(parts[0])) return false;
            int iterations = Integer.parseInt(parts[1]);
            if (iterations < 100_000 || iterations > 2_000_000) return false;
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            if (salt.length != 16 || expected.length != 32) return false;
            byte[] actual = derive(password, salt, iterations);
            try { return MessageDigest.isEqual(expected, actual); }
            finally { Arrays.fill(actual, (byte) 0); }
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    static boolean verifyLegacy(String password, String stored) {
        return password != null && stored != null && MessageDigest.isEqual(
                password.getBytes(StandardCharsets.UTF_8), stored.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        char[] characters = password.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(characters, salt, iterations, 256);
        Arrays.fill(characters, '\0');
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("当前 Java 环境不支持安全密码存储", e);
        } finally {
            spec.clearPassword();
        }
    }
}
