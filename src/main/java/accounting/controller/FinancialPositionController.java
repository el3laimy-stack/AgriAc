package accounting.controller;

import accounting.model.BalanceSheet;
import accounting.util.ErrorHandler;
import accounting.service.FinancialSummaryService;
import accounting.formatter.FormatUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class FinancialPositionController implements Initializable {

    // --- FXML Fields from the actual FXML file ---
    @FXML private Label totalAssetsLabel;
    @FXML private Label totalLiabilitiesAndEquityLabel;
    
    @FXML private Label totalCashLabel;
    @FXML private TableView<Map.Entry<String, Double>> cashAccountsTable;
    @FXML private TableColumn<Map.Entry<String, Double>, String> cashAccountNameColumn;
    @FXML private TableColumn<Map.Entry<String, Double>, Double> cashAccountBalanceColumn;

    @FXML private Label totalInventoryLabel;
    @FXML private PieChart inventoryPieChart;

    @FXML private Label totalReceivablesLabel;
    @FXML private ListView<String> topDebtorsListView;

    @FXML private Label totalPayablesLabel;
    @FXML private ListView<String> topCreditorsListView;

    @FXML private Label openingCapitalLabel;
    @FXML private Label netProfitLabel;
    
    @FXML private ProgressIndicator progressIndicator;
    @FXML private GridPane financialPositionGrid;


    private FinancialSummaryService summaryService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.summaryService = new FinancialSummaryService();
        setupTableColumns();
        loadFinancialPosition();
    }

    private void setupTableColumns() {
        // Setup for cash accounts table
        cashAccountNameColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getKey()));
        cashAccountBalanceColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getValue()));
    }


    private void loadFinancialPosition() {
        Task<BalanceSheet> loadTask = new Task<>() {
            @Override
            protected BalanceSheet call() throws Exception {
                return summaryService.getBalanceSheet(LocalDate.now());
            }
        };

        loadTask.setOnSucceeded(event -> {
            BalanceSheet bs = loadTask.getValue();
            populateFinancialPosition(bs);
            progressIndicator.visibleProperty().unbind();
            progressIndicator.setVisible(false);
            financialPositionGrid.disableProperty().unbind();
            financialPositionGrid.setDisable(false);
        });

        loadTask.setOnFailed(event -> {
            progressIndicator.visibleProperty().unbind();
            progressIndicator.setVisible(false);
            financialPositionGrid.disableProperty().unbind();
            financialPositionGrid.setDisable(false);
            ErrorHandler.showException("Database Error", "Failed to load financial position data from the database.", (Exception) loadTask.getException());
        });

        if (progressIndicator != null) {
            progressIndicator.visibleProperty().bind(loadTask.runningProperty());
        }
        if (financialPositionGrid != null) {
            financialPositionGrid.disableProperty().bind(loadTask.runningProperty());
        }

        new Thread(loadTask).start();
    }
    
    private void populateFinancialPosition(BalanceSheet bs) {
        try {
            // --- Populate Assets Section ---
            totalAssetsLabel.setText(FormatUtils.formatCurrency(bs.getTotalAssets()));

            // Cash
            double totalCash = bs.getAssets().entrySet().stream()
                .filter(e -> e.getKey().contains("نقدي") || e.getKey().contains("بنكي"))
                .mapToDouble(Map.Entry::getValue).sum();
            totalCashLabel.setText(FormatUtils.formatCurrency(totalCash));

            ObservableList<Map.Entry<String, Double>> cashItems = FXCollections.observableArrayList(
                bs.getAssets().entrySet().stream()
                    .filter(e -> e.getKey().contains("نقدي") || e.getKey().contains("بنكي"))
                    .collect(Collectors.toList())
            );
            cashAccountsTable.setItems(cashItems);


            // Inventory
            totalInventoryLabel.setText(FormatUtils.formatCurrency(bs.getAssets().getOrDefault("المخزون", 0.0)));
            inventoryPieChart.setData(bs.getInventoryBreakdown());

            // Receivables
            totalReceivablesLabel.setText(FormatUtils.formatCurrency(bs.getAssets().getOrDefault("ذمم مدينة", 0.0)));
            topDebtorsListView.setItems(bs.getTopDebtors());


            // --- Populate Liabilities and Equity Section ---
            totalLiabilitiesAndEquityLabel.setText(FormatUtils.formatCurrency(bs.getTotalLiabilitiesAndEquity()));

            // Payables
            totalPayablesLabel.setText(FormatUtils.formatCurrency(bs.getLiabilities().getOrDefault("ذمم دائنة", 0.0)));
            topCreditorsListView.setItems(bs.getTopCreditors());

            // Equity
            openingCapitalLabel.setText(FormatUtils.formatCurrency(bs.getEquity().getOrDefault("رأس المال", 0.0)));
            netProfitLabel.setText(FormatUtils.formatCurrency(bs.getEquity().getOrDefault("صافي الربح", 0.0)));


        } catch (Exception e) {
            e.printStackTrace();
            // You should show an error alert to the user here
        }
    }
}
