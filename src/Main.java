import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        try {
            FlatLightLaf.setup();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        UITools.setupGlobalAppleStyle();

        SwingUtilities.invokeLater(() -> {
            // 🚀 核心修改：改变启动顺序，先展示动画加载数据，再弹出登录！
            startBootSequence();
        });
    }

    private static void startBootSequence() {
        AppSplashScreen splash = new AppSplashScreen();
        splash.setVisible(true);

        new SwingWorker<DataManager, Void>() {
            @Override
            protected DataManager doInBackground() throws Exception {
                return new DataManager("ScoreData.db", (percent, message) -> {
                    SwingUtilities.invokeLater(() -> {
                        splash.updateProgress(percent, message);
                    });
                });
            }

            @Override
            protected void done() {
                try {
                    DataManager dataManager = get();
                    splash.dispose(); // 数据加载完，关掉动画

                    // 🚀 将加载好的 dataManager 传给登录界面，让它能查数据库
                    LoginFrame loginFrame = new LoginFrame(dataManager, () -> {
                        // 登录成功后，瞬间打开主界面
                        new ScoreManagerGUI(dataManager);
                    });
                    loginFrame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null, "系统启动致命错误: " + e.getMessage());
                    System.exit(1);
                }
            }
        }.execute();
    }
}