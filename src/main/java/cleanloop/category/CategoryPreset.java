package cleanloop.category;

/**
 * 추천 카테고리 프리셋. DB 컬럼명은 preset_key지만 API 노출 필드는 key다.
 */
public record CategoryPreset(
        String key,
        String name,
        String icon,
        int cycleDays,
        String note,
        int sortOrder,
        boolean isDefault,
        boolean active
) {
}
