package accounting.controller;

import accounting.model.Contact;
import accounting.model.Crop;
import accounting.model.FinancialAccount;
import accounting.model.SaleRecord;
import accounting.service.ContactDataService;
import accounting.service.CropDataService;
import accounting.util.ErrorHandler;
import accounting.service.FinancialAccountDataService;
import accounting.util.FormValidator;
import accounting.service.SaleDataService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.List;

public class SaleFormController implements BaseFormController {

    // Common fields from the old controller
    @FXML private DatePicker saleDatePicker;
    @FXML private ComboBox<Crop> cropComboBox;
    @FXML private ComboBox<Contact> customerComboBox;
    @FXML private TextField quantityKgField;
    @FXML private ComboBox<String> pricingUnitComboBox;
    @FXML private TextField unitPriceField;
    @FXML private TextField totalAmountField;
    @FXML private TextField invoiceNumberField;
    @FXML private TextField amountReceivedField;
    @FXML private RadioButton cashPaymentRadio;
    @FXML private RadioButton bankPaymentRadio;
    @FXML private Label bankAccountLabel;
    @FXML private ComboBox<FinancialAccount> bankAccountComboBox;
    @FXML private Label currentStockLabel;

    // New FXML fields for the wizard
    @FXML private VBox step1Indicator, step2Indicator, step3Indicator;
    @FXML private VBox step1Pane, step2Pane, step3Pane;
    @FXML private Button previousButton, nextButton, saveButton;

    // Summary labels
    @FXML private Label summaryCustomerLabel, summaryCropLabel, summaryQuantityLabel, summaryTotalLabel;

    private Stage dialogStage;
    private SaleRecord sale;
    private boolean okClicked = false;
    private int currentStep = 1;

    private SaleDataService saleDataService;
    private CropDataService cropDataService;
    private ContactDataService contactDataService;
    private FinancialAccountDataService accountDataService;
    private ToggleGroup paymentMethodToggleGroup;

