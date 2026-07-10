package cleanloop.summary;

import cleanloop.category.CategoryRepository;
import cleanloop.category.CategorySchedule;
import cleanloop.category.CategorySchedule.StatusCode;
import cleanloop.category.CategoryStatusService;
import cleanloop.category.CleaningCategory;
import cleanloop.category.dto.CategoryResponse;
import cleanloop.completion.CompletionLogRepository;
import cleanloop.completion.dto.CompletionLogResponse;
import cleanloop.notification.NotificationService;
import cleanloop.selection.SelectionService;
import cleanloop.summary.dto.HomeResponse;
import cleanloop.summary.dto.MeSummaryResponse;
import cleanloop.user.User;
import cleanloop.user.UserService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 별도 집계 테이블 없이 users, cleaning_categories, completion_logs를 조합한다.
 * MVP 데이터 규모에서는 요청 시점 실시간 계산으로 충분하다.
 */
@Service
public class SummaryService {

    private static final int RECENT_LOG_LIMIT = 5;
    private static final int FOOTPRINT_WEEKS = 12;

    private final CategoryRepository categoryRepository;
    private final CompletionLogRepository completionLogRepository;
    private final CategoryStatusService statusService;
    private final UserService userService;
    private final SelectionService selectionService;
    private final NotificationService notificationService;

    public SummaryService(CategoryRepository categoryRepository,
                          CompletionLogRepository completionLogRepository,
                          CategoryStatusService statusService,
                          UserService userService,
                          SelectionService selectionService,
                          NotificationService notificationService) {
        this.categoryRepository = categoryRepository;
        this.completionLogRepository = completionLogRepository;
        this.statusService = statusService;
        this.userService = userService;
        this.selectionService = selectionService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public HomeResponse home(LocalDate today) {
        User user = userService.getMe();
        ZoneId timezone = user.timezone();

        // today를 주면 "지금"의 날짜만 옮긴다. 시각은 유지해야 today 생략 시와 결과가 일치한다.
        ZonedDateTime now = statusService.now(timezone);
        ZonedDateTime reference = today != null ? now.with(today) : now;
        LocalDate referenceDate = reference.toLocalDate();

        List<CleaningCategory> categories = categoryRepository.findActiveByUserId(user.id());
        List<CategorySchedule> schedules = categories.stream()
                .map(category -> statusService.scheduleOf(
                        category.lastDoneAt(), category.cycleDays(), timezone, reference))
                .toList();

        List<CategoryResponse> categoryResponses = new ArrayList<>();
        for (int i = 0; i < categories.size(); i++) {
            categoryResponses.add(CategoryResponse.of(categories.get(i), schedules.get(i), timezone));
        }

        return new HomeResponse(
                referenceDate,
                messageOf(schedules),
                monthlyCompletionCount(user.id(), referenceDate),
                notificationService.countUnread(user.id()),
                categoryResponses,
                recentLogs(user.id(), timezone));
    }

    @Transactional(readOnly = true)
    public MeSummaryResponse meSummary() {
        User user = userService.getMe();
        ZoneId timezone = user.timezone();
        LocalDate today = statusService.now(timezone).toLocalDate();

        MeSummaryResponse.Stats stats = new MeSummaryResponse.Stats(
                monthlyCompletionCount(user.id(), today),
                categoryRepository.countActiveByUserId(user.id()),
                selectionService.countSaved(user.id()));

        return new MeSummaryResponse(
                MeSummaryResponse.Profile.from(user),
                stats,
                weeklyFootprints(user.id(), today),
                recentLogs(user.id(), timezone),
                selectionService.findSaved());
    }

    private List<CompletionLogResponse> recentLogs(UUID userId, ZoneId timezone) {
        return completionLogRepository.findRecent(userId, RECENT_LOG_LIMIT).stream()
                .map(log -> CompletionLogResponse.of(log, timezone))
                .toList();
    }

    /** 사용자 타임존 기준 이번 달 완료 기록 수. */
    private int monthlyCompletionCount(UUID userId, LocalDate today) {
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        return completionLogRepository.countBetween(userId, monthStart, monthStart.plusMonths(1));
    }

    /**
     * 최근 12주를 주 시작일(월요일) 기준으로 집계한다.
     * 완료 기록이 없는 주도 0건으로 채워 항상 12개 구간을 반환한다.
     */
    private List<WeeklyFootprint> weeklyFootprints(UUID userId, LocalDate today) {
        LocalDate currentWeekStart = weekStartOf(today);
        LocalDate windowStart = currentWeekStart.minusWeeks(FOOTPRINT_WEEKS - 1L);

        Map<LocalDate, Long> countsByWeek = completionLogRepository
                .findCompletedAtBetween(userId, windowStart.atStartOfDay(),
                        currentWeekStart.plusWeeks(1).atStartOfDay())
                .stream()
                .collect(Collectors.groupingBy(
                        completedAt -> weekStartOf(completedAt.toLocalDate()), Collectors.counting()));

        List<WeeklyFootprint> footprints = new ArrayList<>(FOOTPRINT_WEEKS);
        for (int week = 0; week < FOOTPRINT_WEEKS; week++) {
            LocalDate weekStart = windowStart.plusWeeks(week);
            footprints.add(WeeklyFootprint.of(weekStart, countsByWeek.getOrDefault(weekStart, 0L).intValue()));
        }
        return footprints;
    }

    private LocalDate weekStartOf(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /**
     * 알림과 마찬가지로 압박형 표현을 피하고 제안형으로 쓴다.
     */
    private String messageOf(List<CategorySchedule> schedules) {
        boolean hasDue = schedules.stream().anyMatch(schedule -> schedule.code() == StatusCode.DUE);
        if (hasDue) {
            return "오늘 청소부터 챙겨요.";
        }
        boolean hasSoon = schedules.stream().anyMatch(schedule -> schedule.code() == StatusCode.SOON);
        if (hasSoon) {
            return "이번 주에 챙기면 좋은 청소가 있어요.";
        }
        return "지금은 여유로워요. 이대로 유지해도 충분해요.";
    }
}
