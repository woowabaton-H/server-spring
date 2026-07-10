package cleanloop.notification;

import cleanloop.category.CategoryRepository;
import cleanloop.category.CategorySchedule;
import cleanloop.category.CategoryStatusService;
import cleanloop.category.CleaningCategory;
import cleanloop.common.error.ApiException;
import cleanloop.common.error.ErrorCode;
import cleanloop.notification.dto.NotificationListResponse;
import cleanloop.notification.dto.NotificationReadResponse;
import cleanloop.notification.dto.NotificationResponse;
import cleanloop.notification.dto.ReadAllNotificationsResponse;
import cleanloop.user.User;
import cleanloop.user.UserRepository;
import cleanloop.user.UserService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    /** 알림함은 커서 없이 한 번에 내려준다. 이 수를 넘는 오래된 알림은 보여주지 않는다. */
    private static final int LIST_LIMIT = 50;

    private final NotificationRepository notificationRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryStatusService statusService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final Clock clock;

    public NotificationService(NotificationRepository notificationRepository,
                               CategoryRepository categoryRepository,
                               CategoryStatusService statusService,
                               UserRepository userRepository,
                               UserService userService,
                               Clock clock) {
        this.notificationRepository = notificationRepository;
        this.categoryRepository = categoryRepository;
        this.statusService = statusService;
        this.userRepository = userRepository;
        this.userService = userService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public NotificationListResponse findAll() {
        User user = userService.getMe();
        List<NotificationResponse> notifications = notificationRepository.findRecent(user.id(), LIST_LIMIT).stream()
                .map(notification -> NotificationResponse.of(notification, user.timezone()))
                .toList();

        return new NotificationListResponse(notificationRepository.countUnread(user.id()), notifications);
    }

    public int countUnread(UUID userId) {
        return notificationRepository.countUnread(userId);
    }

    /**
     * 이미 읽은 알림에 다시 요청해도 성공으로 본다. 처음 읽은 시각은 유지한다.
     */
    @Transactional
    public NotificationReadResponse read(UUID notificationId) {
        User user = userService.getMe();
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, user.id())
                .orElseThrow(() -> new ApiException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (notification.read()) {
            return toReadResponse(notification.id(), notification.readAt(), user.timezone());
        }

        LocalDateTime readAt = LocalDateTime.now(clock.withZone(user.timezone()));
        notificationRepository.markRead(notification.id(), user.id(), readAt);
        return toReadResponse(notification.id(), readAt, user.timezone());
    }

    /** 알림함을 열 때 한 번에 비운다. */
    @Transactional
    public ReadAllNotificationsResponse readAll() {
        User user = userService.getMe();
        LocalDateTime readAt = LocalDateTime.now(clock.withZone(user.timezone()));

        int updated = notificationRepository.markAllRead(user.id(), readAt);
        return new ReadAllNotificationsResponse(updated, notificationRepository.countUnread(user.id()));
    }

    /**
     * backend-design 13.1-5. 카테고리를 완료하면 그 카테고리를 챙기라던 알림은 역할이 끝난다.
     * 남겨두면 알림함에 이미 한 일이 계속 떠 있고, 13.1-3 때문에 다음 알림도 만들어지지 않는다.
     */
    @Transactional
    public int markCategoryNotificationsRead(UUID userId, UUID categoryId, LocalDateTime readAt) {
        return notificationRepository.markReadByCategory(userId, categoryId, readAt);
    }

    /**
     * backend-design 13.1. 챙길 시점이 된 카테고리마다 알림을 만든다.
     * 같은 카테고리에 읽지 않은 알림이 남아 있으면 건너뛰어, 미룰수록 알림이 쌓이지 않게 한다.
     */
    @Transactional
    public int generateDueNotifications() {
        int created = 0;
        for (User user : userRepository.findAll()) {
            created += generateDueNotificationsFor(user);
        }
        log.info("due_category_notifications_generated count={}", created);
        return created;
    }

    private int generateDueNotificationsFor(User user) {
        ZoneId timezone = user.timezone();
        LocalDateTime now = LocalDateTime.now(clock.withZone(timezone));

        int created = 0;
        for (CleaningCategory category : categoryRepository.findActiveByUserId(user.id())) {
            CategorySchedule schedule = statusService.scheduleOf(category.lastDoneAt(), category.cycleDays(), timezone);
            if (schedule.code() != CategorySchedule.StatusCode.DUE) {
                continue;
            }
            if (notificationRepository.existsUnreadByCategory(user.id(), category.id())) {
                continue;
            }
            notificationRepository.insert(new Notification(
                    UUID.randomUUID(),
                    user.id(),
                    category.id(),
                    category.name(),
                    NotificationMessages.dueTitle(category.name()),
                    NotificationMessages.dueBody(category.name()),
                    NotificationMessages.categoryDeepLink(category.id()),
                    false,
                    now,
                    null));
            created++;
        }
        return created;
    }

    private NotificationReadResponse toReadResponse(UUID id, LocalDateTime readAt, ZoneId timezone) {
        return new NotificationReadResponse(
                id.toString(), true, readAt != null ? readAt.atZone(timezone).toOffsetDateTime() : null);
    }
}
