package cleanloop.notification.dto;

import java.time.OffsetDateTime;

public record NotificationReadResponse(String id, boolean isRead, OffsetDateTime readAt) {
}
