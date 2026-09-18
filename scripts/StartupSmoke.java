import java.awt.Frame;
import java.awt.Window;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;

/** CI-only probe: verifies a visible login/setup window in an isolated user home. */
public final class StartupSmoke {
    public static void main(String[] args) throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            error.printStackTrace();
            failure.set(error);
        });
        Main.main(new String[0]);
        long deadline = System.nanoTime() + 40_000_000_000L;
        while (System.nanoTime() < deadline) {
            AtomicReference<String> title = new AtomicReference<>();
            SwingUtilities.invokeAndWait(() -> {
                for (Frame frame : Frame.getFrames()) {
                    if (frame instanceof LoginFrame && frame.isShowing()) {
                        title.set(frame.getTitle());
                    }
                }
            });
            if (failure.get() != null) throw new AssertionError("Startup failed", failure.get());
            if (title.get() != null) {
                System.out.println("Swing startup passed: visible LoginFrame, title=" + title.get());
                SwingUtilities.invokeAndWait(() -> {
                    for (Window window : Window.getWindows()) window.dispose();
                });
                System.exit(0);
            }
            Thread.sleep(200);
        }
        System.err.println("No visible LoginFrame within 40 seconds");
        System.exit(1);
    }
}
