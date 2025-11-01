package accounting.controller;

import accounting.service.CropDataService;
import accounting.service.CropDataService.CropStatistics;
import accounting.util.ErrorHandler;
import accounting.formatter.FormatUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InventoryController implements javafx.fxml.Initializable {

    private static final Logger LOGGER = Logger.getLogger(InventoryController.class.getName());

    @FXML private FlowPane inventoryFlowPane;
    @FXML private TextField searchField;
    @FXML private Button refreshButton;
    @FXML private Label totalInventoryValueLabel;
    @FXML private Button addAdjustmentButton;

    private CropDataService cropDataService;
    private ObservableList<CropStatistics> inventoryList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.cropDataService = new CropDataService();
        loadInventoryData();
        refreshButton.setOnAction(e -> handleRefresh());
    }

    @FXML
    private void handleRefresh() {
        loadInventoryData();
    }

    private void loadInventoryData() {
        Task<List<CropStatistics>> loadDataTask = new Task<>() {
            @Override
            protected List<CropStatistics> call() throws Exception {
                return cropDataService.getAllCropStatistics();
            }
        };

        loadDataTask.setOnSucceeded(e -> {
            inventoryList.setAll(loadDataTask.getValue());
            setupSearchFilter();
            updateTotalValue();
        });

        loadDataTask.setOnFailed(e -> {
            ErrorHandler.showException("خطأ في تحميل البيانات", "لا يمكن تحميل بيانات المخزون.", (Exception) loadDataTask.getException());
        });

        new Thread(loadDataTask).start();
    }

    private void setupSearchFilter() {
        FilteredList<CropStatistics> filteredData = new FilteredList<>(inventoryList, p -> true);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(crop -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return crop.getCropName().toLowerCase().contains(lowerCaseFilter);
            });
        });

        // Bind the filtered data to the FlowPane
        updateFlowPane(filteredData);
        filteredData.addListener((javafx.beans.Observable observable) -> updateFlowPane(filteredData));
    }

    private void updateFlowPane(ObservableList<CropStatistics> data) {
        inventoryFlowPane.getChildren().clear();
        for (CropStatistics stats : data) {
            inventoryFlowPane.getChildren().add(createInventoryCard(stats));
        }
    }

    private VBox createInventoryCard(CropStatistics stats) {
        VBox card = new VBox(10);
        card.getStyleClass().add("inventory-card");

        // Title
        Label cropName = new Label(stats.getCropName());
        cropName.getStyleClass().add("inventory-card-title");

        // Stock Progress Bar
        double stockRatio = stats.getCurrentStock() / Math.max(stats.getCurrentStock(), 1000); // Assume 1000kg is a good stock level for now
        ProgressBar stockProgress = new ProgressBar(stockRatio);
        stockProgress.setMaxWidth(Double.MAX_VALUE);
        if (stockRatio < 0.1) {
            stockProgress.getStyleClass().add("progress-bar-danger");
        } else if (stockRatio < 0.4) {
            stockProgress.getStyleClass().add("progress-bar-warning");
        } else {
            stockProgress.getStyleClass().add("progress-bar-success");
        }

        // Metrics
        VBox metrics = new VBox(5);
        metrics.getChildren().addAll(
            createMetricRow("المخزون الحالي:", FormatUtils.formatQuantityWithUnit(stats.getCurrentStock(), "كجم")),
            createMetricRow("متوسط التكلفة:", FormatUtils.formatCurrency(stats.getAverageCost()) + " /كجم"),
            createMetricRow("القيمة الإجمالية:", FormatUtils.formatCurrency(stats.getInventoryValue()))
        );

        card.getChildren().addAll(cropName, stockProgress, metrics);
        return card;
    }

    private HBox createMetricRow(String title, String value) {
        HBox row = new HBox();
        Label titleLabel = new Label(title);
        Label valueLabel = new Label(value);
        HBox.setHgrow(row, javafx.scene.layout.Priority.ALWAYS);
        valueLabel.setAlignment(Pos.CENTER_RIGHT);
        valueLabel.setMaxWidth(Double.MAX_VALUE);
        row.getChildren().addAll(titleLabel, valueLabel);
        return row;
    }

    private void updateTotalValue() {
        double totalValue = inventoryList.stream().mapToDouble(CropStatistics::getInventoryValue).sum();
        totalInventoryValueLabel.setText("إجمالي قيمة المخزون: " + FormatUtils.formatCurrency(totalValue));
    }

    @FXML
    private void handleAddAdjustment() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/InventoryAdjustmentView.fxml"));
            VBox page = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("تسوية مخزون");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);

            InventoryAdjustmentController controller = loader.getController();
            controller.setDialogStage(dialogStage);

            dialogStage.showAndWait();

            if (controller.isSaved()) {
                loadInventoryData();
            }

        } catch (IOException e) {
            ErrorHandler.showException("خطأ في التحميل", "فشل تحميل واجهة تسوية المخزون.", e);
        }
    }
}