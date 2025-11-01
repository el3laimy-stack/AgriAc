package accounting.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import accounting.model.DetailedTransactionEntry;
import accounting.model.LedgerEntry;
import accounting.util.ImprovedDataManager;
import accounting.formatter.FormatUtils;

public class ReportDataService {

    private final ImprovedDataManager dataManager;

    public ReportDataService() {
        this.dataManager = ImprovedDataManager.getInstance();
    }

    /**
     * الحصول على كشف حساب مفصل للعميل/المورد
     */
    public List<DetailedTransactionEntry> getDetailedContactStatement(int contactId, LocalDate fromDate, LocalDate toDate) throws SQLException {
        List<DetailedTransactionEntry> statementEntries = new ArrayList<>();
        
        // الحصول على رصيد أول المدة
        double openingBalance = getContactOpeningBalance(contactId, fromDate);
        double runningBalance = openingBalance;
        
        String sql = """
            SELECT 
                'SALE' as type,
                s.sale_date as date,
                s.sale_invoice_number as reference,
                c.name as contact_name,
                cr.crop_name as item_name,
                s.quantity_sold_kg as quantity,
                s.selling_pricing_unit as unit,
                s.selling_unit_price as unit_price,
                s.total_sale_amount as total_amount,
                COALESCE(
                    (SELECT SUM(p.amount) 
                     FROM payments p 
                     WHERE p.contact_id = s.customer_id 
                       AND p.payment_date = s.sale_date 
                       AND p.payment_type = 'RECEIVE'
                       AND p.description LIKE '%' || s.sale_invoice_number || '%'), 
                    0
                ) as paid_amount,
                'فاتورة بيع' as main_description
            FROM sales s
            JOIN contacts c ON s.customer_id = c.contact_id
            JOIN crops cr ON s.crop_id = cr.crop_id
            WHERE s.customer_id = ? AND s.sale_date BETWEEN ? AND ?
            
            UNION ALL
            
            SELECT 
                'PURCHASE' as type,
                p.purchase_date as date,
                p.invoice_number as reference,
                c.name as contact_name,
                cr.crop_name as item_name,
                p.quantity_kg as quantity,
                'كيلو' as unit,
                p.unit_price as unit_price,
                p.total_cost as total_amount,
                COALESCE(p.amount_paid, 0) as paid_amount,
                'فاتورة شراء' as main_description
            FROM purchases p
            JOIN contacts c ON p.supplier_id = c.contact_id
            JOIN crops cr ON p.crop_id = cr.crop_id
            WHERE p.supplier_id = ? AND p.purchase_date BETWEEN ? AND ?
            
            UNION ALL
            
            SELECT 
                p.payment_type as type,
                p.payment_date as date,
                'PAY-' || p.payment_id as reference,
                c.name as contact_name,
                NULL as item_name,
                NULL as quantity,
                NULL as unit,
                NULL as unit_price,
                p.amount as total_amount,
                p.amount as paid_amount,
                p.description as main_description
            FROM payments p
            JOIN contacts c ON p.contact_id = c.contact_id
            WHERE p.contact_id = ? AND p.payment_date BETWEEN ? AND ?
            
            ORDER BY date, reference
        """;

        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String fromDateStr = FormatUtils.formatDateForDatabase(fromDate);
            String toDateStr = FormatUtils.formatDateForDatabase(toDate);

            // Set parameters for all three UNION queries
            stmt.setInt(1, contactId);
            stmt.setString(2, fromDateStr);
            stmt.setString(3, toDateStr);
            stmt.setInt(4, contactId);
            stmt.setString(5, fromDateStr);
            stmt.setString(6, toDateStr);
            stmt.setInt(7, contactId);
            stmt.setString(8, fromDateStr);
            stmt.setString(9, toDateStr);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String type = rs.getString("type");
                    LocalDate date = FormatUtils.parseDateFromDatabase(rs.getString("date"));
                    String reference = rs.getString("reference");
                    String contactName = rs.getString("contact_name");
                    String itemName = rs.getString("item_name");
                    Double quantity = rs.getObject("quantity") != null ? rs.getDouble("quantity") : null;
                    String unit = rs.getString("unit");
                    Double unitPrice = rs.getObject("unit_price") != null ? rs.getDouble("unit_price") : null;
                    Double totalAmount = rs.getObject("total_amount") != null ? rs.getDouble("total_amount") : null;
                    Double paidAmount = rs.getObject("paid_amount") != null ? rs.getDouble("paid_amount") : null;
                    String mainDescription = rs.getString("main_description");
                    
                    // حساب المبلغ المتبقي
                    Double remainingAmount = null;
                    if (totalAmount != null && paidAmount != null) {
                        remainingAmount = totalAmount - paidAmount;
                    }
                    
                    // حساب المدين والدائن
                    double debit = 0.0;
                    double credit = 0.0;
                    
                    switch (type) {
                        case "SALE":
                            debit = totalAmount != null ? totalAmount : 0.0;
                            runningBalance += debit;
                            break;
                        case "PURCHASE":
                            credit = totalAmount != null ? totalAmount : 0.0;
                            runningBalance -= credit;
                            break;
                        case "PAY":
                            credit = totalAmount != null ? totalAmount : 0.0;
                            runningBalance -= credit;
                            break;
                        case "RECEIVE":
                            debit = totalAmount != null ? totalAmount : 0.0;
                            runningBalance += debit;
                            break;
                    }
                    
                    // تحديد طريقة الدفع
                    String paymentMethod = "";
                    if (paidAmount != null && paidAmount > 0) {
                        if (remainingAmount != null && remainingAmount > 0) {
                            paymentMethod = "نقدي جزئي";
                        } else {
                            paymentMethod = "نقدي كامل";
                        }
                    } else {
                        paymentMethod = "آجل";
                    }
                    
                    // إنشاء ملاحظات
                    String notes = "";
                    if (remainingAmount != null && remainingAmount > 0) {
                        notes = "الباقي عنده " + String.format("%.2f", remainingAmount);
                    } else if ("SALE".equals(type) || "PURCHASE".equals(type)) {
                        notes = "مسدد بالكامل";
                    }
                    
                    DetailedTransactionEntry entry = new DetailedTransactionEntry(
                        date, reference, type, mainDescription,
                        itemName, quantity, unit, unitPrice, totalAmount,
                        paidAmount, remainingAmount, debit, credit, runningBalance,
                        contactName, paymentMethod, notes
                    );
                    
                    statementEntries.add(entry);
                }
            }
        }
        
        return statementEntries;
    }
    
    /**
     * حساب رصيد أول المدة للعميل/المورد
     */
    private double getContactOpeningBalance(int contactId, LocalDate fromDate) throws SQLException {
        String sql = """
            SELECT 
                COALESCE(SUM(
                    CASE 
                        WHEN s.sale_date < ? THEN s.total_sale_amount
                        ELSE 0
                    END
                ), 0) -
                COALESCE(SUM(
                    CASE 
                        WHEN p.purchase_date < ? THEN p.total_cost
                        ELSE 0
                    END
                ), 0) -
                COALESCE(SUM(
                    CASE 
                        WHEN pay.payment_date < ? AND pay.payment_type = 'PAY' THEN pay.amount
                        WHEN pay.payment_date < ? AND pay.payment_type = 'RECEIVE' THEN -pay.amount
                        ELSE 0
                    END
                ), 0) as opening_balance
            FROM (SELECT ?) dummy
            LEFT JOIN sales s ON s.customer_id = ?
            LEFT JOIN purchases p ON p.supplier_id = ?
            LEFT JOIN payments pay ON pay.contact_id = ?
        """;
        
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            String dateStr = FormatUtils.formatDateForDatabase(fromDate);
            stmt.setString(1, dateStr);
            stmt.setString(2, dateStr);
            stmt.setString(3, dateStr);
            stmt.setString(4, dateStr);
            stmt.setInt(5, contactId);
            stmt.setInt(6, contactId);
            stmt.setInt(7, contactId);
            stmt.setInt(8, contactId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("opening_balance");
                }
            }
        }
        
        return 0.0;
    }

    public List<LedgerEntry> getExpenseTransactions(LocalDate fromDate, LocalDate toDate) throws SQLException {
        List<LedgerEntry> entries = new ArrayList<>();
        String sql = "SELECT * FROM general_ledger WHERE source_type = 'EXPENSE' AND entry_date BETWEEN ? AND ? ORDER BY entry_date DESC";
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, FormatUtils.formatDateForDatabase(fromDate));
            stmt.setString(2, FormatUtils.formatDateForDatabase(toDate));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    entries.add(new LedgerEntry(
                            FormatUtils.parseDateFromDatabase(rs.getString("entry_date")),
                            rs.getString("description"),
                            rs.getString("transaction_ref"),
                            rs.getDouble("debit"),
                            rs.getDouble("credit"),
                            "EXPENSE",
                            rs.getString("source_type"),
                            rs.getInt("source_id"),
                            null, null, null, null // Pass null for new detailed fields
                    ));
                }
            }
        }
        return entries;
    }
}