package cleanloop.notification;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 앱 내부 알림. MVP에서는 푸시를 보내지 않고 알림함에만 쌓는다.
 *
 * <p>categoryName은 테이블에 없다. 카테고리 이름이 바뀌면 알림에도 바뀐 이름이 보여야 하므로
 * 조회 시 cleaning_categories와 조인해 채운다. 카테고리가 없는 알림은 null이다.
 */
public record Notification(
        UUID id,
        UUID userId,
        UUID categoryId,
        String categoryName,
        String title,
        String body,
        String deepLink,
        boolean read,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {
}
