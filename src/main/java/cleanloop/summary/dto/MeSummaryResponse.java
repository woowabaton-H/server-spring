package cleanloop.summary.dto;

import cleanloop.completion.dto.CompletionLogResponse;
import cleanloop.summary.WeeklyFootprint;
import cleanloop.user.User;
import java.util.List;

public record MeSummaryResponse(
        Profile profile,
        Stats stats,
        List<WeeklyFootprint> weeklyFootprints,
        List<CompletionLogResponse> recentLogs,
        /* Selection 도메인 구현 전까지는 빈 목록이다. */
        List<Object> savedSelections
) {

    public record Profile(String id, String name, String avatarText) {

        public static Profile from(User user) {
            return new Profile(user.id().toString(), user.name(), user.avatarText());
        }
    }

    public record Stats(int monthlyCompletionCount, int categoryCount, int savedSelectionCount) {
    }
}
