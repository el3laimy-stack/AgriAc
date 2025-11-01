package accounting.service;

import accounting.dao.AbstractDAO;
import accounting.formatter.FormatUtils;
import accounting.model.Contact;
import accounting.model.ContactStatementEntry;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class ContactDataService extends AbstractDAO<Contact, Integer> {

    public ContactDataService() {
        super("contacts");
    }

    @Override
    protected Contact mapResultSetToEntity(ResultSet rs) throws SQLException {
        return new Contact(
            rs.getInt("contact_id"),
            rs.getString("name"),
            rs.getString("phone"),
            rs.getString("address"),
            rs.getBoolean("is_supplier"),
            rs.getBoolean("is_customer")
        );
    }

    @Override
    protected void mapEntityToPreparedStatement(Contact contact, PreparedStatement ps) throws SQLException {
        ps.setString(1, contact.getName());
        ps.setString(2, contact.getPhone());
        ps.setString(3, contact.getAddress());
        ps.setBoolean(4, contact.isSupplier());
        ps.setBoolean(5, contact.isCustomer());
    }

    @Override
    protected void mapEntityToUpdatePreparedStatement(Contact contact, PreparedStatement ps) throws SQLException {
        ps.setString(1, contact.getName());
        ps.setString(2, contact.getPhone());
        ps.setString(3, contact.getAddress());
        ps.setBoolean(4, contact.isSupplier());
        ps.setBoolean(5, contact.isCustomer());
        ps.setInt(6, contact.getContactId());
    }

    @Override
    protected String getPkColumnName() {
        return "contact_id";
    }

    @Override
    protected String getInsertSql() {
        return "INSERT INTO contacts(name, phone, address, is_supplier, is_customer) VALUES(?, ?, ?, ?, ?)";
    }

    @Override
    protected String getUpdateSql() {
        return "UPDATE contacts SET name = ?, phone = ?, address = ?, is_supplier = ?, is_customer = ? WHERE contact_id = ?";
    }
    
    @Override
    protected String getSelectAllSql() {
        return "SELECT contact_id, name, phone, address, is_supplier, is_customer FROM contacts";
    }

    public Optional<Contact> addContact(Contact contact) throws SQLException {
        Integer id = save(contact);
        if (id != null) {
            contact.setContactId(id);
            return Optional.of(contact);
        }
        return Optional.empty();
    }

    public boolean updateContact(Contact contact) throws SQLException {
        update(contact);
        return true;
    }

    public boolean deleteContact(int contactId) throws SQLException {
        // Soft delete is not handled by the generic DAO, so we do it manually.
        return dataManager.executeTransaction(conn -> {
            if (hasContactTransactions(conn, contactId)) {
                throw new SQLException("لا يمكن حذف جهة التعامل لوجود معاملات مرتبطة بها.");
            }
            
            String oldValues = getContactAsJson(conn, contactId);
            String sql = "UPDATE contacts SET is_active = 0, updated_at = CURRENT_TIMESTAMP WHERE contact_id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, contactId);
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    dataManager.logAuditEntry("contacts", contactId, "DELETE", oldValues, null, "SYSTEM", conn);
                }
                return rowsAffected > 0;
            }
        });
    }

    public List<Contact> getAllContacts() throws SQLException {
        return findAll();
    }

    public List<ContactStatementEntry> getContactStatement(int contactId, LocalDate fromDate, LocalDate toDate) throws SQLException {
        Contact contact = findById(contactId).orElse(null);
        if (contact == null) {
            throw new SQLException("Contact not found with ID: " + contactId);
        }

        List<ContactStatementEntry> statementEntries = new ArrayList<>();
        
        // 1. Get Opening Balance
        double openingBalance = 0;

        if (contact.isCustomer()) {
            String customerOpeningBalanceSql = """
                SELECT 
                    (SELECT COALESCE(SUM(total_sale_amount), 0) FROM sales WHERE customer_id = ? AND sale_date < ?) - 
                    (SELECT COALESCE(SUM(amount), 0) FROM payments WHERE contact_id = ? AND payment_type = 'RECEIVE' AND payment_date < ?) - 
                    (SELECT COALESCE(SUM(credit), 0) FROM general_ledger gl JOIN sales s ON gl.source_id = s.sale_id WHERE s.customer_id = ? AND gl.transaction_type = 'SALE_PAYMENT' AND gl.account_id = 10104 AND gl.entry_date < ?) -
                    (SELECT COALESCE(SUM(refund_amount), 0) FROM sale_returns WHERE original_sale_id IN (SELECT sale_id FROM sales WHERE customer_id = ?) AND return_date < ?) 
                    AS opening_balance
            """;
            try (Connection conn = dataManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(customerOpeningBalanceSql)) {
                stmt.setInt(1, contactId);
                stmt.setString(2, FormatUtils.formatDateForDatabase(fromDate));
                stmt.setInt(3, contactId);
                stmt.setString(4, FormatUtils.formatDateForDatabase(fromDate));
                stmt.setInt(5, contactId);
                stmt.setString(6, FormatUtils.formatDateForDatabase(fromDate));
                stmt.setInt(7, contactId);
                stmt.setString(8, FormatUtils.formatDateForDatabase(fromDate));
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        openingBalance += rs.getDouble("opening_balance");
                    }
                }
            }
        }

        if (contact.isSupplier()) {
            String supplierOpeningBalanceSql = """
                SELECT 
                    (SELECT COALESCE(SUM(total_cost), 0) FROM purchases WHERE supplier_id = ? AND purchase_date < ?) - 
                    (SELECT COALESCE(SUM(amount), 0) FROM payments WHERE contact_id = ? AND payment_type = 'PAY' AND payment_date < ?) -
                    (SELECT COALESCE(SUM(debit), 0) FROM general_ledger gl JOIN purchases p ON gl.source_id = p.purchase_id WHERE p.supplier_id = ? AND gl.transaction_type = 'PURCHASE_PAYMENT' AND gl.account_id = 20101 AND gl.entry_date < ?) -
                    (SELECT COALESCE(SUM(returned_cost), 0) FROM purchase_returns pr JOIN purchases p ON pr.original_purchase_id = p.purchase_id WHERE p.supplier_id = ? AND pr.return_date < ?)
                    AS opening_balance
            """;
            try (Connection conn = dataManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(supplierOpeningBalanceSql)) {
                stmt.setInt(1, contactId);
                stmt.setString(2, FormatUtils.formatDateForDatabase(fromDate));
                stmt.setInt(3, contactId);
                stmt.setString(4, FormatUtils.formatDateForDatabase(fromDate));
                stmt.setInt(5, contactId);
                stmt.setString(6, FormatUtils.formatDateForDatabase(fromDate));
                stmt.setInt(7, contactId);
                stmt.setString(8, FormatUtils.formatDateForDatabase(fromDate));
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        openingBalance -= rs.getDouble("opening_balance");
                    }
                }
            }
        }

        ContactStatementEntry openingEntry = new ContactStatementEntry(fromDate.minusDays(1), "رصيد أول المدة", "", null, null, "", openingBalance, openingBalance > 0);
        openingEntry.setBalance(openingBalance);
        statementEntries.add(openingEntry);

        if (contact.isCustomer()) {
            // Get Sales (Debits)
            String salesSql = "SELECT s.sale_date, s.total_sale_amount, c.crop_name, s.quantity_sold_kg, s.selling_unit_price, s.notes FROM sales s JOIN crops c ON s.crop_id = c.crop_id WHERE s.customer_id = ? AND s.sale_date BETWEEN ? AND ?";
            try (Connection conn = dataManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(salesSql)) {
                stmt.setInt(1, contactId);
                stmt.setString(2, FormatUtils.formatDateForDatabase(fromDate));
                stmt.setString(3, FormatUtils.formatDateForDatabase(toDate));
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        statementEntries.add(new ContactStatementEntry(
                            LocalDate.parse(rs.getString("sale_date")),
                            "صادر له بضاعة",
                            rs.getString("crop_name"),
                            rs.getDouble("quantity_sold_kg"),
                            rs.getDouble("selling_unit_price"),
                            rs.getString("notes"),
                            rs.getDouble("total_sale_amount"),
                            true // isDebit
                        ));
                    }
                }
            }

            // Get Payments Received (Credits) from payments table
            String paymentsSql = "SELECT payment_date, amount, description FROM payments WHERE contact_id = ? AND payment_type = 'RECEIVE' AND payment_date BETWEEN ? AND ?";
            try (Connection conn = dataManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(paymentsSql)) {
                stmt.setInt(1, contactId);
                stmt.setString(2, FormatUtils.formatDateForDatabase(fromDate));
                stmt.setString(3, FormatUtils.formatDateForDatabase(toDate));
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        statementEntries.add(new ContactStatementEntry(
                            LocalDate.parse(rs.getString("payment_date")),
                            "واصل منه نقدية",
                            "",
                            null, null, 
                            rs.getString("description"),
                            rs.getDouble("amount"),
                            false // isCredit
                        ));
                    }
                }
            }
            
            // Get Payments Received (Credits) from general_ledger
            String ledgerPaymentsSql = "SELECT gl.entry_date, gl.credit, gl.description FROM general_ledger gl JOIN sales s ON gl.source_id = s.sale_id WHERE s.customer_id = ? AND gl.transaction_type = 'SALE_PAYMENT' AND gl.account_id = 10104 AND gl.entry_date BETWEEN ? AND ?";
            try (Connection conn = dataManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(ledgerPaymentsSql)) {
                stmt.setInt(1, contactId);
                stmt.setString(2, FormatUtils.formatDateForDatabase(fromDate));
                stmt.setString(3, FormatUtils.formatDateForDatabase(toDate));
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        statementEntries.add(new ContactStatementEntry(
                            LocalDate.parse(rs.getString("entry_date")),
                            "واصل منه نقدية",
                            "",
                            null, null, 
                            rs.getString("description"),
                            rs.getDouble("credit"),
                            false // isCredit
                        ));
                    }
                }
            }
        }

        if (contact.isSupplier()) {
            // Get Purchases (Credits for supplier)
            String purchasesSql = "SELECT p.purchase_date, p.total_cost, c.crop_name, p.quantity_kg, p.unit_price, p.invoice_number FROM purchases p JOIN crops c ON p.crop_id = c.crop_id WHERE p.supplier_id = ? AND p.purchase_date BETWEEN ? AND ?";
            try (Connection conn = dataManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(purchasesSql)) {
                stmt.setInt(1, contactId);
                stmt.setString(2, FormatUtils.formatDateForDatabase(fromDate));
                stmt.setString(3, FormatUtils.formatDateForDatabase(toDate));
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        statementEntries.add(new ContactStatementEntry(
                            LocalDate.parse(rs.getString("purchase_date")),
                            "وارد منه بضاعة",
                            rs.getString("crop_name"),
                            rs.getDouble("quantity_kg"),
                            rs.getDouble("unit_price"),
                            "فاتورة رقم: " + rs.getString("invoice_number"),
                            rs.getDouble("total_cost"),
                            false // isCredit for supplier
                        ));
                    }
                }
            }

            // Get Payments Made (Debits for supplier) from payments table
            String paymentsMadeSql = "SELECT payment_date, amount, description FROM payments WHERE contact_id = ? AND payment_type = 'PAY' AND payment_date BETWEEN ? AND ?";
            try (Connection conn = dataManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(paymentsMadeSql)) {
                stmt.setInt(1, contactId);
                stmt.setString(2, FormatUtils.formatDateForDatabase(fromDate));
                stmt.setString(3, FormatUtils.formatDateForDatabase(toDate));
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        statementEntries.add(new ContactStatementEntry(
                            LocalDate.parse(rs.getString("payment_date")),
                            "مدفوع له نقدية",
                            "",
                            null, null,
                            rs.getString("description"),
                            rs.getDouble("amount"),
                            true // isDebit for supplier
                        ));
                    }
                }
            }
            
            // Get Payments Made (Debits for supplier) from general_ledger
            String ledgerPaymentsMadeSql = "SELECT gl.entry_date, gl.debit, gl.description FROM general_ledger gl JOIN purchases p ON gl.source_id = p.purchase_id WHERE p.supplier_id = ? AND gl.transaction_type = 'PURCHASE_PAYMENT' AND gl.account_id = 20101 AND gl.entry_date BETWEEN ? AND ?";
            try (Connection conn = dataManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(ledgerPaymentsMadeSql)) {
                stmt.setInt(1, contactId);
                stmt.setString(2, FormatUtils.formatDateForDatabase(fromDate));
                stmt.setString(3, FormatUtils.formatDateForDatabase(toDate));
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        statementEntries.add(new ContactStatementEntry(
                            LocalDate.parse(rs.getString("entry_date")),
                            "مدفوع له نقدية",
                            "",
                            null, null,
                            rs.getString("description"),
                            rs.getDouble("debit"),
                            true // isDebit for supplier
                        ));
                    }
                }
            }
        }

        // 4. Sort all transactions by date
        statementEntries.sort(Comparator.comparing(ContactStatementEntry::getDate));

        // 5. Calculate running balance
        double currentBalance = 0;
        // Find opening balance entry and start from there
        Optional<ContactStatementEntry> opening = statementEntries.stream().filter(e -> e.getReason().equals("رصيد أول المدة")).findFirst();
        if(opening.isPresent()){
            currentBalance = opening.get().getAmount();
        }

        for (ContactStatementEntry entry : statementEntries) {
            if (!entry.getReason().equals("رصيد أول المدة")) { // Don't re-calculate for opening balance
                if (entry.isDebit()) {
                    currentBalance += entry.getAmount();
                } else {
                    currentBalance -= entry.getAmount();
                }
                entry.setBalance(currentBalance);
            }
            // Set balance description
            if (entry.getBalance() > 0) {
                entry.setBalanceDescription("الباقي عليه");
            } else if (entry.getBalance() < 0) {
                entry.setBalanceDescription("الباقي له");
                entry.setBalance(entry.getBalance() * -1); // show as positive
            } else {
                entry.setBalanceDescription("خالص");
            }
        }

        return statementEntries;
    }
    
    private Contact mapResultSetToContact(ResultSet rs) throws SQLException {
        return new Contact(
            rs.getInt("contact_id"),
            rs.getString("name"),
            rs.getString("phone"),
            rs.getString("address"),
            rs.getBoolean("is_supplier"),
            rs.getBoolean("is_customer")
        );
    }
    
    private String getContactAsJson(Connection conn, int contactId) throws SQLException {
        String query = "SELECT name FROM contacts WHERE contact_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, contactId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("name");
                }
            }
        }
        return null;
    }

    private boolean hasContactTransactions(Connection conn, int contactId) throws SQLException {
        String query = """
            SELECT 1 FROM purchases WHERE supplier_id = ?
            UNION
            SELECT 1 FROM sales WHERE customer_id = ?
            LIMIT 1
            """;
        
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, contactId);
            stmt.setInt(2, contactId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }
}
