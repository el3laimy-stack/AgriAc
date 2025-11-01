package accounting.service;

import accounting.util.ImprovedDataManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ProfitMarginService {

    private final ImprovedDataManager dataManager;

    public ProfitMarginService() {
        this.dataManager = ImprovedDataManager.getInstance();
    }

    public Map<String, Double> getProfitMarginsByCrop() throws SQLException {
        Map<String, Double> profitMargins = new HashMap<>();
        String sql = """
            SELECT 
                c.crop_name, 
                SUM(s.total_sale_amount) as total_revenue,
                SUM(s.quantity_sold_kg * i.average_cost_per_kg) as total_cogs
            FROM sales s
            JOIN crops c ON s.crop_id = c.crop_id
            JOIN inventory i ON s.crop_id = i.crop_id
            GROUP BY c.crop_name
            """;

        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String cropName = rs.getString("crop_name");
                double totalRevenue = rs.getDouble("total_revenue");
                double totalCogs = rs.getDouble("total_cogs");
                
                if (totalRevenue > 0) {
                    double margin = ((totalRevenue - totalCogs) / totalRevenue) * 100;
                    profitMargins.put(cropName, margin);
                }
            }
        }
        return profitMargins;
    }
}
