package accounting.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ErrorHandler {

    private static final Logger LOGGER = Logger.getLogger(ErrorHandler.class.getName());
    private static FileHandler fileHandler;

    static {
        try {
            // Create a file handler for error logging
            fileHandler = new FileHandler("application_errors.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fileHandler);
            LOGGER.setLevel(Level.ALL);
        } catch (Exception e) {
            System.err.println("Failed to initialize file logger: " + e.getMessage());
        }
    }

    /**
     * Shows an error dialog to the user and logs the error to both console and file
     * @param title The title of the error dialog
     * @param header The header text of the error dialog
     * @param content The content/description of the error
     * @param ex The exception that caused the error (can be null)
     */
    public static void showError(String title, String header, String content, Throwable ex) {
        // Log the error
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String logMessage = String.format("[%s] %s: %s", timestamp, header, content);
        LOGGER.log(Level.SEVERE, logMessage, ex);

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);

            if (ex != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                ex.printStackTrace(pw);
                String exceptionText = sw.toString();

                Label label = new Label("تفاصيل الخطأ:");

                TextArea textArea = new TextArea(exceptionText);
                textArea.setEditable(false);
                textArea.setWrapText(true);
                textArea.setPrefRowCount(10);
                textArea.setPrefColumnCount(50);

                textArea.setMaxWidth(Double.MAX_VALUE);
                textArea.setMaxHeight(Double.MAX_VALUE);
                GridPane.setVgrow(textArea, Priority.ALWAYS);
                GridPane.setHgrow(textArea, Priority.ALWAYS);

                GridPane expContent = new GridPane();
                expContent.setMaxWidth(Double.MAX_VALUE);
                expContent.add(label, 0, 0);
                expContent.add(textArea, 0, 1);

                alert.getDialogPane().setExpandableContent(expContent);
            }

            alert.showAndWait();
        });
    }

    /**
     * Shows an error dialog with a simplified interface
     * @param title The title of the error dialog
     * @param content The content/description of the error
     */
    public static void showError(String title, String content) {
        showError(title, title, content, null);
    }

    /**
     * Shows an error dialog with exception details
     * @param title The title of the error dialog
     * @param content The content/description of the error
     * @param ex The exception that caused the error
     */
    public static void showException(String title, String content, Exception ex) {
        showError(title, title, content, ex);
    }

    /**
     * Shows an information dialog to the user
     * @param title The title of the information dialog
     * @param header The header text of the information dialog
     * @param content The content of the information
     */
    public static void showInfo(String title, String header, String content) {
        LOGGER.log(Level.INFO, header + ": " + content);
        
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    /**
     * Shows an information dialog with a simplified interface
     * @param title The title of the information dialog
     * @param content The content of the information
     */
    public static void showInfo(String title, String content) {
        showInfo(title, title, content);
    }

    /**
     * Shows a warning dialog to the user
     * @param title The title of the warning dialog
     * @param header The header text of the warning dialog
     * @param content The content of the warning
     */
    public static void showWarning(String title, String header, String content) {
        LOGGER.log(Level.WARNING, header + ": " + content);
        
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    /**
     * Shows a warning dialog with a simplified interface
     * @param title The title of the warning dialog
     * @param content The content of the warning
     */
    public static void showWarning(String title, String content) {
        showWarning(title, title, content);
    }

    /**
     * Logs a message without showing a dialog
     * @param level The logging level
     * @param message The message to log
     */
    public static void log(Level level, String message) {
        LOGGER.log(level, message);
    }

    /**
     * Logs a severe error without showing a dialog
     * @param message The error message to log
     */
    public static void logSevere(String message) {
        LOGGER.log(Level.SEVERE, message);
    }

    /**
     * Logs a warning without showing a dialog
     * @param message The warning message to log
     */
    public static void logWarning(String message) {
        LOGGER.log(Level.WARNING, message);
    }

    /**
     * Logs an info message without showing a dialog
     * @param message The info message to log
     */
    public static void logInfo(String message) {
        LOGGER.log(Level.INFO, message);
    }
}