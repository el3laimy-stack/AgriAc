package accounting.service;

import accounting.model.Contact;
import accounting.model.FinancialAccount;
import accounting.model.Payment;
import accounting.util.ImprovedDataManager;
import accounting.formatter.FormatUtils;

import java.sql.*;
import java.time.LocalDate;

public class PaymentDataService {

    private final ImprovedDataManager dataManager;

    public PaymentDataService() {
        this.dataManager = ImprovedDataManager.getInstance();
    }

    public int addPayment(Payment payment) throws SQLException {
        return dataManager.executeTransaction(conn -> addPaymentLogic(conn, payment));
    }

    private int addPaymentLogic(Connection conn, Payment payment) throws SQLException {
        String sql = "INSERT INTO payments (payment_date, contact_id, payment_account_id, amount, payment_type, description) VALUES (?, ?, ?, ?, ?, ?)";
        int paymentId;
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, FormatUtils.formatDateForDatabase(payment.getPaymentDate()));
            stmt.setInt(2, payment.getContact().getContactId());
            stmt.setInt(3, payment.getPaymentAccount().getAccountId());
            stmt.setDouble(4, payment.getAmount());
            stmt.setString(5, payment.getPaymentType());
            stmt.setString(6, payment.getDescription());
            stmt.executeUpdate();

            try (var rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    paymentId = rs.getInt(1);
                    payment.setPaymentId(paymentId);
                } else {
                    throw new SQLException("Creating payment failed, no ID obtained.");
                }
            }
        }

        String transactionRef = "PAY-" + paymentId;
        if ("PAY".equals(payment.getPaymentType())) {
            int accountsPayableId = 20101;
            dataManager.addLedgerEntry(conn, transactionRef, payment.getPaymentDate(), accountsPayableId, payment.getAmount(), 0.0, payment.getDescription(), "PAYMENT", paymentId, "PAYMENT");
            dataManager.addLedgerEntry(conn, transactionRef, payment.getPaymentDate(), payment.getPaymentAccount().getAccountId(), 0.0, payment.getAmount(), payment.getDescription(), "PAYMENT", paymentId, "PAYMENT");
            dataManager.updateAccountBalance(accountsPayableId, -payment.getAmount(), conn);
            dataManager.updateAccountBalance(payment.getPaymentAccount().getAccountId(), -payment.getAmount(), conn);
        } else if ("RECEIVE".equals(payment.getPaymentType())) {
            int accountsReceivableId = 10104;
            dataManager.addLedgerEntry(conn, transactionRef, payment.getPaymentDate(), payment.getPaymentAccount().getAccountId(), payment.getAmount(), 0.0, payment.getDescription(), "PAYMENT", paymentId, "PAYMENT");
            dataManager.addLedgerEntry(conn, transactionRef, payment.getPaymentDate(), accountsReceivableId, 0.0, payment.getAmount(), payment.getDescription(), "PAYMENT", paymentId, "PAYMENT");
            dataManager.updateAccountBalance(accountsReceivableId, -payment.getAmount(), conn);
            dataManager.updateAccountBalance(payment.getPaymentAccount().getAccountId(), payment.getAmount(), conn);
        }
        return paymentId;
    }

    public boolean deletePayment(int paymentId) throws SQLException {
        return dataManager.executeTransaction(conn -> deletePaymentLogic(conn, paymentId));
    }

    private boolean deletePaymentLogic(Connection conn, int paymentId) throws SQLException {
        Payment payment = getPaymentById(paymentId, conn);
        if (payment == null) {
            throw new SQLException("Payment with ID " + paymentId + " not found.");
        }

        String transactionRef = "PAY-" + paymentId;

        if ("PAY".equals(payment.getPaymentType())) {
            int accountsPayableId = 20101;
            dataManager.updateAccountBalance(accountsPayableId, payment.getAmount(), conn);
            dataManager.updateAccountBalance(payment.getPaymentAccount().getAccountId(), payment.getAmount(), conn);
        } else if ("RECEIVE".equals(payment.getPaymentType())) {
            int accountsReceivableId = 10104;
            dataManager.updateAccountBalance(accountsReceivableId, payment.getAmount(), conn);
            dataManager.updateAccountBalance(payment.getPaymentAccount().getAccountId(), -payment.getAmount(), conn);
        }

        try (PreparedStatement deleteLedgerStmt = conn.prepareStatement("DELETE FROM general_ledger WHERE transaction_ref = ?")) {
            deleteLedgerStmt.setString(1, transactionRef);
            deleteLedgerStmt.executeUpdate();
        }

        try (PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM payments WHERE payment_id = ?")) {
            deleteStmt.setInt(1, paymentId);
            int rowsAffected = deleteStmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    public void updatePayment(Payment payment) throws SQLException {
        dataManager.executeTransaction(conn -> {
            deletePaymentLogic(conn, payment.getPaymentId());
            addPaymentLogic(conn, payment);
            return null;
        });
    }

    private Payment getPaymentById(int paymentId, Connection conn) throws SQLException {
        String sql = "SELECT p.*, c.name as contact_name, fa.account_name FROM payments p JOIN contacts c ON p.contact_id = c.contact_id JOIN financial_accounts fa ON p.payment_account_id = fa.account_id WHERE p.payment_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, paymentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Payment payment = new Payment();
                    payment.setPaymentId(rs.getInt("payment_id"));
                    
                    Contact contact = new Contact();
                    contact.setContactId(rs.getInt("contact_id"));
                    contact.setName(rs.getString("contact_name"));
                    payment.setContact(contact);

                    FinancialAccount account = new FinancialAccount();
                    account.setAccountId(rs.getInt("payment_account_id"));
                    account.setAccountName(rs.getString("account_name"));
                    payment.setPaymentAccount(account);

                    payment.setPaymentDate(FormatUtils.parseDateFromDatabase(rs.getString("payment_date")));
                    payment.setAmount(rs.getDouble("amount"));
                    payment.setPaymentType(rs.getString("payment_type"));
                    payment.setDescription(rs.getString("description"));
                    return payment;
                }
            }
        }
        return null;
    }
}