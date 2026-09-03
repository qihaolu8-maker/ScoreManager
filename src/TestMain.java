import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;

public class TestMain {
    public static void main(String[] args) {
        try {
            // ✨ 纯净加载：让 FlatLaf 接管全局主题
            FlatLightLaf.setup();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        UITools.setupGlobalAppleStyle();

        SwingUtilities.invokeLater(() -> {
            // 🚀 核心修改：直接跳过登录界面，调用数据加载和启动主程序！
            startDataLoading();
        });
    }

    private static void startDataLoading() {
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
                    splash.dispose();
                    new ScoreManagerGUI(dataManager);
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null, "系统启动致命错误: " + e.getMessage());
                    System.exit(1);
                }
            }
        }.execute();
    }
}