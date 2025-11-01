package accounting.controller;

import accounting.model.Season;
import accounting.service.SeasonalReportService;
import accounting.service.SeasonalReportService.CropSeasonAnalysis;
import accounting.service.SeasonalReportService.SeasonReport;
import accounting.service.SeasonalReportService.SeasonStatistics;
import accounting.service.SeasonalReportService.SeasonInsight;

import accounting.util.ErrorHandler;
import accounting.formatter.FormatUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;

public class SeasonalReportsController {

    @FXML private ComboBox<Season> seasonComboBox;
    @FXML private Label seasonDatesLabel;
    @FXML private Label totalRevenueLabel;
    @FXML private Label totalCostLabel;
    @FXML private Label netProfitLabel;
    @FXML private BarChart<String, Number> cropProfitabilityChart;
    @FXML private TableView<CropSeasonAnalysis> cropAnalysisTable;
    @FXML private TableColumn<CropSeasonAnalysis, String> cropNameColumn;
    @FXML private TableColumn<CropSeasonAnalysis, Double> revenueColumn;
    @FXML private TableColumn<CropSeasonAnalysis, Double> costColumn;
    @FXML private TableColumn<CropSeasonAnalysis, Double> profitColumn;
    @FXML private TableColumn<CropSeasonAnalysis, Double> marginColumn;
    @FXML private TableColumn<CropSeasonAnalysis, Double> roiColumn;
    @FXML private TableColumn<CropSeasonAnalysis, String> performanceColumn;
    @FXML private VBox insightsVBox;

    private SeasonalReportService reportService;

    @FXML
    public void initialize() {
        this.reportService = new SeasonalReportService();
        setupTable();
        loadSeasons();
    }

    private void setupTable() {
        cropNameColumn.setCellValueFactory(new PropertyValueFactory<>("cropName"));
        revenueColumn.setCellValueFactory(new PropertyValueFactory<>("totalRevenue"));
        costColumn.setCellValueFactory(new PropertyValueFactory<>("totalCost"));
        profitColumn.setCellValueFactory(new PropertyValueFactory<>("netProfit"));
        marginColumn.setCellValueFactory(new PropertyValueFactory<>("profitMargin"));
        roiColumn.setCellValueFactory(new PropertyValueFactory<>("returnOnInvestment"));
        performanceColumn.setCellValueFactory(new PropertyValueFactory<>("performanceRating"));
    }

    private void loadSeasons() {
        try {
            seasonComboBox.setItems(FXCollections.observableArrayList(reportService.getAllSeasons()));
            seasonComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    generateReportForSeason(newVal.getId());
                }
            });
        } catch (SQLException e) {
            ErrorHandler.showException("خطأ", "فشل تحميل المواسم.", e);
        }
    }

    private void generateReportForSeason(int seasonId) {
        try {
            SeasonReport report = reportService.generateSeasonReport(seasonId);
            populateReport(report);
        } catch (SQLException e) {
            ErrorHandler.showException("خطأ", "فشل إنشاء تقرير الموسم.", e);
        }
    }

    private void populateReport(SeasonReport report) {
        SeasonStatistics stats = report.getStatistics();
        seasonDatesLabel.setText("من " + FormatUtils.formatDateForDisplay(stats.getStartDate()) + " إلى " + FormatUtils.formatDateForDisplay(stats.getEndDate()));
        totalRevenueLabel.setText(FormatUtils.formatCurrency(stats.getTotalRevenue()));
        totalCostLabel.setText(FormatUtils.formatCurrency(stats.getTotalCost()));
        netProfitLabel.setText(FormatUtils.formatCurrency(stats.getNetProfit()));

        cropAnalysisTable.setItems(FXCollections.observableArrayList(report.getCropAnalyses()));
        populateChart(report.getCropAnalyses());
        populateInsights(report.getInsights());
    }

    private void populateChart(List<CropSeasonAnalysis> analyses) {
        cropProfitabilityChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("ربحية المحاصيل");
        for (CropSeasonAnalysis analysis : analyses) {
            series.getData().add(new XYChart.Data<>(analysis.getCropName(), analysis.getNetProfit()));
        }
        cropProfitabilityChart.getData().add(series);
    }

    private void populateInsights(List<SeasonInsight> insights) {
        insightsVBox.getChildren().clear();
        for (SeasonInsight insight : insights) {
            Label insightLabel = new Label("• " + insight.getDescription());
            insightLabel.setWrapText(true);
            insightsVBox.getChildren().add(insightLabel);
        }
    }
}