package accounting.service;

import accounting.model.Season;
import accounting.util.ImprovedDataManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SeasonalReportService {

    private final ImprovedDataManager dataManager;

    public SeasonalReportService() {
        this.dataManager = ImprovedDataManager.getInstance();
    }

    public List<Season> getAllSeasons() throws SQLException {
        List<Season> seasons = new ArrayList<>();
        String sql = "SELECT * FROM seasons ORDER BY start_date DESC";
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Season season = new Season();
                season.setId(rs.getInt("season_id"));
                season.setName(rs.getString("season_name"));
                season.setStartDate(rs.getDate("start_date").toLocalDate());
                season.setEndDate(rs.getDate("end_date").toLocalDate());
                season.setStatus(Season.Status.valueOf(rs.getString("status")));
                seasons.add(season);
            }
        }
        return seasons;
    }

    public SeasonReport generateSeasonReport(int seasonId) throws SQLException {
        // This is a placeholder implementation
        return null;
    }

    public Map<String, Double> getSeasonPerformance(int seasonId) throws SQLException {
        Map<String, Double> performanceData = new HashMap<>();
        String sql = """
            SELECT 
                (SELECT COALESCE(SUM(total_sale_amount), 0) FROM sales WHERE season_id = ?) as total_revenue,
                (SELECT COALESCE(SUM(total_cost), 0) FROM purchases WHERE season_id = ?) as total_cost,
                (SELECT COALESCE(SUM(amount), 0) FROM financial_transactions WHERE season_id = ? AND transaction_type = 'EXPENSE') as total_expenses
            """;

        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, seasonId);
            stmt.setInt(2, seasonId);
            stmt.setInt(3, seasonId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    double totalRevenue = rs.getDouble("total_revenue");
                    double totalCost = rs.getDouble("total_cost");
                    double totalExpenses = rs.getDouble("total_expenses");
                    double netProfit = totalRevenue - (totalCost + totalExpenses);

                    performanceData.put("totalRevenue", totalRevenue);
                    performanceData.put("totalCost", totalCost);
                    performanceData.put("totalExpenses", totalExpenses);
                    performanceData.put("netProfit", netProfit);
                }
            }
        }
        return performanceData;
    }

    public static class SeasonReport {
        private final SeasonStatistics statistics;
        private final List<CropSeasonAnalysis> cropAnalyses;
        private final List<SeasonInsight> insights;

        public SeasonReport(SeasonStatistics statistics, List<CropSeasonAnalysis> cropAnalyses, List<SeasonInsight> insights) {
            this.statistics = statistics;
            this.cropAnalyses = cropAnalyses;
            this.insights = insights;
        }

        public SeasonStatistics getStatistics() {
            return statistics;
        }

        public List<CropSeasonAnalysis> getCropAnalyses() {
            return cropAnalyses;
        }

        public List<SeasonInsight> getInsights() {
            return insights;
        }
    }

    public static class SeasonStatistics {
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final double totalRevenue;
        private final double totalCost;
        private final double netProfit;

        public SeasonStatistics(LocalDate startDate, LocalDate endDate, double totalRevenue, double totalCost, double netProfit) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.totalRevenue = totalRevenue;
            this.totalCost = totalCost;
            this.netProfit = netProfit;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public double getTotalRevenue() {
            return totalRevenue;
        }

        public double getTotalCost() {
            return totalCost;
        }

        public double getNetProfit() {
            return netProfit;
        }
    }

    public static class CropSeasonAnalysis {
        private final String cropName;
        private final double totalRevenue;
        private final double totalCost;
        private final double netProfit;
        private final double profitMargin;
        private final double returnOnInvestment;
        private final String performanceRating;

        public CropSeasonAnalysis(String cropName, double totalRevenue, double totalCost, double netProfit, double profitMargin, double returnOnInvestment, String performanceRating) {
            this.cropName = cropName;
            this.totalRevenue = totalRevenue;
            this.totalCost = totalCost;
            this.netProfit = netProfit;
            this.profitMargin = profitMargin;
            this.returnOnInvestment = returnOnInvestment;
            this.performanceRating = performanceRating;
        }

        public String getCropName() {
            return cropName;
        }

        public double getTotalRevenue() {
            return totalRevenue;
        }

        public double getTotalCost() {
            return totalCost;
        }

        public double getNetProfit() {
            return netProfit;
        }

        public double getProfitMargin() {
            return profitMargin;
        }

        public double getReturnOnInvestment() {
            return returnOnInvestment;
        }

        public String getPerformanceRating() {
            return performanceRating;
        }
    }

    public static class SeasonInsight {
        private final String description;

        public SeasonInsight(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
