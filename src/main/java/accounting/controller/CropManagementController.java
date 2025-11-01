package accounting.controller;

import accounting.model.Crop;
import accounting.service.CropDataService;
import accounting.util.ErrorHandler;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class CropManagementController implements javafx.fxml.Initializable {

    @FXML private TableView<Crop> cropTable;
    @FXML private TableColumn<Crop, String> cropNameColumn;
    @FXML private TableColumn<Crop, String> pricingUnitsColumn;
    @FXML private TableColumn<Crop, Void> actionsColumn;

    @FXML private TextField searchField;

    private CropDataService cropDataService;
    private ObservableList<Crop> cropList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.cropDataService = new CropDataService();
        setupTable();
        setupSearchFilter();
        loadCrops();
    }

    private void setupTable() {
        cropNameColumn.setCellValueFactory(new PropertyValueFactory<>("cropName"));
        pricingUnitsColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(String.join(", ", cellData.getValue().getAllowedPricingUnits()))
        );
        addActionsToTable();
    }

    private void setupSearchFilter() {
        FilteredList<Crop> filteredData = new FilteredList<>(cropList, p -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(crop -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return crop.getCropName().toLowerCase().contains(lowerCaseFilter);
            });
        });
        cropTable.setItems(filteredData);
    }

    private void loadCrops() {
        Task<List<Crop>> loadDataTask = new Task<>() {
            @Override
            protected List<Crop> call() throws Exception {
                return cropDataService.getAllActiveCrops();
            }
        };
        loadDataTask.setOnSucceeded(e -> cropList.setAll(loadDataTask.getValue()));
        loadDataTask.setOnFailed(e -> ErrorHandler.showException("خطأ", "فشل تحميل قائمة المحاصيل.", (Exception) loadDataTask.getException()));
        new Thread(loadDataTask).start();
    }

    private void addActionsToTable() {
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button("تعديل");
            private final Button deleteButton = new Button("حذف");
            private final HBox pane = new HBox(5, editButton, deleteButton);

            {
                editButton.getStyleClass().add("warning");
                deleteButton.getStyleClass().add("danger");

                editButton.setOnAction(event -> {
                    Crop crop = getTableView().getItems().get(getIndex());
                    handleEditCrop(crop);
                });
                deleteButton.setOnAction(event -> {
                    Crop crop = getTableView().getItems().get(getIndex());
                    handleDeleteCrop(crop);
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
    private void handleAddCrop() {
        showCropEditDialog(new Crop());
    }

    private void handleEditCrop(Crop crop) {
        showCropEditDialog(crop);
    }

    private void showCropEditDialog(Crop crop) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CropForm.fxml"));
            VBox page = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("تحرير بيانات المحصول");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.setScene(new Scene(page));

            CropFormController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setCrop(crop);

            dialogStage.showAndWait();

            if (controller.isOkClicked()) {
                loadCrops(); // Refresh list
            }
        } catch (IOException e) {
            ErrorHandler.showException("خطأ في التحميل", "فشل تحميل واجهة تحرير المحصول.", e);
        }
    }

    private void handleDeleteCrop(Crop crop) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("تأكيد الحذف");
        alert.setContentText("هل أنت متأكد من حذف: " + crop.getCropName() + "؟");
        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                cropDataService.deleteCrop(crop.getCropId());
                loadCrops();
            } catch (SQLException e) {
                ErrorHandler.showException("خطأ في الحذف", "فشل حذف المحصول.", e);
            }
        }
    }
}
