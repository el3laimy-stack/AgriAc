package accounting.controller;

import accounting.model.Season;
import accounting.util.ErrorHandler;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class SeasonFormController {

    @FXML private TextField nameField;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<Season.Status> statusComboBox;

    private Stage dialogStage;
    private Season season;
    private boolean okClicked = false;

    @FXML
    private void initialize() {
        statusComboBox.setItems(FXCollections.observableArrayList(Season.Status.values()));
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setSeason(Season season) {
        this.season = season;
        if (season != null) {
            nameField.setText(season.getName());
            startDatePicker.setValue(season.getStartDate());
            endDatePicker.setValue(season.getEndDate());
            statusComboBox.setValue(season.getStatus());
        }
    }

    public boolean isOkClicked() {
        return okClicked;
    }

    @FXML
    private void handleSave() {
        if (isInputValid()) {
            season.setName(nameField.getText());
            season.setStartDate(startDatePicker.getValue());
            season.setEndDate(endDatePicker.getValue());
            season.setStatus(statusComboBox.getValue());
            okClicked = true;
            dialogStage.close();
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    private boolean isInputValid() {
        String errorMessage = "";
        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            errorMessage += "اسم الموسم مطلوب.\n";
        }
        if (startDatePicker.getValue() == null) {
            errorMessage += "تاريخ البدء مطلوب.\n";
        }
        if (endDatePicker.getValue() == null) {
            errorMessage += "تاريخ الانتهاء مطلوب.\n";
        }
        if (statusComboBox.getValue() == null) {
            errorMessage += "حالة الموسم مطلوبة.\n";
        }
        if (startDatePicker.getValue() != null && endDatePicker.getValue() != null && startDatePicker.getValue().isAfter(endDatePicker.getValue())) {
            errorMessage += "تاريخ البدء يجب أن يكون قبل تاريخ الانتهاء.\n";
        }

        if (!errorMessage.isEmpty()) {
            ErrorHandler.showError("خطأ في الإدخال", errorMessage);
            return false;
        }
        return true;
    }
}
