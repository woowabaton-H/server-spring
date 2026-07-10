package cleanloop.community.dto;

import java.time.OffsetDateTime;

public record SavePostResponse(String postId, boolean isSaved, OffsetDateTime savedAt) {
}
