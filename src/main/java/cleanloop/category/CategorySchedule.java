package cleanloop.category;

import java.time.OffsetDateTime;

/**
 * 조회 시점에 계산한 다음 관리 시점과 상태. 영속 데이터가 아니다.
 */
public record CategorySchedule(
        OffsetDateTime nextDueAt,
        StatusCode code,
        String label,
        int daysUntilNext
) {

    public enum StatusCode {
        DUE, SOON, GOOD;

        public String code() {
            return name().toLowerCase();
        }
    }
}
