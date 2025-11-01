package accounting.service;

import accounting.dao.AbstractDAO;
import accounting.formatter.FormatUtils;
import accounting.model.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * خدمات البيانات المحسنة للمشتريات
 * 
 * توفر هذه الخدمة وظائف متكاملة لإدارة عمليات الشراء في النظام، بما في ذلك:
 * - إضافة مشتريات جديدة مع التحديث التلقائي للمخزون والحسابات
 * - تعديل وحذف عمليات الشراء الموجودة
 * - استرجاع تقارير واحصائيات عن المشتريات
 * - إدارة مرتجعات المشتريات
 * 
 * تستخدم هذه الخدمة نموذج المعاملات لضمان استمرارية البيانات،
 * وتقوم بتحديث دفتر الأستاذ تلقائياً عند كل عملية شراء.
 */
public class PurchaseDataService extends AbstractDAO<PurchaseRecord, Integer> {

    private static final Logger LOGGER = Logger.getLogger(PurchaseDataService.class.getName());

    /**
     * مُنشئ خدمة بيانات المشتريات
     */
    public PurchaseDataService() {
        super("purchases");
    }

    @Override
    protected PurchaseRecord mapResultSetToEntity(ResultSet rs) throws SQLException {
        PurchaseRecord purchase = new PurchaseRecord();
        purchase.setPurchaseId(rs.getInt("purchase_id"));
        
        Crop crop = new Crop();
        crop.setCropId(rs.getInt("crop_id"));
        crop.setCropName(rs.getString("crop_name"));
        purchase.setCrop(crop);
        
        Contact supplier = new Contact();
        supplier.setContactId(rs.getInt("supplier_id"));
        supplier.setName(rs.getString("supplier_name"));
        purchase.setSupplier(supplier);
        
        purchase.setPurchaseDate(FormatUtils.parseDateFromDatabase(rs.getString("purchase_date")));
        purchase.setQuantityKg(rs.getDouble("quantity_kg"));
        purchase.setPricingUnit(rs.getString("pricing_unit"));
        purchase.setSpecificFactor(rs.getDouble("specific_factor"));
        purchase.setUnitPrice(rs.getDouble("unit_price"));
        purchase.setTotalCost(rs.getDouble("total_cost"));
        purchase.setInvoiceNumber(rs.getString("invoice_number"));
        
        return purchase;
    }

    @Override
    protected void mapEntityToPreparedStatement(PurchaseRecord purchase, PreparedStatement ps) throws SQLException {
        ps.setInt(1, purchase.getCrop().getCropId());
        ps.setInt(2, purchase.getSupplier().getContactId());
        ps.setString(3, FormatUtils.formatDateForDatabase(purchase.getPurchaseDate()));
        ps.setDouble(4, purchase.getQuantityKg());
        ps.setString(5, purchase.getPricingUnit());
        ps.setDouble(6, purchase.getSpecificFactor());
        ps.setDouble(7, purchase.getUnitPrice());
        ps.setDouble(8, purchase.getTotalCost());
        ps.setString(9, purchase.getInvoiceNumber());
        ps.setDouble(10, 0); // amount_paid
        ps.setString(11, "PENDING"); // payment_status
        ps.setString(12, ""); // notes
    }

    @Override
    protected void mapEntityToUpdatePreparedStatement(PurchaseRecord purchase, PreparedStatement ps) throws SQLException {
        // This is more complex due to the ledger entries, so we will not use the generic update for now.
    }

    @Override
    protected String getPkColumnName() {
        return "purchase_id";
    }

