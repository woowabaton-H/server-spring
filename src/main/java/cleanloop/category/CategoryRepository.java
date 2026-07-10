package cleanloop.category;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CategoryRepository {

    private static final RowMapper<CleaningCategory> ROW_MAPPER = (rs, rowNum) -> new CleaningCategory(
            rs.getObject("id", UUID.class),
            rs.getObject("user_id", UUID.class),
            rs.getString("preset_key"),
            rs.getString("name"),
            rs.getString("icon"),
            rs.getInt("cycle_days"),
            rs.getTimestamp("last_done_at").toLocalDateTime(),
            rs.getString("note"),
            rs.getInt("sort_order"),
            rs.getBoolean("is_active"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime()
    );

    private static final String COLUMNS = """
            id, user_id, preset_key, name, icon, cycle_days,
            last_done_at, note, sort_order, is_active, created_at, updated_at
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public CategoryRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<CleaningCategory> findActiveByUserId(UUID userId) {
        String sql = """
                select %s from cleaning_categories
                where user_id = :userId and is_active = true
                order by sort_order
                """.formatted(COLUMNS);
        return jdbc.query(sql, new MapSqlParameterSource("userId", userId), ROW_MAPPER);
    }

    public Optional<CleaningCategory> findActiveByIdAndUserId(UUID id, UUID userId) {
        String sql = """
                select %s from cleaning_categories
                where id = :id and user_id = :userId and is_active = true
                """.formatted(COLUMNS);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("userId", userId);
        try {
            return Optional.ofNullable(jdbc.queryForObject(sql, params, ROW_MAPPER));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 완료 처리처럼 읽은 값을 근거로 갱신하는 흐름에서 쓴다.
     * 연속 클릭 시 같은 카테고리를 동시에 갱신하지 않도록 행을 잠근다.
     */
    public Optional<CleaningCategory> findActiveByIdAndUserIdForUpdate(UUID id, UUID userId) {
        String sql = """
                select %s from cleaning_categories
                where id = :id and user_id = :userId and is_active = true
                for update
                """.formatted(COLUMNS);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("userId", userId);
        try {
            return Optional.ofNullable(jdbc.queryForObject(sql, params, ROW_MAPPER));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public void updateLastDoneAt(UUID id, LocalDateTime lastDoneAt) {
        String sql = """
                update cleaning_categories
                set last_done_at = :lastDoneAt, updated_at = current_timestamp
                where id = :id
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("lastDoneAt", lastDoneAt);
        jdbc.update(sql, params);
    }

    public int countActiveByUserId(UUID userId) {
        String sql = """
                select count(*) from cleaning_categories
                where user_id = :userId and is_active = true
                """;
        return count(sql, new MapSqlParameterSource("userId", userId));
    }

    public boolean existsActiveByName(UUID userId, String name) {
        String sql = """
                select count(*) from cleaning_categories
                where user_id = :userId and is_active = true and name = :name
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("name", name);
        return count(sql, params) > 0;
    }

    /**
     * 이름 수정 시 자기 자신은 중복 검사에서 제외한다.
     */
    public boolean existsActiveByNameExcluding(UUID userId, String name, UUID excludedId) {
        String sql = """
                select count(*) from cleaning_categories
                where user_id = :userId and is_active = true and name = :name and id <> :excludedId
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("name", name)
                .addValue("excludedId", excludedId);
        return count(sql, params) > 0;
    }

    public boolean existsActiveByPresetKey(UUID userId, String presetKey) {
        String sql = """
                select count(*) from cleaning_categories
                where user_id = :userId and is_active = true and preset_key = :presetKey
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("presetKey", presetKey);
        return count(sql, params) > 0;
    }

    public int nextSortOrder(UUID userId) {
        String sql = """
                select coalesce(max(sort_order), 0) + 1 from cleaning_categories
                where user_id = :userId and is_active = true
                """;
        return count(sql, new MapSqlParameterSource("userId", userId));
    }

    public void insert(CleaningCategory category) {
        String sql = """
                insert into cleaning_categories
                    (id, user_id, preset_key, name, icon, cycle_days, last_done_at, note, sort_order, is_active)
                values
                    (:id, :userId, :presetKey, :name, :icon, :cycleDays, :lastDoneAt, :note, :sortOrder, true)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", category.id())
                .addValue("userId", category.userId())
                .addValue("presetKey", category.presetKey())
                .addValue("name", category.name())
                .addValue("icon", category.icon())
                .addValue("cycleDays", category.cycleDays())
                .addValue("lastDoneAt", category.lastDoneAt())
                .addValue("note", category.note())
                .addValue("sortOrder", category.sortOrder());
        jdbc.update(sql, params);
    }

    public void update(UUID id, String name, int cycleDays, String note) {
        String sql = """
                update cleaning_categories
                set name = :name, cycle_days = :cycleDays, note = :note, updated_at = current_timestamp
                where id = :id
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("name", name)
                .addValue("cycleDays", cycleDays)
                .addValue("note", note);
        jdbc.update(sql, params);
    }

    public void deactivate(UUID id) {
        String sql = """
                update cleaning_categories
                set is_active = false, updated_at = current_timestamp
                where id = :id
                """;
        jdbc.update(sql, new MapSqlParameterSource("id", id));
    }

    private int count(String sql, MapSqlParameterSource params) {
        Integer result = jdbc.queryForObject(sql, params, Integer.class);
        return result != null ? result : 0;
    }
}
