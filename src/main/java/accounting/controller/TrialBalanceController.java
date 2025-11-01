package accounting.controller;

import accounting.model.TrialBalanceEntry;
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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;

import java.time.LocalDate;
import java.util.List;

public class TrialBalanceController {

    @FXML private DatePicker toDatePicker;
    @FXML private Button viewButton;
    @FXML private TableView<TrialBalanceEntry> trialBalanceTable;
    @FXML private TableColumn<TrialBalanceEntry, Integer> accountIdColumn;
    @FXML private TableColumn<TrialBalanceEntry, String> accountNameColumn;
    @FXML private TableColumn<TrialBalanceEntry, Double> debitColumn;
    @FXML private TableColumn<TrialBalanceEntry, Double> creditColumn;
    @FXML private Label totalDebitLabel;
    @FXML private Label totalCreditLabel;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progressIndicator;

    private FinancialSummaryService summaryService;
    private ObservableList<TrialBalanceEntry> trialBalanceList;

    @FXML
    public void initialize() {
        this.summaryService = new FinancialSummaryService();
        this.trialBalanceList = FXCollections.observableArrayList();
        
        toDatePicker.setValue(LocalDate.now());
        setupTable();
        
        viewButton.setOnAction(e -> loadData());
        loadData(); // تحميل البيانات عند فتح الشاشة لأول مرة
    }

    private void setupTable() {
        trialBalanceTable.setItems(trialBalanceList);
        accountIdColumn.setCellValueFactory(new PropertyValueFactory<>("accountId"));
        accountNameColumn.setCellValueFactory(new PropertyValueFactory<>("accountName"));

        // Debit Column
        debitColumn.setCellValueFactory(new PropertyValueFactory<>("totalDebit"));
        debitColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("text-danger", "text-success");
                if (item == null || empty || item == 0) {
                    setText(null);
                } else {
                    setText(FormatUtils.formatCurrency(item));
                    getStyleClass().add("text-danger");
                }
            }
        });

        // Credit Column
        creditColumn.setCellValueFactory(new PropertyValueFactory<>("totalCredit"));
        creditColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("text-danger", "text-success");
                if (item == null || empty || item == 0) {
                    setText(null);
                } else {
                    setText(FormatUtils.formatCurrency(item));
                    getStyleClass().add("text-success");
                }
            }
        });
    }

    private void loadData() {
        LocalDate toDate = toDatePicker.getValue();
        if (toDate == null) return;

        Task<List<TrialBalanceEntry>> loadDataTask = new Task<>() {
            @Override
            protected List<TrialBalanceEntry> call() throws Exception {
                return summaryService.getTrialBalance(toDate);
            }
        };

        loadDataTask.setOnSucceeded(e -> {
            trialBalanceList.setAll(loadDataTask.getValue());
            updateTotals();
        });

        loadDataTask.setOnFailed(e -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("خطأ في تحميل البيانات");
            alert.setHeaderText("لا يمكن تحميل بيانات ميزان المراجعة.");
            alert.setContentText("حدث خطأ أثناء الاتصال بقاعدة البيانات. يرجى المحاولة مرة أخرى.");
            alert.showAndWait();
        });

        progressIndicator.visibleProperty().bind(loadDataTask.runningProperty());
        new Thread(loadDataTask).start();
    }

    private void updateTotals() {
        double totalDebit = trialBalanceList.stream().mapToDouble(TrialBalanceEntry::getTotalDebit).sum();
        double totalCredit = trialBalanceList.stream().mapToDouble(TrialBalanceEntry::getTotalCredit).sum();

        totalDebitLabel.setText("الإجمالي المدين: " + FormatUtils.formatCurrency(totalDebit));
        totalCreditLabel.setText("الإجمالي الدائن: " + FormatUtils.formatCurrency(totalCredit));

        if (Math.abs(totalDebit - totalCredit) < 0.01) {
            statusLabel.setText("الحالة: متزن");
            statusLabel.setTextFill(Color.GREEN);
        } else {
            statusLabel.setText("الحالة: غير متزن!");
            statusLabel.setTextFill(Color.RED);
        }
    }

    private void formatCurrencyCell(TableColumn<TrialBalanceEntry, Double> column) {
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : FormatUtils.formatCurrency(item));
            }
        });
    }
}