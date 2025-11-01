package accounting.controller;

import accounting.model.Contact;
import accounting.service.ContactDataService;
import accounting.util.ErrorHandler;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class ContactManagementController implements javafx.fxml.Initializable {

    @FXML private TableView<Contact> contactTable;
    @FXML private TableColumn<Contact, String> nameColumn;
    @FXML private TableColumn<Contact, String> typeColumn;
    @FXML private TableColumn<Contact, String> phoneColumn;
    @FXML private TableColumn<Contact, String> addressColumn;
    @FXML private TableColumn<Contact, Void> actionsColumn;

    @FXML private TextField searchField;
    @FXML private ToggleGroup filterToggleGroup;
    @FXML private ToggleButton allFilterButton, customersFilterButton, suppliersFilterButton;

    private ContactDataService contactDataService;
    private ObservableList<Contact> contactList = FXCollections.observableArrayList();
    private FilteredList<Contact> filteredData;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.contactDataService = new ContactDataService();
        setupTable();
        setupFilters();
        loadContacts();
    }

    private void setupTable() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("contactType"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        addressColumn.setCellValueFactory(new PropertyValueFactory<>("address"));
        addActionsToTable();
    }

    private void setupFilters() {
        filteredData = new FilteredList<>(contactList, p -> true);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        filterToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        contactTable.setItems(filteredData);
    }

    private void applyFilters() {
        String searchText = searchField.getText();
        ToggleButton selectedToggle = (ToggleButton) filterToggleGroup.getSelectedToggle();

        filteredData.setPredicate(contact -> {
            boolean searchMatch = (searchText == null || searchText.isEmpty())
                || contact.getName().toLowerCase().contains(searchText.toLowerCase())
                || (contact.getPhone() != null && contact.getPhone().contains(searchText));

            boolean filterMatch = (selectedToggle == allFilterButton)
                || (selectedToggle == customersFilterButton && contact.isCustomer())
                || (selectedToggle == suppliersFilterButton && contact.isSupplier());

            return searchMatch && filterMatch;
        });
    }

    private void loadContacts() {
        Task<List<Contact>> loadDataTask = new Task<>() {
            @Override
            protected List<Contact> call() throws Exception {
                return contactDataService.getAllContacts();
            }
        };
        loadDataTask.setOnSucceeded(e -> contactList.setAll(loadDataTask.getValue()));
        loadDataTask.setOnFailed(e -> ErrorHandler.showException("خطأ", "فشل تحميل قائمة جهات التعامل.", (Exception) loadDataTask.getException()));
        new Thread(loadDataTask).start();
    }

    private void addActionsToTable() {
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button("تعديل");
            private final Button deleteButton = new Button("حذف");
            private final Button statementButton = new Button("كشف حساب");
            private final HBox pane = new HBox(5, editButton, deleteButton, statementButton);

            {
                editButton.getStyleClass().add("warning");
                deleteButton.getStyleClass().add("danger");
                statementButton.getStyleClass().add("primary");

                editButton.setOnAction(event -> {
                    Contact contact = getTableView().getItems().get(getIndex());
                    handleEditContact(contact);
                });
                deleteButton.setOnAction(event -> {
                    Contact contact = getTableView().getItems().get(getIndex());
                    handleDeleteContact(contact);
                });
                statementButton.setOnAction(event -> {
                    Contact contact = getTableView().getItems().get(getIndex());
                    handleViewStatement(contact);
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
    private void handleAddContact() {
        showContactEditDialog(new Contact());
    }

    private void handleEditContact(Contact contact) {
        showContactEditDialog(contact);
    }

    private void showContactEditDialog(Contact contact) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ContactForm.fxml"));
            Parent page = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("تحرير بيانات جهة التعامل");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.setScene(new Scene(page));

            ContactFormController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setContact(contact);

            dialogStage.showAndWait();
            loadContacts(); // Refresh list after dialog closes
        } catch (IOException e) {
            ErrorHandler.showException("خطأ في التحميل", "فشل تحميل واجهة تحرير جهة التعامل.", e);
        }
    }

    private void handleDeleteContact(Contact contact) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("تأكيد الحذف");
        alert.setContentText("هل أنت متأكد من حذف: " + contact.getName() + "؟");
        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                contactDataService.deleteContact(contact.getContactId());
                loadContacts();
            } catch (SQLException e) {
                ErrorHandler.showException("خطأ في الحذف", "فشل حذف جهة التعامل.", e);
            }
        }
    }

    private void handleViewStatement(Contact contact) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ContactStatementView.fxml"));
            Parent page = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("كشف حساب: " + contact.getName());
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.setScene(new Scene(page));

            ContactStatementController controller = loader.getController();
            controller.loadStatement(contact);

            dialogStage.showAndWait();
        } catch (IOException e) {
            ErrorHandler.showException("خطأ في التحميل", "فشل تحميل واجهة كشف الحساب.", e);
        }
    }
}