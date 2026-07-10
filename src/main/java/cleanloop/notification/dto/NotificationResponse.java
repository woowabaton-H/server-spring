package cleanloop.notification.dto;

import cleanloop.notification.Notification;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public record NotificationResponse(
        String id,
        String categoryId,
        String categoryName,
        String title,
        String body,

        /** 알림함에서 눌렀을 때 이동할 앱 내부 경로. 없는 알림도 있다. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String deepLink,

        boolean isRead,
        OffsetDateTime createdAt
) {

    public static NotificationResponse of(Notification notification, ZoneId timezone) {
        return new NotificationResponse(
                notification.id().toString(),
                notification.categoryId() != null ? notification.categoryId().toString() : null,
                notification.categoryName(),
                notification.title(),
                notification.body(),
                notification.deepLink(),
                notification.read(),
                notification.createdAt().atZone(timezone).toOffsetDateTime()
        );
    }
}
