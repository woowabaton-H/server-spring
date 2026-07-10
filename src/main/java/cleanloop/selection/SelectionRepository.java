package cleanloop.selection;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SelectionRepository {

    /** 사용자에게는 공개 상태만 노출한다. draft/hidden은 제외한다. */
    private static final String PUBLISHED = "published";

    /** 모든 카테고리 화면에 함께 노출되는 항목의 category 값. */
    public static final String CATEGORY_ALL = "전체";

    private static final RowMapper<SelectionItem> ROW_MAPPER = (rs, rowNum) -> new SelectionItem(
            rs.getObject("id", UUID.class),
            rs.getString("slug"),
            rs.getString("type"),
            rs.getString("category"),
            rs.getString("title"),
            rs.getString("label"),
            rs.getString("price_text"),
            rs.getString("affiliate_text"),
            rs.getString("reason"),
            rs.getString("fit_for"),
            rs.getString("notice"),
            rs.getString("image_url"),
            rs.getString("rating_text"),
            rs.getString("review_count_text"),
            rs.getBoolean("is_highlighted"),
            rs.getString("external_url"),
            rs.getString("status"),
            rs.getInt("sort_order"),
            rs.getTimestamp("created_at").toLocalDateTime()
    );

    private static final String COLUMNS = """
            id, slug, type, category, title, label, price_text, affiliate_text,
            reason, fit_for, notice, image_url, rating_text, review_count_text,
            is_highlighted, external_url, status, sort_order, created_at
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public SelectionRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<SelectionItem> findPublishedBySlug(String slug) {
        String sql = """
                select %s from selection_items
                where slug = :slug and status = :status
                """.formatted(COLUMNS);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("slug", slug)
                .addValue("status", PUBLISHED);
        try {
            return Optional.ofNullable(jdbc.queryForObject(sql, params, ROW_MAPPER));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<SelectionItem> findPublishedByIds(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        String sql = """
                select %s from selection_items
                where id in (:ids) and status = :status
                """.formatted(COLUMNS);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ids", ids)
                .addValue("status", PUBLISHED);
        return jdbc.query(sql, params, ROW_MAPPER);
    }

    /**
     * 정렬은 is_highlighted desc, sort_order asc, created_at desc, id desc.
     * 마지막 id는 tie-breaker로, 앞 세 값이 모두 같은 행이 있어도 페이지 경계가 안정적이다.
     * 다음 페이지 존재 여부를 알기 위해 limit + 1건을 읽는다.
     */
    public List<SelectionItem> findPage(String category, String type, Cursor cursor, int limit) {
        StringBuilder sql = new StringBuilder("""
                select %s from selection_items
                where status = :status
                """.formatted(COLUMNS));
        MapSqlParameterSource params = new MapSqlParameterSource("status", PUBLISHED);

        // 특정 카테고리를 요청하면 그 카테고리와 '전체' 항목을 함께 보여준다
        if (category != null && !CATEGORY_ALL.equals(category)) {
            sql.append(" and category in (:category, :categoryAll)");
            params.addValue("category", category);
            params.addValue("categoryAll", CATEGORY_ALL);
        }
        if (type != null) {
            sql.append(" and type = :type");
            params.addValue("type", type);
        }
        if (cursor != null) {
            sql.append(cursorPredicate());
            params.addValue("cursorHighlighted", cursor.highlighted())
                    .addValue("cursorSortOrder", cursor.sortOrder())
                    .addValue("cursorCreatedAt", cursor.createdAt())
                    .addValue("cursorId", cursor.id());
        }
        sql.append(" order by is_highlighted desc, sort_order asc, created_at desc, id desc limit :limit");
        params.addValue("limit", limit + 1);

        return jdbc.query(sql.toString(), params, ROW_MAPPER);
    }

    /**
     * 정렬 방향이 섞여 있어 튜플 비교를 그대로 쓸 수 없다.
     * 앞 키가 같을 때만 다음 키를 보는 사전식 비교를 펼쳐 쓴다.
     */
    private String cursorPredicate() {
        return """
                 and (
                   is_highlighted < :cursorHighlighted
                   or (is_highlighted = :cursorHighlighted and (
                     sort_order > :cursorSortOrder
                     or (sort_order = :cursorSortOrder and (
                       created_at < :cursorCreatedAt
                       or (created_at = :cursorCreatedAt and id < :cursorId)
                     ))
                   ))
                 )
                """;
    }

    public record Cursor(boolean highlighted, int sortOrder, LocalDateTime createdAt, UUID id) {
    }
}
