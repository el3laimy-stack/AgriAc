package accounting.controller;

import accounting.model.CashFlowEntry;
import accounting.util.ErrorHandler;
import accounting.service.FinancialTransactionDataService;
import accounting.formatter.FormatUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.NodeOrientation;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class JournalViewController {

    @FXML private TableView<CashFlowEntry> journalTable;
    @FXML private TableColumn<CashFlowEntry, LocalDate> dateColumn;
    @FXML private TableColumn<CashFlowEntry, String> descriptionColumn;
    @FXML private TableColumn<CashFlowEntry, Double> inflowColumn;
    @FXML private TableColumn<CashFlowEntry, Double> outflowColumn;
    @FXML private TableColumn<CashFlowEntry, Double> balanceColumn;

    @FXML private TextField searchField;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;

    @FXML private Label openingBalanceLabel, totalInflowLabel, totalOutflowLabel, closingBalanceLabel;

    private FinancialTransactionDataService transactionService;
    private ObservableList<CashFlowEntry> journalEntries = FXCollections.observableArrayList();
    private double openingBalance;

    @FXML
    public void initialize() {
        this.transactionService = new FinancialTransactionDataService();
        setupTable();
        handleFilterToday(); // Load today's data by default
    }

    private void setupTable() {
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        inflowColumn.setCellValueFactory(new PropertyValueFactory<>("inflow"));
        outflowColumn.setCellValueFactory(new PropertyValueFactory<>("outflow"));
        balanceColumn.setCellValueFactory(new PropertyValueFactory<>("balance"));

        // Custom cell factories
        inflowColumn.setCellFactory(col -> createCurrencyCell(true));
        outflowColumn.setCellFactory(col -> createCurrencyCell(false));
        balanceColumn.setCellFactory(col -> createCurrencyCell(false));
        descriptionColumn.setCellFactory(col -> createDescriptionCell());

        journalTable.setItems(journalEntries);
    }

    @FXML
    private void handleFilter() {
        loadJournalData();
    }

    @FXML
    private void handleClearFilters() {
        searchField.clear();
        fromDatePicker.setValue(null);
        toDatePicker.setValue(null);
        loadJournalData();
    }

    private void loadJournalData() {
        LocalDate from = fromDatePicker.getValue();
        LocalDate to = toDatePicker.getValue();

        Task<List<CashFlowEntry>> loadDataTask = new Task<>() {
            @Override
            protected List<CashFlowEntry> call() throws Exception {
                openingBalance = 0;
                if (from != null) {
                    openingBalance = transactionService.getOpeningBalance(from);
                }
                return transactionService.getCashFlowEntries(from, to);
            }
        };

        loadDataTask.setOnSucceeded(e -> {
            journalEntries.setAll(loadDataTask.getValue());
            openingBalanceLabel.setText(FormatUtils.formatCurrency(openingBalance));
            updateStatisticalCards();
        });

        loadDataTask.setOnFailed(e -> ErrorHandler.showException("خطأ", "فشل تحميل دفتر اليومية.", (Exception) loadDataTask.getException()));
        new Thread(loadDataTask).start();
    }

    private void updateStatisticalCards() {
        if (journalEntries.isEmpty()) {
            totalInflowLabel.setText(FormatUtils.formatCurrency(0));
            totalOutflowLabel.setText(FormatUtils.formatCurrency(0));
            closingBalanceLabel.setText(FormatUtils.formatCurrency(openingBalance));
            return;
        }

        double totalIn = journalEntries.stream().mapToDouble(CashFlowEntry::getInflow).sum();
        double totalOut = journalEntries.stream().mapToDouble(CashFlowEntry::getOutflow).sum();
        double closingBalance = openingBalance + totalIn - totalOut;

        totalInflowLabel.setText(FormatUtils.formatCurrency(totalIn));
        totalOutflowLabel.setText(FormatUtils.formatCurrency(totalOut));
        closingBalanceLabel.setText(FormatUtils.formatCurrency(closingBalance));
    }

    // --- Quick Filter Handlers ---
    @FXML private void handleFilterToday() { setDateRangeAndLoad(LocalDate.now(), LocalDate.now()); }
    @FXML private void handleFilterThisWeek() { setDateRangeAndLoad(LocalDate.now().with(java.time.DayOfWeek.SATURDAY), LocalDate.now().with(java.time.DayOfWeek.FRIDAY).plusWeeks(1)); }
    @FXML private void handleFilterThisMonth() { setDateRangeAndLoad(LocalDate.now().withDayOfMonth(1), LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth())); }

    private void setDateRangeAndLoad(LocalDate from, LocalDate to) {
        fromDatePicker.setValue(from);
        toDatePicker.setValue(to);
        loadJournalData();
    }

    // --- Action Handlers for new buttons ---
    @FXML private void handleNewSaleBill() { openModalForm("/fxml/SaleForm.fxml", "فاتورة بيع جديدة"); }
    @FXML private void handleNewPurchaseBill() { openModalForm("/fxml/PurchaseForm.fxml", "فاتورة شراء جديدة"); }
    @FXML private void handleNewExpense() { openModalForm("/fxml/ExpenseForm.fxml", "تسجيل مصروف جديد"); }
    @FXML private void handleNewPaymentReceipt() { openModalForm("/fxml/PaymentForm.fxml", "سند صرف/قبض جديد"); }

    private void openModalForm(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            root.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.initModality(Modality.WINDOW_MODAL);

            Object controller = loader.getController();
            if (controller instanceof BaseFormController) {
                ((BaseFormController) controller).setDialogStage(stage);
            }

            stage.showAndWait();

            if (controller instanceof BaseFormController && ((BaseFormController) controller).isOkClicked()) {
                loadJournalData();
            }
        } catch (IOException e) {
            ErrorHandler.showException("خطأ في التحميل", "فشل تحميل الواجهة.", e);
        }
    }

    // --- Custom Cell Factories ---
    private TableCell<CashFlowEntry, Double> createCurrencyCell(boolean isSuccess) {
        return new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item == 0) {
                    setText(null);
                    getStyleClass().removeAll("success-text", "danger-text");
                } else {
                    setText(FormatUtils.formatCurrency(item));
                    getStyleClass().add(isSuccess ? "success-text" : "danger-text");
                }
            }
        };
    }

    private TableCell<CashFlowEntry, String> createDescriptionCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    CashFlowEntry entry = getTableView().getItems().get(getIndex());
                    VBox vbox = new VBox(2);
                    Label descLabel = new Label(item);
                    descLabel.getStyleClass().add("description-label");
                    Label typeLabel = new Label(entry.getType());
                    typeLabel.getStyleClass().add("type-label");
                    vbox.getChildren().addAll(descLabel, typeLabel);
                    
                    FontIcon icon = new FontIcon();
                    icon.getStyleClass().add("type-icon");
                    switch (entry.getType()) {
                        case "SALE": icon.setIconLiteral("fa-tag"); break;
                        case "PURCHASE": icon.setIconLiteral("fa-shopping-cart"); break;
                        case "EXPENSE": icon.setIconLiteral("fa-credit-card"); break;
                        default: icon.setIconLiteral("fa-file-text-o"); break;
                    }

                    HBox hbox = new HBox(10, icon, vbox);
                    setGraphic(hbox);
                }
            }
        };
    }
}