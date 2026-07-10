package cleanloop.notification.dto;

import java.util.List;

/**
 * unreadCount를 meta가 아니라 data 안에 담는다.
 * 이 프로젝트의 meta는 requestId, nextCursor처럼 전송 계층 값만 싣는 자리이고,
 * 미확인 알림 수는 화면이 그리는 도메인 값이기 때문이다.
 */
public record NotificationListResponse(int unreadCount, List<NotificationResponse> notifications) {
}
