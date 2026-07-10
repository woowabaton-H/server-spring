package cleanloop.user;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private static final RowMapper<User> ROW_MAPPER = (rs, rowNum) -> new User(
            rs.getObject("id", UUID.class),
            rs.getString("name"),
            rs.getString("avatar_text"),
            ZoneId.of(rs.getString("timezone")),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime()
    );

    private final NamedParameterJdbcTemplate jdbc;

    public UserRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 알림 생성 스케줄러처럼 특정 사용자가 아닌 전체를 훑는 배치에서 쓴다. */
    public List<User> findAll() {
        String sql = """
                select id, name, avatar_text, timezone, created_at, updated_at
                from users
                order by created_at
                """;
        return jdbc.query(sql, ROW_MAPPER);
    }

    public Optional<User> findById(UUID id) {
        String sql = """
                select id, name, avatar_text, timezone, created_at, updated_at
                from users
                where id = :id
                """;
        try {
            return Optional.ofNullable(jdbc.queryForObject(sql, new MapSqlParameterSource("id", id), ROW_MAPPER));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public int updateProfile(UUID id, String name, String avatarText) {
        String sql = """
                update users
                set name = :name, avatar_text = :avatarText, updated_at = current_timestamp
                where id = :id
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("name", name)
                .addValue("avatarText", avatarText);
        return jdbc.update(sql, params);
    }
}
