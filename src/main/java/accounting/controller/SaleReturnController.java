package accounting.controller;

import accounting.model.SaleRecord;
import accounting.model.SaleReturn;
import accounting.service.SaleDataService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.sql.SQLException;
import java.time.LocalDate;

public class SaleReturnController {

    @FXML private Label saleInfoLabel;
    @FXML private DatePicker returnDatePicker;
    @FXML private TextField quantityField;
    @FXML private TextArea reasonArea;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private Stage dialogStage;
    private SaleDataService saleDataService;
    private SaleRecord saleToReturn;
    private boolean saved = false;

    @FXML
    private void initialize() {
        this.saleDataService = new SaleDataService();
        returnDatePicker.setValue(LocalDate.now());
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public boolean isSaved() {
        return saved;
    }

    public void setSaleToReturn(SaleRecord saleRecord) {
        this.saleToReturn = saleRecord;
        saleInfoLabel.setText("مرتجع من فاتورة #" + saleRecord.getSaleInvoiceNumber()
            + " للعميل: " + saleRecord.getCustomer().getName());
    }

    @FXML
    private void handleSave() {
        if (isInputValid()) {
            SaleReturn newReturn = new SaleReturn();
            newReturn.setOriginalSale(this.saleToReturn);
            newReturn.setReturnDate(returnDatePicker.getValue());
            newReturn.setQuantityKg(Double.parseDouble(quantityField.getText()));
            newReturn.setReturnReason(reasonArea.getText());

            double originalUnitPrice = saleToReturn.getTotalSaleAmount() / saleToReturn.getQuantitySoldKg();
            newReturn.setRefundAmount(newReturn.getQuantityKg() * originalUnitPrice);

            try {
                saleDataService.addSaleReturn(newReturn);
                saved = true;
                showInfoAlert("نجاح", "تم تسجيل مرتجع المبيعات بنجاح.");
                dialogStage.close();
            } catch (SQLException e) {
                e.printStackTrace();
                showErrorAlert("خطأ في الحفظ", "فشل تسجيل مرتجع المبيعات.\n" + e.getMessage());
            }
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    private boolean isInputValid() {
        String errorMessage = "";
        if (this.saleToReturn == null) {
            errorMessage += "لم يتم تحديد فاتورة البيع الأصلية.\n";
        }
        if (returnDatePicker.getValue() == null) {
            errorMessage += "يجب تحديد تاريخ المرتجع.\n";
        }
        try {
            double quantity = Double.parseDouble(quantityField.getText());
            if (quantity <= 0) {
                errorMessage += "الكمية المرتجعة يجب أن تكون أكبر من صفر.\n";
            }
            if (this.saleToReturn != null && quantity > this.saleToReturn.getQuantitySoldKg()) {
                errorMessage += "الكمية المرتجعة لا يمكن أن تكون أكبر من الكمية الأصلية في الفاتورة.\n";
            }
        } catch (NumberFormatException e) {
            errorMessage += "الكمية المرتجعة يجب أن تكون رقماً صحيحاً.\n";
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
    
    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(dialogStage);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}