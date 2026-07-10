package cleanloop.notification.dto;

/**
 * unreadCount는 항상 0이지만 응답에 포함한다.
 * 알림함을 여는 화면이 배지를 지우려고 홈을 다시 부르지 않아도 되게 한다.
 */
public record ReadAllNotificationsResponse(int updatedCount, int unreadCount) {
}
