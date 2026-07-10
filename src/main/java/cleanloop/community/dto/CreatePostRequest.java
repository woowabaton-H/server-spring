package cleanloop.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
        @NotBlank(message = "type은 필수입니다.")
        String type,

        @NotBlank(message = "title은 필수입니다.")
        @Size(max = 160, message = "title은 160자 이하여야 합니다.")
        String title,

        @Size(max = 40, message = "tag는 40자 이하여야 합니다.")
        String tag,

        @NotBlank(message = "body는 필수입니다.")
        @Size(max = 2000, message = "body는 2000자 이하여야 합니다.")
        String body
) {
}
