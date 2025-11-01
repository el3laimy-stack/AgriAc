package accounting.controller;

import accounting.model.Contact;
import accounting.model.Crop;
import accounting.model.FinancialAccount;
import accounting.model.PurchaseRecord;
import accounting.service.ContactDataService;
import accounting.service.CropDataService;
import accounting.util.ErrorHandler;
import accounting.service.FinancialAccountDataService;
import accounting.util.FormValidator;
import accounting.service.PurchaseDataService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.List;

public class PurchaseFormController implements BaseFormController {

    // Common fields
    @FXML private DatePicker purchaseDatePicker;
    @FXML private ComboBox<Crop> cropComboBox;
    @FXML private ComboBox<Contact> supplierComboBox;
    @FXML private TextField quantityKgField;
    @FXML private ComboBox<String> pricingUnitComboBox;
    @FXML private TextField unitPriceField;
    @FXML private TextField totalCostField;
    @FXML private TextField invoiceNumberField;
    @FXML private TextField amountPaidField;
    @FXML private RadioButton cashPaymentRadio;
    @FXML private RadioButton bankPaymentRadio;
    @FXML private Label bankAccountLabel;
    @FXML private ComboBox<FinancialAccount> bankAccountComboBox;

    // New FXML fields for the wizard
    @FXML private VBox step1Indicator, step2Indicator, step3Indicator;
    @FXML private VBox step1Pane, step2Pane, step3Pane;
    @FXML private Button previousButton, nextButton, saveButton;

    // Summary labels
    @FXML private Label summarySupplierLabel, summaryCropLabel, summaryQuantityLabel, summaryTotalLabel;

    private Stage dialogStage;
    private PurchaseRecord purchase;
    private boolean okClicked = false;
    private int currentStep = 1;

    private PurchaseDataService purchaseDataService;
    private CropDataService cropDataService;
    private ContactDataService contactDataService;
    private FinancialAccountDataService accountDataService;
    private ToggleGroup paymentMethodToggleGroup;

    @FXML
    private void initialize() {
        this.purchase = new PurchaseRecord();
        this.purchaseDataService = new PurchaseDataService();
        this.cropDataService = new CropDataService();
        this.contactDataService = new ContactDataService();
        this.accountDataService = new FinancialAccountDataService();

        setupPaymentControls();
        updateStepView();
        loadComboBoxData();

        // Add listeners for automatic calculation
        quantityKgField.textProperty().addListener((obs, oldVal, newVal) -> calculateTotalCost());
        unitPriceField.textProperty().addListener((obs, oldVal, newVal) -> calculateTotalCost());
        pricingUnitComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> calculateTotalCost());
        cropComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> onCropSelected(newVal));
    }

    private void updateStepView() {
        step1Pane.setVisible(currentStep == 1);
        step2Pane.setVisible(currentStep == 2);
        step3Pane.setVisible(currentStep == 3);

        updateIndicatorStyle(step1Indicator, 1);
        updateIndicatorStyle(step2Indicator, 2);
        updateIndicatorStyle(step3Indicator, 3);

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
        summarySupplierLabel.setText(supplierComboBox.getValue() != null ? supplierComboBox.getValue().toString() : "-");
        summaryCropLabel.setText(cropComboBox.getValue() != null ? cropComboBox.getValue().toString() : "-");
        summaryQuantityLabel.setText(quantityKgField.getText() + " كجم");
        summaryTotalLabel.setText(totalCostField.getText() + " ج.م");
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
            List<Contact> suppliers = contactDataService.getAllContacts().stream().filter(Contact::isSupplier).toList();
            supplierComboBox.setItems(FXCollections.observableArrayList(suppliers));
            bankAccountComboBox.setItems(FXCollections.observableArrayList(accountDataService.getBankAccounts()));
        } catch (SQLException e) {
            ErrorHandler.showException("خطأ", "فشل تحميل البيانات الأساسية.", e);
        }
    }

    private void onCropSelected(Crop selectedCrop) {
        if (selectedCrop != null) {
            pricingUnitComboBox.setItems(FXCollections.observableArrayList(selectedCrop.getAllowedPricingUnits()));
            pricingUnitComboBox.getSelectionModel().selectFirst();
        } else {
            pricingUnitComboBox.getItems().clear();
        }
    }

    private void calculateTotalCost() {
        try {
            double quantityKg = Double.parseDouble(quantityKgField.getText());
            double unitPrice = Double.parseDouble(unitPriceField.getText());
            String selectedUnit = pricingUnitComboBox.getValue();
            Crop selectedCrop = cropComboBox.getValue();
            if (selectedUnit == null || selectedCrop == null) return;

            double conversionFactor = selectedCrop.getFirstConversionFactor(selectedUnit);
            if (conversionFactor <= 0) conversionFactor = 1.0;

            double totalCost = (quantityKg / conversionFactor) * unitPrice;
            totalCostField.setText(new DecimalFormat("#.##").format(totalCost));
        } catch (NumberFormatException e) {
            totalCostField.clear();
        }
    }

    // --- Validation per Step ---
    private boolean isStep1Valid() {
        FormValidator validator = new FormValidator();
        validator.validateRequiredDatePicker(purchaseDatePicker, "تاريخ الشراء")
                 .validateRequiredComboBox(supplierComboBox, "المورد")
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
        validator.validateRequiredTextField(amountPaidField, "المبلغ المدفوع")
                 .validateNumericField(amountPaidField, "المبلغ المدفوع");
        validator.validateFieldNotGreaterThan(amountPaidField, totalCostField, "المبلغ المدفوع", "إجمالي التكلفة");
        try {
            if (Double.parseDouble(amountPaidField.getText()) > 0 && bankPaymentRadio.isSelected()) {
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
        if (!isStep3Valid()) return; // Final validation

        purchase.setPurchaseDate(purchaseDatePicker.getValue());
        purchase.setCrop(cropComboBox.getValue());
        purchase.setSupplier(supplierComboBox.getValue());
        purchase.setQuantityKg(Double.parseDouble(quantityKgField.getText()));
        purchase.setPricingUnit(pricingUnitComboBox.getValue());
        purchase.setUnitPrice(Double.parseDouble(unitPriceField.getText()));
        purchase.setTotalCost(Double.parseDouble(totalCostField.getText()));
        purchase.setInvoiceNumber(invoiceNumberField.getText());
        if (cropComboBox.getValue() != null) {
            purchase.setSpecificFactor(cropComboBox.getValue().getFirstConversionFactor(pricingUnitComboBox.getValue()));
        }

        double amountPaid = Double.parseDouble(amountPaidField.getText());
        FinancialAccount paymentAccount = null;
        if (amountPaid > 0) {
            if (cashPaymentRadio.isSelected()) {
                paymentAccount = new FinancialAccount();
                paymentAccount.setAccountId(10101); // Main Treasury Account
            } else {
                paymentAccount = bankAccountComboBox.getValue();
            }
        }

        try {
            purchaseDataService.addPurchase(purchase, paymentAccount, amountPaid);
            okClicked = true;
            dialogStage.close();
        } catch (SQLException e) {
            ErrorHandler.showException("خطأ في الحفظ", "فشل حفظ بيانات الشراء.", e);
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

    public void setPurchase(PurchaseRecord purchase) {
        this.purchase = purchase;
        // This part would need to be adapted for the wizard if editing is implemented
    }
}