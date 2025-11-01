package accounting.service;

import accounting.model.DailyPrice;
import accounting.formatter.FormatUtils;
import accounting.util.ImprovedDataManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DailyPriceService {

    private final ImprovedDataManager dataManager;

    public DailyPriceService() {
        this.dataManager = ImprovedDataManager.getInstance();
    }

    public void addOrUpdatePrice(DailyPrice price) throws SQLException {
        String checkSql = "SELECT price_id FROM daily_prices WHERE crop_id = ? AND price_date = ?";
        String insertSql = "INSERT INTO daily_prices (crop_id, price_date, opening_price, high_price, low_price, closing_price, average_price) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String updateSql = "UPDATE daily_prices SET opening_price = ?, high_price = ?, low_price = ?, closing_price = ?, average_price = ?, updated_at = CURRENT_TIMESTAMP WHERE price_id = ?";

        try (Connection conn = dataManager.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            
            checkStmt.setInt(1, price.getCropId());
            checkStmt.setString(2, FormatUtils.formatDateForDatabase(price.getPriceDate()));
            
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    // Update existing
                    int priceId = rs.getInt("price_id");
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setDouble(1, price.getOpeningPrice());
                        updateStmt.setDouble(2, price.getHighPrice());
                        updateStmt.setDouble(3, price.getLowPrice());
                        updateStmt.setDouble(4, price.getClosingPrice());
                        updateStmt.setDouble(5, price.getAveragePrice());
                        updateStmt.setInt(6, priceId);
                        updateStmt.executeUpdate();
                    }
                } else {
                    // Insert new
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                        insertStmt.setInt(1, price.getCropId());
                        insertStmt.setString(2, FormatUtils.formatDateForDatabase(price.getPriceDate()));
                        insertStmt.setDouble(3, price.getOpeningPrice());
                        insertStmt.setDouble(4, price.getHighPrice());
                        insertStmt.setDouble(5, price.getLowPrice());
                        insertStmt.setDouble(6, price.getClosingPrice());
                        insertStmt.setDouble(7, price.getAveragePrice());
                        insertStmt.executeUpdate();
                    }
                }
            }
        }
    }

    public List<DailyPrice> getPricesForCrop(int cropId, LocalDate fromDate, LocalDate toDate) throws SQLException {
        List<DailyPrice> prices = new ArrayList<>();
        String sql = "SELECT * FROM daily_prices WHERE crop_id = ? AND price_date BETWEEN ? AND ? ORDER BY price_date ASC";
        
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, cropId);
            stmt.setString(2, FormatUtils.formatDateForDatabase(fromDate));
            stmt.setString(3, FormatUtils.formatDateForDatabase(toDate));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    prices.add(mapRowToDailyPrice(rs));
                }
            }
        }
        return prices;
    }

    private DailyPrice mapRowToDailyPrice(ResultSet rs) throws SQLException {
        return new DailyPrice(
            rs.getInt("price_id"),
            rs.getInt("crop_id"),
            rs.getDate("price_date").toLocalDate(),
            rs.getDouble("opening_price"),
            rs.getDouble("high_price"),
            rs.getDouble("low_price"),
            rs.getDouble("closing_price"),
            rs.getDouble("average_price")
        );
    }

    public void updateDailyPricesFromTransactions(LocalDate date) throws SQLException {
        String sql = """
            SELECT
                crop_id,
                MIN(unit_price) as low_price,
                MAX(unit_price) as high_price,
                AVG(unit_price) as avg_price,
                (SELECT unit_price FROM purchases WHERE crop_id = p.crop_id AND purchase_date = ? ORDER BY purchase_id ASC LIMIT 1) as open_price,
                (SELECT unit_price FROM purchases WHERE crop_id = p.crop_id AND purchase_date = ? ORDER BY purchase_id DESC LIMIT 1) as close_price
            FROM purchases p
            WHERE p.purchase_date = ?
            GROUP BY crop_id
        """;

        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, FormatUtils.formatDateForDatabase(date));
            stmt.setString(2, FormatUtils.formatDateForDatabase(date));
            stmt.setString(3, FormatUtils.formatDateForDatabase(date));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    DailyPrice price = new DailyPrice();
                    price.setCropId(rs.getInt("crop_id"));
                    price.setPriceDate(date);
                    price.setOpeningPrice(rs.getDouble("open_price"));
                    price.setHighPrice(rs.getDouble("high_price"));
                    price.setLowPrice(rs.getDouble("low_price"));
                    price.setClosingPrice(rs.getDouble("close_price"));
                    price.setAveragePrice(rs.getDouble("avg_price"));
                    addOrUpdatePrice(price);
                }
            }
        }
    }

}
