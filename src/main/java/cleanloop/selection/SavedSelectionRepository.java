package cleanloop.selection;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SavedSelectionRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public SavedSelectionRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Set<UUID> findSavedItemIds(UUID userId) {
        String sql = "select selection_item_id from saved_selections where user_id = :userId";
        return jdbc.query(sql, new MapSqlParameterSource("userId", userId),
                        (rs, rowNum) -> rs.getObject("selection_item_id", UUID.class))
                .stream()
                .collect(Collectors.toUnmodifiableSet());
    }

    public Optional<LocalDateTime> findSavedAt(UUID userId, UUID selectionItemId) {
        String sql = """
                select created_at from saved_selections
                where user_id = :userId and selection_item_id = :selectionItemId
                """;
        List<LocalDateTime> found = jdbc.query(sql, params(userId, selectionItemId),
                (rs, rowNum) -> rs.getTimestamp("created_at").toLocalDateTime());
        return found.stream().findFirst();
    }

    /**
     * 같은 항목을 반복 저장해도 결과가 같도록 멱등 처리한다.
     * 유니크 제약 위반은 이미 저장된 상태이므로 성공으로 취급한다.
     */
    public LocalDateTime save(UUID userId, UUID selectionItemId) {
        String sql = """
                insert into saved_selections (id, user_id, selection_item_id)
                values (:id, :userId, :selectionItemId)
                """;
        MapSqlParameterSource params = params(userId, selectionItemId).addValue("id", UUID.randomUUID());
        try {
            jdbc.update(sql, params);
        } catch (DuplicateKeyException alreadySaved) {
            // 이미 저장된 항목이다. 아래에서 기존 저장 시각을 읽어 돌려준다.
        }
        return findSavedAt(userId, selectionItemId).orElseThrow();
    }

    /** 저장돼 있지 않아도 오류가 아니다. 삭제된 행 수를 반환한다. */
    public int delete(UUID userId, UUID selectionItemId) {
        String sql = """
                delete from saved_selections
                where user_id = :userId and selection_item_id = :selectionItemId
                """;
        return jdbc.update(sql, params(userId, selectionItemId));
    }

    public int countByUserId(UUID userId) {
        String sql = "select count(*) from saved_selections where user_id = :userId";
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource("userId", userId), Integer.class);
        return count != null ? count : 0;
    }

    /** 저장 시각 최신순. 공개 상태가 아닌 항목은 제외한다. */
    public List<UUID> findSavedItemIdsOrderBySavedAtDesc(UUID userId) {
        String sql = """
                select s.selection_item_id from saved_selections s
                join selection_items i on i.id = s.selection_item_id
                where s.user_id = :userId and i.status = 'published'
                order by s.created_at desc
                """;
        return jdbc.query(sql, new MapSqlParameterSource("userId", userId),
                (rs, rowNum) -> rs.getObject("selection_item_id", UUID.class));
    }

    private MapSqlParameterSource params(UUID userId, UUID selectionItemId) {
        return new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("selectionItemId", selectionItemId);
    }
}
