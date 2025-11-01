package accounting.controller;

import accounting.model.Contact;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ContactFormController {

    @FXML private TextField nameField;
    @FXML private TextField phoneField;
    @FXML private TextField addressField;
    @FXML private CheckBox isSupplierCheckBox;
    @FXML private CheckBox isCustomerCheckBox;

    private Stage dialogStage;
    private Contact contact;
    private boolean okClicked = false;

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
        nameField.setText(contact.getName());
        phoneField.setText(contact.getPhone());
        addressField.setText(contact.getAddress());
        isSupplierCheckBox.setSelected(contact.isSupplier());
        isCustomerCheckBox.setSelected(contact.isCustomer());
    }

    public boolean isOkClicked() {
        return okClicked;
    }

    @FXML
    private void handleSave() {
        if (isInputValid()) {
            contact.setName(nameField.getText());
            contact.setPhone(phoneField.getText());
            contact.setAddress(addressField.getText());
            contact.setSupplier(isSupplierCheckBox.isSelected());
            contact.setCustomer(isCustomerCheckBox.isSelected());

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
        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            errorMessage += "اسم جهة التعامل مطلوب.\n";
        }
        if (!isSupplierCheckBox.isSelected() && !isCustomerCheckBox.isSelected()) {
            errorMessage += "يجب تحديد نوع جهة التعامل (مورد أو عميل أو كلاهما).\n";
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