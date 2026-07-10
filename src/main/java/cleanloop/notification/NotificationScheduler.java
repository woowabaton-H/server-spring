package cleanloop.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * backend-design 13의 due-category-notification 스케줄러.
 *
 * <p>사용자 타임존별로 각자의 오전에 돌리는 것이 원칙이지만, MVP 사용자는 모두 Asia/Seoul이다.
 * 타임존이 갈리면 매시 정각에 돌면서 사용자 로컬 시각이 9시인 사람만 처리하도록 바꾼다.
 */
@Component
public class NotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduler.class);

    private final NotificationService notificationService;

    public NotificationScheduler(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * 예외를 삼킨다. 스케줄러가 죽으면 다음 실행까지 알림이 아예 생기지 않기 때문이다.
     */
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void generateDueNotifications() {
        try {
            notificationService.generateDueNotifications();
        } catch (Exception e) {
            log.error("due_category_notification_failed", e);
        }
    }
}
