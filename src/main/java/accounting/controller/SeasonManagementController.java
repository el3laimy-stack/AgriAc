package accounting.controller;

import accounting.model.Season;
import accounting.util.ErrorHandler;
import accounting.service.SeasonDataService;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class SeasonManagementController implements javafx.fxml.Initializable {

    @FXML private TableView<Season> seasonsTable;
    @FXML private TableColumn<Season, String> nameColumn;
    @FXML private TableColumn<Season, LocalDate> startDateColumn;
    @FXML private TableColumn<Season, LocalDate> endDateColumn;
    @FXML private TableColumn<Season, Season.Status> statusColumn;
    @FXML private TableColumn<Season, Void> actionsColumn;

    @FXML private TextField searchField;

    private SeasonDataService seasonDataService;
    private ObservableList<Season> seasonsList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.seasonDataService = new SeasonDataService();
        setupTable();
        setupSearchFilter();
        loadSeasons();
    }

    private void setupTable() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        startDateColumn.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        endDateColumn.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        addActionsToTable();
    }

    private void setupSearchFilter() {
        FilteredList<Season> filteredData = new FilteredList<>(seasonsList, p -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(season -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return season.getName().toLowerCase().contains(lowerCaseFilter);
            });
        });
        seasonsTable.setItems(filteredData);
    }

    private void loadSeasons() {
        Task<List<Season>> loadDataTask = new Task<>() {
            @Override
            protected List<Season> call() throws Exception {
                return seasonDataService.getAllSeasons();
            }
        };
        loadDataTask.setOnSucceeded(e -> seasonsList.setAll(loadDataTask.getValue()));
        loadDataTask.setOnFailed(e -> ErrorHandler.showException("خطأ", "فشل تحميل المواسم.", (Exception) loadDataTask.getException()));
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
                    Season season = getTableView().getItems().get(getIndex());
                    handleEditSeason(season);
                });
                deleteButton.setOnAction(event -> {
                    Season season = getTableView().getItems().get(getIndex());
                    handleDeleteSeason(season);
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
    private void handleNewSeason() {
        showSeasonEditDialog(new Season());
    }

    private void handleEditSeason(Season season) {
        showSeasonEditDialog(season);
    }

    private void showSeasonEditDialog(Season season) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SeasonForm.fxml"));
            VBox page = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("تحرير بيانات الموسم");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.setScene(new Scene(page));

            SeasonFormController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setSeason(season);

            dialogStage.showAndWait();

            if (controller.isOkClicked()) {
                // Save the season to the database
                if (season.getId() == 0) {
                    seasonDataService.addSeason(season);
                } else {
                    seasonDataService.updateSeason(season);
                }
                loadSeasons(); // Refresh list
            }
        } catch (IOException | SQLException e) {
            ErrorHandler.showException("خطأ", "فشل في التعامل مع بيانات الموسم.", e);
        }
    }

    private void handleDeleteSeason(Season season) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("تأكيد الحذف");
        alert.setContentText("هل أنت متأكد من حذف: " + season.getName() + "؟");
        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                seasonDataService.deleteSeason(season.getId());
                loadSeasons();
            } catch (SQLException e) {
                ErrorHandler.showException("خطأ في الحذف", "فشل حذف الموسم.", e);
            }
        }
    }
}