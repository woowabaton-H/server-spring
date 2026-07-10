package cleanloop.category.dto;

import jakarta.validation.constraints.Size;

/**
 * 부분 수정 요청. 생략한(null) 필드는 기존 값을 유지한다.
 */
public record UpdateCategoryRequest(
        @Size(min = 1, max = 40) String name,
        Integer cycleDays,
        @Size(max = 500) String note
) {
}
