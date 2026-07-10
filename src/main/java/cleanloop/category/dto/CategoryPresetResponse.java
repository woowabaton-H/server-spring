package cleanloop.category.dto;

import cleanloop.category.CategoryPreset;

public record CategoryPresetResponse(
        String key,
        String name,
        String icon,
        int cycleDays,
        String note
) {

    public static CategoryPresetResponse from(CategoryPreset preset) {
        return new CategoryPresetResponse(
                preset.key(),
                preset.name(),
                preset.icon(),
                preset.cycleDays(),
                preset.note()
        );
    }
}
