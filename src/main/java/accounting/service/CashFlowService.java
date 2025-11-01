package accounting.service;

import accounting.model.CashFlowEntry;
import accounting.util.ImprovedDataManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CashFlowService {

    private final ImprovedDataManager dataManager;

    public CashFlowService() {
        this.dataManager = ImprovedDataManager.getInstance();
    }

    public List<CashFlowEntry> getCashFlow(LocalDate from, LocalDate to) throws SQLException {
        List<CashFlowEntry> entries = new ArrayList<>();
        String sql = """
            SELECT 
                transaction_date as date, 
                description, 
                transaction_type as type, 
                CASE WHEN amount > 0 THEN amount ELSE 0 END as inflow, 
                CASE WHEN amount < 0 THEN -amount ELSE 0 END as outflow
            FROM financial_transactions
            WHERE transaction_date BETWEEN ? AND ?
            ORDER BY transaction_date;
            """;

        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, from.toString());
            stmt.setString(2, to.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    entries.add(new CashFlowEntry(
                        rs.getDate("date").toLocalDate(),
                        rs.getString("description"),
                        rs.getString("type"),
                        rs.getDouble("inflow"),
                        rs.getDouble("outflow")
                    ));
                }
            }
        }
        return entries;
    }
}
