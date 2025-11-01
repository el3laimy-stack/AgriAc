package accounting.controller;

import accounting.model.Contact;
import accounting.model.FinancialAccount;
import accounting.service.ContactDataService;
import accounting.service.FinancialAccountDataService;
import accounting.service.FinancialTransactionDataService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ResourceBundle;

import java.util.logging.Logger;

public class JournalEntryFormController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(JournalEntryFormController.class.getName());

    // FXML Components
    @FXML private GridPane formGrid;
    @FXML private ComboBox<String> transactionTypeComboBox;
    @FXML private DatePicker datePicker;
    @FXML private TextArea descriptionArea;
    @FXML private Label amountLabel;
    @FXML private TextField amountField;
    @FXML private Label fromAccountLabel;
    @FXML private ComboBox<FinancialAccount> fromAccountComboBox;
    @FXML private Label toAccountLabel;
    @FXML private ComboBox<FinancialAccount> toAccountComboBox;
    @FXML private Label contactLabel;
    @FXML private ComboBox<Contact> contactComboBox;
    @FXML private Label paidAmountLabel;
    @FXML private TextField paidAmountField;

    // Services
    private final FinancialAccountDataService accountService = new FinancialAccountDataService();
    private final ContactDataService contactService = new ContactDataService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTransactionTypes();
        loadInitialData();
        setupFieldVisibility();
    }

    private void setupTransactionTypes() {
        transactionTypeComboBox.setItems(FXCollections.observableArrayList(
                "مصروف نقدي",
                "إيداع رأس المال",
                "فاتورة شراء (آجل)",
                "فاتورة بيع (آجل)",
                "سداد لمورد",
                "تحصيل من عميل",
                "قيد يومية يدوي"
        ));
    }

    private void loadInitialData() {
        try {
            fromAccountComboBox.setItems(FXCollections.observableArrayList(accountService.getAllAccounts()));
            toAccountComboBox.setItems(FXCollections.observableArrayList(accountService.getAllAccounts()));
            contactComboBox.setItems(FXCollections.observableArrayList(contactService.getAllContacts()));
            datePicker.setValue(LocalDate.now());
        } catch (SQLException e) {
            e.printStackTrace();
            // Show error alert
        }
    }

    public void setInitialTransactionType(String type) {
        if (type != null && !type.isEmpty()) {
            transactionTypeComboBox.setValue(type);
        }
    }

    private void setupFieldVisibility() {
        transactionTypeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;

            // Reset all fields to a default state
            setFieldVisibility("fromAccount", false);
            setFieldVisibility("toAccount", false);
            setFieldVisibility("contact", false);
            setFieldVisibility("paidAmount", false);
            amountLabel.setText("المبلغ:");

            switch (newVal) {
                case "فاتورة شراء (آجل)":
                    setFieldVisibility("contact", true);
                    contactLabel.setText("المورد:");
                    amountLabel.setText("إجمالي الفاتورة:");
                    setFieldVisibility("paidAmount", true);
                    break;
                case "فاتورة بيع (آجل)":
                    setFieldVisibility("contact", true);
                    contactLabel.setText("العميل:");
                    amountLabel.setText("إجمالي الفاتورة:");
                    setFieldVisibility("paidAmount", true);
                    break;
                case "سداد لمورد":
                case "تحصيل من عميل":
                    setFieldVisibility("contact", true);
                    contactLabel.setText(newVal.equals("سداد لمورد") ? "المورد:" : "العميل:");
                    setFieldVisibility("fromAccount", true);
                    fromAccountLabel.setText("حساب الدفع (البنك/الخزينة):");
                    break;
                case "مصروف نقدي":
                    setFieldVisibility("toAccount", true);
                    toAccountLabel.setText("حساب المصروف:");
                    setFieldVisibility("fromAccount", true);
                    fromAccountLabel.setText("حساب الدفع (البنك/الخزينة):");
                    break;
                case "إيداع رأس المال":
                    setFieldVisibility("fromAccount", true);
                    fromAccountLabel.setText("حساب الإيداع (البنك):");
                    setFieldVisibility("toAccount", true);
                    toAccountLabel.setText("حساب رأس المال:");
                    break;
                case "قيد يومية يدوي":
                default:
                    setFieldVisibility("fromAccount", true);
                    fromAccountLabel.setText("من حساب (مدين):");
                    setFieldVisibility("toAccount", true);
                    toAccountLabel.setText("إلى حساب (دائن):");
                    break;
            }
        });
        // Trigger the listener for the initial value
        transactionTypeComboBox.setValue("قيد يومية يدوي");
    }

    private void setFieldVisibility(String fieldName, boolean isVisible) {
        switch (fieldName) {
            case "fromAccount":
                fromAccountLabel.setVisible(isVisible);
                fromAccountComboBox.setVisible(isVisible);
                break;
            case "toAccount":
                toAccountLabel.setVisible(isVisible);
                toAccountComboBox.setVisible(isVisible);
                break;
            case "contact":
                contactLabel.setVisible(isVisible);
                contactComboBox.setVisible(isVisible);
                break;
            case "paidAmount":
                paidAmountLabel.setVisible(isVisible);
                paidAmountField.setVisible(isVisible);
                break;
        }
    }

    public void processAndSaveEntry() {
        String type = transactionTypeComboBox.getValue();
        if (type == null || !validateInput(type)) {
            // Show validation error
            LOGGER.warning("Validation failed for journal entry.");
            return;
        }

        try {
            // This is where the logic for each transaction type will go.
            // For now, we'll just print a success message.
            LOGGER.info("Saving entry of type: " + type);
            
            // In a real implementation, you would call a service method here, e.g.:
            // transactionService.createJournalEntry(buildEntryData());

            closeDialog();
        } catch (Exception e) {
            e.printStackTrace();
            // Show error alert
        }
    }

    private boolean validateInput(String type) {
        // Basic validation, can be expanded
        if (datePicker.getValue() == null || descriptionArea.getText().isEmpty() || amountField.getText().isEmpty()) {
            return false;
        }
        // Add more specific validation based on type
        return true;
    }

    private Runnable onCloseRequestHandler;

    public void setOnCloseRequestHandler(Runnable onCloseRequestHandler) {
        this.onCloseRequestHandler = onCloseRequestHandler;
    }

    private void closeDialog() {
        if (onCloseRequestHandler != null) {
            onCloseRequestHandler.run();
        }
    }
}