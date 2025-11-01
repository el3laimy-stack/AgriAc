package accounting.service;

import accounting.model.Season;
import accounting.util.ImprovedDataManager;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SeasonDataService {

    private final ImprovedDataManager dataManager;

    public SeasonDataService() {
        this.dataManager = ImprovedDataManager.getInstance();
    }

    public void addSeason(Season season) throws SQLException {
        String sql = "INSERT INTO seasons (name, start_date, end_date, status) VALUES (?, ?, ?, ?)";
        dataManager.executeTransaction(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, season.getName());
                stmt.setDate(2, Date.valueOf(season.getStartDate()));
                stmt.setDate(3, Date.valueOf(season.getEndDate()));
                stmt.setString(4, season.getStatus().name());
                stmt.executeUpdate();
                
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        season.setId(generatedKeys.getInt(1));
                    }
                }
            }
            return null;
        });
    }

    public void updateSeason(Season season) throws SQLException {
        String sql = "UPDATE seasons SET name = ?, start_date = ?, end_date = ?, status = ?, updated_at = CURRENT_TIMESTAMP WHERE season_id = ?";
        dataManager.executeTransaction(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, season.getName());
                stmt.setDate(2, Date.valueOf(season.getStartDate()));
                stmt.setDate(3, Date.valueOf(season.getEndDate()));
                stmt.setString(4, season.getStatus().name());
                stmt.setInt(5, season.getId());
                stmt.executeUpdate();
            }
            return null;
        });
    }

    public void deleteSeason(int seasonId) throws SQLException {
        // We should add a check here to ensure we are not deleting a season that has transactions associated with it.
        // For now, we will just delete it.
        String sql = "DELETE FROM seasons WHERE season_id = ?";
        dataManager.executeTransaction(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, seasonId);
                stmt.executeUpdate();
            }
            return null;
        });
    }

    public List<Season> getAllSeasons() throws SQLException {
        List<Season> seasons = new ArrayList<>();
        String sql = "SELECT * FROM seasons ORDER BY start_date DESC";
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                seasons.add(mapRowToSeason(rs));
            }
        }
        return seasons;
    }

    public Optional<Season> getSeasonById(int id) throws SQLException {
        String sql = "SELECT * FROM seasons WHERE season_id = ?";
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToSeason(rs));
                }
            }
        }
        return Optional.empty();
    }
    
    public Optional<Season> getActiveSeason() throws SQLException {
        String sql = "SELECT * FROM seasons WHERE status = 'ACTIVE' LIMIT 1";
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return Optional.of(mapRowToSeason(rs));
            }
        }
        return Optional.empty();
    }

    private Season mapRowToSeason(ResultSet rs) throws SQLException {
        return new Season(
                rs.getInt("season_id"),
                rs.getString("name"),
                rs.getDate("start_date").toLocalDate(),
                rs.getDate("end_date").toLocalDate(),
                Season.Status.valueOf(rs.getString("status"))
        );
    }
}