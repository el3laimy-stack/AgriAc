package accounting.controller;

import accounting.model.Contact;
import accounting.model.Crop;
import accounting.model.PurchaseRecord;
import accounting.service.ContactDataService;
import accounting.service.CropDataService;
import accounting.util.ErrorHandler;
import accounting.service.PurchaseDataService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class PurchaseHistoryController {

    @FXML private TableView<PurchaseRecord> purchasesTable;
    @FXML private TableColumn<PurchaseRecord, LocalDate> dateColumn;
    @FXML private TableColumn<PurchaseRecord, String> invoiceNumberColumn;
    @FXML private TableColumn<PurchaseRecord, String> supplierColumn;
    @FXML private TableColumn<PurchaseRecord, String> cropColumn;
    @FXML private TableColumn<PurchaseRecord, Double> unitPriceColumn;
    @FXML private TableColumn<PurchaseRecord, Double> totalAmountColumn;
    @FXML private TableColumn<PurchaseRecord, Double> amountPaidColumn;
    @FXML private TableColumn<PurchaseRecord, Double> balanceColumn;
    @FXML private TableColumn<PurchaseRecord, String> statusColumn;
    @FXML private TableColumn<PurchaseRecord, Void> actionsColumn;

    @FXML private TextField searchField;
    @FXML private DatePicker fromDate;
    @FXML private DatePicker toDate;
    @FXML private ComboBox<Contact> supplierFilterComboBox;
    @FXML private ComboBox<Crop> cropFilterComboBox;

    @FXML private Label totalPurchasesLabel, totalPaidLabel, totalBalanceLabel;

    private PurchaseDataService purchaseDataService;
    private ObservableList<PurchaseRecord> purchasesList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        this.purchaseDataService = new PurchaseDataService();
        setupTableColumns();
        loadFilters();
        loadPurchasesData();
    }

    private void setupTableColumns() {
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("purchaseDate"));
        invoiceNumberColumn.setCellValueFactory(new PropertyValueFactory<>("invoiceNumber"));
        supplierColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSupplier().getName()));
        cropColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCrop().getCropName()));
        unitPriceColumn.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        totalAmountColumn.setCellValueFactory(new PropertyValueFactory<>("totalCost"));
        amountPaidColumn.setCellValueFactory(new PropertyValueFactory<>("amountPaid"));
        balanceColumn.setCellValueFactory(new PropertyValueFactory<>("balance"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("paymentStatus"));

        statusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                getStyleClass().removeAll("status-paid", "status-partial", "status-unpaid");
                if (item != null && !empty) {
                    if (item.equals("مدفوع")) getStyleClass().add("status-paid");
                    else if (item.equals("مدفوع جزئياً")) getStyleClass().add("status-partial");
                    else getStyleClass().add("status-unpaid");
                }
            }
        });

        addActionsToTable();
    }

    private void loadFilters() {
        try {
            supplierFilterComboBox.setItems(FXCollections.observableArrayList(new ContactDataService().getAllContacts()));
            cropFilterComboBox.setItems(FXCollections.observableArrayList(new CropDataService().getAllActiveCrops()));
        } catch (SQLException e) {
            ErrorHandler.showException("خطأ", "فشل تحميل بيانات الفلاتر.", e);
        }
    }

    @FXML
    private void applyFilters() {
        loadPurchasesData();
    }

    @FXML
    private void clearFilters() {
        searchField.clear();
        fromDate.setValue(null);
        toDate.setValue(null);
        supplierFilterComboBox.getSelectionModel().clearSelection();
        cropFilterComboBox.getSelectionModel().clearSelection();
        loadPurchasesData();
    }

    private void loadPurchasesData() {
        try {
            List<PurchaseRecord> purchases = purchaseDataService.getPurchases(
                fromDate.getValue(),
                toDate.getValue(),
                cropFilterComboBox.getValue() != null ? cropFilterComboBox.getValue().getCropId() : null,
                supplierFilterComboBox.getValue() != null ? supplierFilterComboBox.getValue().getContactId() : null,
                searchField.getText()
            );
            purchasesList.setAll(purchases);
            purchasesTable.setItems(purchasesList);
            updateTotals();
        } catch (SQLException e) {
            ErrorHandler.showException("خطأ", "فشل تحميل سجل المشتريات.", e);
        }
    }

    private void updateTotals() {
        double totalPurchases = purchasesList.stream().mapToDouble(PurchaseRecord::getTotalCost).sum();
        double totalPaid = purchasesList.stream().mapToDouble(purchase -> purchase.getAmountPaid()).sum();
        double totalBalance = purchasesList.stream().mapToDouble(purchase -> purchase.getBalance()).sum();

        totalPurchasesLabel.setText(String.format("إجمالي المشتريات: %.2f", totalPurchases));
        totalPaidLabel.setText(String.format("إجمالي المدفوع: %.2f", totalPaid));
        totalBalanceLabel.setText(String.format("إجمالي المتبقي: %.2f", totalBalance));
    }

    private void addActionsToTable() {
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button viewButton = new Button("عرض");
            private final Button returnButton = new Button("مرتجع");
            private final HBox pane = new HBox(5, viewButton, returnButton);

            {
                viewButton.setOnAction(event -> {
                    // View logic here
                });
                returnButton.setOnAction(event -> {
                    // Return logic here
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    @FXML
    private void handleNewPurchase() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PurchaseForm.fxml"));
            Parent page = loader.load();
            page.setNodeOrientation(javafx.geometry.NodeOrientation.RIGHT_TO_LEFT);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("فاتورة شراء جديدة");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.setScene(new Scene(page));

            PurchaseFormController controller = loader.getController();
            controller.setDialogStage(dialogStage);

            dialogStage.showAndWait();

            if (controller.isOkClicked()) {
                loadPurchasesData();
            }

        } catch (IOException e) {
            ErrorHandler.showException("خطأ في التحميل", "فشل تحميل واجهة فاتورة الشراء.", e);
        }
    }
}
