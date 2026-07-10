package cleanloop.category;

import cleanloop.category.CategorySchedule.StatusCode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Service;

/**
 * 카테고리 상태 계산. 부수효과 없는 순수 계산이며 Category/Completion/Home/Me에서 함께 쓴다.
 *
 * <pre>
 * next_due_at     = last_done_at + cycle_days
 * days_until_next = ceil((next_due_at - now) / 1 day)
 * </pre>
 */
@Service
public class CategoryStatusService {

    private static final long SECONDS_PER_DAY = Duration.ofDays(1).toSeconds();

    /** soon 판정 상한. 이 값 이하로 남으면 곧 챙길 시점으로 본다. */
    private static final int SOON_THRESHOLD_DAYS = 2;

    private final Clock clock;

    public CategoryStatusService(Clock clock) {
        this.clock = clock;
    }

    public CategorySchedule scheduleOf(LocalDateTime lastDoneAt, int cycleDays, ZoneId timezone) {
        return scheduleOf(lastDoneAt, cycleDays, timezone, now(timezone));
    }

    /**
     * 기준 시각을 직접 주는 형태. 홈의 today 파라미터처럼 "오늘"을 옮겨 계산할 때 쓴다.
     */
    public CategorySchedule scheduleOf(LocalDateTime lastDoneAt, int cycleDays, ZoneId timezone,
                                       ZonedDateTime reference) {
        ZonedDateTime nextDue = lastDoneAt.plusDays(cycleDays).atZone(timezone);

        int daysUntilNext = ceilDays(Duration.between(reference, nextDue));
        StatusCode code = codeOf(daysUntilNext);
        return new CategorySchedule(nextDue.toOffsetDateTime(), code, labelOf(code, daysUntilNext), daysUntilNext);
    }

    public ZonedDateTime now(ZoneId timezone) {
        return ZonedDateTime.now(clock.withZone(timezone));
    }

    /**
     * 남은 시간이 하루에 못 미쳐도 "1일 남음"으로 올려 센다. 지난 시점은 음수가 된다.
     */
    private int ceilDays(Duration remaining) {
        return (int) Math.ceil((double) remaining.toSeconds() / SECONDS_PER_DAY);
    }

    private StatusCode codeOf(int daysUntilNext) {
        if (daysUntilNext <= 0) {
            return StatusCode.DUE;
        }
        if (daysUntilNext <= SOON_THRESHOLD_DAYS) {
            return StatusCode.SOON;
        }
        return StatusCode.GOOD;
    }

    private String labelOf(StatusCode code, int daysUntilNext) {
        return switch (code) {
            case DUE -> "이번 주에 챙기면 좋아요";
            case SOON -> "%d일 안에 하면 좋아요".formatted(daysUntilNext);
            case GOOD -> "%d일 뒤 다시 보면 충분해요".formatted(daysUntilNext);
        };
    }
}
