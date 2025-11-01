package accounting.controller;

import accounting.model.LedgerEntry;
import accounting.util.ErrorHandler;
import accounting.service.ReportDataService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.time.LocalDate;

public class ExpenseReportController {

    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private Button filterButton;
    @FXML private TableView<LedgerEntry> expenseTable;
    @FXML private TableColumn<LedgerEntry, LocalDate> dateColumn;
    @FXML private TableColumn<LedgerEntry, String> descriptionColumn;
    @FXML private TableColumn<LedgerEntry, Double> amountColumn;
    @FXML private Label totalExpensesLabel;

    private ReportDataService reportDataService;

    @FXML
    private void initialize() {
        reportDataService = new ReportDataService();
        setupTable();
        fromDatePicker.setValue(LocalDate.now().withDayOfMonth(1));
        toDatePicker.setValue(LocalDate.now());
        filterButton.setOnAction(e -> loadReportData());
        loadReportData();
    }

    private void setupTable() {
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("debit")); // Expenses are debits
    }

    private void loadReportData() {
        LocalDate from = fromDatePicker.getValue();
        LocalDate to = toDatePicker.getValue();

        if (from == null || to == null) {
            ErrorHandler.showError("تنبيه", "يرجى تحديد تاريخ البداية والنهاية.");
            return;
        }

        try {
            expenseTable.setItems(FXCollections.observableArrayList(
                    reportDataService.getExpenseTransactions(from, to)
            ));
            calculateTotal();
        } catch (SQLException e) {
            ErrorHandler.showException("خطأ في تحميل البيانات", "فشل تحميل تقرير المصروفات.", e);
        }
    }

    private void calculateTotal() {
        double total = expenseTable.getItems().stream()
                .mapToDouble(LedgerEntry::getDebit)
                .sum();
        totalExpensesLabel.setText(String.format("الإجمالي: %.2f", total));
    }
}