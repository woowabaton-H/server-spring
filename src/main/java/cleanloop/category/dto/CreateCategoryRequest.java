package cleanloop.category.dto;

import jakarta.validation.constraints.Size;

/**
 * 프리셋 기반 생성이거나 직접 입력 생성이다.
 * presetKey가 있으면 프리셋 값을 그대로 쓰고, 없으면 name/icon/cycleDays가 필요하다.
 */
public record CreateCategoryRequest(
        @Size(min = 1, max = 50) String presetKey,
        @Size(min = 1, max = 40) String name,
        @Size(min = 1, max = 40) String icon,
        Integer cycleDays,
        @Size(max = 500) String note
) {

    public boolean isPresetBased() {
        return presetKey != null;
    }
}
