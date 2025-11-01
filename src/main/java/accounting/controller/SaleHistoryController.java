package accounting.controller;

import accounting.model.Contact;
import accounting.model.Crop;
import accounting.model.SaleRecord;
import accounting.service.ContactDataService;
import accounting.service.CropDataService;
import accounting.util.ErrorHandler;
import accounting.service.SaleDataService;
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

public class SaleHistoryController {

    @FXML private TableView<SaleRecord> salesTable;
    @FXML private TableColumn<SaleRecord, LocalDate> dateColumn;
    @FXML private TableColumn<SaleRecord, String> invoiceNumberColumn;
    @FXML private TableColumn<SaleRecord, String> customerColumn;
    @FXML private TableColumn<SaleRecord, String> cropColumn;
    @FXML private TableColumn<SaleRecord, Double> sellingUnitPriceColumn;
    @FXML private TableColumn<SaleRecord, Double> totalAmountColumn;
    @FXML private TableColumn<SaleRecord, Double> amountPaidColumn;
    @FXML private TableColumn<SaleRecord, Double> balanceColumn;
    @FXML private TableColumn<SaleRecord, String> statusColumn;
    @FXML private TableColumn<SaleRecord, Void> actionsColumn;

    @FXML private TextField searchField;
    @FXML private DatePicker fromDate;
    @FXML private DatePicker toDate;
    @FXML private ComboBox<Contact> customerFilterComboBox;
    @FXML private ComboBox<Crop> cropFilterComboBox;

    @FXML private Label totalSalesLabel, totalPaidLabel, totalBalanceLabel;

    private SaleDataService saleDataService;
    private ObservableList<SaleRecord> salesList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        this.saleDataService = new SaleDataService();
        setupTableColumns();
        loadFilters();
        loadSalesData();
    }

    private void setupTableColumns() {
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("saleDate"));
        invoiceNumberColumn.setCellValueFactory(new PropertyValueFactory<>("saleInvoiceNumber"));
        customerColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCustomer().getName()));
        cropColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCrop().getCropName()));
        sellingUnitPriceColumn.setCellValueFactory(new PropertyValueFactory<>("sellingUnitPrice"));
        totalAmountColumn.setCellValueFactory(new PropertyValueFactory<>("totalSaleAmount"));
        amountPaidColumn.setCellValueFactory(new PropertyValueFactory<>("amountPaid"));
        balanceColumn.setCellValueFactory(new PropertyValueFactory<>("balance"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("paymentStatus"));

        // Add styling for status column
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
            customerFilterComboBox.setItems(FXCollections.observableArrayList(new ContactDataService().getAllContacts()));
            cropFilterComboBox.setItems(FXCollections.observableArrayList(new CropDataService().getAllActiveCrops()));
        } catch (SQLException e) {
            ErrorHandler.showException("خطأ", "فشل تحميل بيانات الفلاتر.", e);
        }
    }

    @FXML
    private void applyFilters() {
        loadSalesData();
    }

    @FXML
    private void clearFilters() {
        searchField.clear();
        fromDate.setValue(null);
        toDate.setValue(null);
        customerFilterComboBox.getSelectionModel().clearSelection();
        cropFilterComboBox.getSelectionModel().clearSelection();
        loadSalesData();
    }

    private void loadSalesData() {
        try {
            List<SaleRecord> sales = saleDataService.getSales(
                fromDate.getValue(),
                toDate.getValue(),
                cropFilterComboBox.getValue() != null ? cropFilterComboBox.getValue().getCropId() : null,
                customerFilterComboBox.getValue() != null ? customerFilterComboBox.getValue().getContactId() : null,
                searchField.getText()
            );
            salesList.setAll(sales);
            salesTable.setItems(salesList);
            updateTotals();
        } catch (SQLException e) {
            ErrorHandler.showException("خطأ", "فشل تحميل سجل المبيعات.", e);
        }
    }

    private void updateTotals() {
        double totalSales = salesList.stream().mapToDouble(SaleRecord::getTotalSaleAmount).sum();
        double totalPaid = salesList.stream().mapToDouble(sale -> sale.getAmountPaid()).sum();
        double totalBalance = salesList.stream().mapToDouble(sale -> sale.getBalance()).sum();

        totalSalesLabel.setText(String.format("إجمالي المبيعات: %.2f", totalSales));
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
    private void handleNewSale() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SaleForm.fxml"));
            Parent page = loader.load();
            page.setNodeOrientation(javafx.geometry.NodeOrientation.RIGHT_TO_LEFT);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("فاتورة بيع جديدة");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.setScene(new Scene(page));

            SaleFormController controller = loader.getController();
            controller.setDialogStage(dialogStage);

            dialogStage.showAndWait();

            if (controller.isOkClicked()) {
                loadSalesData();
            }

        } catch (IOException e) {
            ErrorHandler.showException("خطأ في التحميل", "فشل تحميل واجهة فاتورة البيع.", e);
        }
    }
}
