package cleanloop.user;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * 사용자 도메인 모델.
 * timestamp 컬럼은 타임존 없이 저장하므로, 표현 시각은 {@code timezone} 기준으로 해석한다.
 */
public record User(
        UUID id,
        String name,
        String avatarText,
        ZoneId timezone,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
