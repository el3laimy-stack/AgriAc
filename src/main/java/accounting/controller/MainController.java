package accounting.controller;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

import accounting.util.ErrorHandler;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.NodeOrientation;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MainController implements Initializable {

    private static MainController instance;

    @FXML private BorderPane mainBorderPane;
    @FXML private VBox sideNavigationBar;
    @FXML public StackPane contentArea;
    @FXML public Label viewTitleLabel;
    @FXML private Label dateTimeLabel;

    // --- Navigation Buttons ---
    @FXML private Button dashboardBtn;
    @FXML private Button journalBtn;
    @FXML private Button inventoryBtn;
    @FXML private Button contactsBtn;
    @FXML private Button cropManagementBtn;
    @FXML private Button financialAccountManagementBtn;
    @FXML private Button seasonManagementBtn;
    @FXML private Button dailyPriceBtn;
    @FXML private Button reportsMenuBtn;
    @FXML private Button expenseBtn;

    @FXML private Button settingsBtn;
    @FXML private Button saleHistoryBtn;
    @FXML private Button purchaseHistoryBtn;
    
    private Button currentActiveButton;
    private Timer statusTimer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        instance = this;
        if (mainBorderPane != null) {
            mainBorderPane.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        }
        setupStatusBar();
        // Set Journal as the default view
        showJournal();
    }

    public static void loadView(String fxmlPath, String title) {
        if (instance != null) {
            instance.loadViewInternal(fxmlPath, title, null);
        }
    }

    private void setupStatusBar() {
        statusTimer = new Timer(true);
        statusTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    if (dateTimeLabel != null) {
                         dateTimeLabel.setText(java.time.LocalDateTime.now()
                            .format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")));
                    }
                });
            }
        }, 0, 1000);
    }
    
    private void loadViewInternal(String fxmlPath, String title, Button activeButton) {
        try {
            // Deactivate menu button if another button is clicked
            reportsMenuBtn.getStyleClass().remove("active");

            Parent view = FXMLLoader.load(getClass().getResource("/fxml/" + fxmlPath));
            contentArea.getChildren().setAll(view);
            viewTitleLabel.setText(title);
            
            if (currentActiveButton != null) {
                currentActiveButton.getStyleClass().remove("active");
            }
            currentActiveButton = activeButton;
        } catch (IOException e) {
            ErrorHandler.showException("خطأ في تحميل الواجهة", "فشل تحميل الواجهة: " + fxmlPath, e);
        }
    }

    // --- Main View Handlers ---
    @FXML private void showDashboard() { loadViewInternal("Dashboard.fxml", "لوحة التحكم", dashboardBtn); }
    @FXML private void showJournal() { loadViewInternal("JournalView.fxml", "دفتر اليومية", journalBtn); }
    @FXML private void showInventory() { loadViewInternal("Inventory.fxml", "المخزون", inventoryBtn); }
    @FXML private void handleManageContacts() { loadViewInternal("ContactManagement.fxml", "جهات التعامل", contactsBtn); }
    @FXML private void showCropManagement() { loadViewInternal("CropManagement.fxml", "إدارة المحاصيل", cropManagementBtn); }
    @FXML private void showFinancialAccountManagement() { loadViewInternal("FinancialAccountManagement.fxml", "إدارة الحسابات المالية", financialAccountManagementBtn); }
    @FXML private void showSeasonManagement() { loadViewInternal("SeasonManagement.fxml", "إدارة المواسم", seasonManagementBtn); }
    @FXML private void showDailyPrices() { loadViewInternal("DailyPriceView.fxml", "الأسعار اليومية", dailyPriceBtn); }

    @FXML private void showSaleHistory() { loadViewInternal("SaleHistoryView.fxml", "سجل المبيعات", saleHistoryBtn); }

    @FXML private void showPurchaseHistory() { loadViewInternal("PurchaseHistoryView.fxml", "سجل المشتريات", purchaseHistoryBtn); }

    @FXML
    private void handleShowExpenseForm() {
        loadViewInternal("ExpenseManagement.fxml", "إدارة المصروفات", expenseBtn);
    }


    
    // --- Report Handlers ---
    @FXML private void showReportsDashboard() { 
        loadViewInternal("ReportsDashboard.fxml", "مركز التقارير", reportsMenuBtn);
    }

    @FXML private void handleSettings() { /* No view yet */ }

    public void cleanup() {
        if (statusTimer != null) {
            statusTimer.cancel();
        }
    }

    public static void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
