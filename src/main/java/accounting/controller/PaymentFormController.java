package accounting.controller;

import accounting.model.Contact;
import accounting.model.FinancialAccount;
import accounting.model.Payment;
import accounting.service.ContactDataService;
import accounting.util.ErrorHandler;
import accounting.service.FinancialAccountDataService;
import accounting.service.PaymentDataService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class PaymentFormController {

    @FXML private DatePicker paymentDatePicker;
    @FXML private RadioButton receiveRadio;
    @FXML private RadioButton payRadio;
    @FXML private ComboBox<Contact> contactComboBox;
    @FXML private TextField amountField;
    @FXML private RadioButton cashRadio;
    @FXML private RadioButton bankRadio;
    @FXML private Label bankAccountLabel;
    @FXML private ComboBox<FinancialAccount> bankAccountComboBox;
    @FXML private TextArea descriptionArea;

    private Stage dialogStage;
    private boolean okClicked = false;

    private ContactDataService contactDataService;
    private FinancialAccountDataService accountDataService;
    private PaymentDataService paymentDataService;

    private ToggleGroup paymentTypeToggleGroup;
    private ToggleGroup paymentMethodToggleGroup;

    @FXML
    private void initialize() {
        contactDataService = new ContactDataService();
        accountDataService = new FinancialAccountDataService();
        paymentDataService = new PaymentDataService();

        paymentDatePicker.setValue(LocalDate.now());

        setupToggles();
        loadInitialData();
    }

    private void setupToggles() {
        paymentTypeToggleGroup = new ToggleGroup();
        receiveRadio.setToggleGroup(paymentTypeToggleGroup);
        payRadio.setToggleGroup(paymentTypeToggleGroup);

        paymentMethodToggleGroup = new ToggleGroup();
        cashRadio.setToggleGroup(paymentMethodToggleGroup);
        bankRadio.setToggleGroup(paymentMethodToggleGroup);

        bankAccountLabel.visibleProperty().bind(bankRadio.selectedProperty());
        bankAccountComboBox.visibleProperty().bind(bankRadio.selectedProperty());

        receiveRadio.setSelected(true);
        cashRadio.setSelected(true);

        paymentTypeToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            loadContacts();
        });
    }

    private void loadInitialData() {
        try {
            bankAccountComboBox.setItems(FXCollections.observableArrayList(accountDataService.getBankAccounts()));
            loadContacts();
        } catch (SQLException e) {
            ErrorHandler.showException("خطأ في تحميل البيانات", "فشل تحميل البيانات الأساسية.", e);
        }
    }

    private void loadContacts() {
        try {
            List<Contact> contacts = contactDataService.getAllContacts();
            if (receiveRadio.isSelected()) {
                contactComboBox.setItems(FXCollections.observableArrayList(
                        contacts.stream().filter(Contact::isCustomer).collect(Collectors.toList())));
            } else {
                contactComboBox.setItems(FXCollections.observableArrayList(
                        contacts.stream().filter(Contact::isSupplier).collect(Collectors.toList())));
            }
        } catch (SQLException e) {
            ErrorHandler.showException("خطأ في تحميل البيانات", "فشل تحميل قائمة جهات التعامل.", e);
        }
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public boolean isOkClicked() {
        return okClicked;
    }

    public void setContact(Contact contact) {
        contactComboBox.setValue(contact);
        contactComboBox.setDisable(true);

        if (contact.isCustomer()) {
            receiveRadio.setSelected(true);
        } else if (contact.isSupplier()) {
            payRadio.setSelected(true);
        }

        receiveRadio.setDisable(true);
        payRadio.setDisable(true);
    }

    @FXML
    private void handleSave() {
        if (isInputValid()) {
            Payment payment = new Payment();
            payment.setPaymentDate(paymentDatePicker.getValue());
            payment.setContact(contactComboBox.getValue());
            payment.setAmount(Double.parseDouble(amountField.getText()));
            payment.setDescription(descriptionArea.getText());

            if (receiveRadio.isSelected()) {
                payment.setPaymentType("RECEIVE");
            } else {
                payment.setPaymentType("PAY");
            }

            FinancialAccount paymentAccount = null;
            if (cashRadio.isSelected()) {
                paymentAccount = new FinancialAccount();
                paymentAccount.setAccountId(10101); // Main Treasury
            } else {
                paymentAccount = bankAccountComboBox.getValue();
            }
            payment.setPaymentAccount(paymentAccount);

            try {
                paymentDataService.addPayment(payment);
                okClicked = true;
                dialogStage.close();
            } catch (SQLException e) {
                ErrorHandler.showException("خطأ في الحفظ", "فشل حفظ حركة الدفع.", e);
            }
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    private boolean isInputValid() {
        String errorMessage = "";
        if (paymentDatePicker.getValue() == null) errorMessage += "التاريخ مطلوب.\n";
        if (contactComboBox.getValue() == null) errorMessage += "يجب اختيار العميل أو المورد.\n";
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
        if (bankRadio.isSelected() && bankAccountComboBox.getValue() == null) {
            errorMessage += "يجب اختيار حساب بنكي.\n";
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