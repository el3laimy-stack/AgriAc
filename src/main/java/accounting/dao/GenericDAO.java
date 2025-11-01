package accounting.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface GenericDAO<T, K> {
    Optional<T> findById(K id) throws SQLException;
    List<T> findAll() throws SQLException;
    K save(T entity) throws SQLException;
    void update(T entity) throws SQLException;
    void delete(K id) throws SQLException;
}
