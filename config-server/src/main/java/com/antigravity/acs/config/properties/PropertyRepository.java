package com.antigravity.acs.config.properties;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class PropertyRepository {

    private final JdbcClient jdbc;

    public PropertyRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<PropertyEntity> findByFilters(String application, String profile, String label) {
        var sb = new StringBuilder(
                "SELECT ID, APPLICATION, PROFILE, LABEL, PROP_KEY, PROP_VALUE, CREATED_AT, UPDATED_AT FROM PROPERTIES WHERE 1=1");
        var params = new ArrayList<>();

        if (application != null && !application.isBlank()) {
            sb.append(" AND APPLICATION = ?");
            params.add(application);
        }
        if (profile != null && !profile.isBlank()) {
            sb.append(" AND PROFILE = ?");
            params.add(profile);
        }
        if (label != null && !label.isBlank()) {
            sb.append(" AND LABEL = ?");
            params.add(label);
        }
        sb.append(" ORDER BY APPLICATION, PROFILE, LABEL, PROP_KEY");

        var stmt = jdbc.sql(sb.toString());
        for (int i = 0; i < params.size(); i++) {
            stmt = stmt.param(params.get(i));
        }
        return stmt.query(this::mapRow).list();
    }

    public Optional<PropertyEntity> findById(Long id) {
        return jdbc.sql("SELECT ID, APPLICATION, PROFILE, LABEL, PROP_KEY, PROP_VALUE, CREATED_AT, UPDATED_AT FROM PROPERTIES WHERE ID = ?")
                .param(id)
                .query(this::mapRow)
                .optional();
    }

    public PropertyEntity create(PropertyCreateRequest req) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.sql("INSERT INTO PROPERTIES (APPLICATION, PROFILE, LABEL, PROP_KEY, PROP_VALUE) VALUES (?, ?, ?, ?, ?)")
                .param(req.application())
                .param(req.profile())
                .param(req.label())
                .param(req.propKey())
                .param(req.propValue())
                .update(keyHolder, "ID");

        Long id = keyHolder.getKeyAs(Long.class);
        return findById(id).orElseThrow();
    }

    public Optional<PropertyEntity> update(Long id, PropertyUpdateRequest req) {
        int updated = jdbc.sql("UPDATE PROPERTIES SET PROP_VALUE = ?, UPDATED_AT = CURRENT_TIMESTAMP WHERE ID = ?")
                .param(req.propValue())
                .param(id)
                .update();
        if (updated == 0) return Optional.empty();
        return findById(id);
    }

    public boolean delete(Long id) {
        return jdbc.sql("DELETE FROM PROPERTIES WHERE ID = ?")
                .param(id)
                .update() > 0;
    }

    public List<String> findDistinctApplications() {
        return jdbc.sql("SELECT DISTINCT APPLICATION FROM PROPERTIES ORDER BY APPLICATION")
                .query((rs, rowNum) -> rs.getString("APPLICATION"))
                .list();
    }

    public List<String> findDistinctProfiles() {
        return jdbc.sql("SELECT DISTINCT PROFILE FROM PROPERTIES ORDER BY PROFILE")
                .query((rs, rowNum) -> rs.getString("PROFILE"))
                .list();
    }

    public List<String> findDistinctLabels() {
        return jdbc.sql("SELECT DISTINCT LABEL FROM PROPERTIES ORDER BY LABEL")
                .query((rs, rowNum) -> rs.getString("LABEL"))
                .list();
    }

    private PropertyEntity mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new PropertyEntity(
                rs.getLong("ID"),
                rs.getString("APPLICATION"),
                rs.getString("PROFILE"),
                rs.getString("LABEL"),
                rs.getString("PROP_KEY"),
                rs.getString("PROP_VALUE"),
                rs.getTimestamp("CREATED_AT") != null ? rs.getTimestamp("CREATED_AT").toLocalDateTime() : null,
                rs.getTimestamp("UPDATED_AT") != null ? rs.getTimestamp("UPDATED_AT").toLocalDateTime() : null
        );
    }
}
