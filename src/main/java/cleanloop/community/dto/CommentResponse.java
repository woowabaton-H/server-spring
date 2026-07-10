package cleanloop.community.dto;

import cleanloop.community.CommunityComment;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

public record CommentResponse(
        String id,
        String postId,
        String authorName,
        boolean authorIsMe,
        String body,
        OffsetDateTime createdAt
) {

    public static CommentResponse of(CommunityComment comment, UUID currentUserId, ZoneId timezone) {
        return new CommentResponse(
                comment.id().toString(),
                comment.postId().toString(),
                comment.authorName(),
                comment.userId().equals(currentUserId),
                comment.body(),
                comment.createdAt().atZone(timezone).toOffsetDateTime());
    }
}
