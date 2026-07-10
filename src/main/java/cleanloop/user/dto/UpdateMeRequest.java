package cleanloop.user.dto;

import jakarta.validation.constraints.Size;

/**
 * 부분 수정 요청. 생략한(null) 필드는 기존 값을 유지한다.
 */
public record UpdateMeRequest(
        @Size(min = 1, max = 40, message = "name은 1~40자여야 합니다.")
        String name,

        @Size(min = 1, max = 4, message = "avatarText는 1~4자여야 합니다.")
        String avatarText
) {
}
