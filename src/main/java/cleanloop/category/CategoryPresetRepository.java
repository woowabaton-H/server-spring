package cleanloop.category;

import java.util.List;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CategoryPresetRepository {

    private static final RowMapper<CategoryPreset> ROW_MAPPER = (rs, rowNum) -> new CategoryPreset(
            rs.getString("preset_key"),
            rs.getString("name"),
            rs.getString("icon"),
            rs.getInt("cycle_days"),
            rs.getString("note"),
            rs.getInt("sort_order"),
            rs.getBoolean("is_default"),
            rs.getBoolean("is_active")
    );

    private static final String COLUMNS =
            "preset_key, name, icon, cycle_days, note, sort_order, is_default, is_active";

    private final NamedParameterJdbcTemplate jdbc;

    public CategoryPresetRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<CategoryPreset> findAllActive() {
        String sql = """
                select %s from category_presets
                where is_active = true
                order by sort_order
                """.formatted(COLUMNS);
        return jdbc.query(sql, ROW_MAPPER);
    }

    public Optional<CategoryPreset> findActiveByKey(String key) {
        String sql = """
                select %s from category_presets
                where preset_key = :key and is_active = true
                """.formatted(COLUMNS);
        try {
            return Optional.ofNullable(jdbc.queryForObject(sql, new MapSqlParameterSource("key", key), ROW_MAPPER));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}
