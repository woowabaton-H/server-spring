package cleanloop.completion;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 청소 완료 기록.
 * categoryName은 완료 당시 이름 스냅샷이라, 이후 카테고리 이름이 바뀌어도 기록은 그대로 남는다.
 */
public record CompletionLog(
        UUID id,
        UUID userId,
        UUID categoryId,
        String categoryName,
        LocalDateTime completedAt,
        LocalDateTime createdAt
) {
}
