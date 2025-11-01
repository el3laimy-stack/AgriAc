package accounting.controller;

import javafx.fxml.FXML;
import javafx.stage.Stage;

public class TransferFormController implements BaseFormController {

    private Stage dialogStage;
    private boolean okClicked = false;

    @Override
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    @Override
    public boolean isOkClicked() {
        return okClicked;
    }

    @FXML
    private void handleOk() {
        okClicked = true;
        dialogStage.close();
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }
}
