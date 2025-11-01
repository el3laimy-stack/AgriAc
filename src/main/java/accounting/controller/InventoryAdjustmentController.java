package accounting.controller;

import accounting.model.Crop;
import accounting.model.InventoryAdjustment;
import accounting.service.CropDataService;
import accounting.util.ErrorHandler;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.sql.SQLException;
import java.time.LocalDate;

public class InventoryAdjustmentController {

    @FXML private ComboBox<Crop> cropComboBox;
    @FXML private DatePicker adjustmentDatePicker;
    @FXML private ComboBox<InventoryAdjustment.AdjustmentType> adjustmentTypeComboBox;
    @FXML private TextField quantityField;
    @FXML private TextArea reasonArea;

    private Stage dialogStage;
    private CropDataService cropDataService;
    private boolean saved = false;

    @FXML
    private void initialize() {
        this.cropDataService = new CropDataService();
        loadInitialData();
        adjustmentDatePicker.setValue(LocalDate.now());
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public boolean isSaved() {
        return saved;
    }

    private void loadInitialData() {
        try {
            cropComboBox.setItems(FXCollections.observableArrayList(cropDataService.getAllActiveCrops()));
            adjustmentTypeComboBox.setItems(FXCollections.observableArrayList(InventoryAdjustment.AdjustmentType.values()));
        } catch (SQLException e) {
            ErrorHandler.showException("خطأ في قاعدة البيانات", "فشل تحميل قائمة المحاصيل.", e);
        }
    }

    @FXML
    private void handleSave() {
        if (isInputValid()) {
            InventoryAdjustment adjustment = new InventoryAdjustment();
            adjustment.setCrop(cropComboBox.getValue());
            adjustment.setAdjustmentDate(adjustmentDatePicker.getValue());
            adjustment.setAdjustmentType(adjustmentTypeComboBox.getValue());
            adjustment.setQuantityKg(Math.abs(Double.parseDouble(quantityField.getText()))); // نأخذ القيمة المطلقة دائماً
            adjustment.setReason(reasonArea.getText());

            try {
                cropDataService.addInventoryAdjustment(adjustment);
                saved = true;
                showInfoAlert("نجاح", "تم تسجيل تسوية المخزون بنجاح.");
                dialogStage.close();
            } catch (SQLException e) {
                ErrorHandler.showException("خطأ في الحفظ", "فشل تسجيل التسوية.", e);
            }
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    private boolean isInputValid() {
        String errorMessage = "";
        if (cropComboBox.getValue() == null) errorMessage += "يجب اختيار المحصول.\n";
        if (adjustmentDatePicker.getValue() == null) errorMessage += "يجب تحديد تاريخ التسوية.\n";
        if (adjustmentTypeComboBox.getValue() == null) errorMessage += "يجب تحديد نوع التسوية.\n";
        
        try {
            double quantity = Double.parseDouble(quantityField.getText());
            if (quantity <= 0) {
                errorMessage += "الكمية يجب أن تكون أكبر من صفر.\n";
            }
        } catch (NumberFormatException e) {
            errorMessage += "الكمية يجب أن تكون رقماً صحيحاً.\n";
        }

        if (errorMessage.isEmpty()) {
            return true;
        } else {
            ErrorHandler.showError("حقول غير صالحة", errorMessage);
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
    
    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(dialogStage);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
