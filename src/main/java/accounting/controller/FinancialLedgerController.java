package accounting.controller;

import accounting.model.FinancialAccount;
import accounting.model.LedgerEntry;
import accounting.util.ErrorHandler;
import accounting.service.FinancialAccountDataService;
import accounting.service.FinancialTransactionDataService;
import accounting.formatter.FormatUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javafx.concurrent.Task;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Alert;

import java.util.logging.Level;
import java.util.logging.Logger;

public class FinancialLedgerController implements Initializable {

    @FXML private ListView<FinancialAccount> accountsListView;
    @FXML private Label accountNameLabel;
    @FXML private Label currentBalanceLabel;
    @FXML private TableView<LedgerEntry> transactionsTableView;
    @FXML private TableColumn<LedgerEntry, LocalDate> dateColumn;
    @FXML private TableColumn<LedgerEntry, String> descriptionColumn;
    @FXML private TableColumn<LedgerEntry, Double> debitColumn;
    @FXML private TableColumn<LedgerEntry, Double> creditColumn;
    @FXML private TableColumn<LedgerEntry, Double> balanceColumn;
    @FXML private ProgressIndicator progressIndicator;

    private FinancialAccountDataService accountService;
    private FinancialTransactionDataService transactionService;
    private ObservableList<LedgerEntry> ledgerEntries = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.accountService = new FinancialAccountDataService();
        this.transactionService = new FinancialTransactionDataService();
        setupAccountsList();
        setupTransactionTable();
    }

    private void setupAccountsList() {
        Task<List<FinancialAccount>> loadAccountsTask = new Task<>() {
            @Override
            protected List<FinancialAccount> call() throws Exception {
                return accountService.getAllAccounts();
            }
        };

        loadAccountsTask.setOnSucceeded(e -> {
            accountsListView.setItems(FXCollections.observableArrayList(loadAccountsTask.getValue()));
            accountsListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> displayAccountLedger(newVal)
            );
        });

        loadAccountsTask.setOnFailed(e -> {
            Exception exception = (Exception) loadAccountsTask.getException();
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Failed to load financial accounts", exception);
            ErrorHandler.showException("خطأ في تحميل البيانات", "لا يمكن تحميل قائمة الحسابات المالية.", exception);
        });

        new Thread(loadAccountsTask).start();
    }

    private void setupTransactionTable() {
        transactionsTableView.setItems(ledgerEntries);
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        debitColumn.setCellValueFactory(new PropertyValueFactory<>("debit"));
        creditColumn.setCellValueFactory(new PropertyValueFactory<>("credit"));
        balanceColumn.setCellValueFactory(new PropertyValueFactory<>("balance"));
    }

    private void displayAccountLedger(FinancialAccount account) {
        if (account == null) return;

        accountNameLabel.setText("كشف حساب: " + account.getAccountName());
        currentBalanceLabel.setText("الرصيد الحالي: " + FormatUtils.formatCurrency(account.getCurrentBalance()));

        Task<List<LedgerEntry>> loadLedgerTask = new Task<>() {
            @Override
            protected List<LedgerEntry> call() throws Exception {
                List<LedgerEntry> allEntries = transactionService.getGeneralLedgerEntries(null, null);
                
                List<LedgerEntry> accountEntries = allEntries.stream()
                    .filter(entry -> entry.getDescription().contains(account.getAccountName()))
                    .collect(Collectors.toList());

                double runningBalance = 0;
                for (LedgerEntry entry : accountEntries) {
                    runningBalance += entry.getDebit() - entry.getCredit();
                    entry.setBalance(runningBalance);
                }
                return accountEntries;
            }
        };

        loadLedgerTask.setOnSucceeded(e -> {
            ledgerEntries.setAll(loadLedgerTask.getValue());
        });

        loadLedgerTask.setOnFailed(e -> {
            Exception exception = (Exception) loadLedgerTask.getException();
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Failed to load ledger entries", exception);
            ErrorHandler.showException("خطأ في تحميل البيانات", "لا يمكن تحميل قيود دفتر الأستاذ.", exception);
        });

        progressIndicator.visibleProperty().bind(loadLedgerTask.runningProperty());
        new Thread(loadLedgerTask).start();
    }
}
