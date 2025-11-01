package accounting.controller;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import accounting.model.Crop;
import accounting.model.DailyPrice;
import accounting.service.CropDataService;
import accounting.service.DailyPriceService;
import accounting.util.ErrorHandler;
import accounting.formatter.FormatUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

/**
 * تحكم في واجهة إدارة الأسعار اليومية للمحاصيل
 * مخصص لشركات تجارة المحاصيل الزراعية
 */
public class DailyPriceController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(DailyPriceController.class.getName());

    // جدول الأسعار اليومية
    @FXML private TableView<DailyPrice> pricesTable;
    @FXML private TableColumn<DailyPrice, String> cropNameColumn;
    @FXML private TableColumn<DailyPrice, LocalDate> dateColumn;
    @FXML private TableColumn<DailyPrice, String> openingPriceColumn;
    @FXML private TableColumn<DailyPrice, String> highPriceColumn;
    @FXML private TableColumn<DailyPrice, String> lowPriceColumn;
    @FXML private TableColumn<DailyPrice, String> closingPriceColumn;
    @FXML private TableColumn<DailyPrice, String> changeColumn;
    @FXML private TableColumn<DailyPrice, String> changePercentColumn;
    @FXML private TableColumn<DailyPrice, String> conditionColumn;

    // نموذج الإدخال
    @FXML private GridPane inputForm;
    @FXML private ComboBox<Crop> cropComboBox;
    @FXML private DatePicker datePicker;
    @FXML private TextField openingPriceField;
    @FXML private TextField highPriceField;
    @FXML private TextField lowPriceField;
    @FXML private TextField closingPriceField;
    @FXML private TextField volumeField;
    @FXML private TextArea notesArea;

    // عناصر التحكم
    @FXML private Button addButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;
    @FXML private Button refreshButton;
    @FXML private Button autoUpdateButton;
    @FXML private ProgressIndicator progressIndicator;

    // فلاتر البحث
    @FXML private ComboBox<Crop> filterCropComboBox;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private Button filterButton;
    @FXML private Button clearFilterButton;

    // معلومات إحصائية
    @FXML private Label avgPriceLabel;
    @FXML private Label minPriceLabel;
    @FXML private Label maxPriceLabel;
    @FXML private Label volatilityLabel;

    private DailyPriceService dailyPriceService;
    private CropDataService cropDataService;
    private ObservableList<DailyPrice> pricesList;
    private DailyPrice currentPrice;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.dailyPriceService = new DailyPriceService();
        this.cropDataService = new CropDataService();
        this.pricesList = FXCollections.observableArrayList();

        setupTable();
        setupForm();
        setupEventHandlers();
        loadData();
    }

    private void setupTable() {
        pricesTable.setItems(pricesList);

        cropNameColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getCrop().getCropName()));
        
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("priceDate"));
        
        openingPriceColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(FormatUtils.formatCurrency(cellData.getValue().getOpeningPrice())));
        
        highPriceColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(FormatUtils.formatCurrency(cellData.getValue().getHighPrice())));
        
        lowPriceColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(FormatUtils.formatCurrency(cellData.getValue().getLowPrice())));
        
        closingPriceColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(FormatUtils.formatCurrency(cellData.getValue().getClosingPrice())));
        
        changeColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(FormatUtils.formatCurrency(cellData.getValue().getPriceChange())));
        
        changePercentColumn.setCellValueFactory(cellData -> {
            double percent = cellData.getValue().getPriceChangePercentage();
            return new SimpleStringProperty(String.format("%.2f%%", percent));
        });

        conditionColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getMarketCondition()));

        // تلوين صف حسب اتجاه السعر
        pricesTable.setRowFactory(tv -> {
            TableRow<DailyPrice> row = new TableRow<>();
            row.itemProperty().addListener((obs, oldItem, newItem) -> {
                if (newItem == null) {
                    row.setStyle("");
                } else {
                    double change = newItem.getPriceChange();
                    if (change > 0) {
                        row.setStyle("-fx-background-color: #e8f5e8;"); // أخضر فاتح للارتفاع
                    } else if (change < 0) {
                        row.setStyle("-fx-background-color: #ffe8e8;"); // أحمر فاتح للانخفاض
                    } else {
                        row.setStyle("-fx-background-color: #f0f0f0;"); // رمادي للاستقرار
                    }
                }
            });
            return row;
        });

        // التحديد في الجدول
        pricesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            currentPrice = newSelection;
            populateForm(newSelection);
            updateButton.setDisable(newSelection == null);
            deleteButton.setDisable(newSelection == null);
        });
    }

    private void setupForm() {
        datePicker.setValue(LocalDate.now());
        
        // تحميل قائمة المحاصيل
        loadCrops();

        // تحديد قائمة المحاصيل في الفلاتر
        filterCropComboBox.setItems(cropComboBox.getItems());

        // تحديد التواريخ الافتراضية للفلتر
        fromDatePicker.setValue(LocalDate.now().minusDays(30));
        toDatePicker.setValue(LocalDate.now());

        // تعطيل زر التحديث في البداية
        updateButton.setDisable(true);
        deleteButton.setDisable(true);
    }

    private void setupEventHandlers() {
        // تحديث تلقائي للحقول
        openingPriceField.textProperty().addListener((obs, oldVal, newVal) -> validatePriceInput());
        highPriceField.textProperty().addListener((obs, oldVal, newVal) -> validatePriceInput());
        lowPriceField.textProperty().addListener((obs, oldVal, newVal) -> validatePriceInput());
        closingPriceField.textProperty().addListener((obs, oldVal, newVal) -> validatePriceInput());
    }

    private void loadCrops() {
        try {
            List<Crop> crops = cropDataService.getAllActiveCrops();
            cropComboBox.setItems(FXCollections.observableArrayList(crops));
            filterCropComboBox.setItems(FXCollections.observableArrayList(crops));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load crops", e);
            ErrorHandler.showException("خطأ في تحميل البيانات", "فشل تحميل قائمة المحاصيل.", e);
        }
    }

    private void loadData() {
        Task<List<DailyPrice>> loadTask = new Task<>() {
            @Override
            protected List<DailyPrice> call() throws Exception {
                // تحميل آخر 30 يوم لجميع المحاصيل
                return dailyPriceService.getPricesForCrop(0, 
                    LocalDate.now().minusDays(30), LocalDate.now());
            }
        };

        loadTask.setOnSucceeded(e -> {
            pricesList.setAll(loadTask.getValue());
            updateStatistics();
        });

        loadTask.setOnFailed(e -> {
            Exception exception = (Exception) loadTask.getException();
            LOGGER.log(Level.SEVERE, "Failed to load daily prices", exception);
            ErrorHandler.showException("خطأ في تحميل البيانات", "فشل تحميل الأسعار اليومية.", exception);
        });

        progressIndicator.visibleProperty().bind(loadTask.runningProperty());
        new Thread(loadTask).start();
    }

    @FXML
    private void handleAdd() {
        if (!validateInput()) return;

        DailyPrice newPrice = createPriceFromForm();
        
        try {
            dailyPriceService.addOrUpdatePrice(newPrice);
            showSuccessAlert("نجح الحفظ", "تم إضافة السعر اليومي بنجاح.");
            clearForm();
            applyCurrentFilter();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to add daily price", e);
            ErrorHandler.showException("خطأ في الحفظ", "فشل إضافة السعر اليومي.", e);
        }
    }

    @FXML
    private void handleUpdate() {
        if (currentPrice == null || !validateInput()) return;

        updatePriceFromForm(currentPrice);
        
        try {
            dailyPriceService.addOrUpdatePrice(currentPrice);
            showSuccessAlert("نجح التحديث", "تم تحديث السعر اليومي بنجاح.");
            applyCurrentFilter();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to update daily price", e);
            ErrorHandler.showException("خطأ في التحديث", "فشل تحديث السعر اليومي.", e);
        }
    }

    @FXML
    private void handleDelete() {
        if (currentPrice == null) return;

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("تأكيد الحذف");
        confirmation.setHeaderText("حذف السعر اليومي");
        confirmation.setContentText("هل أنت متأكد من رغبتك في حذف هذا السعر؟");

        if (confirmation.showAndWait().get() == ButtonType.OK) {
            try {
                // TODO: إضافة method للحذف في DailyPriceService
                showSuccessAlert("نجح الحذف", "تم حذف السعر اليومي بنجاح.");
                clearForm();
                applyCurrentFilter();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to delete daily price", e);
                ErrorHandler.showException("خطأ في الحذف", "فشل حذف السعر اليومي.", e);
            }
        }
    }

    @FXML
    private void handleAutoUpdate() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("تحديث تلقائي");
        confirmation.setHeaderText("تحديث الأسعار من المعاملات");
        confirmation.setContentText("سيتم تحديث أسعار اليوم تلقائياً من معاملات الشراء والبيع. هل تريد المتابعة؟");

        if (confirmation.showAndWait().get() == ButtonType.OK) {
            Task<Void> updateTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    dailyPriceService.updateDailyPricesFromTransactions(LocalDate.now());
                    return null;
                }
            };

            updateTask.setOnSucceeded(e -> {
                showSuccessAlert("نجح التحديث", "تم تحديث الأسعار تلقائياً من المعاملات.");
                applyCurrentFilter();
            });

            updateTask.setOnFailed(e -> {
                Exception exception = (Exception) updateTask.getException();
                LOGGER.log(Level.SEVERE, "Failed to auto-update prices", exception);
                ErrorHandler.showException("خطأ في التحديث", "فشل التحديث التلقائي للأسعار.", exception);
            });

            progressIndicator.visibleProperty().bind(updateTask.runningProperty());
            new Thread(updateTask).start();
        }
    }

    @FXML
    private void handleFilter() {
        applyCurrentFilter();
    }

    @FXML
    private void handleClearFilter() {
        filterCropComboBox.setValue(null);
        fromDatePicker.setValue(LocalDate.now().minusDays(30));
        toDatePicker.setValue(LocalDate.now());
        applyCurrentFilter();
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }

    private void applyCurrentFilter() {
        Crop selectedCrop = filterCropComboBox.getValue();
        LocalDate fromDate = fromDatePicker.getValue();
        LocalDate toDate = toDatePicker.getValue();

        Task<List<DailyPrice>> filterTask = new Task<>() {
            @Override
            protected List<DailyPrice> call() throws Exception {
                int cropId = selectedCrop != null ? selectedCrop.getCropId() : 0;
                return dailyPriceService.getPricesForCrop(cropId, fromDate, toDate);
            }
        };

        filterTask.setOnSucceeded(e -> {
            pricesList.setAll(filterTask.getValue());
            updateStatistics();
        });

        filterTask.setOnFailed(e -> {
            Exception exception = (Exception) filterTask.getException();
            LOGGER.log(Level.SEVERE, "Failed to filter prices", exception);
            ErrorHandler.showException("خطأ في البحث", "فشل تطبيق المرشح.", exception);
        });

        progressIndicator.visibleProperty().bind(filterTask.runningProperty());
        new Thread(filterTask).start();
    }

    private void updateStatistics() {
        if (pricesList.isEmpty()) {
            avgPriceLabel.setText("0.00");
            minPriceLabel.setText("0.00");
            maxPriceLabel.setText("0.00");
            volatilityLabel.setText("0.00%%");
            return;
        }

        double sum = 0;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;

        for (DailyPrice price : pricesList) {
            double avg = price.getAveragePrice();
            sum += avg;
            min = Math.min(min, avg);
            max = Math.max(max, avg);
        }

        double average = sum / pricesList.size();
        double variance = pricesList.stream()
            .mapToDouble(p -> Math.pow(p.getAveragePrice() - average, 2))
            .average().orElse(0);
        double volatility = (Math.sqrt(variance) / average) * 100;

        avgPriceLabel.setText(FormatUtils.formatCurrency(average));
        minPriceLabel.setText(FormatUtils.formatCurrency(min));
        maxPriceLabel.setText(FormatUtils.formatCurrency(max));
        volatilityLabel.setText(String.format("%.2f%%", volatility));
    }

    private DailyPrice createPriceFromForm() {
        DailyPrice price = new DailyPrice();
        updatePriceFromForm(price);
        return price;
    }

    private void updatePriceFromForm(DailyPrice price) {
        price.setCrop(cropComboBox.getValue());
        price.setPriceDate(datePicker.getValue());
        price.setOpeningPrice(Double.parseDouble(openingPriceField.getText()));
        price.setHighPrice(Double.parseDouble(highPriceField.getText()));
        price.setLowPrice(Double.parseDouble(lowPriceField.getText()));
        price.setClosingPrice(Double.parseDouble(closingPriceField.getText()));
        price.setTradingVolume(volumeField.getText().isEmpty() ? 0 : Double.parseDouble(volumeField.getText()));
        price.setNotes(notesArea.getText());
    }

    private void populateForm(DailyPrice price) {
        if (price == null) {
            clearForm();
            return;
        }

        cropComboBox.setValue(price.getCrop());
        datePicker.setValue(price.getPriceDate());
        openingPriceField.setText(String.valueOf(price.getOpeningPrice()));
        highPriceField.setText(String.valueOf(price.getHighPrice()));
        lowPriceField.setText(String.valueOf(price.getLowPrice()));
        closingPriceField.setText(String.valueOf(price.getClosingPrice()));
        volumeField.setText(String.valueOf(price.getTradingVolume()));
        notesArea.setText(price.getNotes());
    }

    private void clearForm() {
        cropComboBox.setValue(null);
        datePicker.setValue(LocalDate.now());
        openingPriceField.clear();
        highPriceField.clear();
        lowPriceField.clear();
        closingPriceField.clear();
        volumeField.clear();
        notesArea.clear();
        currentPrice = null;
        updateButton.setDisable(true);
        deleteButton.setDisable(true);
    }

    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();

        if (cropComboBox.getValue() == null) {
            errors.append("• يجب اختيار المحصول\n");
        }

        if (datePicker.getValue() == null) {
            errors.append("• يجب تحديد التاريخ\n");
        }

        try {
            double opening = Double.parseDouble(openingPriceField.getText());
            double high = Double.parseDouble(highPriceField.getText());
            double low = Double.parseDouble(lowPriceField.getText());
            double closing = Double.parseDouble(closingPriceField.getText());

            if (opening <= 0 || high <= 0 || low <= 0 || closing <= 0) {
                errors.append("• جميع الأسعار يجب أن تكون أكبر من صفر\n");
            }

            if (high < low) {
                errors.append("• أعلى سعر يجب أن يكون أكبر من أو يساوي أقل سعر\n");
            }

            if (high < opening || high < closing) {
                errors.append("• أعلى سعر يجب أن يكون أكبر من أو يساوي سعر الافتتاح والإغلاق\n");
            }

            if (low > opening || low > closing) {
                errors.append("• أقل سعر يجب أن يكون أقل من أو يساوي سعر الافتتاح والإغلاق\n");
            }

        } catch (NumberFormatException e) {
            errors.append("• يجب إدخال أرقام صحيحة في جميع حقول الأسعار\n");
        }

        if (!volumeField.getText().isEmpty()) {
            try {
                double volume = Double.parseDouble(volumeField.getText());
                if (volume < 0) {
                    errors.append("• حجم التداول يجب أن يكون موجباً أو صفر\n");
                }
            } catch (NumberFormatException e) {
                errors.append("• حجم التداول يجب أن يكون رقماً صحيحاً\n");
            }
        }

        if (errors.length() > 0) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("خطأ في البيانات");
            alert.setHeaderText("يرجى تصحيح الأخطاء التالية:");
            alert.setContentText(errors.toString());
            alert.showAndWait();
            return false;
        }

        return true;
    }

    private void validatePriceInput() {
        // التحقق الفوري من صحة الأسعار أثناء الإدخال
        try {
            if (!openingPriceField.getText().isEmpty() && !highPriceField.getText().isEmpty() && 
                !lowPriceField.getText().isEmpty() && !closingPriceField.getText().isEmpty()) {
                
                double opening = Double.parseDouble(openingPriceField.getText());
                double high = Double.parseDouble(highPriceField.getText());
                double low = Double.parseDouble(lowPriceField.getText());
                double closing = Double.parseDouble(closingPriceField.getText());

                // تلوين الحقول حسب الصحة
                openingPriceField.setStyle(opening > 0 ? "" : "-fx-border-color: red;");
                highPriceField.setStyle((high > 0 && high >= low) ? "" : "-fx-border-color: red;");
                lowPriceField.setStyle((low > 0 && low <= high) ? "" : "-fx-border-color: red;");
                closingPriceField.setStyle(closing > 0 ? "" : "-fx-border-color: red;");
            }
        } catch (NumberFormatException ignored) {
            // تجاهل أخطاء التحويل أثناء الكتابة
        }
    }

    private void showSuccessAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
