package accounting.controller;

import accounting.model.BalanceSheet;
import accounting.util.ErrorHandler;
import accounting.service.FinancialSummaryService;
import accounting.formatter.FormatUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.util.stream.Collectors;

public class BalanceSheetController {

    @FXML private DatePicker toDatePicker;
    @FXML private Button viewButton;
    @FXML private ProgressIndicator progressIndicator;

    // Assets Table
    @FXML private TableView<BalanceSheet.BalanceItem> assetsTable;
    @FXML private TableColumn<BalanceSheet.BalanceItem, String> assetNameColumn;
    @FXML private TableColumn<BalanceSheet.BalanceItem, Double> assetValueColumn;
    @FXML private Label totalAssetsLabel;

    // Liabilities Table
    @FXML private TableView<BalanceSheet.BalanceItem> liabilitiesTable;
    @FXML private TableColumn<BalanceSheet.BalanceItem, String> liabilityNameColumn;
    @FXML private TableColumn<BalanceSheet.BalanceItem, Double> liabilityValueColumn;
    @FXML private Label totalLiabilitiesLabel;

    // Equity Table
    @FXML private TableView<BalanceSheet.BalanceItem> equityTable;
    @FXML private TableColumn<BalanceSheet.BalanceItem, String> equityNameColumn;
    @FXML private TableColumn<BalanceSheet.BalanceItem, Double> equityValueColumn;
    @FXML private Label totalEquityLabel;
    
    @FXML private Label totalLiabilitiesAndEquityLabel;

    private final FinancialSummaryService summaryService = new FinancialSummaryService();

    @FXML
    public void initialize() {
        toDatePicker.setValue(LocalDate.now());
        setupTableColumns();
        loadData();
    }
    
    @FXML
    private void handleViewButton() {
        loadData();
    }

    private void setupTableColumns() {
        // Assets
        assetNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        assetValueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        assetValueColumn.setCellFactory(col -> FormatUtils.createCurrencyCell());

        // Liabilities
        liabilityNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        liabilityValueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        liabilityValueColumn.setCellFactory(col -> FormatUtils.createCurrencyCell());

        // Equity
        equityNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        equityValueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        equityValueColumn.setCellFactory(col -> FormatUtils.createCurrencyCell());
    }

    private void loadData() {
        LocalDate toDate = toDatePicker.getValue();
        if (toDate == null) return;

        Task<BalanceSheet> loadDataTask = new Task<>() {
            @Override
            protected BalanceSheet call() throws Exception {
                return summaryService.getBalanceSheet(toDate);
            }
        };

        loadDataTask.setOnSucceeded(e -> {
            populateReport(loadDataTask.getValue());
        });

        loadDataTask.setOnFailed(e -> {
            Exception exception = (Exception) loadDataTask.getException();
            ErrorHandler.showException("خطأ في تحميل البيانات", "لا يمكن تحميل بيانات الميزانية العمومية.", exception);
        });

        progressIndicator.visibleProperty().bind(loadDataTask.runningProperty());
        new Thread(loadDataTask).start();
    }

    private void populateReport(BalanceSheet statement) {
        // Convert maps to ObservableList of BalanceItem
        ObservableList<BalanceSheet.BalanceItem> assets = statement.getAssets().entrySet().stream()
                .map(entry -> new BalanceSheet.BalanceItem(entry.getKey(), entry.getValue()))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
        
        ObservableList<BalanceSheet.BalanceItem> liabilities = statement.getLiabilities().entrySet().stream()
                .map(entry -> new BalanceSheet.BalanceItem(entry.getKey(), entry.getValue()))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
        
        ObservableList<BalanceSheet.BalanceItem> equity = statement.getEquity().entrySet().stream()
                .map(entry -> new BalanceSheet.BalanceItem(entry.getKey(), entry.getValue()))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
        
        // Add retained earnings to equity list
        equity.add(new BalanceSheet.BalanceItem("الأرباح المحتجزة (أرباح الفترة)", statement.getRetainedEarnings()));

        // Set items to tables
        assetsTable.setItems(assets);
        liabilitiesTable.setItems(liabilities);
        equityTable.setItems(equity);

        // Set totals
        totalAssetsLabel.setText(FormatUtils.formatCurrency(statement.getTotalAssets()));
        totalLiabilitiesLabel.setText(FormatUtils.formatCurrency(statement.getTotalLiabilities()));
        
        double totalEquity = statement.getTotalEquity() + statement.getRetainedEarnings();
        totalEquityLabel.setText(FormatUtils.formatCurrency(totalEquity));
        
        totalLiabilitiesAndEquityLabel.setText(FormatUtils.formatCurrency(statement.getTotalLiabilitiesAndEquity()));
    }
}
