package cleanloop.selection;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProviderOptionRepository {

    private static final RowMapper<ProviderOption> ROW_MAPPER = (rs, rowNum) -> new ProviderOption(
            rs.getObject("id", UUID.class),
            rs.getObject("selection_item_id", UUID.class),
            rs.getString("name"),
            rs.getString("rating_text"),
            rs.getString("price_text"),
            rs.getString("note"),
            rs.getString("external_url"),
            rs.getInt("sort_order")
    );

    private static final String COLUMNS =
            "id, selection_item_id, name, rating_text, price_text, note, external_url, sort_order";

    private final NamedParameterJdbcTemplate jdbc;

    public ProviderOptionRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<ProviderOption> findActiveBySelectionItemId(UUID selectionItemId) {
        String sql = """
                select %s from provider_options
                where selection_item_id = :selectionItemId and is_active = true
                order by sort_order
                """.formatted(COLUMNS);
        return jdbc.query(sql, new MapSqlParameterSource("selectionItemId", selectionItemId), ROW_MAPPER);
    }

    public Optional<ProviderOption> findActiveById(UUID id, UUID selectionItemId) {
        String sql = """
                select %s from provider_options
                where id = :id and selection_item_id = :selectionItemId and is_active = true
                """.formatted(COLUMNS);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("selectionItemId", selectionItemId);
        try {
            return Optional.ofNullable(jdbc.queryForObject(sql, params, ROW_MAPPER));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}
