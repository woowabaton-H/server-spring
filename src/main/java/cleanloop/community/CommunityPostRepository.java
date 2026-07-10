package cleanloop.community;

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
public class CommunityPostRepository {

    /** 숨김 처리된 글은 사용자 API에서 제외한다. */
    private static final String PUBLISHED = "published";

    /** 인기 글로 표시할 상위 개수. */
    public static final int POPULAR_TOP_N = 3;

    private static final String POPULAR_SCORE = "(helpful_count + saved_count)";

    private static final RowMapper<CommunityPost> ROW_MAPPER = (rs, rowNum) -> new CommunityPost(
            rs.getObject("id", UUID.class),
            rs.getString("type"),
            rs.getString("title"),
            rs.getString("tag"),
            rs.getString("body"),
            rs.getInt("helpful_count"),
            rs.getInt("comments_count"),
            rs.getInt("answers_count"),
            rs.getInt("saved_count"),
            rs.getString("status"),
            rs.getBoolean("is_recommended"),
            rs.getTimestamp("created_at").toLocalDateTime()
    );

    private static final String COLUMNS = """
            id, type, title, tag, body, helpful_count, comments_count,
            answers_count, saved_count, status, is_recommended, created_at
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public CommunityPostRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<CommunityPost> findPublishedById(UUID id) {
        String sql = """
                select %s from community_posts
                where id = :id and status = :status
                """.formatted(COLUMNS);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("status", PUBLISHED);
        try {
            return Optional.ofNullable(jdbc.queryForObject(sql, params, ROW_MAPPER));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 인기순((helpful_count + saved_count) desc) 정렬. created_at desc, id desc가 tie-breaker다.
     * 다음 페이지 존재 여부를 알기 위해 limit + 1건을 읽는다.
     */
    public List<CommunityPost> findPage(PostType type, String tag, Cursor cursor, int limit) {
        StringBuilder sql = new StringBuilder("""
                select %s from community_posts
                where status = :status and type = :type
                """.formatted(COLUMNS));
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("status", PUBLISHED)
                .addValue("type", type.code());

        if (tag != null) {
            sql.append(" and tag = :tag");
            params.addValue("tag", tag);
        }
        if (cursor != null) {
            sql.append("""
                     and (%s < :cursorScore
                       or (%s = :cursorScore and (
                         created_at < :cursorCreatedAt
                         or (created_at = :cursorCreatedAt and id < :cursorId)
                       )))
                    """.formatted(POPULAR_SCORE, POPULAR_SCORE));
            params.addValue("cursorScore", cursor.popularScore())
                    .addValue("cursorCreatedAt", cursor.createdAt())
                    .addValue("cursorId", cursor.id());
        }
        sql.append(" order by %s desc, created_at desc, id desc limit :limit".formatted(POPULAR_SCORE));
        params.addValue("limit", limit + 1);

        return jdbc.query(sql.toString(), params, ROW_MAPPER);
    }

    /**
     * type 안에서 상위 N번째 글의 인기 점수.
     *
     * <p>순위가 아니라 점수 문턱값으로 비교하므로 페이지를 넘겨도 isPopular 판정이 흔들리지 않는다.
     * tag는 일부러 빼고 계산한다. 인기 여부는 글 자체의 속성이어야 하며,
     * 태그로 좁혔다고 해서 1위 글이 비인기로 뒤집히면 안 된다.
     *
     * <p>글이 N건보다 적으면 비어 있다.
     */
    public Optional<Integer> findPopularScoreThreshold(PostType type) {
        String sql = """
                select %s as score from community_posts
                where status = :status and type = :type
                order by score desc limit 1 offset :offset
                """.formatted(POPULAR_SCORE);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("status", PUBLISHED)
                .addValue("type", type.code())
                .addValue("offset", POPULAR_TOP_N - 1);

        return jdbc.query(sql, params, (rs, rowNum) -> rs.getInt("score")).stream().findFirst();
    }

    /**
     * 반응 행이 실제로 삽입된 경우에만 호출한다. 컬럼명은 열거형이 소유하므로 외부 입력이 섞이지 않는다.
     */
    public void incrementCounter(UUID postId, ReactionType reactionType) {
        String sql = """
                update community_posts set %s = %s + 1 where id = :postId
                """.formatted(reactionType.counterColumn(), reactionType.counterColumn());
        jdbc.update(sql, new MapSqlParameterSource("postId", postId));
    }

    /** 반응 행이 실제로 삭제된 경우에만 호출한다. 카운터가 0 미만이 되지 않도록 막는다. */
    public void decrementCounter(UUID postId, ReactionType reactionType) {
        String column = reactionType.counterColumn();
        String sql = """
                update community_posts set %s = case when %s > 0 then %s - 1 else 0 end
                where id = :postId
                """.formatted(column, column, column);
        jdbc.update(sql, new MapSqlParameterSource("postId", postId));
    }

    public record Cursor(int popularScore, LocalDateTime createdAt, UUID id) {
    }
}
