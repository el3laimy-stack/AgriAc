package accounting.service;

import accounting.dao.AbstractDAO;
import accounting.formatter.FormatUtils;
import accounting.model.FinancialAccount;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FinancialAccountDataService extends AbstractDAO<FinancialAccount, Integer> {

    public FinancialAccountDataService() {
        super("financial_accounts");
    }

    @Override
    protected FinancialAccount mapResultSetToEntity(ResultSet rs) throws SQLException {
        FinancialAccount account = new FinancialAccount(
            rs.getInt("account_id"),
            rs.getString("account_name"),
            FinancialAccount.AccountType.valueOf(rs.getString("account_type")),
            rs.getDouble("opening_balance"),
            FormatUtils.parseDateFromDatabase(rs.getString("opening_balance_date"))
        );
        account.setCurrentBalance(rs.getDouble("current_balance"));
        return account;
    }

    @Override
    protected void mapEntityToPreparedStatement(FinancialAccount account, PreparedStatement ps) throws SQLException {
        ps.setString(1, account.getAccountName());
        ps.setString(2, account.getAccountType().name());
        ps.setDouble(3, account.getOpeningBalance());
        ps.setString(4, FormatUtils.formatDateForDatabase(account.getOpeningBalanceDate()));
        ps.setDouble(5, account.getOpeningBalance());
    }

    @Override
    protected void mapEntityToUpdatePreparedStatement(FinancialAccount account, PreparedStatement ps) throws SQLException {
        ps.setString(1, account.getAccountName());
        ps.setString(2, account.getAccountType().name());
        ps.setDouble(3, account.getOpeningBalance());
        ps.setString(4, FormatUtils.formatDateForDatabase(account.getOpeningBalanceDate()));
        ps.setDouble(5, account.getCurrentBalance());
        ps.setInt(6, account.getAccountId());
    }

    @Override
    protected String getPkColumnName() {
        return "account_id";
    }

    @Override
    protected String getInsertSql() {
        return "INSERT INTO financial_accounts (account_name, account_type, opening_balance, opening_balance_date, current_balance) VALUES (?, ?, ?, ?, ?)";
    }

    @Override
    protected String getUpdateSql() {
        return "UPDATE financial_accounts SET account_name = ?, account_type = ?, opening_balance = ?, opening_balance_date = ?, current_balance = ? WHERE account_id = ?";
    }
    
    @Override
    protected String getSelectAllSql() {
        return "SELECT account_id, account_name, account_type, opening_balance, opening_balance_date, current_balance FROM financial_accounts";
    }

    public void addAccount(FinancialAccount account) throws SQLException {
        save(account);
    }

    public void updateAccount(FinancialAccount account) throws SQLException {
        update(account);
    }

    public void deleteAccount(int accountId) throws SQLException {
        // Soft delete, not using the generic DAO delete
        dataManager.executeTransaction(conn -> {
            if (dataManager.hasTransactions(accountId, conn)) {
                throw new SQLException("لا يمكن حذف هذا الحساب لوجود حركات مسجلة عليه.");
            }
            String sql = "UPDATE financial_accounts SET is_active = 0 WHERE account_id = ?";
             try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, accountId);
                stmt.executeUpdate();
                dataManager.logAuditEntry("financial_accounts", accountId, "DELETE", null, null, "SYSTEM", conn);
             }
            return null;
        });
    }

    public List<FinancialAccount> getAllAccounts() throws SQLException {
        return findAll();
    }

    public FinancialAccount getAccountById(int accountId) throws SQLException {
        return findById(accountId).orElse(null);
    }

    public List<FinancialAccount> getExpenseAccounts() throws SQLException {
        String sql = "SELECT * FROM financial_accounts WHERE account_type = ? AND is_active = 1 ORDER BY account_name";
        List<FinancialAccount> accounts = new ArrayList<>();
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, FinancialAccount.AccountType.EXPENSE.name());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    accounts.add(mapResultSetToEntity(rs));
                }
            }
        }
        return accounts;
    }

    public List<FinancialAccount> getBankAccounts() throws SQLException {
        String sql = "SELECT * FROM financial_accounts WHERE account_type = ? AND is_active = 1 ORDER BY account_name";
        List<FinancialAccount> accounts = new ArrayList<>();
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, FinancialAccount.AccountType.BANK.name());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    accounts.add(mapResultSetToEntity(rs));
                }
            }
        }
        return accounts;
    }

    public List<FinancialAccount> getCashAndBankAccounts() throws SQLException {
        String sql = "SELECT * FROM financial_accounts WHERE (account_type = ? OR account_type = ?) AND is_active = 1 ORDER BY account_name";
        List<FinancialAccount> accounts = new ArrayList<>();
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, FinancialAccount.AccountType.BANK.name());
            stmt.setString(2, FinancialAccount.AccountType.CASH.name());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    accounts.add(mapResultSetToEntity(rs));
                }
            }
        }
        return accounts;
    }
}