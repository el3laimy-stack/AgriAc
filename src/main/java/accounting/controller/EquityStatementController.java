package accounting.controller;

import accounting.model.BalanceSheet;
import accounting.model.IncomeStatement;
import accounting.service.FinancialSummaryService;
import accounting.formatter.FormatUtils;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;

import java.sql.SQLException;
import java.time.LocalDate;

import javafx.concurrent.Task;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Alert;

import java.util.logging.Level;
import java.util.logging.Logger;

public class EquityStatementController {

    private static final Logger LOGGER = Logger.getLogger(EquityStatementController.class.getName());

    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private Button generateReportBtn;
    @FXML private Label beginningEquityLabel;
    @FXML private Label netProfitLabel;
    @FXML private Label capitalAdditionsLabel;
    @FXML private Label ownerWithdrawalsLabel;
    @FXML private Label endingEquityLabel;
    @FXML private LineChart<String, Number> equityChart;
    @FXML private ProgressIndicator progressIndicator;

    private FinancialSummaryService summaryService;

    @FXML
    private void initialize() {
        this.summaryService = new FinancialSummaryService();
        generateReportBtn.setOnAction(e -> generateReport());
    }

    private void generateReport() {
        LocalDate fromDate = fromDatePicker.getValue();
        LocalDate toDate = toDatePicker.getValue();

        if (fromDate == null || toDate == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("بيانات ناقصة");
            alert.setHeaderText(null);
            alert.setContentText("الرجاء تحديد تاريخ البدء وتاريخ الانتهاء.");
            alert.showAndWait();
            return;
        }

        Task<Void> generateReportTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                BalanceSheet beginningBs = summaryService.getBalanceSheet(fromDate.minusDays(1));
                IncomeStatement periodIs = summaryService.getIncomeStatement(fromDate, toDate);
                BalanceSheet endingBs = summaryService.getBalanceSheet(toDate);

                double beginningEquity = beginningBs.getTotalEquity();
                double netProfit = periodIs.getNetIncome();
                double endingEquity = endingBs.getTotalEquity();
                
                double additionsAndWithdrawals = endingEquity - beginningEquity - netProfit;

                // Update UI on JavaFX Application Thread
                javafx.application.Platform.runLater(() -> {
                    beginningEquityLabel.setText(FormatUtils.formatCurrency(beginningEquity));
                    netProfitLabel.setText(FormatUtils.formatCurrency(netProfit));
                    endingEquityLabel.setText(FormatUtils.formatCurrency(endingEquity));
                    
                    if (additionsAndWithdrawals >= 0) {
                        capitalAdditionsLabel.setText(FormatUtils.formatCurrency(additionsAndWithdrawals));
                        ownerWithdrawalsLabel.setText(FormatUtils.formatCurrency(0));
                    } else {
                        capitalAdditionsLabel.setText(FormatUtils.formatCurrency(0));
                        ownerWithdrawalsLabel.setText(FormatUtils.formatCurrency(-additionsAndWithdrawals));
                    }
                });
                return null;
            }
        };

        generateReportTask.setOnSucceeded(e -> {
            // Chart update logic can go here if needed
        });

        generateReportTask.setOnFailed(e -> {
            LOGGER.log(Level.SEVERE, "Failed to generate equity statement", generateReportTask.getException());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("خطأ في إنشاء التقرير");
            alert.setHeaderText("لا يمكن إنشاء بيان حقوق الملكية.");
            alert.setContentText("حدث خطأ أثناء الاتصال بقاعدة البيانات. يرجى المحاولة مرة أخرى.");
            alert.showAndWait();
        });

        progressIndicator.visibleProperty().bind(generateReportTask.runningProperty());
        new Thread(generateReportTask).start();
    }
}