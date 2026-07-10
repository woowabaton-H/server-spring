package cleanloop.community;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CommunityCommentRepository {

    /** 숨김 처리된 댓글은 사용자 API에서 제외한다. */
    private static final String PUBLISHED = "published";

    private static final RowMapper<CommunityComment> ROW_MAPPER = (rs, rowNum) -> new CommunityComment(
            rs.getObject("id", UUID.class),
            rs.getObject("post_id", UUID.class),
            rs.getObject("user_id", UUID.class),
            rs.getString("author_name"),
            rs.getString("body"),
            rs.getTimestamp("created_at").toLocalDateTime()
    );

    private static final String SELECT_WITH_AUTHOR = """
            select c.id, c.post_id, c.user_id, u.name as author_name, c.body, c.created_at
            from community_comments c
            join users u on u.id = c.user_id
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public CommunityCommentRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 대화 흐름대로 읽도록 오래된 순(created_at asc, id asc)으로 정렬한다.
     * 다른 목록과 커서 방향이 반대인 이유다. 다음 페이지 존재 여부를 알기 위해 limit + 1건을 읽는다.
     */
    public List<CommunityComment> findPage(UUID postId, Cursor cursor, int limit) {
        StringBuilder sql = new StringBuilder(SELECT_WITH_AUTHOR)
                .append(" where c.post_id = :postId and c.status = :status");
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("postId", postId)
                .addValue("status", PUBLISHED);

        if (cursor != null) {
            sql.append(" and (c.created_at > :cursorCreatedAt"
                    + " or (c.created_at = :cursorCreatedAt and c.id > :cursorId))");
            params.addValue("cursorCreatedAt", cursor.createdAt())
                    .addValue("cursorId", cursor.id());
        }
        sql.append(" order by c.created_at asc, c.id asc limit :limit");
        params.addValue("limit", limit + 1);

        return jdbc.query(sql.toString(), params, ROW_MAPPER);
    }

    public CommunityComment insert(UUID id, UUID postId, UUID userId, String body) {
        String sql = """
                insert into community_comments (id, post_id, user_id, body)
                values (:id, :postId, :userId, :body)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("postId", postId)
                .addValue("userId", userId)
                .addValue("body", body);
        jdbc.update(sql, params);

        return jdbc.queryForObject(SELECT_WITH_AUTHOR + " where c.id = :id",
                new MapSqlParameterSource("id", id), ROW_MAPPER);
    }

    public record Cursor(LocalDateTime createdAt, UUID id) {
    }
}
