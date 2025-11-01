package accounting.controller;

import accounting.model.IncomeStatement;
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
import javafx.scene.chart.PieChart;

import java.time.LocalDate;
import java.util.stream.Collectors;

public class IncomeStatementController {

    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private Button viewButton;
    @FXML private ProgressIndicator progressIndicator;

    // Revenue Table
    @FXML private TableView<IncomeStatement.IncomeItem> revenueTable;
    @FXML private TableColumn<IncomeStatement.IncomeItem, String> revenueNameColumn;
    @FXML private TableColumn<IncomeStatement.IncomeItem, Double> revenueValueColumn;
    @FXML private Label totalRevenueLabel;

    // Expense Table
    @FXML private TableView<IncomeStatement.IncomeItem> expenseTable;
    @FXML private TableColumn<IncomeStatement.IncomeItem, String> expenseNameColumn;
    @FXML private TableColumn<IncomeStatement.IncomeItem, Double> expenseValueColumn;
    @FXML private Label totalExpenseLabel;
    @FXML private PieChart expensePieChart;

    @FXML private Label netIncomeLabel;

    private final FinancialSummaryService summaryService = new FinancialSummaryService();

    @FXML
    public void initialize() {
        fromDatePicker.setValue(LocalDate.now().withDayOfYear(1));
        toDatePicker.setValue(LocalDate.now());
        setupTableColumns();
        loadData();
    }
    
    @FXML
    private void handleViewButton() {
        loadData();
    }

    private void setupTableColumns() {
        // Revenue
        revenueNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        revenueValueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        revenueValueColumn.setCellFactory(col -> FormatUtils.createCurrencyCell());

        // Expense
        expenseNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        expenseValueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        expenseValueColumn.setCellFactory(col -> FormatUtils.createCurrencyCell());
    }

    private void loadData() {
        LocalDate from = fromDatePicker.getValue();
        LocalDate to = toDatePicker.getValue();
        if (from == null || to == null) return;

        Task<IncomeStatement> loadDataTask = new Task<>() {
            @Override
            protected IncomeStatement call() throws Exception {
                return summaryService.getIncomeStatement(from, to);
            }
        };

        loadDataTask.setOnSucceeded(e -> {
            populateReport(loadDataTask.getValue());
        });

        loadDataTask.setOnFailed(e -> {
            Exception exception = (Exception) loadDataTask.getException();
            ErrorHandler.showException("خطأ في تحميل البيانات", "لا يمكن تحميل بيانات قائمة الدخل.", exception);
        });

        progressIndicator.visibleProperty().bind(loadDataTask.runningProperty());
        new Thread(loadDataTask).start();
    }

    private void populateReport(IncomeStatement statement) {
        // Convert maps to ObservableList of IncomeItem
        ObservableList<IncomeStatement.IncomeItem> revenues = statement.getRevenueDetails().entrySet().stream()
                .map(entry -> new IncomeStatement.IncomeItem(entry.getKey(), entry.getValue()))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
        
        ObservableList<IncomeStatement.IncomeItem> expenses = statement.getExpenseDetails().entrySet().stream()
                .map(entry -> new IncomeStatement.IncomeItem(entry.getKey(), entry.getValue()))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));

        // Set items to tables
        revenueTable.setItems(revenues);
        expenseTable.setItems(expenses);

        // Populate Pie Chart for expenses
        ObservableList<PieChart.Data> pieChartData = statement.getExpenseDetails().entrySet().stream()
            .map(entry -> new PieChart.Data(entry.getKey(), entry.getValue()))
            .collect(Collectors.toCollection(FXCollections::observableArrayList));
        expensePieChart.setData(pieChartData);

        // Set totals
        totalRevenueLabel.setText(FormatUtils.formatCurrency(statement.getTotalRevenue()));
        totalExpenseLabel.setText(FormatUtils.formatCurrency(statement.getTotalExpenses()));
        
        double netIncome = statement.getNetIncome();
        netIncomeLabel.setText(FormatUtils.formatCurrency(netIncome));
        
        // Clear previous styles and apply new ones based on profit/loss
        netIncomeLabel.getStyleClass().removeAll("text-success", "text-danger");
        if (netIncome >= 0) {
            netIncomeLabel.getStyleClass().add("text-success");
        } else {
            netIncomeLabel.getStyleClass().add("text-danger");
        }
    }
}