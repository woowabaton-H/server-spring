package cleanloop.notification;

/**
 * 알림 문구. backend-design 13.1-4와 final-plan 12.3에 따라 압박형이 아니라 제안형으로 쓴다.
 * "관리일이 지났어요"처럼 못 한 일을 지적하는 문구는 쓰지 않는다.
 */
final class NotificationMessages {

    private NotificationMessages() {
    }

    static String dueTitle(String categoryName) {
        return "이번 주에는 %s만 챙겨도 충분해요".formatted(categoryName);
    }

    static String dueBody(String categoryName) {
        return "%s 카테고리를 이번 주 안에 한 번 완료하면 다음 관리는 자동으로 다시 잡아둘게요.".formatted(categoryName);
    }

    static String categoryDeepLink(Object categoryId) {
        return "/categories/" + categoryId;
    }
}
