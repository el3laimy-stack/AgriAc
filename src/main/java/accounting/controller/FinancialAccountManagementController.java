package accounting.controller;

import accounting.model.FinancialAccount;
import accounting.util.ErrorHandler;
import accounting.service.FinancialAccountDataService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class FinancialAccountManagementController implements javafx.fxml.Initializable {

    @FXML private TableView<FinancialAccount> accountTable;
    @FXML private TableColumn<FinancialAccount, Integer> accountIdColumn;
    @FXML private TableColumn<FinancialAccount, String> accountNameColumn;
    @FXML private TableColumn<FinancialAccount, String> accountTypeColumn;
    @FXML private TableColumn<FinancialAccount, Double> openingBalanceColumn;
    @FXML private TableColumn<FinancialAccount, Double> currentBalanceColumn;
    @FXML private TableColumn<FinancialAccount, Void> actionsColumn;

    @FXML private TextField searchField;

    private FinancialAccountDataService accountDataService;
    private ObservableList<FinancialAccount> accountList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.accountDataService = new FinancialAccountDataService();
        setupTable();
        setupSearchFilter();
        loadAccounts();
    }

    private void setupTable() {
        accountIdColumn.setCellValueFactory(new PropertyValueFactory<>("accountId"));
        accountNameColumn.setCellValueFactory(new PropertyValueFactory<>("accountName"));
        accountTypeColumn.setCellValueFactory(new PropertyValueFactory<>("accountType"));
        openingBalanceColumn.setCellValueFactory(new PropertyValueFactory<>("openingBalance"));
        currentBalanceColumn.setCellValueFactory(new PropertyValueFactory<>("currentBalance"));
        addActionsToTable();
    }

    private void setupSearchFilter() {
        FilteredList<FinancialAccount> filteredData = new FilteredList<>(accountList, p -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(account -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return account.getAccountName().toLowerCase().contains(lowerCaseFilter)
                    || account.getAccountType().toString().toLowerCase().contains(lowerCaseFilter);
            });
        });
        accountTable.setItems(filteredData);
    }

    private void loadAccounts() {
        Task<List<FinancialAccount>> loadDataTask = new Task<>() {
            @Override
            protected List<FinancialAccount> call() throws Exception {
                return accountDataService.getAllAccounts();
            }
        };
        loadDataTask.setOnSucceeded(e -> accountList.setAll(loadDataTask.getValue()));
        loadDataTask.setOnFailed(e -> ErrorHandler.showException("خطأ", "فشل تحميل الحسابات.", (Exception) loadDataTask.getException()));
        new Thread(loadDataTask).start();
    }

    private void addActionsToTable() {
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button("تعديل");
            private final Button deleteButton = new Button("حذف");
            private final HBox pane = new HBox(5, editButton, deleteButton);

            {
                editButton.getStyleClass().add("warning");
                deleteButton.getStyleClass().add("danger");

                editButton.setOnAction(event -> {
                    FinancialAccount account = getTableView().getItems().get(getIndex());
                    handleEditAccount(account);
                });
                deleteButton.setOnAction(event -> {
                    FinancialAccount account = getTableView().getItems().get(getIndex());
                    handleDeleteAccount(account);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    @FXML
    private void handleAddAccount() {
        showAccountEditDialog(new FinancialAccount());
    }

    private void handleEditAccount(FinancialAccount account) {
        showAccountEditDialog(account);
    }

    private void showAccountEditDialog(FinancialAccount account) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/FinancialAccountForm.fxml"));
            VBox page = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("تحرير بيانات الحساب");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.setScene(new Scene(page));

            FinancialAccountFormController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setAccount(account);

            dialogStage.showAndWait();
            loadAccounts(); // Refresh list
        } catch (IOException e) {
            ErrorHandler.showException("خطأ في التحميل", "فشل تحميل واجهة تحرير الحساب.", e);
        }
    }

    private void handleDeleteAccount(FinancialAccount account) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("تأكيد الحذف");
        alert.setContentText("هل أنت متأكد من حذف: " + account.getAccountName() + "؟");
        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                accountDataService.deleteAccount(account.getAccountId());
                loadAccounts();
            } catch (SQLException e) {
                ErrorHandler.showException("خطأ في الحذف", "فشل حذف الحساب.", e);
            }
        }
    }
}
