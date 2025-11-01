package accounting.controller;

import accounting.model.Crop;
import accounting.model.UnitFactor;
import accounting.service.CropDataService;
import accounting.util.ErrorHandler;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;
import javafx.util.converter.DoubleStringConverter;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CropFormController {

    @FXML private TextField cropNameField;
    @FXML private TableView<UnitFactor> unitsTable;
    @FXML private TableColumn<UnitFactor, String> unitNameColumn;
    @FXML private TableColumn<UnitFactor, Double> conversionFactorColumn;
    @FXML private TableColumn<UnitFactor, Void> actionsColumn;

    private Stage dialogStage;
    private Crop crop;
    private CropDataService cropDataService;
    private boolean okClicked = false;
    private ObservableList<UnitFactor> unitFactorsList;

    @FXML
    private void initialize() {
        this.cropDataService = new CropDataService();
        unitFactorsList = FXCollections.observableArrayList();
        unitsTable.setItems(unitFactorsList);

        unitNameColumn.setCellValueFactory(new PropertyValueFactory<>("unitName"));
        unitNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        unitNameColumn.setOnEditCommit(event -> event.getTableView().getItems().get(event.getTablePosition().getRow()).setUnitName(event.getNewValue()));

        conversionFactorColumn.setCellValueFactory(new PropertyValueFactory<>("conversionFactor"));
        conversionFactorColumn.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        conversionFactorColumn.setOnEditCommit(event -> event.getTableView().getItems().get(event.getTablePosition().getRow()).setConversionFactor(event.getNewValue()));
        
        addActionsToTable();
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setCrop(Crop crop) {
        this.crop = crop;
        cropNameField.setText(crop.getCropName());
        if (crop.getConversionFactors() != null) {
            crop.getConversionFactors().forEach((unit, factors) -> {
                if (factors != null && !factors.isEmpty()) {
                    unitFactorsList.add(new UnitFactor(unit, factors.get(0)));
                }
            });
        }
    }

    public boolean isOkClicked() {
        return okClicked;
    }

    @FXML
    private void handleAddUnit() {
        unitFactorsList.add(new UnitFactor("وحدة جديدة", 1.0));
    }

    private void addActionsToTable() {
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button deleteButton = new Button("حذف");
            {
                deleteButton.getStyleClass().add("danger");
                deleteButton.setOnAction(event -> {
                    UnitFactor unit = getTableView().getItems().get(getIndex());
                    unitFactorsList.remove(unit);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteButton);
            }
        });
    }

    @FXML
    private void handleSave() {
        if (isInputValid()) {
            crop.setCropName(cropNameField.getText());

            Map<String, List<Double>> conversionFactorsMap = new HashMap<>();
            List<String> allowedUnits = new ArrayList<>();
            
            for (UnitFactor uf : unitFactorsList) {
                allowedUnits.add(uf.getUnitName());
                conversionFactorsMap.put(uf.getUnitName(), List.of(uf.getConversionFactor()));
            }

            crop.setAllowedPricingUnits(allowedUnits);
            crop.setConversionFactors(conversionFactorsMap);
            
            try {
                if (crop.getCropId() == 0) {
                    cropDataService.addCrop(crop);
                } else {
                    cropDataService.updateCrop(crop);
                }
                okClicked = true;
                dialogStage.close();
            } catch (SQLException e) {
                ErrorHandler.showException("خطأ في الحفظ", "فشل حفظ المحصول.", e);
            }
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    private boolean isInputValid() {
        if (cropNameField.getText() == null || cropNameField.getText().trim().isEmpty()) {
            ErrorHandler.showError("خطأ في الإدخال", "اسم المحصول مطلوب.");
            return false;
        }
        for (UnitFactor uf : unitFactorsList) {
            if (uf.getUnitName() == null || uf.getUnitName().trim().isEmpty() || uf.getConversionFactor() <= 0) {
                ErrorHandler.showError("خطأ في الإدخال", "كل الوحدات يجب أن تحتوي على اسم صحيح ومعامل تحويل أكبر من صفر.");
                return false;
            }
        }
        return true;
    }
}