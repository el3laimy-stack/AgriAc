package accounting.controller;

import accounting.service.DashboardService;
import accounting.util.ErrorHandler;
import accounting.formatter.FormatUtils;
import javafx.animation.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.util.List;

public class DashboardController {

    @FXML private StackPane rootPane;
    @FXML private VBox animatedBackground;
    @FXML private GridPane kpiContainer;
    @FXML private BarChart<String, Number> cashFlowChart;
    @FXML private StackPane doughnutChartContainer;
    @FXML private ListView<String> recentActivityListView;

    private DashboardService dashboardService;

    @FXML
    public void initialize() {
        dashboardService = new DashboardService();
        setupRecentActivityListView();
        startAnimations();
        loadDashboardData();
    }

    private void startAnimations() {
        // Animate Background
        animateBackground();

        // Staggered fade-in for components
        SequentialTransition seqTransition = new SequentialTransition();
        seqTransition.getChildren().addAll(
            createFadeIn(kpiContainer, 200),
            createFadeIn(cashFlowChart.getParent(), 300),
            createFadeIn(doughnutChartContainer.getParent(), 300),
            createFadeIn(recentActivityListView.getParent(), 300)
        );
        seqTransition.play();
    }

    private void animateBackground() {
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(animatedBackground.backgroundProperty(), createGradient(Color.web("#2c3e50"), Color.web("#3498db")))),
            new KeyFrame(Duration.seconds(10), new KeyValue(animatedBackground.backgroundProperty(), createGradient(Color.web("#34495e"), Color.web("#2980b9")))),
            new KeyFrame(Duration.seconds(20), new KeyValue(animatedBackground.backgroundProperty(), createGradient(Color.web("#2c3e50"), Color.web("#3498db"))))
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    private javafx.scene.layout.Background createGradient(Color start, Color end) {
        return new javafx.scene.layout.Background(new javafx.scene.layout.BackgroundFill(
            new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, new Stop(0, start), new Stop(1, end)),
            null, null));
    }

    private FadeTransition createFadeIn(Node node, int delay) {
        FadeTransition ft = new FadeTransition(Duration.millis(500), node);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.setDelay(Duration.millis(delay));
        return ft;
    }

    private void loadDashboardData() {
        loadKpiData();
        loadCashFlowChart();
        loadExpenseDoughnutChart();
        loadRecentActivity();
    }

    private void loadKpiData() {
        try {
            DashboardService.DashboardSummary summary = dashboardService.getDashboardSummary();
            kpiContainer.getChildren().clear();
            kpiContainer.add(createKpiCard("السيولة النقدية", summary.getCashBalance(), "fa-money", dashboardService.getCashTrend(7)), 0, 0);
            kpiContainer.add(createKpiCard("إجمالي المستحقات", summary.getReceivables(), "fa-arrow-up", dashboardService.getReceivablesTrend(7)), 1, 0);
            kpiContainer.add(createKpiCard("إجمالي الالتزامات", summary.getPayables(), "fa-arrow-down", dashboardService.getPayablesTrend(7)), 2, 0);
            kpiContainer.add(createKpiCard("صافي الربح (30 يوم)", summary.getNetProfit(), "fa-pie-chart", dashboardService.getNetProfitTrend(7)), 3, 0);
        } catch (Exception e) {
            ErrorHandler.showException("Dashboard Error", "Failed to load KPI data.", e);
        }
    }

    private VBox createKpiCard(String title, double value, String iconLiteral, List<XYChart.Data<String, Number>> trendData) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/kpi_card.fxml"));
            VBox card = loader.load();
            ((Label) card.lookup("#kpiTitleLabel")).setText(title);
            ((Label) card.lookup("#kpiValueLabel")).setText(FormatUtils.formatCurrency(value));
            ((FontIcon) card.lookup("#kpiIcon")).setIconLiteral(iconLiteral);

            AreaChart<String, Number> sparkline = (AreaChart<String, Number>) card.lookup("#sparklineChart");
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setData(FXCollections.observableArrayList(trendData));
            sparkline.getData().add(series);

            return card;
        } catch (IOException e) {
            e.printStackTrace();
            return new VBox();
        }
    }

    private void loadCashFlowChart() {
        try {
            cashFlowChart.setData(FXCollections.observableArrayList(dashboardService.getCashFlowSeries()));
        } catch (Exception e) {
            ErrorHandler.showException("Dashboard Error", "Failed to load cash flow data.", e);
        }
    }

    private void loadExpenseDoughnutChart() {
        try {
            List<PieChart.Data> pieData = dashboardService.getExpenseBreakdown();
            double totalExpenses = pieData.stream().mapToDouble(PieChart.Data::getPieValue).sum();

            PieChart pieChart = new PieChart(FXCollections.observableArrayList(pieData));
            pieChart.setLabelsVisible(false);
            pieChart.setLegendVisible(true);
            pieChart.setStartAngle(90);

            // Create the hole for the doughnut chart
            final Circle hole = new Circle(50, Color.TRANSPARENT); // Transparent for now, will be styled by CSS if needed
            hole.setStyle("-fx-fill: -fx-base;"); // Use the base background color for the hole

            VBox centerLabel = new VBox();
            centerLabel.getStyleClass().add("doughnut-chart-label-container");
            Label titleLabel = new Label("إجمالي المصروفات");
            titleLabel.getStyleClass().add("doughnut-chart-title");
            Label valueLabel = new Label(FormatUtils.formatCurrency(totalExpenses));
            valueLabel.getStyleClass().add("doughnut-chart-value");
            centerLabel.getChildren().addAll(titleLabel, valueLabel);

            doughnutChartContainer.getChildren().addAll(pieChart, hole, centerLabel);

        } catch (Exception e) {
            ErrorHandler.showException("Dashboard Error", "Failed to load expense breakdown data.", e);
        }
    }

    private void loadRecentActivity() {
        try {
            recentActivityListView.setItems(FXCollections.observableArrayList(dashboardService.getRecentTransactions(10)));
        } catch (Exception e) {
            ErrorHandler.showException("Dashboard Error", "Failed to load recent activity.", e);
        }
    }

    private void setupRecentActivityListView() {
        recentActivityListView.setCellFactory(param -> new ListCell<>() {
            private final HBox hbox = new HBox(10);
            private final FontIcon icon = new FontIcon();
            private final Label label = new Label();

            {
                hbox.getChildren().addAll(icon, label);
                hbox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    label.setText(item);
                    if (item.startsWith("بيع")) {
                        icon.setIconLiteral("fa-tag");
                        icon.getStyleClass().setAll("activity-icon", "sale");
                    } else if (item.startsWith("شراء")) {
                        icon.setIconLiteral("fa-shopping-cart");
                        icon.getStyleClass().setAll("activity-icon", "purchase");
                    } else {
                        icon.setIconLiteral("fa-file-text-o");
                        icon.getStyleClass().setAll("activity-icon", "other");
                    }
                    setGraphic(hbox);
                }
            }
        });
    }
}