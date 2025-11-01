package accounting.service;

import accounting.model.FinancialAccount;
import accounting.model.LedgerEntry;
import accounting.model.CashFlowEntry;
import accounting.formatter.FormatUtils;
import accounting.util.ImprovedDataManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class FinancialTransactionDataService {

    private final ImprovedDataManager dataManager;

    public FinancialTransactionDataService() {
        this.dataManager = ImprovedDataManager.getInstance();
    }

    public List<CashFlowEntry> getCashFlowEntries(LocalDate from, LocalDate to) throws SQLException {
        List<CashFlowEntry> entries = new ArrayList<>();
        String sql = """
            SELECT
                gl.entry_date,
                gl.description,
                gl.source_type,
                gl.debit,
                gl.credit
            FROM
                general_ledger gl
            JOIN
                financial_accounts fa ON gl.account_id = fa.account_id
            WHERE
                fa.account_type IN ('CASH', 'BANK')
                AND (? IS NULL OR gl.entry_date >= ?)
                AND (? IS NULL OR gl.entry_date <= ?)
            ORDER BY
                gl.entry_date, gl.entry_id;
            """;

        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String fromDate = (from != null) ? from.toString() : null;
            String toDate = (to != null) ? to.toString() : null;

            stmt.setString(1, fromDate);
            stmt.setString(2, fromDate);
            stmt.setString(3, toDate);
            stmt.setString(4, toDate);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    entries.add(new CashFlowEntry(
                        LocalDate.parse(rs.getString("entry_date"), accounting.formatter.FormatUtils.DATE_FORMATTER),
                        rs.getString("description"),
                        rs.getString("source_type"),
                        rs.getDouble("debit"), // Inflow is debit to cash/bank
                        rs.getDouble("credit")  // Outflow is credit to cash/bank
                    ));
                }
            }
        }
        calculateRunningBalance(entries, from);
        return entries;
    }

    private void calculateRunningBalance(List<CashFlowEntry> entries, LocalDate from) throws SQLException {
        double openingBalance = 0;
        if (from != null) {
            openingBalance = getOpeningBalance(from);
        }

        double runningBalance = openingBalance;
        for (CashFlowEntry entry : entries) {
            runningBalance += entry.getInflow() - entry.getOutflow();
            entry.setBalance(runningBalance);
        }
    }

    public double getOpeningBalance(LocalDate from) throws SQLException {
        String sql = """
            SELECT SUM(gl.debit) - SUM(gl.credit) as opening_balance 
            FROM general_ledger gl
            JOIN financial_accounts fa ON gl.account_id = fa.account_id
            WHERE fa.account_type IN ('CASH', 'BANK') AND gl.entry_date < ?""";
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, from.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("opening_balance");
                }
            }
        }
        return 0;
    }
    

    public List<LedgerEntry> getGeneralLedgerEntries(LocalDate from, LocalDate to) throws SQLException {
        List<LedgerEntry> entries = new ArrayList<>();
        String sql = """
            SELECT *
            FROM general_ledger
            WHERE (? IS NULL OR entry_date >= ?) AND (? IS NULL OR entry_date <= ?)
            ORDER BY entry_date, entry_id;
            """;

        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String fromDate = (from != null) ? from.toString() : null;
            String toDate = (to != null) ? to.toString() : null;

            stmt.setString(1, fromDate);
            stmt.setString(2, fromDate);
            stmt.setString(3, toDate);
            stmt.setString(4, toDate);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    entries.add(new LedgerEntry(
                        LocalDate.parse(rs.getString("entry_date")),
                        rs.getString("description"),
                        rs.getString("transaction_ref"),
                        rs.getDouble("debit"),
                        rs.getDouble("credit"),
                        rs.getString("account_type"),
                        rs.getString("source_type"),
                        rs.getInt("source_id"),
                        rs.getString("contact_name"),
                        rs.getString("item_name"),
                        rs.getObject("quantity", Double.class),
                        rs.getObject("unit_price", Double.class)
                    ));
                }
            }
        }
        return entries;
    }

    public String addJournalEntry(FinancialAccount debitAccount, FinancialAccount creditAccount, LocalDate date, String description, double amount) throws SQLException {
        return dataManager.executeTransaction(conn -> {
            String transactionRef = "MAN-" + System.currentTimeMillis();
            
            // Debit Account
            dataManager.addLedgerEntry(conn, transactionRef, date, debitAccount.getAccountId(), amount, 0.0, description, "MANUAL", 0, "MANUAL");
            dataManager.updateAccountBalance(debitAccount.getAccountId(), amount, conn);
            
            // Credit Account
            dataManager.addLedgerEntry(conn, transactionRef, date, creditAccount.getAccountId(), 0.0, amount, description, "MANUAL", 0, "MANUAL");
            dataManager.updateAccountBalance(creditAccount.getAccountId(), -amount, conn);
            
            return transactionRef;
        });
    }

    public String addExpense(LocalDate date, double amount, String description, int expenseAccountId, int paymentAccountId) throws SQLException {

        return dataManager.executeTransaction(conn -> {
            String transactionRef = "EXP-" + System.currentTimeMillis();
            
            // Debit Expense Account
            dataManager.addLedgerEntry(conn, transactionRef, date, expenseAccountId, amount, 0.0, description, "EXPENSE", null, "EXPENSE");
            dataManager.updateAccountBalance(expenseAccountId, amount, conn);
            
            // Credit Payment Account
            dataManager.addLedgerEntry(conn, transactionRef, date, paymentAccountId, 0.0, amount, description, "EXPENSE", null, "EXPENSE");
            dataManager.updateAccountBalance(paymentAccountId, -amount, conn);
            
            return transactionRef;
        });
    }

    public boolean deleteJournalEntry(String transactionRef) throws SQLException {
        return dataManager.executeTransaction(conn -> {
            List<LedgerEntry> entries = dataManager.getLedgerEntriesByRef(transactionRef, conn);
            if (entries.isEmpty()) {
                return false;
            }

            for (LedgerEntry entry : entries) {
                // Reverse the transaction by creating a new entry with opposite amounts
                dataManager.updateAccountBalance(entry.getAccountId(), -entry.getDebit() + entry.getCredit(), conn);
            }

            // Delete the original entries
            dataManager.deleteLedgerEntriesByRef(transactionRef, conn);

            return true;
        });
    }

    public boolean deleteExpense(String transactionRef) throws SQLException {
        return deleteJournalEntry(transactionRef);
    }
}