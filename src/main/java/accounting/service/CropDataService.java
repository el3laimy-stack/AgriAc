package accounting.service;

import accounting.dao.AbstractDAO;
import accounting.formatter.FormatUtils;
import accounting.model.Crop;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import accounting.model.InventoryAdjustment;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

/**
 * خدمات البيانات المحسنة للمحاصيل
 */
public class CropDataService extends AbstractDAO<Crop, Integer> {

    private static final Logger LOGGER = Logger.getLogger(CropDataService.class.getName());
    private static final Gson gson = new Gson();

    public CropDataService() {
        super("crops");
    }

    @Override
    protected Crop mapResultSetToEntity(ResultSet rs) throws SQLException {
        Crop crop = new Crop();
        crop.setCropId(rs.getInt("crop_id"));
        crop.setCropName(rs.getString("crop_name"));
        crop.setAllowedPricingUnits(convertJsonToList(rs.getString("allowed_pricing_units")));
        crop.setConversionFactors(convertJsonToMap(rs.getString("conversion_factors")));
        return crop;
    }

    @Override
    protected void mapEntityToPreparedStatement(Crop crop, PreparedStatement ps) throws SQLException {
        ps.setString(1, crop.getCropName());
        ps.setString(2, convertListToJson(crop.getAllowedPricingUnits()));
        ps.setString(3, convertMapToJson(crop.getConversionFactors()));
    }

    @Override
    protected void mapEntityToUpdatePreparedStatement(Crop crop, PreparedStatement ps) throws SQLException {
        ps.setString(1, crop.getCropName());
        ps.setString(2, convertListToJson(crop.getAllowedPricingUnits()));
        ps.setString(3, convertMapToJson(crop.getConversionFactors()));
        ps.setInt(4, crop.getCropId());
    }

    @Override
    protected String getSelectAllSql() {
        return "SELECT crop_id, crop_name, allowed_pricing_units, conversion_factors FROM crops";
    }

    @Override
    protected String getInsertSql() {
        return "INSERT INTO crops (crop_name, allowed_pricing_units, conversion_factors) VALUES (?, ?, ?)";
    }

    @Override
    protected String getUpdateSql() {
        return "UPDATE crops SET crop_name = ?, allowed_pricing_units = ?, conversion_factors = ?, updated_at = CURRENT_TIMESTAMP WHERE crop_id = ?";
    }

    @Override
    protected String getPkColumnName() {
        return "crop_id";
    }

    public int addCrop(Crop crop) throws SQLException {
        Integer cropId = save(crop);
        if (cropId != null) {
            try (Connection conn = dataManager.getConnection()) {
                createInventoryRecord(conn, cropId);
            }
            return cropId;
        } else {
            throw new SQLException("Failed to retrieve new crop ID.");
        }
    }

    public boolean updateCrop(Crop crop) throws SQLException {
        update(crop);
        return true;
    }

