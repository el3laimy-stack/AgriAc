package accounting.service;

import accounting.model.BalanceSheet;
import accounting.model.IncomeStatement;
import accounting.model.TrialBalanceEntry;
import accounting.util.ImprovedDataManager;
import javafx.collections.FXCollections;
import javafx.scene.chart.PieChart;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FinancialSummaryService {

    private final ImprovedDataManager dataManager;

    public FinancialSummaryService() {
        this.dataManager = ImprovedDataManager.getInstance();
    }

    public List<TrialBalanceEntry> getTrialBalance(LocalDate toDate) throws SQLException {
        String sql = """
            SELECT 
                fa.account_id, 
                fa.account_name, 
                SUM(gl.debit) as total_debit, 
                SUM(gl.credit) as total_credit
            FROM general_ledger gl
            JOIN financial_accounts fa ON gl.account_id = fa.account_id
            WHERE gl.entry_date <= ?
            GROUP BY fa.account_id, fa.account_name
            ORDER BY fa.account_id;
            """;
        List<TrialBalanceEntry> entries = new ArrayList<>();
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, toDate.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    entries.add(new TrialBalanceEntry(
                        rs.getInt("account_id"),
                        rs.getString("account_name"),
                        rs.getDouble("total_debit"),
                        rs.getDouble("total_credit")
                    ));
                }
            }
        }
        return entries;
    }

    public IncomeStatement getIncomeStatement(LocalDate fromDate, LocalDate toDate) throws SQLException {
        Map<String, Double> revenues = getAccountBalances("REVENUE", fromDate, toDate);
        Map<String, Double> expenses = getAccountBalances("EXPENSE", fromDate, toDate);
        return new IncomeStatement(revenues, expenses);
    }

    public BalanceSheet getBalanceSheet(LocalDate toDate) throws SQLException {
        LocalDate startOfYear = toDate.withDayOfYear(1);
        IncomeStatement periodIncome = getIncomeStatement(startOfYear, toDate);
        double retainedEarnings = periodIncome.getNetIncome();

        Map<String, Double> assets = getAccountBalances("ASSET", null, toDate);
        assets.putAll(getAccountBalances("CURRENT_ASSET", null, toDate));
        assets.putAll(getAccountBalances("CASH", null, toDate));
        assets.putAll(getAccountBalances("BANK", null, toDate));
        assets.putAll(getAccountBalances("ACCOUNTS_RECEIVABLE", null, toDate));

        Map<String, Double> liabilities = getAccountBalances("LIABILITY", null, toDate);
        liabilities.putAll(getAccountBalances("ACCOUNTS_PAYABLE", null, toDate));

        Map<String, Double> equity = getAccountBalances("EQUITY", null, toDate);

        return new BalanceSheet(assets, liabilities, equity, retainedEarnings, 
                                FXCollections.observableArrayList(), 
                                FXCollections.observableArrayList(), 
                                FXCollections.observableArrayList());
    }

    private Map<String, Double> getAccountBalances(String accountType, LocalDate fromDate, LocalDate toDate) throws SQLException {
        String sql = String.format("""
            SELECT fa.account_name, SUM(gl.debit) - SUM(gl.credit) as balance
            FROM general_ledger gl
            JOIN financial_accounts fa ON gl.account_id = fa.account_id
            WHERE fa.account_type = '%s'
            %s
            GROUP BY fa.account_name
            HAVING balance != 0;
            """, accountType, buildDateFilter(fromDate, toDate));

        Map<String, Double> balances = new HashMap<>();
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            int paramIndex = 1;
            if (fromDate != null) stmt.setString(paramIndex++, fromDate.toString());
            if (toDate != null) stmt.setString(paramIndex, toDate.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    balances.put(rs.getString("account_name"), rs.getDouble("balance"));
                }
            }
        }
        return balances;
    }

    private String buildDateFilter(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null) {
            return " AND gl.entry_date BETWEEN ? AND ?";
        } else if (toDate != null) {
            return " AND gl.entry_date <= ?";
        } else {
            return "";
        }
    }
}