    @Override
    protected String getInsertSql() {
        return "INSERT INTO purchases (crop_id, supplier_id, purchase_date, quantity_kg, pricing_unit, specific_factor, unit_price, total_cost, invoice_number, amount_paid, payment_status, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    @Override
    protected String getUpdateSql() {
        return ""; // Not used for now
    }
    
    @Override
    protected String getSelectAllSql() {
        return "SELECT p.*, c.crop_name, ct.name as supplier_name FROM purchases p JOIN crops c ON p.crop_id = c.crop_id JOIN contacts ct ON p.supplier_id = ct.contact_id";
    }

    private int addPurchaseLogic(Connection conn, PurchaseRecord purchase, FinancialAccount paymentAccount, double amountPaid) throws SQLException {
        // 1. Determine payment status and final amount
        double finalAmountPaid = Math.min(amountPaid, purchase.getTotalCost());
        String paymentStatus;
        if (finalAmountPaid <= 0) {
            paymentStatus = "PENDING";
        } else if (finalAmountPaid >= purchase.getTotalCost()) {
            paymentStatus = "PAID";
        } else {
            paymentStatus = "PARTIAL";
        }

        // 2. Insert the base purchase record
        String insertQuery = """
            INSERT INTO purchases (crop_id, supplier_id, purchase_date, quantity_kg,
                                 pricing_unit, specific_factor, unit_price, total_cost,
                                 invoice_number, amount_paid, payment_status, notes)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        int purchaseId;
        try (PreparedStatement stmt = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, purchase.getCrop().getCropId());
            stmt.setInt(2, purchase.getSupplier().getContactId());
            stmt.setString(3, FormatUtils.formatDateForDatabase(purchase.getPurchaseDate()));
            stmt.setDouble(4, purchase.getQuantityKg());
            stmt.setString(5, purchase.getPricingUnit());
            stmt.setDouble(6, purchase.getSpecificFactor());
            stmt.setDouble(7, purchase.getUnitPrice());
            stmt.setDouble(8, purchase.getTotalCost());
            stmt.setString(9, purchase.getInvoiceNumber());
            stmt.setDouble(10, finalAmountPaid);
            stmt.setString(11, paymentStatus);
            stmt.setString(12, ""); // Notes
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    purchaseId = generatedKeys.getInt(1);
                    purchase.setPurchaseId(purchaseId);
                } else {
                    throw new SQLException("فشل في الحصول على معرف الشراء الجديد");
                }
            }
        }
        
        // 3. Create ledger entries and update balances
        String transactionRef = "PUR-" + purchaseId;
        String description = "شراء فاتورة رقم: " + purchase.getInvoiceNumber();
        int inventoryAccountId = 10103; 
        int accountsPayableId = 20101;

        // --- Main Purchase Entry ---
        dataManager.addLedgerEntry(conn, transactionRef, purchase.getPurchaseDate(), inventoryAccountId, purchase.getTotalCost(), 0.0, description, "PURCHASE", purchaseId, "PURCHASE");
        dataManager.addLedgerEntry(conn, transactionRef, purchase.getPurchaseDate(), accountsPayableId, 0.0, purchase.getTotalCost(), description, "PURCHASE", purchaseId, "PURCHASE");
        
        dataManager.updateAccountBalance(inventoryAccountId, purchase.getTotalCost(), conn);
        dataManager.updateAccountBalance(accountsPayableId, purchase.getTotalCost(), conn);

        // --- Payment Entry (if applicable) ---
        if (finalAmountPaid > 0 && paymentAccount != null) {
            String paymentDesc = "دفعة لفاتورة شراء رقم: " + transactionRef;
            dataManager.addLedgerEntry(conn, transactionRef, purchase.getPurchaseDate(), accountsPayableId, finalAmountPaid, 0.0, paymentDesc, "PURCHASE_PAYMENT", purchaseId, "PURCHASE_PAYMENT");
            dataManager.addLedgerEntry(conn, transactionRef, purchase.getPurchaseDate(), paymentAccount.getAccountId(), 0.0, finalAmountPaid, paymentDesc, "PURCHASE_PAYMENT", purchaseId, "PURCHASE_PAYMENT");

            dataManager.updateAccountBalance(accountsPayableId, -finalAmountPaid, conn);
            dataManager.updateAccountBalance(paymentAccount.getAccountId(), -finalAmountPaid, conn);
        }

        // 4. Update Inventory & Audit Log
        double unitCost = purchase.getQuantityKg() > 0 ? purchase.getTotalCost() / purchase.getQuantityKg() : 0;
        dataManager.updateInventory(purchase.getCrop().getCropId(), purchase.getQuantityKg(), unitCost, "IN", "PURCHASE", purchaseId, conn);
        dataManager.logAuditEntry("purchases", purchaseId, "INSERT", null, purchase.getInvoiceNumber(), "SYSTEM", conn);
        
        return purchaseId;
    }

    public int addPurchase(PurchaseRecord purchase, FinancialAccount paymentAccount, double amountPaid) throws SQLException {
        return dataManager.executeTransaction(conn -> addPurchaseLogic(conn, purchase, paymentAccount, amountPaid));
    }

    public void updatePurchase(PurchaseRecord updatedPurchase) throws SQLException {
        dataManager.executeTransaction(conn -> {
            // First, reverse the old purchase
            deletePurchaseLogic(conn, updatedPurchase.getPurchaseId());
            // Then, add the new purchase. We need to re-assign the ID to the updatedPurchase object for the logic to work.
            int newPurchaseId = addPurchaseLogic(conn, updatedPurchase, null, 0); // Assuming no payment is made during update
            if (newPurchaseId != updatedPurchase.getPurchaseId()) {
                 LOGGER.warning("Purchase ID changed during update from " + updatedPurchase.getPurchaseId() + " to " + newPurchaseId);
            }
            return null; // Return type is Void
        });
    }

    public boolean deletePurchase(int purchaseId) throws SQLException {
        return dataManager.executeTransaction(conn -> deletePurchaseLogic(conn, purchaseId));
    }

    private boolean deletePurchaseLogic(Connection conn, int purchaseId) throws SQLException {
        PurchaseRecord purchase = getPurchaseById(purchaseId, conn);
        if (purchase == null) {
            throw new SQLException("لم يتم العثور على سجل الشراء رقم: " + purchaseId);
        }

        String originalTransactionRef = "PUR-" + purchaseId;
        String reversalRef = "REV-PUR-" + purchaseId;
        String description = "عكس عملية شراء فاتورة رقم: " + purchase.getInvoiceNumber();
        
        int inventoryAccountId = 10103;
        int accountsPayableId = 20101;

        try (PreparedStatement getLedgerStmt = conn.prepareStatement("SELECT account_id, debit, credit FROM general_ledger WHERE transaction_ref = ?")) {
            getLedgerStmt.setString(1, originalTransactionRef);
            try (ResultSet rs = getLedgerStmt.executeQuery()) {
                while (rs.next()) {
                    dataManager.addLedgerEntry(conn, reversalRef, LocalDate.now(), rs.getInt("account_id"), rs.getDouble("credit"), rs.getDouble("debit"), description, "PURCHASE_DELETE", purchaseId, "PURCHASE_DELETE");
                }
            }
        }

        dataManager.updateAccountBalance(inventoryAccountId, -purchase.getTotalCost(), conn);
        dataManager.updateAccountBalance(accountsPayableId, -purchase.getTotalCost(), conn);

        double unitCost = purchase.getQuantityKg() > 0 ? purchase.getTotalCost() / purchase.getQuantityKg() : 0;
        dataManager.updateInventory(purchase.getCrop().getCropId(), -purchase.getQuantityKg(), unitCost, "OUT", "PURCHASE_DELETE", purchaseId, conn);

        String deleteQuery = "DELETE FROM purchases WHERE purchase_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(deleteQuery)) {
            stmt.setInt(1, purchaseId);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                dataManager.logAuditEntry("purchases", purchaseId, "DELETE", purchase.getInvoiceNumber(), null, "SYSTEM", conn);
            }
            return rowsAffected > 0;
        }
    }

    public PurchaseRecord getPurchaseById(int purchaseId) throws SQLException {
        return findById(purchaseId).orElse(null);
    }

    public PurchaseRecord getPurchaseById(int purchaseId, Connection conn) throws SQLException {
        String sql = "SELECT p.*, c.crop_name, ct.name as supplier_name FROM purchases p JOIN crops c ON p.crop_id = c.crop_id JOIN contacts ct ON p.supplier_id = ct.contact_id WHERE p.purchase_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, purchaseId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToEntity(rs);
                }
            }
        }
        return null;
    }

    public List<PurchaseRecord> getPurchases(LocalDate fromDate, LocalDate toDate, Integer cropId, Integer supplierId, String searchText) throws SQLException {
        StringBuilder queryBuilder = new StringBuilder("SELECT p.*, c.crop_name, ct.name as supplier_name FROM purchases p JOIN crops c ON p.crop_id = c.crop_id JOIN contacts ct ON p.supplier_id = ct.contact_id WHERE 1=1");
        List<Object> parameters = new ArrayList<>();
        
        if (fromDate != null) {
            queryBuilder.append(" AND p.purchase_date >= ?");
            parameters.add(FormatUtils.formatDateForDatabase(fromDate));
        }
        if (toDate != null) {
            queryBuilder.append(" AND p.purchase_date <= ?");
            parameters.add(FormatUtils.formatDateForDatabase(toDate));
        }
        if (cropId != null) {
            queryBuilder.append(" AND p.crop_id = ?");
            parameters.add(cropId);
        }
        if (supplierId != null) {
            queryBuilder.append(" AND p.supplier_id = ?");
            parameters.add(supplierId);
        }
        if (searchText != null && !searchText.trim().isEmpty()) {
            queryBuilder.append(" AND (p.invoice_number LIKE ? OR ct.name LIKE ?)");
            String searchPattern = "%" + searchText.toLowerCase() + "%";
            parameters.add(searchPattern);
            parameters.add(searchPattern);
        }
        queryBuilder.append(" ORDER BY p.purchase_date DESC, p.purchase_id DESC");
        
        List<PurchaseRecord> purchases = new ArrayList<>();
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(queryBuilder.toString())) {
            for (int i = 0; i < parameters.size(); i++) {
                stmt.setObject(i + 1, parameters.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    purchases.add(mapResultSetToEntity(rs));
                }
            }
        }
        return purchases;
    }

    public static class PurchaseStatistics {
        private final int totalRecords;
        private final double totalQuantity;
        private final double totalCost;
        private final double averageUnitPrice;
        private final LocalDate firstPurchaseDate;
        private final LocalDate lastPurchaseDate;
        
        public PurchaseStatistics(int totalRecords, double totalQuantity, double totalCost, double averageUnitPrice, LocalDate firstPurchaseDate, LocalDate lastPurchaseDate) {
            this.totalRecords = totalRecords;
            this.totalQuantity = totalQuantity;
            this.totalCost = totalCost;
            this.averageUnitPrice = averageUnitPrice;
            this.firstPurchaseDate = firstPurchaseDate;
            this.lastPurchaseDate = lastPurchaseDate;
        }
        
        public int getTotalRecords() { return totalRecords; }
        public double getTotalQuantity() { return totalQuantity; }
        public double getTotalCost() { return totalCost; }
        public double getAverageUnitPrice() { return averageUnitPrice; }
        public LocalDate getFirstPurchaseDate() { return firstPurchaseDate; }
        public LocalDate getLastPurchaseDate() { return lastPurchaseDate; }
        
        public double getAverageCostPerKg() {
            return totalQuantity > 0 ? totalCost / totalQuantity : 0;
        }
    }

    public PurchaseStatistics getPurchaseStatistics(LocalDate fromDate, LocalDate toDate, Integer cropId, Integer supplierId) throws SQLException {
        StringBuilder queryBuilder = new StringBuilder("SELECT COUNT(*) as totalRecords, SUM(quantity_kg) as totalQuantity, SUM(total_cost) as totalCost, AVG(unit_price) as avgPrice, MIN(purchase_date) as firstPurchase, MAX(purchase_date) as lastPurchase FROM purchases WHERE 1=1");
        List<Object> parameters = new ArrayList<>();

        if (fromDate != null) {
            queryBuilder.append(" AND purchase_date >= ?");
            parameters.add(FormatUtils.formatDateForDatabase(fromDate));
        }
        if (toDate != null) {
            queryBuilder.append(" AND purchase_date <= ?");
            parameters.add(FormatUtils.formatDateForDatabase(toDate));
        }
        if (cropId != null) {
            queryBuilder.append(" AND crop_id = ?");
            parameters.add(cropId);
        }
        if (supplierId != null) {
            queryBuilder.append(" AND supplier_id = ?");
            parameters.add(supplierId);
        }

        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(queryBuilder.toString())) {
            for (int i = 0; i < parameters.size(); i++) {
                stmt.setObject(i + 1, parameters.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new PurchaseStatistics(
                        rs.getInt("totalRecords"),
                        rs.getDouble("totalQuantity"),
                        rs.getDouble("totalCost"),
                        rs.getDouble("avgPrice"),
                        FormatUtils.parseDateFromDatabase(rs.getString("firstPurchase")),
                        FormatUtils.parseDateFromDatabase(rs.getString("lastPurchase"))
                    );
                }
            }
        }
        return new PurchaseStatistics(0, 0, 0, 0, null, null); // Return empty stats if no data
    }

    public int addPurchaseReturn(PurchaseReturn purchaseReturn) throws SQLException {
        return dataManager.executeTransaction(conn -> {
            
            String sql = "INSERT INTO purchase_returns (original_purchase_id, return_date, crop_id, quantity_kg, return_reason, returned_cost) VALUES (?, ?, ?, ?, ?, ?)";
            int returnId;
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, purchaseReturn.getOriginalPurchase().getPurchaseId());
                stmt.setString(2, FormatUtils.formatDateForDatabase(purchaseReturn.getReturnDate()));
                stmt.setInt(3, purchaseReturn.getOriginalPurchase().getCrop().getCropId());
                stmt.setDouble(4, purchaseReturn.getQuantityKg());
                stmt.setString(5, purchaseReturn.getReturnReason());
                stmt.setDouble(6, purchaseReturn.getReturnedCost());
                
                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        returnId = rs.getInt(1);
                    } else {
                        throw new SQLException("Creating purchase return failed, no ID obtained.");
                    }
                }
            }

            String transactionRef = "PUR-RTN-" + returnId;
            String description = "مرتجع شراء للفاتورة رقم: " + purchaseReturn.getOriginalPurchase().getInvoiceNumber();
            int accountsPayableId = 20101;
            int inventoryAccountId = 10103;

            dataManager.addLedgerEntry(conn, transactionRef, purchaseReturn.getReturnDate(), accountsPayableId, purchaseReturn.getReturnedCost(), 0.0, description, "PURCHASE_RETURN", returnId, "PURCHASE_RETURN");
            dataManager.addLedgerEntry(conn, transactionRef, purchaseReturn.getReturnDate(), inventoryAccountId, 0.0, purchaseReturn.getReturnedCost(), description, "PURCHASE_RETURN", returnId, "PURCHASE_RETURN");

            dataManager.updateAccountBalance(accountsPayableId, -purchaseReturn.getReturnedCost(), conn);
            dataManager.updateAccountBalance(inventoryAccountId, -purchaseReturn.getReturnedCost(), conn);

            double originalUnitCost = purchaseReturn.getOriginalPurchase().getQuantityKg() > 0 ? purchaseReturn.getOriginalPurchase().getTotalCost() / purchaseReturn.getOriginalPurchase().getQuantityKg() : 0;
            dataManager.updateInventory(
                purchaseReturn.getOriginalPurchase().getCrop().getCropId(), 
                -purchaseReturn.getQuantityKg(),
                originalUnitCost, 
                "OUT", 
                "PURCHASE_RETURN", 
                returnId, 
                conn
            );

            dataManager.logAuditEntry("purchase_returns", returnId, "INSERT", null, description, "SYSTEM", conn);
            
            return returnId;
        });
    }
}