package accounting.controller;

import accounting.model.LedgerEntry;
import accounting.util.ErrorHandler;
import accounting.service.ReportDataService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.NodeOrientation;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class ExpenseManagementController implements Initializable {

    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private Button filterButton;
    @FXML private Button newExpenseButton;
    @FXML private TableView<LedgerEntry> expenseTable;
    @FXML private TableColumn<LedgerEntry, LocalDate> dateColumn;
    @FXML private TableColumn<LedgerEntry, String> descriptionColumn;
    @FXML private TableColumn<LedgerEntry, String> categoryColumn;
    @FXML private TableColumn<LedgerEntry, Double> amountColumn;
    @FXML private TableColumn<LedgerEntry, Void> actionsColumn;
    @FXML private Label totalExpensesLabel;

    private ReportDataService reportDataService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        reportDataService = new ReportDataService();
        setupTable();
        fromDatePicker.setValue(LocalDate.now().withDayOfMonth(1));
        toDatePicker.setValue(LocalDate.now());
        loadReportData();
    }

    private void setupTable() {
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("debit")); // Expenses are debits
        // categoryColumn will need a custom cell value factory if the data is available
    }

    @FXML
    private void handleFilter() {
        loadReportData();
    }

    @FXML
    private void handleNewExpense() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ExpenseForm.fxml"));
            Parent page = loader.load();
            page.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("تسجيل مصروف جديد");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(expenseTable.getScene().getWindow());
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);

            ExpenseFormController controller = loader.getController();
            controller.setDialogStage(dialogStage);

            dialogStage.showAndWait();

            // Refresh data if an expense was successfully added
            if (controller.isOkClicked()) {
                loadReportData();
            }

        } catch (IOException e) {
            ErrorHandler.showException("خطأ في التحميل", "فشل تحميل واجهة تسجيل المصروف.", e);
        }
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
