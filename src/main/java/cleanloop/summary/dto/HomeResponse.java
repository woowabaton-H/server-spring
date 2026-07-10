package cleanloop.summary.dto;

import cleanloop.category.dto.CategoryResponse;
import cleanloop.completion.dto.CompletionLogResponse;
import java.time.LocalDate;
import java.util.List;

/**
 * unreadNotificationCount는 Notification 도메인이 없으므로 이번 범위에서 생략한다.
 */
public record HomeResponse(
        LocalDate today,
        String message,
        int monthlyCompletionCount,
        List<CategoryResponse> categories,
        List<CompletionLogResponse> recentLogs
) {
}
