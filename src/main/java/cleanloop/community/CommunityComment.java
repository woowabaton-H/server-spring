package cleanloop.community;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 댓글 또는 답변. 저장 구조는 같고, 소속 글의 type에 따라 화면 표기와 집계 컬럼만 달라진다.
 * authorName은 조회 시 users를 조인해 채운다.
 */
public record CommunityComment(
        UUID id,
        UUID postId,
        UUID userId,
        String authorName,
        String body,
        LocalDateTime createdAt
) {
}