    @FXML
    private void initialize() {
        this.sale = new SaleRecord();
        this.saleDataService = new SaleDataService();
        this.cropDataService = new CropDataService();
        this.contactDataService = new ContactDataService();
        this.accountDataService = new FinancialAccountDataService();

        setupPaymentControls();
        updateStepView();
        loadComboBoxData();

        // Add listeners for automatic calculation
        quantityKgField.textProperty().addListener((obs, oldVal, newVal) -> calculateTotalAmount());
        unitPriceField.textProperty().addListener((obs, oldVal, newVal) -> calculateTotalAmount());
        pricingUnitComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> calculateTotalAmount());
        cropComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> onCropSelected(newVal));
    }

    private void updateStepView() {
        // Panes visibility
        step1Pane.setVisible(currentStep == 1);
        step2Pane.setVisible(currentStep == 2);
        step3Pane.setVisible(currentStep == 3);

        // Stepper styling
        updateIndicatorStyle(step1Indicator, 1);
        updateIndicatorStyle(step2Indicator, 2);
        updateIndicatorStyle(step3Indicator, 3);

        // Buttons visibility
        previousButton.setVisible(currentStep > 1);
        nextButton.setVisible(currentStep < 3);
        saveButton.setVisible(currentStep == 3);
    }

    private void updateIndicatorStyle(VBox indicator, int step) {
        indicator.getStyleClass().remove("active");
        indicator.getStyleClass().remove("completed");
        if (step == currentStep) {
            indicator.getStyleClass().add("active");
        } else if (step < currentStep) {
            indicator.getStyleClass().add("completed");
        }
    }

    @FXML
    private void handleNext() {
        if (isStepValid()) {
            if (currentStep < 3) {
                currentStep++;
                if (currentStep == 3) {
                    updateSummaryView();
                }
                updateStepView();
            }
        }
    }

    @FXML
    private void handlePrevious() {
        if (currentStep > 1) {
            currentStep--;
            updateStepView();
        }
    }

    private boolean isStepValid() {
        switch (currentStep) {
            case 1: return isStep1Valid();
            case 2: return isStep2Valid();
            case 3: return isStep3Valid();
            default: return false;
        }
    }

    private void updateSummaryView() {
        summaryCustomerLabel.setText(customerComboBox.getValue() != null ? customerComboBox.getValue().toString() : "-");
        summaryCropLabel.setText(cropComboBox.getValue() != null ? cropComboBox.getValue().toString() : "-");
        summaryQuantityLabel.setText(quantityKgField.getText() + " كجم");
        summaryTotalLabel.setText(totalAmountField.getText() + " ج.م");
    }

    // --- Data Loading and Setup ---
    private void setupPaymentControls() {
        paymentMethodToggleGroup = new ToggleGroup();
        cashPaymentRadio.setToggleGroup(paymentMethodToggleGroup);
        bankPaymentRadio.setToggleGroup(paymentMethodToggleGroup);
        bankAccountLabel.visibleProperty().bind(bankPaymentRadio.selectedProperty());
        bankAccountComboBox.visibleProperty().bind(bankPaymentRadio.selectedProperty());
        cashPaymentRadio.setSelected(true);
    }

    private void loadComboBoxData() {
        try {
            cropComboBox.setItems(FXCollections.observableArrayList(cropDataService.getAllActiveCrops()));
            List<Contact> customers = contactDataService.getAllContacts().stream().filter(Contact::isCustomer).toList();
            customerComboBox.setItems(FXCollections.observableArrayList(customers));
            bankAccountComboBox.setItems(FXCollections.observableArrayList(accountDataService.getBankAccounts()));
        } catch (SQLException e) {
            ErrorHandler.showException("خطأ", "فشل تحميل البيانات الأساسية.", e);
        }
    }

    private void onCropSelected(Crop selectedCrop) {
        if (selectedCrop != null) {
            pricingUnitComboBox.setItems(FXCollections.observableArrayList(selectedCrop.getAllowedPricingUnits()));
            pricingUnitComboBox.getSelectionModel().selectFirst();
            try {
                double stock = cropDataService.getCurrentStock(selectedCrop.getCropId());
                currentStockLabel.setText(String.format("المخزون: %.2f كجم", stock));
            } catch (SQLException e) {
                currentStockLabel.setText("المخزون: خطأ");
            }
        } else {
            pricingUnitComboBox.getItems().clear();
            currentStockLabel.setText("المخزون: --");
        }
    }

    private void calculateTotalAmount() {
        try {
            double quantityKg = Double.parseDouble(quantityKgField.getText());
            double unitPrice = Double.parseDouble(unitPriceField.getText());
            String selectedUnit = pricingUnitComboBox.getValue();
            Crop selectedCrop = cropComboBox.getValue();
            if (selectedUnit == null || selectedCrop == null) return;

            double conversionFactor = selectedCrop.getFirstConversionFactor(selectedUnit);
            if (conversionFactor <= 0) conversionFactor = 1.0;

            double totalAmount = (quantityKg / conversionFactor) * unitPrice;
            totalAmountField.setText(new DecimalFormat("#.##").format(totalAmount));
        } catch (NumberFormatException e) {
            totalAmountField.clear();
        }
    }

    // --- Validation per Step ---
    private boolean isStep1Valid() {
        FormValidator validator = new FormValidator();
        validator.validateRequiredDatePicker(saleDatePicker, "تاريخ البيع")
                 .validateRequiredComboBox(customerComboBox, "العميل")
                 .validateRequiredComboBox(cropComboBox, "المحصول")
                 .validateRequiredTextField(invoiceNumberField, "رقم الفاتورة");
        if (validator.hasErrors()) {
            ErrorHandler.showError("حقول غير صالحة", validator.getErrorMessage());
            return false;
        }
        return true;
    }

    private boolean isStep2Valid() {
        FormValidator validator = new FormValidator();
        validator.validateRequiredTextField(quantityKgField, "الكمية")
                 .validatePositiveNumericField(quantityKgField, "الكمية")
                 .validateRequiredTextField(unitPriceField, "سعر الوحدة")
                 .validatePositiveNumericField(unitPriceField, "سعر الوحدة")
                 .validateRequiredComboBox(pricingUnitComboBox, "وحدة التسعير");
        if (validator.hasErrors()) {
            ErrorHandler.showError("حقول غير صالحة", validator.getErrorMessage());
            return false;
        }
        return true;
    }

    private boolean isStep3Valid() {
        FormValidator validator = new FormValidator();
        validator.validateRequiredTextField(amountReceivedField, "المبلغ المقبوض")
                 .validateNumericField(amountReceivedField, "المبلغ المقبوض");
        validator.validateFieldNotGreaterThan(amountReceivedField, totalAmountField, "المبلغ المقبوض", "إجمالي الفاتورة");
        try {
            if (Double.parseDouble(amountReceivedField.getText()) > 0 && bankPaymentRadio.isSelected()) {
                validator.validateRequiredComboBox(bankAccountComboBox, "حساب البنك");
            }
        } catch (NumberFormatException e) { /* Handled by validateNumericField */ }
        if (validator.hasErrors()) {
            ErrorHandler.showError("حقول غير صالحة", validator.getErrorMessage());
            return false;
        }
        return true;
    }

    // --- Final Save and Dialog Control ---
    @FXML
    private void handleSave() {
        if (!isStep3Valid()) return; // Final validation before save

        sale.setSaleDate(saleDatePicker.getValue());
        sale.setCrop(cropComboBox.getValue());
        sale.setCustomer(customerComboBox.getValue());
        sale.setQuantitySoldKg(Double.parseDouble(quantityKgField.getText()));
        sale.setSellingPricingUnit(pricingUnitComboBox.getValue());
        sale.setSellingUnitPrice(Double.parseDouble(unitPriceField.getText()));
        sale.setTotalSaleAmount(Double.parseDouble(totalAmountField.getText()));
        sale.setSaleInvoiceNumber(invoiceNumberField.getText());
        if (cropComboBox.getValue() != null) {
            sale.setSpecificSellingFactor(cropComboBox.getValue().getFirstConversionFactor(pricingUnitComboBox.getValue()));
        }

        double amountReceived = Double.parseDouble(amountReceivedField.getText());
        FinancialAccount paymentAccount = null;
        if (amountReceived > 0) {
            if (cashPaymentRadio.isSelected()) {
                paymentAccount = new FinancialAccount();
                paymentAccount.setAccountId(10101); // Main Treasury Account
            } else {
                paymentAccount = bankAccountComboBox.getValue();
            }
        }

        try {
            saleDataService.addSale(sale, paymentAccount, amountReceived);
            okClicked = true;
            dialogStage.close();
        } catch (SQLException e) {
            ErrorHandler.showException("خطأ في الحفظ", "فشل حفظ بيانات البيع.", e);
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    @Override
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    @Override
    public boolean isOkClicked() {
        return okClicked;
    }

    // setSale is kept for potential future use (e.g., editing a sale)
    public void setSale(SaleRecord sale) {
        this.sale = sale;
        // This part would need to be adapted for the wizard if editing is implemented
    }
}
