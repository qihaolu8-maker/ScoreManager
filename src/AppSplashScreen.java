import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.RoundRectangle2D;

public class AppSplashScreen extends JWindow {
    private int progress = 0;
    private String statusMessage = "☁️ 课堂积分系统正在启动...";

    public AppSplashScreen() {
        setSize(450, 260);
        setLocationRelativeTo(null);
        setBackground(new Color(0, 0, 0, 0));
        setAlwaysOnTop(true);

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                GradientPaint bgPaint = new GradientPaint(0, 0, new Color(255, 235, 245),
                        getWidth(), getHeight(), new Color(228, 241, 254));
                g2.setPaint(bgPaint);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 30, 30));

                g2.setColor(new Color(255, 255, 255, 120));
                g2.fill(new RoundRectangle2D.Double(2, 2, getWidth() - 4, getHeight() - 4, 28, 28));

                g2.setColor(new Color(110, 160, 220));
                g2.setFont(new Font(Font.DIALOG, Font.BOLD, 32));
                String title = "ScoreManager Pro";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(title, (getWidth() - fm.stringWidth(title)) / 2, 100);

                g2.setFont(new Font(Font.DIALOG, Font.PLAIN, 15));
                g2.setColor(new Color(150, 160, 180));
                g2.drawString(statusMessage, (getWidth() - g2.getFontMetrics().stringWidth(statusMessage)) / 2, 140);

                int cx = getWidth() / 2, cy = 200, r = 16;
                g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                g2.setColor(new Color(200, 210, 230, 150));
                g2.drawOval(cx - r, cy - r, r * 2, r * 2);

                g2.setColor(new Color(135, 206, 250));
                double startAngle = 90 - (progress * 3.6);
                g2.draw(new Arc2D.Double(cx - r, cy - r, r * 2, r * 2, startAngle, 100, Arc2D.OPEN));

                g2.dispose();
            }
        };
        panel.setOpaque(false);
        setContentPane(panel);
    }

    // ✨ 暴露给后台的方法，用于实时刷新画面
    public void updateProgress(int newProgress, String newMessage) {
        this.progress = newProgress;
        this.statusMessage = newMessage;
        this.repaint();
    }
}