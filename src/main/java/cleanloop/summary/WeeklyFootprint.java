package cleanloop.summary;

import java.time.LocalDate;

/**
 * 주 단위 완료 집계. level은 화면 표시용 0~3 단계다.
 */
public record WeeklyFootprint(LocalDate weekStartDate, int completionCount, int level) {

    /**
     * 0건 -> 0, 1건 -> 1, 2~3건 -> 2, 4건 이상 -> 3.
     */
    public static WeeklyFootprint of(LocalDate weekStartDate, int completionCount) {
        return new WeeklyFootprint(weekStartDate, completionCount, levelOf(completionCount));
    }

    private static int levelOf(int completionCount) {
        if (completionCount == 0) {
            return 0;
        }
        if (completionCount == 1) {
            return 1;
        }
        if (completionCount <= 3) {
            return 2;
        }
        return 3;
    }
}
