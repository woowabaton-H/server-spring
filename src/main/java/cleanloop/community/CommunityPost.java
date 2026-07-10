package cleanloop.community;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 커뮤니티 글. helpfulCount와 savedCount는 반응 테이블의 집계값을 컬럼에 들고 있는다.
 * authorId는 운영이 심은 콘텐츠에서 null이다.
 */
public record CommunityPost(
        UUID id,
        String type,
        String title,
        String tag,
        String body,
        String imageUrl,
        UUID authorId,
        int helpfulCount,
        int commentsCount,
        int answersCount,
        int savedCount,
        String status,
        boolean recommended,
        LocalDateTime createdAt
) {

    /** MVP 인기 점수. */
    public int popularScore() {
        return helpfulCount + savedCount;
    }

    public PostType postType() {
        return PostType.from(type);
    }
}
