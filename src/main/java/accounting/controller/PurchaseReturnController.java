package accounting.controller;

import accounting.model.PurchaseRecord;
import accounting.model.PurchaseReturn;
import accounting.util.ErrorHandler;
import accounting.formatter.FormatUtils;
import accounting.service.PurchaseDataService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.sql.SQLException;
import java.time.LocalDate;

public class PurchaseReturnController {

    // --- تم حذف ComboBox وإضافة Label ---
    @FXML private Label purchaseInfoLabel;
    
    @FXML private DatePicker returnDatePicker;
    @FXML private TextField quantityField;
    @FXML private TextArea reasonArea;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private Stage dialogStage;
    private PurchaseDataService purchaseDataService;
    private PurchaseRecord purchaseToReturn; // متغير لتخزين الفاتورة الممررة
    private boolean saved = false;

    @FXML
    private void initialize() {
        this.purchaseDataService = new PurchaseDataService();
        returnDatePicker.setValue(LocalDate.now());
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    /**
     * دالة جديدة لاستقبال الفاتورة المحددة من وحدة التحكم السابقة.
     * @param purchaseRecord الفاتورة التي تم النقر عليها.
     */
    public void setPurchaseToReturn(PurchaseRecord purchaseRecord) {
        this.purchaseToReturn = purchaseRecord;
        // عرض معلومات الفاتورة للمستخدم للتأكيد
        if (purchaseRecord != null) {
            purchaseInfoLabel.setText("مرتجع من فاتورة #" + purchaseRecord.getInvoiceNumber() 
                + " للمورد: " + purchaseRecord.getSupplier().getName());
        }
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void handleSave() {
        if (isInputValid()) {
            PurchaseReturn newReturn = new PurchaseReturn();
            newReturn.setOriginalPurchase(this.purchaseToReturn); // استخدام المتغير الجديد
            newReturn.setReturnDate(returnDatePicker.getValue());
            newReturn.setQuantityKg(Double.parseDouble(quantityField.getText()));
            newReturn.setReturnReason(reasonArea.getText());
            
            // حساب تكلفة البضاعة المرتجعة بناءً على السعر الأصلي
            double originalUnitPrice = newReturn.getOriginalPurchase().getTotalCost() / newReturn.getOriginalPurchase().getQuantityKg();
            newReturn.setReturnedCost(newReturn.getQuantityKg() * originalUnitPrice);

            try {
                purchaseDataService.addPurchaseReturn(newReturn);
                saved = true;
                showInfoAlert("نجاح", "تم تسجيل مرتجع الشراء بنجاح.");
                dialogStage.close();
            } catch (SQLException e) {
                ErrorHandler.showException("خطأ في الحفظ", "فشل تسجيل مرتجع الشراء.", e);
            }
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    private boolean isInputValid() {
        String errorMessage = "";
        
        if (this.purchaseToReturn == null) {
            errorMessage += "لم يتم تحديد الفاتورة الأصلية بشكل صحيح.\n";
        }
        if (returnDatePicker.getValue() == null) {
            errorMessage += "يجب تحديد تاريخ المرتجع.\n";
        }
        
        try {
            double quantity = Double.parseDouble(quantityField.getText());
            if (quantity <= 0) {
                errorMessage += "الكمية المرتجعة يجب أن تكون أكبر من صفر.\n";
            }
            if (this.purchaseToReturn != null && quantity > this.purchaseToReturn.getQuantityKg()) {
                errorMessage += "الكمية المرتجعة لا يمكن أن تكون أكبر من الكمية الأصلية في الفاتورة.\n";
            }
        } catch (NumberFormatException e) {
            errorMessage += "الكمية المرتجعة يجب أن تكون رقماً صحيحاً.\n";
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