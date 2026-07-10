package cleanloop.completion;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CompletionLogRepository {

    private static final RowMapper<CompletionLog> ROW_MAPPER = (rs, rowNum) -> new CompletionLog(
            rs.getObject("id", UUID.class),
            rs.getObject("user_id", UUID.class),
            rs.getObject("category_id", UUID.class),
            rs.getString("category_name"),
            rs.getTimestamp("completed_at").toLocalDateTime(),
            rs.getTimestamp("created_at").toLocalDateTime()
    );

    private static final String COLUMNS = "id, user_id, category_id, category_name, completed_at, created_at";

    private final NamedParameterJdbcTemplate jdbc;

    public CompletionLogRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(CompletionLog log) {
        String sql = """
                insert into completion_logs (id, user_id, category_id, category_name, completed_at)
                values (:id, :userId, :categoryId, :categoryName, :completedAt)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", log.id())
                .addValue("userId", log.userId())
                .addValue("categoryId", log.categoryId())
                .addValue("categoryName", log.categoryName())
                .addValue("completedAt", log.completedAt());
        jdbc.update(sql, params);
    }

    /**
     * (completed_at desc, id desc) 키셋 페이지네이션.
     * 같은 시각에 여러 기록이 있어도 id가 tie-breaker라 페이지 경계에서 누락/중복이 없다.
     * 다음 페이지 존재 여부를 알기 위해 limit + 1건을 읽는다.
     */
    public List<CompletionLog> findPage(UUID userId, LocalDateTime from, LocalDateTime to,
                                        Cursor cursor, int limit) {
        StringBuilder sql = new StringBuilder("""
                select %s from completion_logs
                where user_id = :userId
                """.formatted(COLUMNS));
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);

        if (from != null) {
            sql.append(" and completed_at >= :from");
            params.addValue("from", from);
        }
        if (to != null) {
            sql.append(" and completed_at < :to");
            params.addValue("to", to);
        }
        if (cursor != null) {
            sql.append(" and (completed_at < :cursorCompletedAt"
                    + " or (completed_at = :cursorCompletedAt and id < :cursorId))");
            params.addValue("cursorCompletedAt", cursor.completedAt());
            params.addValue("cursorId", cursor.id());
        }
        sql.append(" order by completed_at desc, id desc limit :limit");
        params.addValue("limit", limit + 1);

        return new ArrayList<>(jdbc.query(sql.toString(), params, ROW_MAPPER));
    }

    public record Cursor(LocalDateTime completedAt, UUID id) {
    }
}
