package accounting.controller;

import accounting.model.FinancialAccount;
import accounting.service.FinancialAccountDataService;
import accounting.service.FinancialTransactionDataService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class FinancialTransactionFormController implements BaseFormController {

    @FXML private DatePicker transactionDatePicker;
    @FXML private ComboBox<FinancialAccount> debitAccountComboBox;
    @FXML private ComboBox<FinancialAccount> creditAccountComboBox;
    @FXML private TextField amountField;
    @FXML private TextArea descriptionArea;

    private Stage dialogStage;
    private boolean okClicked = false;

    private FinancialTransactionDataService transactionDataService;
    private FinancialAccountDataService accountDataService;

    @FXML
    private void initialize() {
        this.transactionDataService = new FinancialTransactionDataService();
        this.accountDataService = new FinancialAccountDataService();
        loadAccountData();
        transactionDatePicker.setValue(LocalDate.now());
    }

    private void loadAccountData() {
        try {
            List<FinancialAccount> accounts = accountDataService.getAllAccounts();
            debitAccountComboBox.setItems(FXCollections.observableArrayList(accounts));
            creditAccountComboBox.setItems(FXCollections.observableArrayList(accounts));
        } catch (SQLException e) {
            showErrorAlert("خطأ في تحميل البيانات", "فشل تحميل قائمة الحسابات المالية.");
            e.printStackTrace();
        }
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public boolean isOkClicked() {
        return okClicked;
    }

    @FXML
    private void handleSave() {
        if (isInputValid()) {
            FinancialAccount debitAccount = debitAccountComboBox.getValue();
            FinancialAccount creditAccount = creditAccountComboBox.getValue();
            LocalDate date = transactionDatePicker.getValue();
            String description = descriptionArea.getText();
            double amount = Double.parseDouble(amountField.getText());

            try {
                transactionDataService.addJournalEntry(debitAccount, creditAccount, date, description, amount);
                okClicked = true;
                dialogStage.close();
            } catch (SQLException e) {
                showErrorAlert("خطأ في الحفظ", "فشل حفظ القيد اليومي.\n" + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    private boolean isInputValid() {
        String errorMessage = "";

        if (transactionDatePicker.getValue() == null) {
            errorMessage += "تاريخ الحركة غير صالح.\n";
        }
        if (debitAccountComboBox.getValue() == null) {
            errorMessage += "يجب اختيار الحساب المحول إليه.\n";
        }
        if (creditAccountComboBox.getValue() == null) {
            errorMessage += "يجب اختيار الحساب المحول منه.\n";
        }
        if (debitAccountComboBox.getValue() != null && debitAccountComboBox.getValue().equals(creditAccountComboBox.getValue())) {
            errorMessage += "حساب مصدر الأموال ووجهتها لا يمكن أن يكونا نفس الحساب.\n";
        }
        if (amountField.getText() == null || amountField.getText().isEmpty()) {
            errorMessage += "المبلغ غير صالح.\n";
        } else {
            try {
                double amount = Double.parseDouble(amountField.getText());
                if (amount <= 0) {
                    errorMessage += "المبلغ يجب أن يكون أكبر من صفر.\n";
                }
            } catch (NumberFormatException e) {
                errorMessage += "المبلغ يجب أن يكون رقماً صحيحاً.\n";
            }
        }
        if (descriptionArea.getText() == null || descriptionArea.getText().trim().isEmpty()) {
            errorMessage += "يجب إدخال وصف/بيان للحركة.\n";
        }

        if (errorMessage.isEmpty()) {
            return true;
        } else {
            showErrorAlert("حقول غير صالحة", errorMessage);
            return false;
        }
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(dialogStage);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}