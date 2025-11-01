package accounting.service;

import accounting.dao.AbstractDAO;
import accounting.formatter.FormatUtils;
import accounting.model.Contact;
import accounting.model.Crop;
import accounting.model.FinancialAccount;
import accounting.model.SaleRecord;
import accounting.model.SaleReturn;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class SaleDataService extends AbstractDAO<SaleRecord, Integer> {

    private static final Logger LOGGER = Logger.getLogger(SaleDataService.class.getName());

    public SaleDataService() {
        super("sales");
    }

    @Override
    protected SaleRecord mapResultSetToEntity(ResultSet rs) throws SQLException {
        Crop crop = new Crop();
        crop.setCropId(rs.getInt("crop_id"));
        // The crop_name is not always available, so we don't set it here.

        Contact customer = new Contact();
        customer.setContactId(rs.getInt("customer_id"));
        // The customer_name is not always available, so we don't set it here.

        return new SaleRecord(
            rs.getInt("sale_id"),
            customer,
            crop,
            rs.getDouble("quantity_sold_kg"),
            rs.getString("selling_pricing_unit"),
            rs.getDouble("specific_selling_factor"),
            rs.getDouble("selling_unit_price"),
            rs.getDouble("total_sale_amount"),
            FormatUtils.parseDateFromDatabase(rs.getString("sale_date")),
            rs.getString("sale_invoice_number"),
            rs.getString("notes")
        );
    }

    @Override
    protected void mapEntityToPreparedStatement(SaleRecord sale, PreparedStatement ps) throws SQLException {
        ps.setInt(1, sale.getCrop().getCropId());
        ps.setInt(2, sale.getCustomer().getContactId());
        ps.setString(3, FormatUtils.formatDateForDatabase(sale.getSaleDate()));
        ps.setDouble(4, sale.getQuantitySoldKg());
        ps.setString(5, sale.getSellingPricingUnit());
        ps.setDouble(6, sale.getSpecificSellingFactor());
        ps.setDouble(7, sale.getSellingUnitPrice());
        ps.setDouble(8, sale.getTotalSaleAmount());
        ps.setString(9, sale.getSaleInvoiceNumber());
        ps.setDouble(10, 0); // amount_paid
        ps.setString(11, "PENDING"); // payment_status
        ps.setString(12, ""); // notes
    }

    @Override
    protected void mapEntityToUpdatePreparedStatement(SaleRecord sale, PreparedStatement ps) throws SQLException {
        // Not used for now
    }

    @Override
    protected String getPkColumnName() {
        return "sale_id";
    }

    @Override
    protected String getInsertSql() {
        return "INSERT INTO sales(crop_id, customer_id, sale_date, quantity_sold_kg, selling_pricing_unit, specific_selling_factor, selling_unit_price, total_sale_amount, sale_invoice_number, amount_paid, payment_status, notes) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    @Override
    protected String getUpdateSql() {
        return ""; // Not used for now
    }
    
    @Override
    protected String getSelectAllSql() {
        return "SELECT sale_id, customer_id, crop_id, quantity_sold_kg, selling_pricing_unit, specific_selling_factor, selling_unit_price, total_sale_amount, sale_date, sale_invoice_number, notes FROM sales";
    }

    public int addSale(SaleRecord sale, FinancialAccount paymentAccount, double amountReceived) throws SQLException {
        return dataManager.executeTransaction(conn -> addSaleLogic(conn, sale, paymentAccount, amountReceived));
    }

    private int addSaleLogic(Connection conn, SaleRecord sale, FinancialAccount paymentAccount, double amountReceived) throws SQLException {
        // 1. Determine payment status and final amount
        double finalAmountReceived = Math.min(amountReceived, sale.getTotalSaleAmount());
        String paymentStatus;
        if (finalAmountReceived <= 0) {
            paymentStatus = "PENDING";
        } else if (finalAmountReceived >= sale.getTotalSaleAmount()) {
            paymentStatus = "PAID";
        } else {
            paymentStatus = "PARTIAL";
        }

        // 2. Insert the base sale record
        String sql = "INSERT INTO sales(crop_id, customer_id, sale_date, quantity_sold_kg, selling_pricing_unit, specific_selling_factor, selling_unit_price, total_sale_amount, sale_invoice_number, amount_paid, payment_status) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        int saleId;
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, sale.getCrop().getCropId());
            pstmt.setInt(2, sale.getCustomer().getContactId());
            pstmt.setString(3, FormatUtils.formatDateForDatabase(sale.getSaleDate()));
            pstmt.setDouble(4, sale.getQuantitySoldKg());
            pstmt.setString(5, sale.getSellingPricingUnit());
            pstmt.setDouble(6, sale.getSpecificSellingFactor());
            pstmt.setDouble(7, sale.getSellingUnitPrice());
            pstmt.setDouble(8, sale.getTotalSaleAmount());
            pstmt.setString(9, sale.getSaleInvoiceNumber());
            pstmt.setDouble(10, finalAmountReceived);
            pstmt.setString(11, paymentStatus);
            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    saleId = generatedKeys.getInt(1);
                    sale.setSaleId(saleId);
                } else {
                    throw new SQLException("Creating sale failed, no ID obtained.");
                }
            }
        }

        // 3. Calculate Cost of Goods Sold (COGS)
        double unitCost = 0;
        try (PreparedStatement costStmt = conn.prepareStatement("SELECT average_cost_per_kg FROM inventory WHERE crop_id = ?")) {
            costStmt.setInt(1, sale.getCrop().getCropId());
            try (ResultSet rs = costStmt.executeQuery()) {
                if (rs.next()) {
                    unitCost = rs.getDouble("average_cost_per_kg");
                }
            }
        }
        double costOfGoodsSold = unitCost * sale.getQuantitySoldKg();

        // 4. Create ledger entries and update balances
        String transactionRef = "SAL-" + saleId;
        String description = "فاتورة بيع رقم: " + sale.getSaleInvoiceNumber() + " للعميل: " + sale.getCustomer().getName();
        int salesRevenueAccountId = 40101;
        int inventoryAccountId = 10103;
        int cogsAccountId = 50101;
        int accountsReceivableId = 10104;

        // --- Main Sale Entry ---
        dataManager.addLedgerEntry(conn, transactionRef, sale.getSaleDate(), accountsReceivableId, sale.getTotalSaleAmount(), 0.0, description, "SALE", saleId, "SALE");
        dataManager.addLedgerEntry(conn, transactionRef, sale.getSaleDate(), salesRevenueAccountId, 0.0, sale.getTotalSaleAmount(), description, "SALE", saleId, "SALE");
        
        dataManager.updateAccountBalance(accountsReceivableId, sale.getTotalSaleAmount(), conn);
        dataManager.updateAccountBalance(salesRevenueAccountId, sale.getTotalSaleAmount(), conn);

        // --- COGS Entry ---
        if (costOfGoodsSold > 0) {
            String cogsDesc = "تكلفة بضاعة مباعة: " + description;
            dataManager.addLedgerEntry(conn, transactionRef, sale.getSaleDate(), cogsAccountId, costOfGoodsSold, 0.0, cogsDesc, "SALE", saleId, "SALE");
            dataManager.addLedgerEntry(conn, transactionRef, sale.getSaleDate(), inventoryAccountId, 0.0, costOfGoodsSold, cogsDesc, "SALE", saleId, "SALE");
            
            dataManager.updateAccountBalance(cogsAccountId, costOfGoodsSold, conn);
            dataManager.updateAccountBalance(inventoryAccountId, -costOfGoodsSold, conn);
        }

        // --- Payment Entry (if applicable) ---
        if (finalAmountReceived > 0 && paymentAccount != null) {
            String paymentDesc = "دفعة من العميل: " + sale.getCustomer().getName();
            dataManager.addLedgerEntry(conn, transactionRef, sale.getSaleDate(), paymentAccount.getAccountId(), finalAmountReceived, 0.0, paymentDesc, "SALE_PAYMENT", saleId, "SALE_PAYMENT");
            dataManager.addLedgerEntry(conn, transactionRef, sale.getSaleDate(), accountsReceivableId, 0.0, finalAmountReceived, paymentDesc, "SALE_PAYMENT", saleId, "SALE_PAYMENT");

            dataManager.updateAccountBalance(paymentAccount.getAccountId(), finalAmountReceived, conn);
            dataManager.updateAccountBalance(accountsReceivableId, -finalAmountReceived, conn);
        }

        // 5. Update Inventory & Audit Log
        dataManager.updateInventory(sale.getCrop().getCropId(), -sale.getQuantitySoldKg(), unitCost, "OUT", "SALE", saleId, conn);
        dataManager.logAuditEntry("sales", saleId, "INSERT", null, sale.getSaleInvoiceNumber(), "SYSTEM", conn);
        
        return saleId;
    }

    public void updateSale(SaleRecord updatedSale, FinancialAccount paymentAccount, double amountReceived) throws SQLException {
        dataManager.executeTransaction(conn -> {
            // First, reverse the old sale
            deleteSaleLogic(conn, updatedSale.getSaleId());
            // Then, add the new sale. We need to re-assign the ID to the updatedSale object for the logic to work.
            int newSaleId = addSaleLogic(conn, updatedSale, paymentAccount, amountReceived);
            if (newSaleId != updatedSale.getSaleId()) {
                 // This case should ideally not happen if the PK sequence is handled well,
                 // but as a safeguard, we log it.
                 LOGGER.warning("Sale ID changed during update from " + updatedSale.getSaleId() + " to " + newSaleId);
            }
            return null; // Return type is Void
        });
    }

    public boolean deleteSale(int saleId) throws SQLException {
        return dataManager.executeTransaction(conn -> deleteSaleLogic(conn, saleId));
    }

    private boolean deleteSaleLogic(Connection conn, int saleId) throws SQLException {
        // 1. Get the original sale to know what to reverse
        SaleRecord sale = getSaleById(saleId, conn);
        if (sale == null) {
            throw new SQLException("Sale with ID " + saleId + " not found for deletion.");
        }

        // 2. Get original financial details for reversal
        String transactionRef = "SAL-" + saleId;
        double originalCostOfGoodsSold = 0;
        double originalAmountPaid = 0;

        try (PreparedStatement cogsStmt = conn.prepareStatement("SELECT debit FROM general_ledger WHERE transaction_ref = ? AND account_id = 50101")) {
            cogsStmt.setString(1, transactionRef);
            try (ResultSet rs = cogsStmt.executeQuery()) {
                if (rs.next()) {
                    originalCostOfGoodsSold = rs.getDouble("debit");
                }
            }
        }
        try (PreparedStatement paidStmt = conn.prepareStatement("SELECT amount_paid FROM sales WHERE sale_id = ?")) {
            paidStmt.setInt(1, saleId);
            try (ResultSet rs = paidStmt.executeQuery()) {
                if (rs.next()) {
                    originalAmountPaid = rs.getDouble("amount_paid");
                }
            }
        }

        // 3. Reverse Balance Updates
        double originalRemainingBalance = sale.getTotalSaleAmount() - originalAmountPaid;
        int salesRevenueAccountId = 40101;
        int inventoryAccountId = 10103;
        int cogsAccountId = 50101;
        int accountsReceivableId = 10104;

        dataManager.updateAccountBalance(salesRevenueAccountId, -sale.getTotalSaleAmount(), conn);
        if (originalCostOfGoodsSold > 0) {
            dataManager.updateAccountBalance(inventoryAccountId, originalCostOfGoodsSold, conn);
            dataManager.updateAccountBalance(cogsAccountId, -originalCostOfGoodsSold, conn);
        }
        if (originalRemainingBalance > 0) {
            dataManager.updateAccountBalance(accountsReceivableId, -originalRemainingBalance, conn);
        }
        // Note: Reversing payment would require knowing the original payment account, which is complex.
        // This implementation assumes payment reversal is handled separately if needed.

        // 4. Reverse inventory quantity
        double unitCost = (sale.getQuantitySoldKg() > 0) ? originalCostOfGoodsSold / sale.getQuantitySoldKg() : 0;
        dataManager.updateInventory(sale.getCrop().getCropId(), sale.getQuantitySoldKg(), unitCost, "IN", "SALE_DELETE", saleId, conn);

        // 5. Delete General Ledger entries for the sale
        try (PreparedStatement deleteLedgerStmt = conn.prepareStatement("DELETE FROM general_ledger WHERE transaction_ref = ?")) {
            deleteLedgerStmt.setString(1, transactionRef);
            deleteLedgerStmt.executeUpdate();
        }

        // 6. Delete the sale record
        try (PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM sales WHERE sale_id = ?")) {
            deleteStmt.setInt(1, saleId);
            int rowsAffected = deleteStmt.executeUpdate();
            if (rowsAffected > 0) {
                dataManager.logAuditEntry("sales", saleId, "DELETE", sale.getSaleInvoiceNumber(), null, "SYSTEM", conn);
            }
            return rowsAffected > 0;
        }
    }

    // Other methods (getSales, mapResultSetToSale, etc.) remain here...
    public List<SaleRecord> getSales(LocalDate fromDate, LocalDate toDate, Integer cropId, Integer customerId, String searchText) throws SQLException {
        StringBuilder queryBuilder = new StringBuilder("""
            SELECT s.*, c.crop_name, ct.name as customer_name
            FROM sales s
            JOIN crops c ON s.crop_id = c.crop_id
            JOIN contacts ct ON s.customer_id = ct.contact_id
            WHERE 1=1
            """);
        
        List<Object> parameters = new ArrayList<>();
        
        if (fromDate != null) {
            queryBuilder.append(" AND s.sale_date >= ?");
            parameters.add(FormatUtils.formatDateForDatabase(fromDate));
        }
        if (toDate != null) {
            queryBuilder.append(" AND s.sale_date <= ?");
            parameters.add(FormatUtils.formatDateForDatabase(toDate));
        }
        if (cropId != null) {
            queryBuilder.append(" AND s.crop_id = ?");
            parameters.add(cropId);
        }
        if (customerId != null) {
            queryBuilder.append(" AND s.customer_id = ?");
            parameters.add(customerId);
        }
        if (searchText != null && !searchText.trim().isEmpty()) {
            queryBuilder.append(" AND (s.sale_invoice_number LIKE ? OR ct.name LIKE ?)");
            String searchPattern = "%" + searchText.toLowerCase() + "%";
            parameters.add(searchPattern);
            parameters.add(searchPattern);
        }
        queryBuilder.append(" ORDER BY s.sale_date DESC, s.sale_id DESC");
        
        List<SaleRecord> sales = new ArrayList<>();
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(queryBuilder.toString())) {
            for (int i = 0; i < parameters.size(); i++) {
                stmt.setObject(i + 1, parameters.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sales.add(mapResultSetToSale(rs));
                }
            }
        }
        return sales;
    }

    private SaleRecord mapResultSetToSale(ResultSet rs) throws SQLException {
        Crop crop = new Crop();
        crop.setCropId(rs.getInt("crop_id"));
        crop.setCropName(rs.getString("crop_name"));
        
        Contact customer = new Contact();
        customer.setContactId(rs.getInt("customer_id"));
        customer.setName(rs.getString("customer_name"));

        return new SaleRecord(
            rs.getInt("sale_id"),
            customer,
            crop,
            rs.getDouble("quantity_sold_kg"),
            rs.getString("selling_pricing_unit"),
            rs.getDouble("specific_selling_factor"),
            rs.getDouble("selling_unit_price"),
            rs.getDouble("total_sale_amount"),
            FormatUtils.parseDateFromDatabase(rs.getString("sale_date")),
            rs.getString("sale_invoice_number"),
            rs.getString("notes")
        );
    }

    public Map<String, Double> getSalesStatistics(LocalDate fromDate, LocalDate toDate) throws SQLException {
        String sql = "SELECT SUM(total_sale_amount) as total_revenue, COUNT(*) as sales_count FROM sales WHERE sale_date BETWEEN ? AND ?";
        Map<String, Double> stats = new HashMap<>();
        stats.put("total_revenue", 0.0);
        stats.put("sales_count", 0.0);

        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, FormatUtils.formatDateForDatabase(fromDate));
            stmt.setString(2, FormatUtils.formatDateForDatabase(toDate));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    stats.put("total_revenue", rs.getDouble("total_revenue"));
                    stats.put("sales_count", rs.getDouble("sales_count"));
                }
            }
        }
        return stats;
    }

    public Map<String, Number> getMonthlySalesForChart(int year) throws SQLException {
        String sql = "SELECT strftime('%m', sale_date) as month, SUM(total_sale_amount) as monthly_total " +
                     "FROM sales WHERE strftime('%Y', sale_date) = ? " +
                     "GROUP BY month ORDER BY month";
        
        Map<String, Number> monthlySales = new LinkedHashMap<>();
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, String.valueOf(year));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    monthlySales.put(rs.getString("month"), rs.getDouble("monthly_total"));
                }
            }
        }
        return monthlySales;
    }

    public int addSaleReturn(SaleReturn saleReturn) throws SQLException {
        return dataManager.executeTransaction(conn -> {
            
            // 1. إضافة سجل المرتجع إلى قاعدة البيانات
            String sql = "INSERT INTO sale_returns (original_sale_id, return_date, crop_id, quantity_kg, return_reason, refund_amount) VALUES (?, ?, ?, ?, ?, ?)";
            int returnId;
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, saleReturn.getOriginalSale().getSaleId());
                stmt.setString(2, FormatUtils.formatDateForDatabase(saleReturn.getReturnDate()));
                stmt.setInt(3, saleReturn.getOriginalSale().getCrop().getCropId());
                stmt.setDouble(4, saleReturn.getQuantityKg());
                stmt.setString(5, saleReturn.getReturnReason());
                stmt.setDouble(6, saleReturn.getRefundAmount());
                
                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        returnId = rs.getInt(1);
                    } else {
                        throw new SQLException("Creating sale return failed, no ID obtained.");
                    }
                }
            }

            // 2. تحديد الحسابات وحساب التكلفة
            String transactionRef = "SAL-RTN-" + returnId;
            String description = "مرتجع مبيعات من فاتورة رقم: " + saleReturn.getOriginalSale().getSaleInvoiceNumber();
            int salesReturnAccountId = 40102;
            int accountsReceivableId = 10104;
            int inventoryAccountId = 10103;
            int cogsAccountId = 50101;

            double costOfReturnedGoods = 0;
            String originalTransactionRef = "SAL-" + saleReturn.getOriginalSale().getSaleId();
            String cogsQuery = "SELECT debit FROM general_ledger WHERE transaction_ref = ? AND account_id = ?";
            
            try (PreparedStatement cogsStmt = conn.prepareStatement(cogsQuery)) {
                cogsStmt.setString(1, originalTransactionRef);
                cogsStmt.setInt(2, cogsAccountId);
                try (ResultSet rs = cogsStmt.executeQuery()) {
                    if (rs.next()) {
                        double totalCogs = rs.getDouble("debit");
                        double originalQuantity = saleReturn.getOriginalSale().getQuantitySoldKg();
                        if (originalQuantity > 0) {
                            double costPerKg = totalCogs / originalQuantity;
                            costOfReturnedGoods = costPerKg * saleReturn.getQuantityKg();
                        }
                    }
                }
            }

            // 3. تسجيل القيود المحاسبية المزدوجة
            dataManager.addLedgerEntry(conn, transactionRef, saleReturn.getReturnDate(), salesReturnAccountId, saleReturn.getRefundAmount(), 0.0, description, "SALE_RETURN", returnId, "SALE_RETURN");
            dataManager.addLedgerEntry(conn, transactionRef, saleReturn.getReturnDate(), accountsReceivableId, 0.0, saleReturn.getRefundAmount(), description, "SALE_RETURN", returnId, "SALE_RETURN");

            if (costOfReturnedGoods > 0) {
                String cogsDescription = "عكس تكلفة بضاعة مرتجعة للفاتورة " + saleReturn.getOriginalSale().getSaleInvoiceNumber();
                dataManager.addLedgerEntry(conn, transactionRef, saleReturn.getReturnDate(), inventoryAccountId, costOfReturnedGoods, 0.0, cogsDescription, "SALE_RETURN", returnId, "SALE_RETURN");
                dataManager.addLedgerEntry(conn, transactionRef, saleReturn.getReturnDate(), cogsAccountId, 0.0, costOfReturnedGoods, cogsDescription, "SALE_RETURN", returnId, "SALE_RETURN");
            }

            // 4. تحديث أرصدة الحسابات الإجمالية
            dataManager.updateAccountBalance(salesReturnAccountId, saleReturn.getRefundAmount(), conn); // مرتجعات المبيعات تزيد (طبيعتها مدينة)
            dataManager.updateAccountBalance(accountsReceivableId, -saleReturn.getRefundAmount(), conn); // الذمم المدينة تقل
            if (costOfReturnedGoods > 0) {
                dataManager.updateAccountBalance(inventoryAccountId, costOfReturnedGoods, conn); // المخزون يزيد
                dataManager.updateAccountBalance(cogsAccountId, -costOfReturnedGoods, conn); // تكلفة البضاعة المباعة تقل
            }

            // 5. تحديث كمية المخزون (عملية إدارية)
            double unitCostOfReturn = (saleReturn.getQuantityKg() > 0) ? costOfReturnedGoods / saleReturn.getQuantityKg() : 0;
            dataManager.updateInventory(
                saleReturn.getOriginalSale().getCrop().getCropId(), 
                saleReturn.getQuantityKg(), // الكمية بالموجب لأنها تعود للمخزون
                unitCostOfReturn, 
                "IN", 
                "SALE_RETURN", 
                returnId, 
                conn
            );

            dataManager.logAuditEntry("sale_returns", returnId, "INSERT", null, description, "SYSTEM", conn);
            
            return returnId;
        });
    }

    public SaleRecord getSaleById(int saleId) throws SQLException {
        try (Connection conn = dataManager.getConnection()) {
            return getSaleById(saleId, conn);
        }
    }

    private SaleRecord getSaleById(int saleId, Connection conn) throws SQLException {
        String sql = "SELECT s.*, c.crop_name, ct.name as customer_name FROM sales s JOIN crops c ON s.crop_id = c.crop_id JOIN contacts ct ON s.customer_id = ct.contact_id WHERE s.sale_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, saleId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToSale(rs);
                }
            }
        }
        return null;
    }
}