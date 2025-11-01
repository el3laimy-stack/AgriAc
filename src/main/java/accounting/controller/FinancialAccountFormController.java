package accounting.controller;

import accounting.model.FinancialAccount;
import accounting.model.FinancialAccount.AccountType;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class FinancialAccountFormController {

    @FXML private TextField accountNameField;
    @FXML private ComboBox<AccountType> accountTypeComboBox;
    @FXML private TextField openingBalanceField;
    @FXML private DatePicker openingBalanceDatePicker;

    private Stage dialogStage;
    private FinancialAccount account;
    private boolean okClicked = false;

    @FXML
    private void initialize() {
        accountTypeComboBox.setItems(FXCollections.observableArrayList(AccountType.values()));
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setAccount(FinancialAccount account) {
        this.account = account;

        accountNameField.setText(account.getAccountName());
        accountTypeComboBox.setValue(account.getAccountType());
        openingBalanceField.setText(Double.toString(account.getOpeningBalance()));
        openingBalanceDatePicker.setValue(account.getOpeningBalanceDate());
    }

    public boolean isOkClicked() {
        return okClicked;
    }

    @FXML
    private void handleSave() {
        if (isInputValid()) {
            account.setAccountName(accountNameField.getText());
            account.setAccountType(accountTypeComboBox.getValue());
            account.setOpeningBalance(Double.parseDouble(openingBalanceField.getText()));
            account.setOpeningBalanceDate(openingBalanceDatePicker.getValue());

            okClicked = true;
            dialogStage.close();
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    private boolean isInputValid() {
        String errorMessage = "";
        if (accountNameField.getText() == null || accountNameField.getText().trim().isEmpty()) {
            errorMessage += "اسم الحساب مطلوب.\n";
        }
        if (accountTypeComboBox.getValue() == null) {
            errorMessage += "نوع الحساب مطلوب.\n";
        }
        if (openingBalanceDatePicker.getValue() == null) {
            errorMessage += "تاريخ الرصيد الافتتاحي مطلوب.\n";
        }
        try {
            Double.parseDouble(openingBalanceField.getText());
        } catch (NumberFormatException e) {
            errorMessage += "الرصيد الافتتاحي يجب أن يكون رقماً صحيحاً.\n";
        }

        if (errorMessage.isEmpty()) {
            return true;
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(dialogStage);
            alert.setTitle("حقول غير صالحة");
            alert.setHeaderText(null);
            alert.setContentText(errorMessage);
            alert.showAndWait();
            return false;
        }
    }
}