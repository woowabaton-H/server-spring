package cleanloop.community;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CommunityReactionRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public CommunityReactionRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 유니크 제약(user_id, post_id, reaction_type)이 사용자당 1회를 보장한다.
     * 실제로 삽입됐을 때만 true를 반환하므로, 호출부는 이때만 카운터를 올리면 된다.
     */
    public boolean insertIfAbsent(UUID userId, UUID postId, ReactionType reactionType) {
        String sql = """
                insert into community_reactions (id, user_id, post_id, reaction_type)
                values (:id, :userId, :postId, :reactionType)
                """;
        MapSqlParameterSource params = params(userId, postId, reactionType)
                .addValue("id", UUID.randomUUID());
        try {
            jdbc.update(sql, params);
            return true;
        } catch (DuplicateKeyException alreadyReacted) {
            return false;
        }
    }

    /** 삭제된 행이 있을 때만 true. 호출부는 이때만 카운터를 내린다. */
    public boolean delete(UUID userId, UUID postId, ReactionType reactionType) {
        String sql = """
                delete from community_reactions
                where user_id = :userId and post_id = :postId and reaction_type = :reactionType
                """;
        return jdbc.update(sql, params(userId, postId, reactionType)) > 0;
    }

    public boolean exists(UUID userId, UUID postId, ReactionType reactionType) {
        return findCreatedAt(userId, postId, reactionType).isPresent();
    }

    public Optional<LocalDateTime> findCreatedAt(UUID userId, UUID postId, ReactionType reactionType) {
        String sql = """
                select created_at from community_reactions
                where user_id = :userId and post_id = :postId and reaction_type = :reactionType
                """;
        return jdbc.query(sql, params(userId, postId, reactionType),
                        (rs, rowNum) -> rs.getTimestamp("created_at").toLocalDateTime())
                .stream()
                .findFirst();
    }

    /** 목록에서 글마다 반응 여부를 붙이기 위해 한 번에 읽는다. */
    public Set<UUID> findReactedPostIds(UUID userId, ReactionType reactionType) {
        String sql = """
                select post_id from community_reactions
                where user_id = :userId and reaction_type = :reactionType
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("reactionType", reactionType.code());
        return jdbc.query(sql, params, (rs, rowNum) -> rs.getObject("post_id", UUID.class))
                .stream()
                .collect(Collectors.toUnmodifiableSet());
    }

    private MapSqlParameterSource params(UUID userId, UUID postId, ReactionType reactionType) {
        return new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("postId", postId)
                .addValue("reactionType", reactionType.code());
    }
}
