import org.knowm.xchart.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

// ✨ 独立出来的数据大屏组件 (纯净明亮版 + 旗舰级触控&滚轮引擎 + 绝对标尺防防抖)
public class DashboardPanel extends JPanel {
    private final int MAX_VISIBLE_BARS = 12;
    private DataManager data;
    private JPanel barContainer;
    private JPanel pieContainer;
    private JLabel dashboardPieTitle;
    private JLabel barTitle;
    private JScrollBar chartScrollBar;
    private CategoryChart barChart;
    private PieChart pieChart;
    private JPanel barChartPanel;
    private JPanel pieChartPanel;
    private List<String> allChartClasses = new ArrayList<>();
    private List<Double> allChartScores = new ArrayList<>();
    // ✨ 将惯性引擎提升为全局变量，方便滚轮和点击随时刹车
    private Timer inertiaTimer;

    public DashboardPanel(DataManager dataManager) {
        this.data = dataManager;
        setLayout(new GridLayout(1, 2, 20, 20));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setOpaque(false);

        // 1. 初始化容器
        barContainer = new JPanel(new BorderLayout(0, 10));
        barContainer.setOpaque(false);
        barTitle = new JLabel("各班平均分对比", SwingConstants.CENTER);
        barTitle.setFont(new Font(Font.DIALOG, Font.BOLD, 18));
        barTitle.setForeground(new Color(60, 70, 80));

        chartScrollBar = new JScrollBar(JScrollBar.HORIZONTAL);
        chartScrollBar.setUnitIncrement(1);
        chartScrollBar.setPreferredSize(new Dimension(0, 18));
        chartScrollBar.putClientProperty("JScrollBar.showButtons", true);
        chartScrollBar.addAdjustmentListener(e -> refreshChartView());

        pieContainer = new JPanel(new BorderLayout(0, 10));
        pieContainer.setOpaque(false);
        dashboardPieTitle = new JLabel("全局 - 分数段分布", SwingConstants.CENTER);
        dashboardPieTitle.setFont(new Font(Font.DIALOG, Font.BOLD, 18));
        dashboardPieTitle.setForeground(new Color(60, 70, 80));

        add(barContainer);
        add(pieContainer);
    }

    private void refreshChartView() {
        if (allChartClasses.isEmpty() || barChart == null || barChartPanel == null) return;
        int start = chartScrollBar.getValue();
        if (start >= allChartClasses.size()) start = 0;
        int end = Math.min(start + MAX_VISIBLE_BARS, allChartClasses.size());

        barChart.updateCategorySeries("平均分", allChartClasses.subList(start, end), allChartScores.subList(start, end), null);
        barChartPanel.repaint();
    }