    public boolean deleteCrop(int cropId) throws SQLException {
        return dataManager.executeTransaction(conn -> {
            String query = "UPDATE crops SET is_active = 0, updated_at = CURRENT_TIMESTAMP WHERE crop_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, cropId);
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    dataManager.logAuditEntry("crops", cropId, "DELETE", null, "Deactivated", "SYSTEM", conn);
                }
                return rowsAffected > 0;
            }
        });
    }

    public Crop getCropById(int cropId) throws SQLException {
        String sql = getSelectAllSql() + " WHERE crop_id = ? AND is_active = 1";
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, cropId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToEntity(rs);
                }
            }
        }
        return null;
    }

    public List<Crop> getAllActiveCrops() throws SQLException {
        List<Crop> activeCrops = new ArrayList<>();
        String sql = getSelectAllSql() + " WHERE is_active = 1";
        try (Connection conn = dataManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                activeCrops.add(mapResultSetToEntity(rs));
            }
        }
        return activeCrops;
    }
    
    /**
     * الحصول على إحصائيات المحصول
     */
    public CropStatistics getCropStatistics(int cropId, LocalDate fromDate, LocalDate toDate) throws SQLException {
        String fromDateStr = (fromDate != null) ? FormatUtils.formatDateForDatabase(fromDate) : null;
        String toDateStr = (toDate != null) ? FormatUtils.formatDateForDatabase(toDate) : null;

        StringBuilder queryBuilder = new StringBuilder("""
            SELECT 
                c.crop_name,
                COALESCE(i.current_stock_kg, 0) as current_stock,
                COALESCE(i.average_cost_per_kg, 0) as average_cost,
                COALESCE(purchase_stats.total_purchased, 0) as total_purchased,
                COALESCE(purchase_stats.total_purchase_cost, 0) as total_purchase_cost,
                COALESCE(sale_stats.total_sold, 0) as total_sold,
                COALESCE(sale_stats.total_sale_revenue, 0) as total_sale_revenue
            FROM crops c
            LEFT JOIN inventory i ON c.crop_id = i.crop_id
            LEFT JOIN (
                SELECT crop_id, SUM(quantity_kg) as total_purchased, SUM(total_cost) as total_purchase_cost
                FROM purchases 
                WHERE crop_id = ?
            """);

        List<Object> params = new ArrayList<>();
        params.add(cropId);

        if (fromDateStr != null) {
            queryBuilder.append(" AND purchase_date >= ?");
            params.add(fromDateStr);
        }
        if (toDateStr != null) {
            queryBuilder.append(" AND purchase_date <= ?");
            params.add(toDateStr);
        }

        queryBuilder.append(" ) purchase_stats ON c.crop_id = purchase_stats.crop_id LEFT JOIN ( SELECT crop_id, SUM(quantity_sold_kg) as total_sold, SUM(total_sale_amount) as total_sale_revenue FROM sales WHERE crop_id = ? ");
        params.add(cropId);

        if (fromDateStr != null) {
            queryBuilder.append(" AND sale_date >= ?");
            params.add(fromDateStr);
        }
        if (toDateStr != null) {
            queryBuilder.append(" AND sale_date <= ?");
            params.add(toDateStr);
        }

        queryBuilder.append(" ) sale_stats ON c.crop_id = sale_stats.crop_id WHERE c.crop_id = ? AND c.is_active = 1");
        params.add(cropId);

        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(queryBuilder.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new CropStatistics(
                        rs.getString("crop_name"),
                        rs.getDouble("current_stock"),
                        rs.getDouble("average_cost"),
                        rs.getDouble("total_purchased"),
                        rs.getDouble("total_purchase_cost"),
                        rs.getDouble("total_sold"),
                        rs.getDouble("total_sale_revenue")
                    );
                }
            }
        }
        return null;
    }
    
    private Crop mapResultSetToCrop(ResultSet rs) throws SQLException {
        Crop crop = new Crop();
        crop.setCropId(rs.getInt("crop_id"));
        crop.setCropName(rs.getString("crop_name"));
        crop.setAllowedPricingUnits(convertJsonToList(rs.getString("allowed_pricing_units")));
        crop.setConversionFactors(convertJsonToMap(rs.getString("conversion_factors")));
        return crop;
    }
    
    private void createInventoryRecord(Connection conn, int cropId) throws SQLException {
        String query = "INSERT INTO inventory (crop_id, current_stock_kg, average_cost_per_kg) VALUES (?, 0, 0)";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, cropId);
            stmt.executeUpdate();
        }
    }
    
    private boolean hasCropTransactions(Connection conn, int cropId) throws SQLException {
        String query = "SELECT 1 FROM purchases WHERE crop_id = ? UNION SELECT 1 FROM sales WHERE crop_id = ? LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, cropId);
            stmt.setInt(2, cropId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }
    
    private String getCropAsJson(Connection conn, int cropId) throws SQLException {
        String query = "SELECT crop_name FROM crops WHERE crop_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, cropId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("crop_name");
                }
            }
        }
        return null;
    }
    
    private String convertListToJson(List<String> list) {
        return gson.toJson(list);
    }
    
    private String convertMapToJson(Map<String, List<Double>> map) {
        return gson.toJson(map);
    }
    
    private List<String> convertJsonToList(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        Type listType = new TypeToken<ArrayList<String>>() {}.getType();
        return gson.fromJson(json, listType);
    }

    private Map<String, List<Double>> convertJsonToMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new HashMap<>();
        }
        Type mapType = new TypeToken<Map<String, List<Double>>>() {}.getType();
        return gson.fromJson(json, mapType);
    }
    /**
     * البحث عن محصول بالاسم (بما في ذلك المحذوفة)
     * @return كائن المحصول إذا وجد، وإلا null
     */
    public Crop findCropByName(String name) throws SQLException {
        String query = "SELECT * FROM crops WHERE crop_name = ?";
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCrop(rs);
                }
            }
        }
        return null;
    }
    /**
     * إعادة تفعيل محصول محذوف
     */
    public boolean reactivateCrop(int cropId) throws SQLException {
        return dataManager.executeTransaction(conn -> {
            String query = "UPDATE crops SET is_active = 1, updated_at = CURRENT_TIMESTAMP WHERE crop_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, cropId);
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    dataManager.logAuditEntry("crops", cropId, "UPDATE", null, "Reactivated", "SYSTEM", conn);
                }
                return rowsAffected > 0;
            }
        });
    }
    /**
     * الحصول على إحصائيات المخزون لجميع المحاصيل النشطة.
     * @return قائمة بإحصائيات المحاصيل.
     * @throws SQLException في حالة حدوث خطأ في قاعدة البيانات.
     */
    public List<CropStatistics> getAllCropStatistics() throws SQLException {
        String query = """
            SELECT 
                c.crop_name,
                COALESCE(i.current_stock_kg, 0) as current_stock,
                COALESCE(i.average_cost_per_kg, 0) as average_cost,
                COALESCE(purchase_stats.total_purchased, 0) as total_purchased,
                COALESCE(purchase_stats.total_purchase_cost, 0) as total_purchase_cost,
                COALESCE(sale_stats.total_sold, 0) as total_sold,
                COALESCE(sale_stats.total_sale_revenue, 0) as total_sale_revenue
            FROM crops c
            LEFT JOIN inventory i ON c.crop_id = i.crop_id
            LEFT JOIN (
                SELECT crop_id, SUM(quantity_kg) as total_purchased, SUM(total_cost) as total_purchase_cost
                FROM purchases
                GROUP BY crop_id
            ) purchase_stats ON c.crop_id = purchase_stats.crop_id
            LEFT JOIN (
                SELECT crop_id, SUM(quantity_sold_kg) as total_sold, SUM(total_sale_amount) as total_sale_revenue
                FROM sales
                GROUP BY crop_id
            ) sale_stats ON c.crop_id = sale_stats.crop_id
            WHERE c.is_active = 1
            ORDER BY c.crop_name
            """;

        List<CropStatistics> allStats = new ArrayList<>();
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                allStats.add(new CropStatistics(
                    rs.getString("crop_name"),
                    rs.getDouble("current_stock"),
                    rs.getDouble("average_cost"),
                    rs.getDouble("total_purchased"),
                    rs.getDouble("total_purchase_cost"),
                    rs.getDouble("total_sold"),
                    rs.getDouble("total_sale_revenue")
                ));
            }
        }
        return allStats;
    }

    
    /**
     * الحصول على المخزون الحالي للمحصول
     */
    public double getCurrentStock(int cropId) throws SQLException {
        String sql = "SELECT current_stock_kg FROM inventory WHERE crop_id = ?";
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, cropId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("current_stock_kg");
                }
            }
        }
        return 0.0; // Return 0 if no inventory record found
    }

    /**
     * فئة إحصائيات المحصول
     */
    public static class CropStatistics {
        private final String cropName;
        private final double currentStock;
        private final double averageCost;
        private final double totalPurchased;
        private final double totalPurchaseCost;
        private final double totalSold;
        private final double totalSaleRevenue;
        
        public CropStatistics(String cropName, double currentStock, double averageCost,
                             double totalPurchased, double totalPurchaseCost,
                             double totalSold, double totalSaleRevenue) {
            this.cropName = cropName;
            this.currentStock = currentStock;
            this.averageCost = averageCost;
            this.totalPurchased = totalPurchased;
            this.totalPurchaseCost = totalPurchaseCost;
            this.totalSold = totalSold;
            this.totalSaleRevenue = totalSaleRevenue;
        }
        
        // Getters
        public String getCropName() { return cropName; }
        public double getCurrentStock() { return currentStock; }
        public double getAverageCost() { return averageCost; }
        public double getTotalPurchased() { return totalPurchased; }
        public double getTotalPurchaseCost() { return totalPurchaseCost; }
        public double getTotalSold() { return totalSold; }
        public double getTotalSaleRevenue() { return totalSaleRevenue; }
        
        public double getGrossProfit() {
            return totalSaleRevenue - (totalSold * averageCost);
        }
        
        public double getProfitMargin() {
            return totalSaleRevenue > 0 ? (getGrossProfit() / totalSaleRevenue) * 100 : 0;
        }
        
        public double getInventoryValue() {
            return currentStock * averageCost;
        }
        
        public double getAveragePurchasePrice() {
            if (totalPurchased == 0) return 0;
            return totalPurchaseCost / totalPurchased;
        }

        public double getAverageSellingPrice() {
            if (totalSold == 0) return 0;
            return totalSaleRevenue / totalSold;
        }

        public double getTurnoverRate() {
            if (currentStock == 0) return 0;
            return totalSold / currentStock;
        }
    }
    /**
     * تسجيل تسوية مخزون جديدة، وتحديث كمية المخزون ودفتر الأستاذ.
     */
    public int addInventoryAdjustment(InventoryAdjustment adjustment) throws SQLException {
        return dataManager.executeTransaction(conn -> {
            
            // 1. حساب تكلفة الكمية المعدلة بناءً على متوسط التكلفة الحالي للمخزون
            double unitCost = 0;
            try (PreparedStatement costStmt = conn.prepareStatement("SELECT average_cost_per_kg FROM inventory WHERE crop_id = ?")) {
                costStmt.setInt(1, adjustment.getCrop().getCropId());
                try (ResultSet rs = costStmt.executeQuery()) {
                    if (rs.next()) {
                        unitCost = rs.getDouble("average_cost_per_kg");
                    }
                }
            }
            double totalCost = unitCost * adjustment.getQuantityKg();
            adjustment.setCost(totalCost);

            // 2. إضافة سجل التسوية
            String sql = "INSERT INTO inventory_adjustments (crop_id, adjustment_date, adjustment_type, quantity_kg, reason, cost) VALUES (?, ?, ?, ?, ?, ?)";
            int adjustmentId;
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, adjustment.getCrop().getCropId());
                stmt.setString(2, FormatUtils.formatDateForDatabase(adjustment.getAdjustmentDate()));
                stmt.setString(3, adjustment.getAdjustmentType().name());
                stmt.setDouble(4, adjustment.getQuantityKg());
                stmt.setString(5, adjustment.getReason());
                stmt.setDouble(6, totalCost);
                
                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        adjustmentId = rs.getInt(1);
                    } else {
                        throw new SQLException("Creating inventory adjustment failed, no ID obtained.");
                    }
                }
            }

            // 3. تسجيل القيد المزدوج وتحديث الأرصدة
            String transactionRef = "INV-ADJ-" + adjustmentId;
            String description = "تسوية مخزون: " + adjustment.getAdjustmentType().getArabicName() + " لـ " + adjustment.getCrop().getCropName();
            double quantityForUpdate = adjustment.getQuantityKg();

            int debitAccountId;
            int creditAccountId;
            int inventoryAccountId = 10103; // ID حساب المخزون

            if (adjustment.getAdjustmentType() == InventoryAdjustment.AdjustmentType.SURPLUS) {
                // حالة الزيادة
                debitAccountId = inventoryAccountId; // مدين: المخزون
                creditAccountId = 40105; // دائن: حساب "أرباح فروقات المخزون"
                quantityForUpdate = adjustment.getQuantityKg();
                dataManager.updateAccountBalance(debitAccountId, totalCost, conn);
                dataManager.updateAccountBalance(creditAccountId, totalCost, conn);
            } else {
                // حالة التلف أو العجز
                creditAccountId = inventoryAccountId; // دائن: المخزون
                debitAccountId = 50108; // مدين: حساب "خسائر المخزون"
                quantityForUpdate = -quantityForUpdate; // الكمية بالسالب لأنها تنقص
                dataManager.updateAccountBalance(creditAccountId, -totalCost, conn);
                dataManager.updateAccountBalance(debitAccountId, totalCost, conn);
            }

            dataManager.addLedgerEntry(conn, transactionRef, adjustment.getAdjustmentDate(), debitAccountId, totalCost, 0.0, description, "ADJUSTMENT", adjustmentId, adjustment.getAdjustmentType().name());
            dataManager.addLedgerEntry(conn, transactionRef, adjustment.getAdjustmentDate(), creditAccountId, 0.0, totalCost, description, "ADJUSTMENT", adjustmentId, adjustment.getAdjustmentType().name());

            // 4. تحديث كمية المخزون
            dataManager.updateInventory(adjustment.getCrop().getCropId(), quantityForUpdate, unitCost, "ADJUSTMENT", "INV_ADJUST", adjustmentId, conn);

            dataManager.logAuditEntry("inventory_adjustments", adjustmentId, "INSERT", null, description, "SYSTEM", conn);
            
            return adjustmentId;
        });
    }
}