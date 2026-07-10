package cleanloop.category;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 사용자가 관리하는 청소 카테고리.
 * nextDueAt과 상태는 저장하지 않고 조회 시점에 계산한다.
 */
public record CleaningCategory(
        UUID id,
        UUID userId,
        String presetKey,
        String name,
        String icon,
        int cycleDays,
        LocalDateTime lastDoneAt,
        String note,
        int sortOrder,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
