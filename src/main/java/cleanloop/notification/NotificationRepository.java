package cleanloop.notification;

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
public class NotificationRepository {

    private static final RowMapper<Notification> ROW_MAPPER = (rs, rowNum) -> new Notification(
            rs.getObject("id", UUID.class),
            rs.getObject("user_id", UUID.class),
            rs.getObject("category_id", UUID.class),
            rs.getString("category_name"),
            rs.getString("title"),
            rs.getString("body"),
            rs.getString("deep_link"),
            rs.getBoolean("is_read"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("read_at") != null ? rs.getTimestamp("read_at").toLocalDateTime() : null
    );

    private static final String SELECT_WITH_CATEGORY = """
            select n.id, n.user_id, n.category_id, c.name as category_name,
                   n.title, n.body, n.deep_link, n.is_read, n.created_at, n.read_at
            from notifications n
            left join cleaning_categories c on c.id = n.category_id
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public NotificationRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * backend-design 11: 읽음 여부, 생성 시각 최신순.
     * 알림함은 한 화면에서 다 보는 목록이라 커서 없이 최근 것만 자른다.
     */
    public List<Notification> findRecent(UUID userId, int limit) {
        String sql = SELECT_WITH_CATEGORY + """
                where n.user_id = :userId
                order by n.is_read asc, n.created_at desc, n.id desc
                limit :limit
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("limit", limit);
        return jdbc.query(sql, params, ROW_MAPPER);
    }

    public Optional<Notification> findByIdAndUserId(UUID id, UUID userId) {
        String sql = SELECT_WITH_CATEGORY + """
                where n.id = :id and n.user_id = :userId
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("userId", userId);
        try {
            return Optional.ofNullable(jdbc.queryForObject(sql, params, ROW_MAPPER));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public int countUnread(UUID userId) {
        String sql = "select count(*) from notifications where user_id = :userId and is_read = false";
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource("userId", userId), Integer.class);
        return count != null ? count : 0;
    }

    public boolean existsUnreadByCategory(UUID userId, UUID categoryId) {
        String sql = """
                select count(*) from notifications
                where user_id = :userId and category_id = :categoryId and is_read = false
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("categoryId", categoryId);
        Integer count = jdbc.queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
    }

    /** 이미 읽은 알림은 건드리지 않는다. 처음 읽은 시각을 덮어쓰지 않기 위해서다. */
    public int markRead(UUID id, UUID userId, LocalDateTime readAt) {
        String sql = """
                update notifications set is_read = true, read_at = :readAt
                where id = :id and user_id = :userId and is_read = false
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("userId", userId)
                .addValue("readAt", readAt);
        return jdbc.update(sql, params);
    }

    public int markAllRead(UUID userId, LocalDateTime readAt) {
        String sql = """
                update notifications set is_read = true, read_at = :readAt
                where user_id = :userId and is_read = false
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("readAt", readAt);
        return jdbc.update(sql, params);
    }

    public int markReadByCategory(UUID userId, UUID categoryId, LocalDateTime readAt) {
        String sql = """
                update notifications set is_read = true, read_at = :readAt
                where user_id = :userId and category_id = :categoryId and is_read = false
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("categoryId", categoryId)
                .addValue("readAt", readAt);
        return jdbc.update(sql, params);
    }

    public void insert(Notification notification) {
        String sql = """
                insert into notifications (id, user_id, category_id, title, body, deep_link, is_read, created_at)
                values (:id, :userId, :categoryId, :title, :body, :deepLink, false, :createdAt)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", notification.id())
                .addValue("userId", notification.userId())
                .addValue("categoryId", notification.categoryId())
                .addValue("title", notification.title())
                .addValue("body", notification.body())
                .addValue("deepLink", notification.deepLink())
                .addValue("createdAt", notification.createdAt());
        jdbc.update(sql, params);
    }
}
