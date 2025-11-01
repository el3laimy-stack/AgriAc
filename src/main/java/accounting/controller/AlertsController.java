package accounting.controller;

import accounting.util.ErrorHandler;
import accounting.util.SmartAlertSystem;
import accounting.util.SmartAlertSystem.SmartAlert;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AlertsController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(AlertsController.class.getName());

    @FXML
    private ListView<SmartAlert> alertsListView;
    @FXML
    private ProgressIndicator progressIndicator;

    private final SmartAlertSystem alertSystem = new SmartAlertSystem();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        alertsListView.setCellFactory(param -> new AlertListCell());
        loadAlerts();
    }

    private void loadAlerts() {
        Task<List<SmartAlert>> loadAlertsTask = new Task<>() {
            @Override
            protected List<SmartAlert> call() throws Exception {
                return alertSystem.getAllActiveAlerts();
            }
        };

        loadAlertsTask.setOnSucceeded(e -> {
            alertsListView.setItems(FXCollections.observableArrayList(loadAlertsTask.getValue()));
            progressIndicator.visibleProperty().unbind();
            progressIndicator.setVisible(false);
            alertsListView.disableProperty().unbind();
            alertsListView.setDisable(false);
        });

        loadAlertsTask.setOnFailed(e -> {
            Exception exception = (Exception) loadAlertsTask.getException();
            progressIndicator.visibleProperty().unbind();
            progressIndicator.setVisible(false);
            alertsListView.disableProperty().unbind();
            ErrorHandler.showException("Error", "Failed to load alerts", exception);
        });

        progressIndicator.visibleProperty().bind(loadAlertsTask.runningProperty());
        alertsListView.disableProperty().bind(loadAlertsTask.runningProperty());
        new Thread(loadAlertsTask).start();
    }

    class AlertListCell extends ListCell<SmartAlert> {
        private final HBox hbox = new HBox(10);
        private final Rectangle priorityIndicator = new Rectangle(10, 10);
        private final VBox vbox = new VBox(5);
        private final Label titleLabel = new Label();
        private final Label detailsLabel = new Label();

        public AlertListCell() {
            super();
            titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
            detailsLabel.setFont(Font.font("System", 12));
            detailsLabel.setWrapText(true);
            vbox.getChildren().addAll(titleLabel, detailsLabel);
            hbox.getChildren().addAll(priorityIndicator, vbox);
            hbox.setPadding(new Insets(5));
        }

        @Override
        protected void updateItem(SmartAlert alert, boolean empty) {
            super.updateItem(alert, empty);
            if (empty || alert == null) {
                setText(null);
                setGraphic(null);
            } else {
                titleLabel.setText(alert.getType().toString());
                detailsLabel.setText(alert.getMessage());
                priorityIndicator.setFill(getPriorityColor(alert.getPriority()));
                setGraphic(hbox);
            }
        }

        private Color getPriorityColor(SmartAlertSystem.AlertPriority priority) {
            return switch (priority) {
                case HIGH -> Color.RED;
                case MEDIUM -> Color.ORANGE;
                case LOW -> Color.GREEN;
                default -> Color.GRAY;
            };
        }
    }
}