    public void updateData(String filterMajor, String filterClass) {
        allChartClasses.clear();
        allChartScores.clear();
        int excellent = 0, good = 0, pass = 0, fail = 0;

        for (Map.Entry<String, List<String>> majorEntry : data.getMajorToClasses().entrySet()) {
            if (filterMajor != null && !filterMajor.equals(majorEntry.getKey())) continue;
            for (String className : majorEntry.getValue()) {
                if (filterClass != null && !filterClass.equals(className)) continue;

                Map<String, Student> classMap = data.getClassesData().get(className);
                if (classMap != null && !classMap.isEmpty()) {
                    allChartClasses.add(className);
                    double totalScore = 0;
                    for (Student s : classMap.values()) {
                        totalScore += s.score;
                        if (s.score >= 90) excellent++;
                        else if (s.score >= 80) good++;
                        else if (s.score >= 60) pass++;
                        else fail++;
                    }
                    allChartScores.add(Math.round((totalScore / classMap.size()) * 10.0) / 10.0);
                }
            }
        }

        Color fontColor = new Color(80, 90, 100);
        Color transparent = new Color(0, 0, 0, 0);
        Color borderColor = new Color(150, 150, 150, 50);

        // ================= 3. 重建柱状图 =================
        barChart = new CategoryChartBuilder().width(400).height(300).xAxisTitle("班级").yAxisTitle("平均分").build();
        barChart.getStyler().setLegendVisible(true);
        barChart.getStyler().setToolTipsEnabled(true);
        barChart.getStyler().setXAxisTicksVisible(true);
        barChart.getStyler().setXAxisLabelRotation(45);
        barChart.getStyler().setSeriesColors(new Color[]{new Color(135, 206, 250)});

        barChart.getStyler().setChartFontColor(fontColor);
        barChart.getStyler().setPlotGridLinesColor(new Color(0, 0, 0, 15));
        barChart.getStyler().setChartBackgroundColor(transparent);
        barChart.getStyler().setPlotBackgroundColor(transparent);
        barChart.getStyler().setLegendBackgroundColor(new Color(255, 255, 255, 200));
        barChart.getStyler().setLegendBorderColor(transparent);

        // 🚀🚀🚀 核心修复：死死锁定 Y 轴坐标系底线和上限，彻底拒绝“视觉欺诈”！
        barChart.getStyler().setYAxisMin(0.0);
        barChart.getStyler().setYAxisMax(100.0);

        if (allChartClasses.isEmpty()) {
            barChart.addSeries("平均分", Arrays.asList("无数据"), Arrays.asList(0.0));
            chartScrollBar.setVisible(false);
        } else {
            int start = chartScrollBar.getValue();
            if (start >= allChartClasses.size()) {
                start = 0;
                chartScrollBar.setValue(0);
            }
            int end = Math.min(start + MAX_VISIBLE_BARS, allChartClasses.size());

            barChart.addSeries("平均分", allChartClasses.subList(start, end), allChartScores.subList(start, end));
            chartScrollBar.setVisible(true);
            chartScrollBar.setValues(start, MAX_VISIBLE_BARS, 0, allChartClasses.size());
        }

        barContainer.removeAll();
        barChartPanel = new XChartPanel<>(barChart);
        barChartPanel.setOpaque(false);
        barChartPanel.setBorder(BorderFactory.createLineBorder(borderColor, 1, true));

        // 带有物理阻尼的旗舰级触屏滑动！
        MouseAdapter chartTouchAdapter = new MouseAdapter() {
            private int startX, startScroll;
            private double velocity = 0;
            private long lastTime;
            private int lastX;
            private double exactScroll;

            @Override
            public void mousePressed(MouseEvent e) {
                if (inertiaTimer != null && inertiaTimer.isRunning()) inertiaTimer.stop();

                startX = e.getX();
                lastX = e.getX();
                startScroll = chartScrollBar.getValue();
                exactScroll = startScroll;
                lastTime = System.currentTimeMillis();
                barChartPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                int currentX = e.getX();
                long currentTime = System.currentTimeMillis();

                int barWidth = Math.max(10, barChartPanel.getWidth() / MAX_VISIBLE_BARS);
                int deltaX = currentX - startX;
                exactScroll = startScroll - ((double) deltaX / barWidth);
                chartScrollBar.setValue((int) Math.round(exactScroll));

                long timeDelta = currentTime - lastTime;
                if (timeDelta > 0) {
                    velocity = (double) (currentX - lastX) / timeDelta;
                }

                lastX = currentX;
                lastTime = currentTime;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                barChartPanel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));

                if (Math.abs(velocity) > 0.2) {
                    inertiaTimer = new Timer(16, evt -> {
                        exactScroll -= velocity * 0.6;
                        chartScrollBar.setValue((int) Math.round(exactScroll));
                        velocity *= 0.90;
                        if (Math.abs(velocity) < 0.05) ((Timer) evt.getSource()).stop();
                    });
                    inertiaTimer.start();
                }
            }
        };

        barChartPanel.addMouseListener(chartTouchAdapter);
        barChartPanel.addMouseMotionListener(chartTouchAdapter);

        // 支持鼠标无缝滚轮滑动！
        barChartPanel.addMouseWheelListener(e -> {
            if (inertiaTimer != null && inertiaTimer.isRunning()) {
                inertiaTimer.stop();
            }

            int direction = e.getWheelRotation();
            int newValue = chartScrollBar.getValue() + (direction * 2);
            chartScrollBar.setValue(newValue);
        });

        barContainer.add(barTitle, BorderLayout.NORTH);
        barContainer.add(barChartPanel, BorderLayout.CENTER);
        barContainer.add(chartScrollBar, BorderLayout.SOUTH);

        // ================= 4. 重建饼图 =================
        pieChart = new PieChartBuilder().width(400).height(300).build();
        pieChart.getStyler().setLegendVisible(true);
        Color[] sliceColors = new Color[]{new Color(60, 179, 113), new Color(135, 206, 250), new Color(255, 215, 0), new Color(255, 110, 130)};
        pieChart.getStyler().setSeriesColors(sliceColors);

        pieChart.getStyler().setChartFontColor(fontColor);
        pieChart.getStyler().setChartBackgroundColor(transparent);
        pieChart.getStyler().setPlotBackgroundColor(transparent);
        pieChart.getStyler().setLegendBackgroundColor(new Color(255, 255, 255, 200));
        pieChart.getStyler().setLegendBorderColor(transparent);

        int total = excellent + good + pass + fail;
        if (total == 0) {
            pieChart.addSeries("优秀(≥90)", 1);
            pieChart.addSeries("良好(80-89)", 1);
            pieChart.addSeries("及格(60-79)", 1);
            pieChart.addSeries("不及格(<60)", 1);
        } else {
            pieChart.addSeries("优秀(≥90)", excellent);
            pieChart.addSeries("良好(80-89)", good);
            pieChart.addSeries("及格(60-79)", pass);
            pieChart.addSeries("不及格(<60)", fail);
        }

        pieContainer.removeAll();
        pieChartPanel = new XChartPanel<>(pieChart);
        pieChartPanel.setOpaque(false);
        pieChartPanel.setBorder(BorderFactory.createLineBorder(borderColor, 1, true));

        String viewTitle = (filterClass != null) ? filterClass : ((filterMajor != null) ? filterMajor : "全局");
        dashboardPieTitle.setText(viewTitle + " - 分数段分布");

        pieContainer.add(dashboardPieTitle, BorderLayout.NORTH);
        pieContainer.add(pieChartPanel, BorderLayout.CENTER);

        barContainer.revalidate();
        barContainer.repaint();
        pieContainer.revalidate();
        pieContainer.repaint();
    }
}