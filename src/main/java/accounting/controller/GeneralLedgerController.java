package accounting.controller;

import accounting.model.LedgerEntry;
import accounting.util.ErrorHandler;
import accounting.service.FinancialTransactionDataService;
import accounting.formatter.FormatUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.scene.control.Tooltip;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

public class GeneralLedgerController implements Initializable {

    // FXML Components
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private Button filterButton;
    @FXML private Button clearButton;
    @FXML private TableView<LedgerEntry> ledgerTable;
    @FXML private TableColumn<LedgerEntry, LocalDate> dateColumn;
    @FXML private TableColumn<LedgerEntry, String> refColumn;
    @FXML private TableColumn<LedgerEntry, String> descriptionColumn;
    @FXML private TableColumn<LedgerEntry, Double> debitColumn;
    @FXML private TableColumn<LedgerEntry, Double> creditColumn;
    @FXML private Label totalDebitLabel;
    @FXML private Label totalCreditLabel;
    @FXML private Label balanceLabel;

    // Services and Data
    private FinancialTransactionDataService transactionService;
    private ObservableList<LedgerEntry> ledgerEntries;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.transactionService = new FinancialTransactionDataService();
        this.ledgerEntries = FXCollections.observableArrayList();
        
        setupTable();
        setupEventHandlers();
        
        // Set default date range to the current month
        fromDatePicker.setValue(LocalDate.now().withDayOfMonth(1));
        toDatePicker.setValue(LocalDate.now().with(java.time.temporal.TemporalAdjusters.lastDayOfMonth()));

        loadData(); // Load initial data for the default range
    }

    private void setupTable() {
        ledgerTable.setItems(ledgerEntries);

        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        refColumn.setCellValueFactory(new PropertyValueFactory<>("reference"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        debitColumn.setCellValueFactory(new PropertyValueFactory<>("debit"));
        creditColumn.setCellValueFactory(new PropertyValueFactory<>("credit"));

        // Format date and currency cells
        formatDateCell(dateColumn);
        formatCurrencyCell(debitColumn);
        formatCurrencyCell(creditColumn);
    
    }

    private void setupEventHandlers() {
        filterButton.setOnAction(e -> loadData());
        clearButton.setOnAction(e -> {
            fromDatePicker.setValue(null);
            toDatePicker.setValue(null);
            loadData();
        });
    }

    private void loadData() {
        try {
            List<LedgerEntry> data = transactionService.getGeneralLedgerEntries(
                fromDatePicker.getValue(),
                toDatePicker.getValue()
            );
            ledgerEntries.setAll(data);

            updateTotals();
        } catch (SQLException e) {
            ErrorHandler.showException("خطأ في تحميل البيانات", "لا يمكن تحميل قيود دفتر الأستاذ العام.", e);
        }
    }
    
    private void updateTotals() {
        double totalDebit = ledgerEntries.stream().mapToDouble(LedgerEntry::getDebit).sum();
        double totalCredit = ledgerEntries.stream().mapToDouble(LedgerEntry::getCredit).sum();

        totalDebitLabel.setText("مدين: " + FormatUtils.formatCurrency(totalDebit));
        totalCreditLabel.setText("دائن: " + FormatUtils.formatCurrency(totalCredit));

        balanceLabel.getStyleClass().removeAll("status-balanced", "status-unbalanced");
        if (Math.abs(totalDebit - totalCredit) < 0.01) {
            balanceLabel.setText("الحالة: متزن");
            balanceLabel.getStyleClass().add("status-balanced");
        } else {
            balanceLabel.setText("الحالة: غير متزن");
            balanceLabel.getStyleClass().add("status-unbalanced");
        }
    }

    private void formatDateCell(TableColumn<LedgerEntry, LocalDate> column) {
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : FormatUtils.formatDateForDisplay(item));
            }
        });
    }

    private void formatCurrencyCell(TableColumn<LedgerEntry, Double> column) {
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item == 0) {
                    setText("");
                    setStyle(""); // Clear any previous style
                } else {
                    setText(FormatUtils.formatCurrency(item));
                    if (item < 0) {
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }
}
