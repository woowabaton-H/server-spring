package cleanloop.community.dto;

import cleanloop.community.CommunityPost;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public record CommunityPostSummaryResponse(
        String id,
        String type,
        String title,
        String tag,
        String bodyPreview,
        String imageUrl,
        int helpfulCount,
        int commentsCount,
        int answersCount,
        int savedCount,
        boolean isPopular,
        boolean isSaved,
        OffsetDateTime createdAt
) {

    private static final int PREVIEW_LENGTH = 80;

    public static CommunityPostSummaryResponse of(CommunityPost post, boolean popular,
                                                  boolean saved, ZoneId timezone) {
        return new CommunityPostSummaryResponse(
                post.id().toString(),
                post.type(),
                post.title(),
                post.tag(),
                previewOf(post.body()),
                post.imageUrl(),
                post.helpfulCount(),
                post.commentsCount(),
                post.answersCount(),
                post.savedCount(),
                popular,
                saved,
                post.createdAt().atZone(timezone).toOffsetDateTime());
    }

    private static String previewOf(String body) {
        if (body == null || body.length() <= PREVIEW_LENGTH) {
            return body;
        }
        return body.substring(0, PREVIEW_LENGTH) + "…";
    }
}
