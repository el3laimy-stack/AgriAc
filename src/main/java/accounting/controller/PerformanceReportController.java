package accounting.controller;

import java.util.List;

import accounting.formatter.FormatUtils;
import accounting.util.SmartReportGenerator.*;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.Node;
import javafx.beans.property.SimpleStringProperty;

public class PerformanceReportController {

    @FXML private Label dateRangeLabel;
    @FXML private Label generatedDateLabel;
    @FXML private GridPane kpiGridPane;
    @FXML private GridPane profitabilityGridPane;
    @FXML private VBox recommendationsVBox;
    @FXML private TableView<CropPerformanceAnalysis> cropPerformanceTable;
    @FXML private TableColumn<CropPerformanceAnalysis, String> cropNameColumn;
    @FXML private TableColumn<CropPerformanceAnalysis, Double> revenueColumn;
    @FXML private TableColumn<CropPerformanceAnalysis, Double> costsColumn;
    @FXML private TableColumn<CropPerformanceAnalysis, Double> profitColumn;
    @FXML private TableColumn<CropPerformanceAnalysis, Double> profitMarginColumn;
    @FXML private TableColumn<CropPerformanceAnalysis, Double> roiColumn;
    @FXML private TableColumn<CropPerformanceAnalysis, String> ratingColumn;

    public void setReport(ComprehensivePerformanceReport report) {
        // تعبئة البيانات الأساسية
        dateRangeLabel.setText("للفترة من: " + FormatUtils.formatDateForDisplay(report.getFromDate()) +
                               " إلى: " + FormatUtils.formatDateForDisplay(report.getToDate()));
        generatedDateLabel.setText("تاريخ الإنشاء: " + FormatUtils.formatDateForDisplay(report.getGeneratedDate()));

        // تعبئة مؤشرات الأداء الرئيسية
        populateKpiGrid(report.getKpis());
        
        // تعبئة تحليل الربحية
        populateProfitabilityGrid(report.getProfitability());
        
        // تعبئة التوصيات
        populateRecommendations(report.getRecommendations());
        
        // تعبئة جدول تحليل المحاصيل
        setupAndPopulateCropTable(report.getCropAnalyses());
    }

    private void populateKpiGrid(KeyPerformanceIndicators kpis) {
        kpiGridPane.getChildren().clear();
        addStatToGrid(kpiGridPane, "إجمالي الإيرادات", FormatUtils.formatCurrency(kpis.getTotalRevenue()), 0, 0);
        addStatToGrid(kpiGridPane, "متوسط العائد على الاستثمار", String.format("%.2f%%", kpis.getAverageROI()), 1, 0);
        addStatToGrid(kpiGridPane, "معدل ربحية المحاصيل", String.format("%.2f%%", kpis.getProfitabilityRate()), 0, 1);
        addStatToGrid(kpiGridPane, "عدد المحاصيل الرابحة", kpis.getProfitableCrops() + " من " + kpis.getTotalCrops(), 1, 1);
    }
    
    private void populateProfitabilityGrid(OverallProfitabilityAnalysis profitability) {
        profitabilityGridPane.getChildren().clear();
        addStatToGrid(profitabilityGridPane, "إجمالي الربح", FormatUtils.formatCurrency(profitability.getTotalProfit()), 0, 0);
        addStatToGrid(profitabilityGridPane, "إجمالي التكاليف", FormatUtils.formatCurrency(profitability.getTotalCosts()), 1, 0);
        addStatToGrid(profitabilityGridPane, "هامش الربح الإجمالي", String.format("%.2f%%", profitability.getOverallProfitMargin()), 0, 1);
        addStatToGrid(profitabilityGridPane, "العائد على الاستثمار الإجمالي", String.format("%.2f%%", profitability.getOverallROI()), 1, 1);
    }

    private void populateRecommendations(List<StrategicRecommendation> recommendations) {
        recommendationsVBox.getChildren().clear();
        for (StrategicRecommendation rec : recommendations) {
            VBox recCard = new VBox(5);
            recCard.setPadding(new Insets(10));
            recCard.setStyle("-fx-border-color: #007bff; -fx-border-radius: 5; -fx-background-color: #f8f9fa;");

            Label title = new Label(rec.getTitle() + " (" + rec.getPriority().getArabicName() + ")");
            title.setStyle("-fx-font-weight: bold; -fx-text-fill: #007bff;");

            Label rationale = new Label("السبب: " + rec.getRationale());
            Label action = new Label("الإجراء المقترح: " + rec.getAction());
            
            recCard.getChildren().addAll(title, rationale, action);
            recommendationsVBox.getChildren().add(recCard);
        }
    }

    private void setupAndPopulateCropTable(List<CropPerformanceAnalysis> analyses) {
        // ---==[[ السطران التاليان تم تصحيحهما ]] ==---
        cropNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCrop().getCropName()));
        ratingColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getRating().getArabicName()));
        
        revenueColumn.setCellValueFactory(new PropertyValueFactory<>("totalRevenue"));
        costsColumn.setCellValueFactory(new PropertyValueFactory<>("totalCosts"));
        profitColumn.setCellValueFactory(new PropertyValueFactory<>("profit"));
        profitMarginColumn.setCellValueFactory(new PropertyValueFactory<>("profitMargin"));
        roiColumn.setCellValueFactory(new PropertyValueFactory<>("roi"));
        
        cropPerformanceTable.getItems().setAll(analyses);
    }

    private void addStatToGrid(GridPane grid, String title, String value, int col, int row) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: #ffffff; -fx-border-color: #dee2e6; -fx-border-radius: 5;");
        Label titleLabel = new Label(title);
        
        // ---==[[ تم تصحيح الأسطر التالية ]] ==---
        titleLabel.getStyleClass().add("stat-label"); 
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("stat-value");
        
        card.getChildren().addAll(titleLabel, valueLabel);
        grid.add(card, col, row);
    }
}