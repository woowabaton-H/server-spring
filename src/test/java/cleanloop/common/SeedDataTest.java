package cleanloop.common;

import static org.assertj.core.api.Assertions.assertThat;

import cleanloop.common.user.CurrentUserProvider;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * schema.sql + data.sql이 부팅 시 실제로 적재되는지 확인한다.
 * 도메인 구현이 기대하는 시드 데이터의 최소 계약이다.
 */
@SpringBootTest
class SeedDataTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CurrentUserProvider currentUserProvider;

    @Test
    void 데모_사용자가_시드된다() {
        UUID userId = currentUserProvider.currentUserId();

        String name = jdbcTemplate.queryForObject(
                "select name from users where id = ?", String.class, userId);

        assertThat(name).isEqualTo("보송");
    }

    @Test
    void 각_테이블에_시드_데이터가_적재된다() {
        assertThat(countOf("users")).isEqualTo(1);
        assertThat(countOf("category_presets")).isEqualTo(7);
        assertThat(countOf("cleaning_categories")).isEqualTo(6);
        assertThat(countOf("completion_logs")).isEqualTo(10);
        assertThat(countOf("selection_items")).isEqualTo(7);
        assertThat(countOf("selection_attributes")).isEqualTo(28);
        assertThat(countOf("provider_options")).isEqualTo(3);
        assertThat(countOf("saved_selections")).isEqualTo(1);
        assertThat(countOf("community_posts")).isEqualTo(7);
        assertThat(countOf("community_reactions")).isEqualTo(2);
        assertThat(countOf("community_comments")).isEqualTo(6);
        assertThat(countOf("notifications")).isEqualTo(2);
    }

    /**
     * 홈의 unreadNotificationCount가 시드 상태에서 1이 되도록 고정한다.
     * 알림함이 비어 있으면 배지 동작을 확인할 수 없다.
     */
    @Test
    void 미확인_알림이_한_건_시드된다() {
        Integer unread = jdbcTemplate.queryForObject(
                "select count(*) from notifications where is_read = false", Integer.class);

        assertThat(unread).isEqualTo(1);
    }

    /**
     * 댓글 조회 API가 생긴 뒤로는 집계 컬럼이 실제 행 수와 어긋나면 안 된다.
     * tips 글은 comments_count로, qa 글은 answers_count로 센다.
     */
    @Test
    void 댓글_집계_컬럼은_실제_행_수와_일치한다() {
        Integer mismatched = jdbcTemplate.queryForObject("""
                select count(*) from community_posts p
                where (case when p.type = 'tips' then p.comments_count else p.answers_count end)
                      <> (select count(*) from community_comments c
                          where c.post_id = p.id and c.status = 'published')
                """, Integer.class);

        assertThat(mismatched).isZero();
    }

    @Test
    void 기본_생성_대상_프리셋은_여섯개다() {
        Integer defaults = jdbcTemplate.queryForObject(
                "select count(*) from category_presets where is_default = true", Integer.class);

        assertThat(defaults).isEqualTo(6);
    }

    private Integer countOf(String table) {
        return jdbcTemplate.queryForObject("select count(*) from " + table, Integer.class);
    }
}
