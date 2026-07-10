package cleanloop.summary.dto;

import cleanloop.category.dto.CategoryResponse;
import cleanloop.completion.dto.CompletionLogResponse;
import java.time.LocalDate;
import java.util.List;

public record HomeResponse(
        LocalDate today,
        String message,
        int monthlyCompletionCount,
        int unreadNotificationCount,
        List<CategoryResponse> categories,
        List<CompletionLogResponse> recentLogs
) {
}
