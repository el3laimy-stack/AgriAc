package accounting.gui;

import accounting.controller.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * التطبيق الرئيسي المحسن لإدارة حسابات المحاصيل الزراعية
 * يستخدم FXML لفصل التصميم عن منطق التحكم
 */
public class ImprovedAccountingApp extends Application {

    private MainController mainController;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load custom fonts
        Font.loadFont(getClass().getResourceAsStream("/fonts/Cairo-Regular.ttf"), 10);
        Font.loadFont(getClass().getResourceAsStream("/fonts/Cairo-Bold.ttf"), 10);

        try {
            // تحميل ملف FXML الرئيسي
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Parent root = loader.load();
            
            // الحصول على مرجع للتحكم الرئيسي
            mainController = loader.getController();

            // إعداد المشهد
            Scene scene = new Scene(root, 1400, 900);
            
            // تطبيق ملف الأنماط
            scene.getStylesheets().add(getClass().getResource("/css/new-theme.css").toExternalForm());

            // إعداد النافذة الرئيسية
            primaryStage.setTitle("برنامج إدارة حسابات المحاصيل الزراعية - الإصدار المحسن");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(700);
            
            // معالج إغلاق التطبيق
            primaryStage.setOnCloseRequest(event -> {
                if (mainController != null) {
                    mainController.cleanup();
                }
            });

            primaryStage.show();
            
            System.out.println("تم تشغيل التطبيق المحسن بنجاح");

        } catch (IOException e) {
            System.err.println("خطأ في تحميل واجهة المستخدم: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void init() throws Exception {
        System.out.println("تهيئة التطبيق المحسن...");
        // يمكن إضافة تهيئة قاعدة البيانات هنا
        // DataManager.initializeDatabase();
    }

    @Override
    public void stop() throws Exception {
        System.out.println("إغلاق التطبيق المحسن...");
        if (mainController != null) {
            mainController.cleanup();
        }
    }

    /**
     * نقطة دخول التطبيق
     */
    public static void main(String[] args) {
        launch(args);
    }
}
