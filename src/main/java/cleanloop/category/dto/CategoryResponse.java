package cleanloop.category.dto;

import cleanloop.category.CategorySchedule;
import cleanloop.category.CleaningCategory;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public record CategoryResponse(
        String id,
        String name,
        String icon,
        int cycleDays,
        OffsetDateTime lastDoneAt,
        OffsetDateTime nextDueAt,
        String note,
        Status status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static CategoryResponse of(CleaningCategory category, CategorySchedule schedule, ZoneId timezone) {
        return new CategoryResponse(
                category.id().toString(),
                category.name(),
                category.icon(),
                category.cycleDays(),
                atZone(category.lastDoneAt(), timezone),
                schedule.nextDueAt(),
                category.note(),
                new Status(schedule.code().code(), schedule.label(), schedule.daysUntilNext()),
                atZone(category.createdAt(), timezone),
                atZone(category.updatedAt(), timezone)
        );
    }

    private static OffsetDateTime atZone(java.time.LocalDateTime value, ZoneId timezone) {
        return value.atZone(timezone).toOffsetDateTime();
    }

    public record Status(String code, String label, int daysUntilNext) {
    }
}
