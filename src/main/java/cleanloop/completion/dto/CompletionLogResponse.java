package cleanloop.completion.dto;

import cleanloop.completion.CompletionLog;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public record CompletionLogResponse(
        String id,
        String categoryId,
        String categoryName,
        OffsetDateTime completedAt
) {

    public static CompletionLogResponse of(CompletionLog log, ZoneId timezone) {
        return new CompletionLogResponse(
                log.id().toString(),
                log.categoryId() != null ? log.categoryId().toString() : null,
                log.categoryName(),
                log.completedAt().atZone(timezone).toOffsetDateTime()
        );
    }
}
