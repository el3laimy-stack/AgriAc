package accounting.controller;

import accounting.model.Contact;
import accounting.model.ContactStatementEntry;
import accounting.service.ContactDataService;
import accounting.util.ErrorHandler;
import accounting.formatter.FormatUtils;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ContactStatementController {

    @FXML private Label contactNameLabel;
    @FXML private VBox statementContainer;

    private ContactDataService contactDataService;

    @FXML
    public void initialize() {
        this.contactDataService = new ContactDataService();
    }

    public void loadStatement(Contact contact) {
        if (contact == null) {
            ErrorHandler.showError("خطأ", "لم يتم تحديد جهة التعامل.");
            return;
        }

        contactNameLabel.setText(contact.getName());

        try {
            LocalDate toDate = LocalDate.now();
            LocalDate fromDate = toDate.minusYears(1);

            List<ContactStatementEntry> entries = contactDataService.getContactStatement(contact.getContactId(), fromDate, toDate);
            buildStatementUI(entries);

        } catch (SQLException e) {
            ErrorHandler.showException("خطأ في قاعدة البيانات", "فشل تحميل كشف الحساب.", e);
        }
    }

    private void buildStatementUI(List<ContactStatementEntry> entries) {
        statementContainer.getChildren().clear();

        if (entries.isEmpty()) {
            statementContainer.getChildren().add(new Label("لا توجد حركات لعرضها."));
            return;
        }
        
        // Add Header Row
        statementContainer.getChildren().add(createHeaderRow());

        // Group entries by date
        Map<LocalDate, List<ContactStatementEntry>> groupedByDate = entries.stream()
                .collect(Collectors.groupingBy(ContactStatementEntry::getDate));

        // Sort dates
        List<LocalDate> sortedDates = groupedByDate.keySet().stream().sorted().collect(Collectors.toList());

        double runningBalance = 0.0;

        int rowIndex = 0;
        for (LocalDate date : sortedDates) {
            List<ContactStatementEntry> dayEntries = groupedByDate.get(date);

            for (ContactStatementEntry entry : dayEntries) {
                if (entry.getReason().equals("رصيد أول المدة")) {
                    runningBalance = entry.getBalance();
                    statementContainer.getChildren().add(createOpeningBalanceRow(entry));
                } else {
                    Node entryRow = createEntryRow(entry);
                    if (rowIndex % 2 != 0) {
                        entryRow.getStyleClass().add("odd-row");
                    }
                    statementContainer.getChildren().add(entryRow);
                    rowIndex++;
                    if(entry.isDebit()){
                        runningBalance += entry.getAmount();
                    } else {
                        runningBalance -= entry.getAmount();
                    }
                }
            }
            statementContainer.getChildren().add(createBalanceRow(date, runningBalance));
        }
    }

    private Node createHeaderRow() {
        HBox headerRow = new HBox();
        headerRow.getStyleClass().add("statement-header-row");
        headerRow.setAlignment(Pos.CENTER_RIGHT);

        Label dateLabel = new Label("التاريخ");
        dateLabel.getStyleClass().add("statement-header-label");
        dateLabel.setPrefWidth(100);

        Label reasonLabel = new Label("السبب");
        reasonLabel.getStyleClass().add("statement-header-label");
        reasonLabel.setPrefWidth(150);
        
        Label typeLabel = new Label("النوع");
        typeLabel.getStyleClass().add("statement-header-label");
        typeLabel.setPrefWidth(100);

        Label weightLabel = new Label("الوزن");
        weightLabel.getStyleClass().add("statement-header-label");
        weightLabel.setPrefWidth(80);

        Label priceLabel = new Label("السعر");
        priceLabel.getStyleClass().add("statement-header-label");
        priceLabel.setPrefWidth(80);
        
        Label notesLabel = new Label("ملاحظات");
        notesLabel.getStyleClass().add("statement-header-label");
        notesLabel.setPrefWidth(200);

        Label amountLabel = new Label("المبلغ");
        amountLabel.getStyleClass().add("statement-header-label");
        amountLabel.setPrefWidth(120);

        headerRow.getChildren().addAll(amountLabel, notesLabel, priceLabel, weightLabel, typeLabel, reasonLabel, dateLabel);
        
        for (Node node : headerRow.getChildren()) {
            node.getStyleClass().add("statement-cell");
        }
        
        return headerRow;
    }
    
    private Node createOpeningBalanceRow(ContactStatementEntry entry) {
        VBox openingBalanceBox = new VBox();
        openingBalanceBox.getStyleClass().add("statement-row");
        
        HBox row = new HBox();
        Label dateLabel = new Label(FormatUtils.formatDateForDisplay(entry.getDate()));
        dateLabel.setPrefWidth(100);

        Label reasonLabel = new Label(entry.getReason());
        reasonLabel.setPrefWidth(630); // Span across multiple columns
        reasonLabel.getStyleClass().add("font-bold");

        Label amountLabel = new Label(FormatUtils.formatCurrency(entry.getAmount()));
        amountLabel.setPrefWidth(120);
        amountLabel.getStyleClass().add("font-bold");

        row.getChildren().addAll(dateLabel, reasonLabel, amountLabel);
        
        openingBalanceBox.getChildren().add(row);
        return openingBalanceBox;
    }


    private Node createEntryRow(ContactStatementEntry entry) {
        HBox entryRow = new HBox();
        entryRow.getStyleClass().add("statement-row");
        entryRow.setAlignment(Pos.CENTER_RIGHT);

        Label dateLabel = new Label(FormatUtils.formatDateForDisplay(entry.getDate()));
        dateLabel.setPrefWidth(100);

        Label reasonLabel = new Label(entry.getReason());
        reasonLabel.setPrefWidth(150);

        Label typeLabel = new Label(entry.getType());
        typeLabel.setPrefWidth(100);

        Label weightLabel = new Label(entry.getWeight() != null ? FormatUtils.formatNumber(entry.getWeight()) : "");
        weightLabel.setPrefWidth(80);

        Label priceLabel = new Label(entry.getPrice() != null ? FormatUtils.formatCurrency(entry.getPrice()) : "");
        priceLabel.setPrefWidth(80);
        
        Label notesLabel = new Label(entry.getNotes());
        notesLabel.setPrefWidth(200);

        Label amountLabel = new Label(FormatUtils.formatCurrency(entry.getAmount()));
        amountLabel.setPrefWidth(120);
        if (entry.isDebit()) {
            amountLabel.getStyleClass().add("danger-text");
        } else {
            amountLabel.getStyleClass().add("success-text");
        }

        entryRow.getChildren().addAll(amountLabel, notesLabel, priceLabel, weightLabel, typeLabel, reasonLabel, dateLabel);

        for (Node node : entryRow.getChildren()) {
            node.getStyleClass().add("statement-cell");
        }

        return entryRow;
    }

    private Node createBalanceRow(LocalDate date, double balance) {
        VBox balanceBox = new VBox();
        balanceBox.getStyleClass().add("balance-row");

        Line separator = new Line(0, 0, 800, 0);
        separator.getStyleClass().add("separator-line");

        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_RIGHT);
        row.setPadding(new Insets(5, 0, 5, 0));

        Label dateLabel = new Label(FormatUtils.formatDateForDisplay(date));
        dateLabel.setPrefWidth(100);
        dateLabel.getStyleClass().add("font-bold");

        Label balanceDescriptionLabel = new Label();
        balanceDescriptionLabel.setPrefWidth(630); // Span
        balanceDescriptionLabel.getStyleClass().add("font-bold");
        balanceDescriptionLabel.setAlignment(Pos.CENTER);

        Label balanceLabel = new Label();
        balanceLabel.setPrefWidth(120);
        balanceLabel.getStyleClass().add("font-bold");

        if (balance > 0) {
            balanceDescriptionLabel.setText("الباقي عليه");
            balanceLabel.setText(FormatUtils.formatCurrency(balance));
            balanceLabel.getStyleClass().add("text-danger");
        } else if (balance < 0) {
            balanceDescriptionLabel.setText("الباقي له");
            balanceLabel.setText(FormatUtils.formatCurrency(balance * -1));
            balanceLabel.getStyleClass().add("text-success");
        } else {
            balanceDescriptionLabel.setText("خالص");
            balanceLabel.setText(FormatUtils.formatCurrency(0));
            balanceLabel.getStyleClass().add("text-primary");
        }

        row.getChildren().addAll(balanceLabel, balanceDescriptionLabel, dateLabel);

        for (Node node : row.getChildren()) {
            node.getStyleClass().add("statement-cell");
        }
        
        balanceBox.getChildren().addAll(separator, row);
        return balanceBox;
    }
}