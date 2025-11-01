package accounting.controller;

import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import java.time.LocalDate;

public class ReportsDashboardController {

    // FXML Fields for DatePickers
    @FXML private DatePicker incomeStmtFromDate;
    @FXML private DatePicker incomeStmtToDate;
    @FXML private DatePicker balanceSheetDate;
    @FXML private DatePicker generalLedgerFromDate;
    @FXML private DatePicker generalLedgerToDate;

    @FXML
    private void initialize() {
        // Set default date ranges
        incomeStmtFromDate.setValue(LocalDate.now().withDayOfMonth(1));
        incomeStmtToDate.setValue(LocalDate.now());
        balanceSheetDate.setValue(LocalDate.now());
        generalLedgerFromDate.setValue(LocalDate.now().withDayOfMonth(1));
        generalLedgerToDate.setValue(LocalDate.now());
    }

    @FXML
    private void generateIncomeStatement() {
        // Here you would normally pass the dates to the controller
        // For now, we just load the view.
        MainController.loadView("IncomeStatementView.fxml", "قائمة الدخل");
    }

    @FXML
    private void generateBalanceSheet() {
        MainController.loadView("BalanceSheetView.fxml", "الميزانية العمومية");
    }

    @FXML
    private void generateGeneralLedger() {
        MainController.loadView("GeneralLedgerView.fxml", "دفتر الأستاذ العام");
    }
}