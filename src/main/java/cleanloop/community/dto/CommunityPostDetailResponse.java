package cleanloop.community.dto;

import cleanloop.community.CommunityPost;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public record CommunityPostDetailResponse(
        String id,
        String type,
        String title,
        String tag,
        String body,
        int helpfulCount,
        int commentsCount,
        int answersCount,
        int savedCount,
        boolean isSaved,
        boolean hasMarkedHelpful,
        OffsetDateTime createdAt
) {

    public static CommunityPostDetailResponse of(CommunityPost post, boolean saved,
                                                 boolean markedHelpful, ZoneId timezone) {
        return new CommunityPostDetailResponse(
                post.id().toString(),
                post.type(),
                post.title(),
                post.tag(),
                post.body(),
                post.helpfulCount(),
                post.commentsCount(),
                post.answersCount(),
                post.savedCount(),
                saved,
                markedHelpful,
                post.createdAt().atZone(timezone).toOffsetDateTime());
    }
}
