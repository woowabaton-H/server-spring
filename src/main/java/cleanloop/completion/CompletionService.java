package cleanloop.completion;

import cleanloop.category.CategoryRepository;
import cleanloop.category.CategorySchedule;
import cleanloop.category.CategoryStatusService;
import cleanloop.category.CleaningCategory;
import cleanloop.category.dto.CategoryResponse;
import cleanloop.common.error.ApiException;
import cleanloop.common.error.ErrorCode;
import cleanloop.common.page.CursorCodec;
import cleanloop.completion.dto.CompleteCategoryRequest;
import cleanloop.completion.dto.CompleteCategoryResponse;
import cleanloop.completion.dto.CompletionLogResponse;
import cleanloop.notification.NotificationService;
import cleanloop.user.User;
import cleanloop.user.UserService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompletionService {

    private static final DateTimeFormatter TOAST_DATE = DateTimeFormatter.ofPattern("M월 d일");

    private final CompletionLogRepository completionLogRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryStatusService statusService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final Clock clock;

    public CompletionService(CompletionLogRepository completionLogRepository,
                             CategoryRepository categoryRepository,
                             CategoryStatusService statusService,
                             UserService userService,
                             NotificationService notificationService,
                             Clock clock) {
        this.completionLogRepository = completionLogRepository;
        this.categoryRepository = categoryRepository;
        this.statusService = statusService;
        this.userService = userService;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    /**
     * 완료 기록 생성과 lastDoneAt 갱신을 한 트랜잭션으로 묶는다.
     * 기록만 남고 카테고리 날짜가 그대로인 상태를 만들지 않기 위해서다.
     * 같은 날 같은 카테고리를 여러 번 완료하는 것은 MVP에서 허용한다.
     */
    @Transactional
    public CompleteCategoryResponse complete(UUID categoryId, CompleteCategoryRequest request) {
        User user = userService.getMe();
        CleaningCategory category = categoryRepository.findActiveByIdAndUserIdForUpdate(categoryId, user.id())
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND));

        LocalDateTime completedAt = resolveCompletedAt(request, user.timezone());
        CompletionLog log = new CompletionLog(
                UUID.randomUUID(), user.id(), category.id(), category.name(), completedAt, completedAt);
        completionLogRepository.insert(log);
        categoryRepository.updateLastDoneAt(category.id(), completedAt);
        // backend-design 13.1-5. 방금 한 일을 챙기라고 남아 있는 알림은 여기서 정리한다.
        // 읽은 시각은 completedAt이 아니라 지금이다. 지난 날짜로 소급 완료해도 알림은 방금 확인한 것이다.
        notificationService.markCategoryNotificationsRead(
                user.id(), category.id(), LocalDateTime.now(clock.withZone(user.timezone())));

        CleaningCategory updated = categoryRepository.findActiveByIdAndUserId(category.id(), user.id())
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND));
        CategorySchedule schedule =
                statusService.scheduleOf(updated.lastDoneAt(), updated.cycleDays(), user.timezone());

        return new CompleteCategoryResponse(
                CategoryResponse.of(updated, schedule, user.timezone()),
                CompletionLogResponse.of(log, user.timezone()),
                toastMessage(updated.name(), schedule));
    }

    @Transactional(readOnly = true)
    public Page findLogs(LocalDate from, LocalDate to, String cursor, int limit) {
        User user = userService.getMe();
        ZoneId timezone = user.timezone();

        List<CompletionLog> logs = completionLogRepository.findPage(
                user.id(),
                from != null ? from.atStartOfDay() : null,
                // to는 그 날 하루를 포함해야 하므로 다음 날 0시 미만으로 비교한다
                to != null ? to.plusDays(1).atStartOfDay() : null,
                decodeCursor(cursor),
                limit);

        boolean hasNext = logs.size() > limit;
        if (hasNext) {
            logs = logs.subList(0, limit);
        }
        String nextCursor = hasNext ? encodeCursor(logs.get(logs.size() - 1)) : null;

        return new Page(logs.stream().map(log -> CompletionLogResponse.of(log, timezone)).toList(), nextCursor);
    }

    private LocalDateTime resolveCompletedAt(CompleteCategoryRequest request, ZoneId timezone) {
        if (request == null || request.completedAt() == null) {
            return LocalDateTime.now(clock.withZone(timezone));
        }
        // 클라이언트가 보낸 오프셋을 사용자 타임존으로 환산해 저장한다
        return request.completedAt().atZoneSameInstant(timezone).toLocalDateTime();
    }

    private String toastMessage(String categoryName, CategorySchedule schedule) {
        return "%s 완료. 다음 관리는 %s에 보면 충분해요."
                .formatted(categoryName, schedule.nextDueAt().format(TOAST_DATE));
    }

    private String encodeCursor(CompletionLog log) {
        return CursorCodec.encode(log.completedAt() + "|" + log.id());
    }

    private CompletionLogRepository.Cursor decodeCursor(String cursor) {
        if (cursor == null) {
            return null;
        }
        String[] parts = CursorCodec.decode(cursor).split("\\|", 2);
        if (parts.length != 2) {
            throw new ApiException(ErrorCode.INVALID_CURSOR);
        }
        try {
            return new CompletionLogRepository.Cursor(LocalDateTime.parse(parts[0]), UUID.fromString(parts[1]));
        } catch (java.time.format.DateTimeParseException | IllegalArgumentException e) {
            throw new ApiException(ErrorCode.INVALID_CURSOR);
        }
    }

    public record Page(List<CompletionLogResponse> logs, String nextCursor) {
    }
}
