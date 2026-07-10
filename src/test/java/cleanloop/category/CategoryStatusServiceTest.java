package cleanloop.category;

import static org.assertj.core.api.Assertions.assertThat;

import cleanloop.category.CategorySchedule.StatusCode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * docs/issues/02-category.md 상태 계산 규칙 검증.
 *
 * <pre>
 * days_until_next <= 0 -> due
 * days_until_next <= 2 -> soon
 * 그 외                -> good
 * </pre>
 */
class CategoryStatusServiceTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    /** 2026-07-10T12:00:00+09:00 */
    private static final Instant NOW = LocalDateTime.of(2026, 7, 10, 12, 0).atZone(SEOUL).toInstant();

    private final CategoryStatusService statusService =
            new CategoryStatusService(Clock.fixed(NOW, SEOUL));

    private CategorySchedule scheduleAfter(int daysSinceLastDone, int cycleDays) {
        LocalDateTime lastDoneAt = LocalDateTime.ofInstant(NOW, SEOUL).minusDays(daysSinceLastDone);
        return statusService.scheduleOf(lastDoneAt, cycleDays, SEOUL);
    }

    @Test
    void nextDueAt은_마지막_완료일에_주기를_더한_값이다() {
        CategorySchedule schedule = scheduleAfter(3, 14);

        assertThat(schedule.nextDueAt())
                .isEqualTo(LocalDateTime.of(2026, 7, 21, 12, 0).atZone(SEOUL).toOffsetDateTime());
    }

    @ParameterizedTest(name = "{0}일 전 완료, 주기 {1}일 -> {2}일 남음")
    @CsvSource({
            "16, 14, -2",  // 이미 2일 지남
            "14, 14,  0",  // 오늘이 관리일
            "13, 14,  1",
            "6,   7,  1",
            "3,  14, 11",
            "25, 28,  3",
    })
    void 남은_일수는_다음_관리일까지의_일수다(int daysSinceLastDone, int cycleDays, int expectedDaysUntilNext) {
        assertThat(scheduleAfter(daysSinceLastDone, cycleDays).daysUntilNext())
                .isEqualTo(expectedDaysUntilNext);
    }

    @Test
    void 관리일이_지났으면_due다() {
        CategorySchedule schedule = scheduleAfter(16, 14);

        assertThat(schedule.code()).isEqualTo(StatusCode.DUE);
        assertThat(schedule.label()).isEqualTo("이번 주에 챙기면 좋아요");
    }

    @Test
    void 관리일_당일이면_due다() {
        assertThat(scheduleAfter(14, 14).code()).isEqualTo(StatusCode.DUE);
    }

    @ParameterizedTest(name = "{0}일 남으면 soon")
    @CsvSource({"13, 14, 1", "12, 14, 2"})
    void 이틀_이하로_남으면_soon이다(int daysSinceLastDone, int cycleDays, int daysUntilNext) {
        CategorySchedule schedule = scheduleAfter(daysSinceLastDone, cycleDays);

        assertThat(schedule.code()).isEqualTo(StatusCode.SOON);
        assertThat(schedule.label()).isEqualTo("%d일 안에 하면 좋아요".formatted(daysUntilNext));
    }

    @Test
    void 사흘_이상_남으면_good이다() {
        CategorySchedule schedule = scheduleAfter(11, 14);

        assertThat(schedule.code()).isEqualTo(StatusCode.GOOD);
        assertThat(schedule.daysUntilNext()).isEqualTo(3);
        assertThat(schedule.label()).isEqualTo("3일 뒤 다시 보면 충분해요");
    }

    @Test
    void 하루에_못_미치는_잔여_시간은_올려_센다() {
        // 다음 관리일까지 1시간 남았다면 "1일 안에"로 안내한다
        LocalDateTime lastDoneAt = LocalDateTime.ofInstant(NOW, SEOUL).minusDays(14).plusHours(1);

        CategorySchedule schedule = statusService.scheduleOf(lastDoneAt, 14, SEOUL);

        assertThat(schedule.daysUntilNext()).isEqualTo(1);
        assertThat(schedule.code()).isEqualTo(StatusCode.SOON);
    }

    @Test
    void 상태_코드는_소문자로_노출된다() {
        assertThat(StatusCode.DUE.code()).isEqualTo("due");
        assertThat(StatusCode.SOON.code()).isEqualTo("soon");
        assertThat(StatusCode.GOOD.code()).isEqualTo("good");
    }
}
