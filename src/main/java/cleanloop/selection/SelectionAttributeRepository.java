package cleanloop.selection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SelectionAttributeRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public SelectionAttributeRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 여러 셀렉션의 속성을 한 번에 읽는다. 목록 조회에서 셀렉션마다 쿼리를 날리지 않기 위해서다.
     * 반환 맵에는 해당 종류의 값이 하나도 없는 셀렉션은 들어 있지 않다.
     */
    public Map<UUID, List<String>> findByItemIds(Collection<UUID> itemIds, SelectionAttributeKind kind) {
        if (itemIds.isEmpty()) {
            return Map.of();
        }
        String sql = """
                select selection_item_id, attribute_value from selection_attributes
                where selection_item_id in (:itemIds) and kind = :kind
                order by selection_item_id, sort_order
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("itemIds", itemIds)
                .addValue("kind", kind.code());

        Map<UUID, List<String>> valuesByItemId = new LinkedHashMap<>();
        jdbc.query(sql, params, rs -> {
            UUID itemId = rs.getObject("selection_item_id", UUID.class);
            valuesByItemId.computeIfAbsent(itemId, key -> new ArrayList<>()).add(rs.getString("attribute_value"));
        });
        return valuesByItemId;
    }

    /**
     * 한 셀렉션의 모든 속성을 종류별로 묶어 한 번에 읽는다.
     * 상세 조회는 tags와 checks를 모두 쓰므로 쿼리를 두 번 날릴 이유가 없다.
     */
    public Map<SelectionAttributeKind, List<String>> findAllByItemId(UUID itemId) {
        String sql = """
                select kind, attribute_value from selection_attributes
                where selection_item_id = :itemId
                order by kind, sort_order
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("itemId", itemId);

        Map<SelectionAttributeKind, List<String>> valuesByKind = new EnumMap<>(SelectionAttributeKind.class);
        jdbc.query(sql, params, rs -> {
            SelectionAttributeKind kind = SelectionAttributeKind.from(rs.getString("kind"));
            if (kind != null) {
                valuesByKind.computeIfAbsent(kind, key -> new ArrayList<>()).add(rs.getString("attribute_value"));
            }
        });
        return valuesByKind;
    }
}
