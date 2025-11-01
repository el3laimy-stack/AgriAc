package accounting.controller;

import accounting.model.FinancialAccount;
import accounting.util.ErrorHandler;
import accounting.service.FinancialAccountDataService;
import accounting.service.FinancialTransactionDataService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ExpenseFormController implements BaseFormController {

    @FXML private DatePicker datePicker;
    @FXML private TextField amountField;
    @FXML private ComboBox<FinancialAccount> expenseCategoryComboBox;
    @FXML private ComboBox<FinancialAccount> paymentAccountComboBox;
    @FXML private TextArea descriptionArea;

    private Stage dialogStage;
    private boolean okClicked = false;

    private FinancialAccountDataService financialAccountDataService;
    private FinancialTransactionDataService transactionService;

    @FXML
    private void initialize() {
        financialAccountDataService = new FinancialAccountDataService();
        transactionService = new FinancialTransactionDataService();
        datePicker.setValue(LocalDate.now());
        loadInitialData();
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public boolean isOkClicked() {
        return okClicked;
    }

    private void loadInitialData() {
        try {
            expenseCategoryComboBox.setItems(FXCollections.observableArrayList(financialAccountDataService.getExpenseAccounts()));
            List<FinancialAccount> paymentAccounts = financialAccountDataService.getCashAndBankAccounts();
            paymentAccountComboBox.setItems(FXCollections.observableArrayList(paymentAccounts));

            for (FinancialAccount account : paymentAccounts) {
                if (account.getAccountName().equals("الخزنة الرئيسية")) {
                    paymentAccountComboBox.setValue(account);
                    break;
                }
            }
        } catch (SQLException e) {
            ErrorHandler.showException("خطأ في تحميل البيانات", "فشل تحميل الحسابات.", e);
        }
    }

    @FXML
    private void handleSave() {
        if (isInputValid()) {
            try {
                transactionService.addExpense(
                    datePicker.getValue(),
                    Double.parseDouble(amountField.getText()),
                    descriptionArea.getText(),
                    expenseCategoryComboBox.getValue().getAccountId(),
                    paymentAccountComboBox.getValue().getAccountId()
                );
                okClicked = true;
                dialogStage.close();
            } catch (SQLException e) {
                ErrorHandler.showException("خطأ في الحفظ", "فشل حفظ المصروف.", e);
            }
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    private boolean isInputValid() {
        String errorMessage = "";

        if (datePicker.getValue() == null) {
            errorMessage += "التاريخ مطلوب.\n";
        }
        if (amountField.getText() == null || amountField.getText().trim().isEmpty()) {
            errorMessage += "المبلغ مطلوب.\n";
        } else {
            try {
                if (Double.parseDouble(amountField.getText()) <= 0) {
                    errorMessage += "المبلغ يجب أن يكون أكبر من صفر.\n";
                }
            } catch (NumberFormatException e) {
                errorMessage += "المبلغ يجب أن يكون رقماً صحيحاً.\n";
            }
        }
        if (expenseCategoryComboBox.getValue() == null) {
            errorMessage += "نوع المصروف مطلوب.\n";
        }
        if (paymentAccountComboBox.getValue() == null) {
            errorMessage += "حساب الدفع مطلوب.\n";
        }
        if (descriptionArea.getText() == null || descriptionArea.getText().trim().isEmpty()) {
            errorMessage += "البيان مطلوب.\n";
        }

        if (errorMessage.isEmpty()) {
            return true;
        } else {
            ErrorHandler.showError("حقول غير صالحة", errorMessage);
            return false;
        }
    }
}
