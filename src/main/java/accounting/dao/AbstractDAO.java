package accounting.dao;

import accounting.util.ImprovedDataManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class AbstractDAO<T, K> implements GenericDAO<T, K> {

    protected final ImprovedDataManager dataManager;
    protected final String tableName;

    public AbstractDAO(String tableName) {
        this.dataManager = ImprovedDataManager.getInstance();
        this.tableName = tableName;
    }

    protected abstract T mapResultSetToEntity(ResultSet rs) throws SQLException;
    protected abstract void mapEntityToPreparedStatement(T entity, PreparedStatement ps) throws SQLException;
    protected abstract void mapEntityToUpdatePreparedStatement(T entity, PreparedStatement ps) throws SQLException;
    protected abstract String getPkColumnName();
    protected abstract String getInsertSql();
    protected abstract String getUpdateSql();
    protected abstract String getSelectAllSql();

    @Override
    public Optional<T> findById(K id) throws SQLException {
        String sql = getSelectAllSql() + " WHERE " + getPkColumnName() + " = ?";
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToEntity(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<T> findAll() throws SQLException {
        List<T> entities = new ArrayList<>();
        try (Connection conn = dataManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(getSelectAllSql())) {
            while (rs.next()) {
                entities.add(mapResultSetToEntity(rs));
            }
        }
        return entities;
    }

    @Override
    public K save(T entity) throws SQLException {
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(getInsertSql(), Statement.RETURN_GENERATED_KEYS)) {
            mapEntityToPreparedStatement(entity, stmt);
            stmt.executeUpdate();
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return (K) generatedKeys.getObject(1);
                }
            }
        }
        return null;
    }

    @Override
    public void update(T entity) throws SQLException {
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(getUpdateSql())) {
            mapEntityToUpdatePreparedStatement(entity, stmt);
            stmt.executeUpdate();
        }
    }

    @Override
    public void delete(K id) throws SQLException {
        String sql = "DELETE FROM " + tableName + " WHERE " + getPkColumnName() + " = ?";
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, id);
            stmt.executeUpdate();
        }
    }
}
